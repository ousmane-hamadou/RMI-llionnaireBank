package com.github.ousmanehamadou;

import com.github.ousmanehamadou.shared.MoneyOrder;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Objects;
import picocli.CommandLine;

public class Main {
  static final CounterConfig config = new CounterConfig();
  static final CommandLine cmd = new CommandLine(config);

  public static void main(String[] args) {
    int exitCode = cmd.execute(args);
    if (exitCode != 0) {
      System.exit(-1);
    }
    System.out.flush();
    try {
      var registry = LocateRegistry.getRegistry(config.getServerIp(), config.getPort());
      var moneyOrderServiceSub = (MoneyOrder) registry.lookup(config.getServiceName());
      Objects.requireNonNull(moneyOrderServiceSub);
      TUI.clearScreen();
      System.out.flush();
      TUI.run(moneyOrderServiceSub);

      System.out.println(
          "[SHUTDOWN] Disconnected Server "
              + config.getServiceName().toUpperCase()
              + " successfully.");
    } catch (NotBoundException e) {
      System.err.println(
          "The '" + config.getServiceName() + "' service is not bound (registered) on the server.");
    } catch (RemoteException e) {
      System.err.println("Critical error: Unable to connect to the RMI Registry.");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
