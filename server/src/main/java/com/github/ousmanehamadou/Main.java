package com.github.ousmanehamadou;

import com.github.ousmanehamadou.shared.IDGenerator;
import com.github.ousmanehamadou.shared.MoneyOrder;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import picocli.CommandLine;

public class Main {
  static final BankServerConfig config = new BankServerConfig();
  static final CommandLine cmd = new CommandLine(config);
  static final DistributedBankNode<MoneyOrder> nodes = new DistributedBankNode<>();

  public static void main(String[] args) {
    if (cmd.execute(args) != 0) {
      System.exit(-1);
    }

    try {
      String idGenName = config.getServerName() + "idGenerator";
      var idGenerator = new IDGeneratorImpl(idGenName, new ArrayList<>());
      var idGeneratorRemote = (IDGenerator) UnicastRemoteObject.exportObject(idGenerator, 0);

      var moneyOrder = new MoneyOrderImpl(config.getServerName(), nodes);
      var moneyOrderRemote = (MoneyOrder) UnicastRemoteObject.exportObject(moneyOrder, 0);

      System.setProperty("java.rmi.server.hostname", config.getIpAddress());
      var registry = LocateRegistry.createRegistry(config.getPort());
      registry.rebind(idGenName, idGeneratorRemote);
      registry.rebind(config.getServerName(), moneyOrderRemote);

      if (!config.getRemotePeers().isEmpty())
        nodes.joinGroup(config.getRemotePeers(), MoneyOrder.class);
    } catch (Exception e) {
      System.out.printf("unable to start %s: %s%n", config.getServerName(), e.getMessage());
    }
  }
}
