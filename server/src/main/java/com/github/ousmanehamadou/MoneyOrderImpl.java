package com.github.ousmanehamadou;

import com.github.ousmanehamadou.shared.*;
import com.github.ousmanehamadou.shared.exception.DomainException.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.Subtask;

public class MoneyOrderImpl implements MoneyOrder {
  private final ActivityLog activityLog;
  private final DistributedBankNode nodes;
  private final String name;
  private boolean isReady = false;

  MoneyOrderImpl(String name, ActivityLog activityLog, DistributedBankNode nodes)
      throws RemoteException {
    super();
    this.name = name;
    this.activityLog = activityLog;
    this.nodes = nodes;
  }

  private void throwOnNotReady() throws ServerIsNotReadyException {
    if (isReady) return;

    throw new ServerIsNotReadyException();
  }

  private Optional<RemoteOrder> findOnExternalNodes(int ref) {
    try (var scope = StructuredTaskScope.open(Joiner.anySuccessfulResultOrThrow())) {
      var subTasks = new ArrayList<Subtask<RemoteOrder>>();

      for (var rm : linkNodes()) {
        subTasks.add(
            scope.fork(() -> RemoteOrder.builder().order(rm.tracking(ref)).provider(rm).build()));

        return subTasks.stream().map(task -> task.get()).filter(Objects::nonNull).findFirst();
      }
    }
    return Optional.empty();
  }

  private Optional<RemoteOrder> findOrderByRef(int ref) {
    return activityLog.orders().stream()
        .filter(rorder -> rorder.order().ref() == ref)
        .findAny()
        .or(() -> findOnExternalNodes(ref));
  }

  private <T extends Status> RemoteOrder updateOrderStatus(RemoteOrder rorder, Class<T> clazz)
      throws IllegalArgumentException {
    var status =
        switch (clazz) {
          case Class<T> c when c == Status.Cashed.class ->
              new Status.Cashed("Money order paid to %s".formatted(rorder.order().to()));
          case Class<T> c when c == Status.Cancelled.class ->
              new Status.Cancelled("Refunded to %s".formatted(rorder.order().by()));
          default ->
              throw new IllegalArgumentException(
                  "update status to %s not allowed".formatted(clazz.getName()));
        };

    if (!((MoneyOrderImpl)rorder.provider()).remoteName().equals(name)) {
      return ((MoneyOrderImpl) rorder.provider()).updateOrderStatus(rorder, clazz);
    }

    activityLog.orders().remove(rorder);
    var updatedOrder =
        rorder.toBuilder().order(rorder.order().toBuilder().status(status).build()).build();
    activityLog.orders().add(updatedOrder);

    return updatedOrder;
  }

  private Status cancelOrder(RemoteOrder rorder) {
    return switch (rorder.order().status()) {
      case Status.AwaitingPayout ignored ->
          updateOrderStatus(rorder, Status.Cancelled.class).order().status();
      case Status status -> status;
    };
  }

  private Status cashOrder(RemoteOrder rorder) {
    return switch (rorder.order().status()) {
      case Status.AwaitingPayout ignored ->
          updateOrderStatus(rorder, Status.Cashed.class).order().status();
      case Status status -> status;
    };
  }

  public void allowedOrder() {
    isReady = true;
  }

  @Override
  public Order issuing(String from, String to, int amount) throws RemoteException {
    throwOnNotReady();

    var status =
        new Status.AwaitingPayout("Money order for %s — Status: Pending payment".formatted(to));
    var order = new Order(0, from, to, status, amount);
    activityLog.orders().add(RemoteOrder.builder().order(order).provider(this).build());
    return order;
  }

  @Override
  public Status cashing(int ref) throws RemoteException, OrderNotFoundException {
    var order = findOrderByRef(ref).orElseThrow(() -> new OrderNotFoundException(ref));
    return cashOrder(order);
  }

  @Override
  public Status cancelling(int ref) {
    var order = findOrderByRef(ref).orElseThrow(() -> new OrderNotFoundException(ref));
    return cancelOrder(order);
  }

  @Override
  public Order tracking(int ref) {
    return findOrderByRef(ref)
        .map(RemoteOrder::order)
        .orElseThrow(() -> new OrderNotFoundException(ref));
  }

  private String remoteName() {
    return name;
  }

  private List<MoneyOrder> linkNodes() {
    return nodes.getPeerServices().values().stream().toList();
  }
}
