package com.github.ousmanehamadou.shared;

import com.github.ousmanehamadou.shared.exception.DomainException.*;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MoneyOrder extends Remote {
  Order issuing(String from, String to, int amount)
      throws RemoteException, ServerIsNotReadyException;

  Status cashing(int ref)
      throws RemoteException, OrderNotFoundException, ServerIsNotReadyException;

  Status cancelling(int ref)
      throws RemoteException, OrderNotFoundException, ServerIsNotReadyException;

  Order tracking(int ref)
      throws RemoteException, OrderNotFoundException, ServerIsNotReadyException;
}
