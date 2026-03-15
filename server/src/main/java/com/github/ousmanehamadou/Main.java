package com.github.ousmanehamadou;

import com.github.ousmanehamadou.shared.MoneyOrder;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;

@Log4j2
public class Main {
  static final BankServerConfig config = new BankServerConfig();
  static final CommandLine cmd = new CommandLine(config);
  static final RemoteNode<MoneyOrder> MONEY_ORDER_REMOTE_NODE = new RemoteNode<>();
  static final RemoteNode<IDGenerator> ID_GENERATOR_REMOTE_NODE = new RemoteNode<>();

  public static void main(String[] args) {
    if (cmd.execute(args) != 0) {
      System.exit(-1);
    }

    log.info("Starting Distributed Node: {}", config.getServerName());
    log.info("Local Endpoint: {}:{}", config.getIpAddress(), config.getPort());

    try {
      System.setProperty("java.rmi.server.hostname", config.getIpAddress());
      String idGenName = config.getServerName() + "idGenerator";
      var idGenerator = new IDGeneratorImpl(idGenName, ID_GENERATOR_REMOTE_NODE);
      var idGeneratorRemote = (IDGenerator) UnicastRemoteObject.exportObject(idGenerator, 0);

      var moneyOrder =
          new MoneyOrderImpl(config.getServerName(), MONEY_ORDER_REMOTE_NODE, idGenerator);
      var moneyOrderRemote = (MoneyOrder) UnicastRemoteObject.exportObject(moneyOrder, 0);

      var registry = LocateRegistry.createRegistry(config.getPort());
      registry.rebind(idGenName, idGeneratorRemote);
      registry.rebind(config.getServerName(), moneyOrderRemote);
      if (!config.getRemotePeers().isEmpty()) {
        log.info(
            "Connecting to Remote Peers: {}",
            config.getRemotePeers().stream().map(Node::name).toList());

        var joinedNodeCount =
            ID_GENERATOR_REMOTE_NODE.joinGroup(
                config.getRemotePeers().stream()
                    .map(n -> n.toBuilder().name(n.name() + "idGenerator").build())
                    .toList(),
                "id-generator",
                IDGenerator.class);

        idGenerator.askOtherChallenge();

        joinedNodeCount +=
            MONEY_ORDER_REMOTE_NODE.joinGroup(
                config.getRemotePeers(), "money-order", MoneyOrder.class);

        if (joinedNodeCount / 2 < config.getRemotePeers().size()) {
          log.warn("some peers are still missing after timeout");
        } else {
          log.info("ALL PEERS CONNECTED. Server is now ACTIVE");
        }
      } else {
        log.info("Standalone mode: No remote peers specified.");
      }
    } catch (Exception e) {
      log.error("unable to start server {}", e.getMessage());
      System.exit(-1);
    }
  }
}
