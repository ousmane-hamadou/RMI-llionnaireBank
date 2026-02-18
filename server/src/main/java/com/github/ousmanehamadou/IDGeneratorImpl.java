package com.github.ousmanehamadou;

import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class IDGeneratorImpl implements IDGenerator {
  private final AtomicInteger id = new AtomicInteger(0);
  private final AtomicBoolean isTokenIsForMine = new AtomicBoolean(false);
  private final RemoteNode<IDGenerator> nodes;
  private final long myChallenge;
  private final String name;

  public IDGeneratorImpl(String name, RemoteNode<IDGenerator> nodes)
      throws NoSuchAlgorithmException {
    this.nodes = nodes;
    this.myChallenge = SecureRandom.getInstanceStrong().nextLong();
    this.name = name;
  }

  private List<IDGenerator> linkNodes() {
    return nodes.getPeerServices().values().stream().toList();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int latchToken() throws RemoteException {
    if (isTokenIsForMine.compareAndSet(true, false)) {
      log.info("ID Generation Token successfully RELEASED to peers.");

      return id.get();
    }

    return -1;
  }

  @Override
  public int getNextId() throws RemoteException {
    var curId = -2;
    do {
      if (!isTokenIsForMine.get()) {
        log.warn("Token currently held by another node. Queuing request...");
        var curNode = whoHasToken(this);

        curId = curNode.latchToken();
        if (curId != -1) {
          isTokenIsForMine.set(true);
          id.set(curId);
        }
      }
    } while (curId == -1);

    log.info("ID Generation Token ACQUIRED. Accessing global sequence...");

    return id.getAndIncrement();
  }

  @Override
  public IDGenerator whoHasToken(IDGenerator asker) throws RemoteException {
    if (isTokenIsForMine.get()) return this;

    if (!isTokenIsForMine.get() && !asker.getName().equals(name)) {
      log.info("Token NOT HELD by this node. Request declined.");
      return null;
    }

    var tasks = new ArrayList<CompletableFuture<IDGenerator>>(nodes.getPeerServices().size());
    for (var node : linkNodes()) {
      tasks.add(
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  log.info(
                      "Requesting ID Generation Token from distributed peer {}", node.getName());
                  return node.whoHasToken(asker);
                } catch (RemoteException e) {
                  return null;
                }
              }));
    }
    return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
        .thenApply(
            ignored -> {
              var idGenerator =
                  tasks.stream().map(CompletableFuture::join).filter(Objects::nonNull).findFirst();

              assert idGenerator.orElse(null) != null;
              return idGenerator.orElse(null);
            })
        .join();
  }

  @Override
  public Challenge challenge() throws RemoteException {
    return new Challenge(this.myChallenge, this);
  }

  @Override
  public void askOtherChallenge() {
    var tasks = new ArrayList<CompletableFuture<Challenge>>();
    isTokenIsForMine.set(true);

    for (var node : linkNodes()) {
      tasks.add(
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  return new Challenge(node.challenge().v(), node);
                } catch (RemoteException e) {
                  return new Challenge(Long.MIN_VALUE, node);
                }
              }));
    }

    CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
        .thenApply(
            ignored -> {
              isTokenIsForMine.set(true);
              tasks.stream()
                  .map(CompletableFuture::join)
                  .filter(c -> c.v() > myChallenge)
                  .findFirst()
                  .ifPresent(
                      c -> {
                        isTokenIsForMine.set(false);

                        try {
                          log.info(
                              "Node {} is the FIRST to acquire the Generation Token",
                              c.generator().getName());
                        } catch (RemoteException e) {
                          throw new RuntimeException(e);
                        }
                      });
              return null;
            })
        .join();

    if (isTokenIsForMine.get())
      log.info("FIRST to acquire the Generation Token. Initializing global sequence...");
  }
}
