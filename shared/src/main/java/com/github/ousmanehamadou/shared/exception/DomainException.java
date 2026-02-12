package com.github.ousmanehamadou.shared.exception;

public sealed interface DomainException
    permits DomainException.OrderAlreadyCashed,
        DomainException.OrderNotFoundException,
        DomainException.ServerIsNotReadyException {
  final class OrderNotFoundException extends RuntimeException implements DomainException {
    public final int status = 404;
    public final String msg;

    public OrderNotFoundException(int id) {
      this.msg = "Order with id %d not exist".formatted(id);
    }
  }

  final class ServerIsNotReadyException extends RuntimeException implements DomainException {}

  final class OrderAlreadyCashed extends RuntimeException implements DomainException {}
}
