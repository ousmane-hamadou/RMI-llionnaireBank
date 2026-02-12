package com.github.ousmanehamadou;

import com.github.ousmanehamadou.shared.*;
import com.github.ousmanehamadou.shared.exception.DomainException.*;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.Subtask;

public class MoneyOrderImpl implements MoneyOrder {
  private final ActivityLog activityLog;
  private final DistributedBankNode nodes;
  private final String name;
  private final ConcurrentHashMap<Order, MoneyOrder> ordersForPrecessExternal =
      new ConcurrentHashMap<>();
  private final ConcurrentSkipListSet<Integer> orderStillBeingResearched =
      new ConcurrentSkipListSet<>();

  MoneyOrderImpl(String name, ActivityLog activityLog, DistributedBankNode nodes)
      throws RemoteException {
    super();
    this.name = name;
    this.activityLog = activityLog;
    this.nodes = nodes;
  }


  private Optional<Pair> findOnExternalNodes(int ref) {
    if (linkNodes().isEmpty()) return Optional.empty();

    if (orderStillBeingResearched.contains(ref)) {
      orderStillBeingResearched.remove(ref);

      return Optional.empty();
    }
    orderStillBeingResearched.add(ref);
    try {
      try (var scope = StructuredTaskScope.open(Joiner.anySuccessfulResultOrThrow())) {
        System.out.println("*********************");

        var subTasks = new ArrayList<Subtask<Order>>();
        var nodes = new ArrayList<MoneyOrder>();

        for (var rm : linkNodes()) {
          subTasks.add(scope.fork(() -> rm.tracking(ref)));
          nodes.add(rm);

          scope.join();

          for (var sb : subTasks) {
            int i = subTasks.indexOf(sb);
            Order resp = sb.get();
            System.out.println(resp);

            if (!Objects.isNull(resp)) {
              return Optional.of(new Pair(resp, nodes.get(i)));
            }
          }
        }
      } catch (OrderNotFoundException e) {
        System.out.println("order not found");
        return Optional.empty();
      } catch (InterruptedException e) {
        return Optional.empty();
      } finally {
        orderStillBeingResearched.remove(ref);
      }
    } catch (Exception e) {
      return Optional.empty();
    }
    return Optional.empty();
  }

  private Optional<Pair> findOrderByRef(int ref) {
    System.out.println("findOrderByRef(" + ref + ")");

    return activityLog.orders().stream()
        .filter(order -> order.ref() == ref)
        .map(o -> new Pair(o, this))
        .map(
            o -> {
              System.out.println(o);
              return o;
            })
        .findAny()
        .or(() -> findOnExternalNodes(ref));
  }

  @Override
  public Order updateOrderStatus(Order order, Status status)
      throws IllegalArgumentException, RemoteException {
    //    throwOnNotReady();
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
        Status status =
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
    //    throwOnNotReady();

    var status =
        new Status.AwaitingPayout("Money order for %s — Status: Pending payment".formatted(to));
    var order = new Order(new Random().nextInt(10), from, to, status, amount);
    activityLog.orders().add(order);
    return order;
  }

  @Override
  public CashedStatus cashing(int ref) throws RemoteException, OrderNotFoundException {
    //    throwOnNotReady();

    System.out.println("catching(" + ref + ")");

    var req = findOrderByRef(ref).orElseThrow(() -> new OrderNotFoundException(ref));
    System.out.println(req);

    if (req.second().getRemoteName().equals(name)) return cashOrder(req.first());

    ordersForPrecessExternal.put(req.first(), req.second());

    return cashOrder(req.first());
  }

  @Override
  public Status cancelling(int ref) throws RemoteException {
    //    throwOnNotReady();
    var req = findOrderByRef(ref).orElseThrow(() -> new OrderNotFoundException(ref));

    if (req.second().getRemoteName().equals(name)) return cancelOrder(req.first());

    ordersForPrecessExternal.put(req.first(), req.second());
    return cancelOrder(req.first());
  }

  @Override
  public Order tracking(int ref) {
    //    throwOnNotReady();

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
