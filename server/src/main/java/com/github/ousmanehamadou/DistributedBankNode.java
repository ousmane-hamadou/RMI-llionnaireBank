package com.github.ousmanehamadou;

import com.github.ousmanehamadou.shared.MoneyOrder;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.concurrent.*;

public class DistributedBankNode {
  private final ConcurrentHashMap<String, MoneyOrder> peerServices = new ConcurrentHashMap<>();

  public Map<String, MoneyOrder> getPeerServices() {
    return peerServices;
  }

  public void joinGroup(BankServerConfig config) {
    int numberOfPeers = config.getRemotePeers().size();
    if (numberOfPeers == 0) return;

    CountDownLatch latch = new CountDownLatch(numberOfPeers);
    try (ExecutorService executor = Executors.newFixedThreadPool(numberOfPeers)) {

      System.out.println("--- Joining Distributed Group (Waiting for all peers) ---");
      System.out.flush();
      for (var node : config.getRemotePeers()) {

        executor.submit(
            () -> {
              boolean connected = false;
              double delay = 0.5;
              double increment = 0.07;

              while (!connected) {
                try {
                  Registry registry = LocateRegistry.getRegistry(node.ip(), node.port());
                  MoneyOrder service = (MoneyOrder) registry.lookup(node.name());

                  peerServices.put(node.name(), service);
                  connected = true;

                  // On décrémente le verrou (latch)
                  latch.countDown();
                  System.out.println(
                      "[JOINED] Connected to "
                          + node.name()
                          + " ("
                          + latch.getCount()
                          + " remaining)");

                  System.out.printf(
                      "[JOINED] Connected to %s on port %d%n", node.name(), node.port());

                } catch (Exception e) {
                  try {
                    Thread.sleep((long) (delay * 1000));
                    delay += increment;
                  } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                  }
                }
              }
            });
      }

      try {
        boolean allConnected = latch.await(2, TimeUnit.MINUTES);

        if (allConnected) {
          System.out.println("=== ALL PEERS CONNECTED. Server is now ACTIVE ===");
        } else {
          System.err.println("=== WARNING: Some peers are still missing after timeout ===");
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } finally {
        executor.shutdown();
      }
    }
  }
}
