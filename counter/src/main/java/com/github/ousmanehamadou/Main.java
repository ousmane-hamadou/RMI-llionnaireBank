package com.github.ousmanehamadou;

import com.github.ousmanehamadou.shared.MoneyOrder;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import picocli.CommandLine;

public class Main {
  static final CounterConfig config = new CounterConfig();
  static final CommandLine cmd = new CommandLine(config);

  public static void main(String[] args) {
    int exitCode = cmd.execute(args);
    if (exitCode != 0) {
      System.exit(-1);
    }

    try {
      var registry = LocateRegistry.getRegistry(config.getServerIp(), config.getPort());
      var moneyOrderServiceSub = (MoneyOrder) registry.lookup(config.getServiceName());
      TUI.clearScreen();
      System.out.flush();
      TUI.run(moneyOrderServiceSub);

      registry.unbind(config.getServiceName());

      UnicastRemoteObject.unexportObject(moneyOrderServiceSub, true);
      System.out.println(
          "[SHUTDOWN] Server " + config.getServiceName() + " disconnected successfully.");
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
