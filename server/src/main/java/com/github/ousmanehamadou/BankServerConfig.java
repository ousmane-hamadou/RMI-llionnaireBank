package com.github.ousmanehamadou;

import java.util.HashMap;
import java.util.Map;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "RMI-llionaireBank-Server",
    mixinStandardHelpOptions = true,
    version = "1.0",
    description = "Server-side configuration for the RMI-llionaireBank transaction system.")
public class BankServerConfig implements Runnable {

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

  @Option(
      names = {"-n", "--name"},
      description =
          "The unique remote service name used to bind the bank object in the RMI registry.",
      required = true)
  private String serverName;

  @Option(
      names = {"-r", "--remote-peers"},
      split = ",",
      description = "Remote peers in 'name:port' format (e.g., ServerB:1100,ServerC:1101).")
  private Map<String, Integer> remotePeers = new HashMap<>();

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

  // Getters for RMI integration
  public int getPort() {
    return port;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public String getServerName() {
    return serverName;
  }

  public Map<String, Integer> getRemotePeers() {
    return remotePeers;
  }
}
