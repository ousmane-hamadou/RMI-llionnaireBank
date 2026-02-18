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
import lombok.extern.log4j.Log4j2;

@Log4j2
public class RemoteNode<T extends Remote> {
  @Getter private final ConcurrentHashMap<String, T> peerServices = new ConcurrentHashMap<>();

  private CompletionStage<T> remoteConnection(
      Node node,
      Class<T> clazz,
      String serviceName,
      int[] attemptCounts,
      int index,
      int maxAttempts) {
    if (attemptCounts[index]++ > maxAttempts) {
      return CompletableFuture.completedStage(null);
    }

    try {
      var registry = LocateRegistry.getRegistry(node.ip(), node.port());
      var service = registry.lookup(node.name());

      peerServices.put(node.name(), clazz.cast(service));
      log.info("{} - connected to {} on port {}", serviceName, node.name(), node.port());

      return CompletableFuture.completedStage(clazz.cast(service));
    } catch (Exception e) {
      return CompletableFuture.failedStage(e);
    }
  }

  public long joinGroup(List<Node> nodes, String serviceName, Class<T> clazz) {
    int numberOfPeers = nodes.size();
    if (numberOfPeers == 0) return 0;
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
                            nodes.get(index),
                            clazz,
                            serviceName,
                            attemptCounts,
                            index,
                            maxAttempts))
                .toCompletableFuture();
        tasks.add(task);
      }

      var allTasks = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]));

      return allTasks
          .thenApply(
              ignored ->
                  tasks.stream().map(CompletableFuture::join).filter(Objects::nonNull).count())
          .join();
    }
  }
}
