package com.github.ousmanehamadou;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IDGenerator extends Remote {
  int getNextId() throws RemoteException;

  IDGenerator whoHasToken(IDGenerator asker) throws RemoteException;

  Challenge challenge() throws RemoteException;

  int latchToken() throws RemoteException;

  void askOtherChallenge() throws RemoteException;

  String getName() throws RemoteException;
}
