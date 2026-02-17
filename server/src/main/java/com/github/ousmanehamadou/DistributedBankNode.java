package com.github.ousmanehamadou;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import lombok.Getter;

public class DistributedBankNode<T extends Remote> {
  @Getter private final ConcurrentHashMap<String, T> peerServices = new ConcurrentHashMap<>();

  private CompletionStage<T> remoteConnection(
      Node node, Class<T> clazz, int[] attemptCounts, int index, int maxAttempts) {
    if (attemptCounts[index]++ > maxAttempts) {
      return CompletableFuture.completedStage(null);
    }

    try {
      var registry = LocateRegistry.getRegistry(node.ip(), node.port());
      var service = registry.lookup(node.name());

      peerServices.put(node.name(), clazz.cast(service));
      System.out.printf("[JOINED] Connected to %s on port %d%n", node.name(), node.port());
      System.out.flush();

      return CompletableFuture.completedStage(clazz.cast(service));
    } catch (Exception e) {
      return CompletableFuture.failedStage(e);
    }
  }

  public void joinGroup(List<Node> nodes, Class<T> clazz) {
    int numberOfPeers = nodes.size();
    if (numberOfPeers == 0) return;
    int maxAttempts = 10;

    var retryConfig =
        RetryConfig.custom()
            .retryExceptions(Exception.class)
            .maxAttempts(maxAttempts)
            .failAfterMaxAttempts(true)
            .intervalFunction(IntervalFunction.ofExponentialBackoff(500, 1.5))
            .build();
    var retry = Retry.of("RMI-llionaireBank RMI service", retryConfig);

    try (ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(numberOfPeers)) {
      var tasks = new ArrayList<CompletableFuture<T>>();
      var attemptCounts = new int[numberOfPeers];

      for (int i = 0; i < numberOfPeers; i++) {
        int index = i;
        var task =
            retry
                .executeCompletionStage(
                    scheduler,
                    () ->
                        remoteConnection(
                            nodes.get(index), clazz, attemptCounts, index, maxAttempts))
                .toCompletableFuture();
        tasks.add(task);
      }

      var allTasks = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]));

      allTasks
          .thenApply(
              ignored -> tasks.stream().map(CompletableFuture::join).filter(Objects::nonNull).count())
          .thenAccept(n -> logMsg(n == numberOfPeers))
          .join();
    }
  }

  private void logMsg(boolean allConnected) {
    if (allConnected) System.out.println("=== ALL PEERS CONNECTED. Server is now ACTIVE ===");
    else System.err.println("=== WARNING: Some peers are still missing after timeout ===");
    System.out.flush();
  }
}
