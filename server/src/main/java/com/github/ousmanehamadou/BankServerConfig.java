package com.github.ousmanehamadou;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Log4j2
@Getter
@Command(
    name = "RMI-llionaireBank-Server",
    mixinStandardHelpOptions = true,
    version = "1.0",
    description = "Server-side configuration for the RMI-llionaireBank transaction system.")
public class BankServerConfig implements Runnable {

  private final List<Node> remotePeers = new ArrayList<>();

  @Option(
      names = {"-i", "--ip"},
      description =
          "The network IP address or hostname for server binding. Default: ${DEFAULT-VALUE}",
      defaultValue = "127.0.0.1")
  private String ipAddress;

  @Option(
      names = {"-p", "--port"},
      description =
          "The port number on which the RMI registry is listening. Default: ${DEFAULT-VALUE}",
      defaultValue = "1099")
  private int port;

  @Option(
      names = {"-n", "--name"},
      description =
          "The unique remote service name used to bind the bank object in the RMI registry.",
      required = true)
  private String serverName;

  @Option(
      names = {"-r", "--remote-peers"},
      description = "Nodes in 'IP:PORT:NAME' or 'PORT:NAME' format. Default IP: 127.0.0.1.",
      split = ",")
  public void setServers(List<String> servers) {
    for (String node : servers) {
      String[] parts = node.split(":");

      if (parts.length == 3) {
        remotePeers.add(new Node(parts[0], Integer.parseInt(parts[1]), parts[2]));
      } else if (parts.length == 2) {
        remotePeers.add(new Node("127.0.0.1", Integer.parseInt(parts[0]), parts[1]));
      } else {
        log.error("invalid format for peers: (Use IP:PORT:NAME or PORT:NAME)");
        System.exit(-1);
      }
    }
  }

  @Override
  public void run() {}
}
