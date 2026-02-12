package com.github.ousmanehamadou;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "RMI-llionaireBank-Server",
    mixinStandardHelpOptions = true,
    version = "1.0",
    description = "Server-side configuration for the RMI-llionaireBank transaction system.")
public class BankServerConfig implements Runnable {

  // Getters for RMI integration
  @Getter
  @Option(
      names = {"-p", "--port"},
      description =
          "The port number on which the RMI registry is listening. Default: ${DEFAULT-VALUE}",
      defaultValue = "1099")
  private int port;

  @Option(
      names = {"-i", "--ip"},
      description =
          "The network IP address or hostname for server binding. Default: ${DEFAULT-VALUE}",
      defaultValue = "127.0.0.1")
  private String ipAddress;

  @Getter
  @Option(
      names = {"-n", "--name"},
      description =
          "The unique remote service name used to bind the bank object in the RMI registry.",
      required = true)
  private String serverName;

  @Getter private List<Node> remotePeers = new ArrayList<>();

  @Option(
      names = {"-r", "--remote-peers"},
      description = "Nodes in 'IP:PORT:NAME' or 'PORT:NAME' format. Default IP: 127.0.0.1.",
      split = ",")
  public void setServers(List<String> servers) {
    for (String node : servers) {
      String[] parts = node.split(":");
      System.out.println(Arrays.toString(parts));

      if (parts.length == 3) {
        remotePeers.add(new Node(parts[0], Integer.parseInt(parts[1]), parts[2]));
      } else if (parts.length == 2) {
        remotePeers.add(new Node("127.0.0.1", Integer.parseInt(parts[1]), parts[0]));
      } else {
        System.err.println("Invalid format for node: " + node + " (Use IP:PORT:NAME or PORT:NAME)");
      }

      System.out.println(remotePeers);
    }
  }

  @Override
  public void run() {
    System.out.println("--- Starting Distributed Node: " + serverName + " ---");
    System.out.println("Local Endpoint: " + ipAddress + ":" + port);

    if (!remotePeers.isEmpty()) {
      System.out.println("Connecting to Remote Peers: " + remotePeers);
    } else {
      System.out.println("Standalone mode: No remote peers specified.");
    }
  }
}
