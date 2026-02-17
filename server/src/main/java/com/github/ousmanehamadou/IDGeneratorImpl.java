package com.github.ousmanehamadou;

import com.github.ousmanehamadou.shared.IDGenerator;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class IDGeneratorImpl implements IDGenerator {
  private final AtomicInteger id = new AtomicInteger(0);

  private boolean isTokenIsForMine = false;
  private List<IDGenerator> nodes;
  private int myChallenge;
  private String name;

  public IDGeneratorImpl(String name, List<IDGenerator> nodes) throws NoSuchAlgorithmException {
    this.nodes = nodes;
    this.myChallenge = SecureRandom.getInstanceStrong().nextInt();
    this.name = name;
  }

  @Override
  public int getNextId() throws RemoteException {
    if (isTokenIsForMine) {
      return id.getAndIncrement();
    } else {
      return whoHasToken().getNextId();
    }
  }

  @Override
  public IDGenerator whoHasToken() throws RemoteException {

    if (isTokenIsForMine) return this;
    //
    //    try (var scope =
    //        StructuredTaskScope.open(StructuredTaskScope.Joiner.anySuccessfulResultOrThrow())) {
    //      var subTasks = new ArrayList<StructuredTaskScope.Subtask<IDGenerator>>();
    //
    //      for (var node : nodes) {
    //        subTasks.add(scope.fork(() -> node.whoHasToken()));
    //      }
    //
    //      scope.join();
    //
    //      isTokenIsForMine = true;
    //      var idGenerator = subTasks.stream().map(sb -> sb.get()).findFirst();
    //      if (idGenerator.isPresent()) {
    //        return idGenerator.get();
    //      }
    //    } catch (InterruptedException e) {
    //      throw new RuntimeException(e);
    //    }
    return null;
  }

  @Override
  public int challenge() throws RemoteException {
    return myChallenge;
  }

  private void askOtherChallenge() {
    //    try (var scope =
    //        StructuredTaskScope.open(StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow())) {
    //      var subTasks = new ArrayList<StructuredTaskScope.Subtask<Integer>>();
    //
    //      for (var node : nodes) {
    //        subTasks.add(scope.fork(() -> node.challenge()));
    //      }
    //
    //      scope.join();
    //
    //      isTokenIsForMine = true;
    //      for (var sb : subTasks) {
    //        if (sb.get() > myChallenge) {
    //          isTokenIsForMine = false;
    //          return;
    //        }
    //      }
    //    } catch (InterruptedException e) {
    //      throw new RuntimeException(e);
    //    }
  }

  @Override
  public String getName() throws RemoteException {
    return name;
  }
}
