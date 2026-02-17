package com.github.ousmanehamadou.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IDGenerator extends Remote {
  int getNextId() throws RemoteException;

  IDGenerator whoHasToken() throws RemoteException;

  int challenge() throws RemoteException;

  String getName() throws RemoteException;
}
