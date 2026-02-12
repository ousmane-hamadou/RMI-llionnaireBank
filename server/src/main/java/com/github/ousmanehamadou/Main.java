package com.github.ousmanehamadou;

import com.github.ousmanehamadou.shared.MoneyOrder;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import picocli.CommandLine;

public class Main {
  static final ActivityLog activityLog = new ActivityLog(new ArrayList<>());
  static final BankServerConfig config = new BankServerConfig();
  static final CommandLine cmd = new CommandLine(config);
  static final DistributedBankNode nodes = new DistributedBankNode();

  public static void main(String[] args) {
    if (cmd.execute(args) != 0) {
      System.exit(-1);
    }

    try {
      var localOrder = new MoneyOrderImpl(config.getServerName(), activityLog, nodes);
      var moneyOrderServiceStub = (MoneyOrder) UnicastRemoteObject.exportObject(localOrder, 0);
      var registry = LocateRegistry.createRegistry(config.getPort());

      registry.rebind(config.getServerName(), moneyOrderServiceStub);

      if(config.getRemotePeers().isEmpty()) {
        nodes.joinGroup(config);
      }
    } catch (Exception e) {
      System.out.printf("Unable to start Agent Network 1: %s%n", e.getMessage());
    }
  }
}
