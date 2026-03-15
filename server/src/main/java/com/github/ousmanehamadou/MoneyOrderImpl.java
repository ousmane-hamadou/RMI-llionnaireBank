package com.github.ousmanehamadou;

import com.github.ousmanehamadou.shared.*;
import com.github.ousmanehamadou.shared.exception.DomainException.*;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class MoneyOrderImpl implements MoneyOrder {
  private final ActivityLog activityLog;
  private final RemoteNode<MoneyOrder> nodes;
  private final String name;
  private final ConcurrentHashMap<Order, MoneyOrder> ordersForPrecessExternal =
      new ConcurrentHashMap<>();
  private final ConcurrentSkipListSet<Integer> orderStillBeingResearched =
      new ConcurrentSkipListSet<>();
  private final IDGenerator idGenerator;

  MoneyOrderImpl(String name, RemoteNode<MoneyOrder> nodes, IDGenerator idGenerator)
      throws RemoteException {
    super();
    this.name = name;
    this.nodes = nodes;
    this.idGenerator = idGenerator;
    activityLog = new ActivityLog(new ConcurrentSkipListSet<>(Comparator.comparing(Order::ref)));
  }

  private Optional<Pair> findOnExternalNodes(int ref) {
    if (linkNodes().isEmpty()) return Optional.empty();

    if (orderStillBeingResearched.contains(ref)) {
      orderStillBeingResearched.remove(ref);

      return Optional.empty();
    }
    orderStillBeingResearched.add(ref);

    try {
      var tasks = new ArrayList<CompletableFuture<Pair>>();

      for (var rm : linkNodes()) {
        tasks.add(
            CompletableFuture.supplyAsync(
                    () -> {
                      try {
                        return rm.tracking(ref);
                      } catch (RemoteException e) {
                        return null;
                      }
                    })
                .thenApply(order -> new Pair(order, rm))
                .exceptionally(cause -> null));
      }

      var allTasks = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]));

      return allTasks
          .thenApply(
              ignored ->
                  tasks.stream()
                      .map(CompletableFuture::join)
                      .filter(Objects::nonNull)
                      .filter(pair -> Objects.nonNull(pair.first()))
                      .peek(
                          ro -> {
                            try {
                              log.info(
                                  "REF [{}] located on REMOTE NODE [{}]. Initiating distributed payout.",
                                  ref,
                                  ro.second().getRemoteName());
                            } catch (RemoteException e) {
                              throw new RuntimeException(e);
                            }
                          })
                      .findFirst())
          .join();

    } catch (Exception e) {
      return Optional.empty();
    } finally {
      orderStillBeingResearched.remove(ref);
    }
  }

  private Optional<Pair> findOrderByRef(int ref) {
    return activityLog.orders().stream()
        .filter(order -> order.ref() == ref)
        .map(o -> new Pair(o, this))
        .peek(
            order -> {
              try {
                if (order.second().getRemoteName().equals(name)) {
                  log.info("REF [{}] found in LOCAL LEDGER. Processing payout...", ref);
                }
              } catch (RemoteException e) {
                throw new RuntimeException(e);
              }
            })
        .findAny()
        .or(
            () -> {
              log.info("REF [{}] not found locally. Searching distributed nodes...", ref);

              return findOnExternalNodes(ref);
            });
  }

  @Override
  public Order updateOrderStatus(Order order, Status status)
      throws IllegalArgumentException, RemoteException {
    if (ordersForPrecessExternal.containsKey(order)) {
      Order remoteOrder = ordersForPrecessExternal.get(order).updateOrderStatus(order, status);
      ordersForPrecessExternal.remove(order);
      return remoteOrder;
    }

    activityLog.orders().remove(order);

    var updatedOrder = order.toBuilder().status(status).build();

    activityLog.orders().add(updatedOrder);

    return updatedOrder;
  }

  private Status cancelOrder(Order order) throws RemoteException {
    return switch (order.status()) {
      case Status.AwaitingPayout ignored ->
          updateOrderStatus(order, new Status.Cancelled("Refunded to %s".formatted(order.by())))
              .status();
      case Status status -> status;
    };
  }

  private CashedStatus cashOrder(Order order) throws RemoteException {
    return switch (order.status()) {
      case Status.AwaitingPayout ignored -> {
        var status =
            updateOrderStatus(
                    order, new Status.Cashed("Money order paid to %s".formatted(order.to())))
                .status();

        yield new CashedStatus(status, true);
      }
      case Status status -> new CashedStatus(status, false);
    };
  }

  @Override
  public Order issuing(String from, String to, int amount) throws RemoteException {
    log.info(
        "ISSUANCE: Preparing new money order. Sender: [{}], Recipient: [{}], Amount: {} FCFA",
        from,
        to,
        amount);

    var status =
        new Status.AwaitingPayout("Money order for %s — Status: Pending payment".formatted(to));
    var nextId = idGenerator.getNextId();

    var order = new Order(nextId, from, to, status, amount);
    activityLog.orders().add(order);
    log.debug("ISSUANCE: Data validated. Order id: {}", nextId);

    return order;
  }

  @Override
  public CashedStatus cashing(int ref) throws RemoteException, OrderNotFoundException {
    var req = findOrderByRef(ref).orElseThrow(() -> new OrderNotFoundException(ref));

    String remoteName = req.second().getRemoteName();
    if (remoteName.equals(name)) {
      CashedStatus cashedStatus = cashOrder(req.first());
      if (cashedStatus.done()) {
        log.info(
            "CASHING_SUCCESS: Local payout completed for beneficiary [{}]. Status: COLLECTED.",
            req.first().to());
      } else {
        log.info(
            "CASHING_REJECTED: Payout for REF [{}]  refused [{}].", ref, cashedStatus.status());
      }
      return cashedStatus;
    }
    ordersForPrecessExternal.put(req.first(), req.second());

    CashedStatus cashedStatus = cashOrder(req.first());

    if (cashedStatus.done()) {
      log.info(
          "SUCCESS: Distributed payout for REF [{}] confirmed by peer node. {}", ref, remoteName);
    } else {
      log.error(
          "REMOTE_CASHING_REJECTED: Payout for REF [{}] refused by remote node [{}]. (status: {}) ",
          ref,
          remoteName,
          cashedStatus.status());
    }
    return cashedStatus;
  }

  @Override
  public Status cancelling(int ref) throws RemoteException {
    var req = findOrderByRef(ref).orElseThrow(() -> new OrderNotFoundException(ref));

    if (req.second().getRemoteName().equals(name)) return cancelOrder(req.first());

    ordersForPrecessExternal.put(req.first(), req.second());
    return cancelOrder(req.first());
  }

  @Override
  public Order tracking(int ref) {
    return findOrderByRef(ref).map(Pair::first).orElseThrow(() -> new OrderNotFoundException(ref));
  }

  @Override
  public String getRemoteName() {
    return name;
  }

  private List<MoneyOrder> linkNodes() {
    return nodes.getPeerServices().values().stream().toList();
  }
}
