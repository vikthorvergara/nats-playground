package com.nats;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class PerformanceBenchmark {
    public static void main(String[] args) throws Exception {
        String natsUrl = System.getenv().getOrDefault("NATS_URL", "nats://localhost:4222");
        int messageCount = 100000;
        String subject = "benchmark.test";

        try (Connection nc = Nats.connect(natsUrl)) {
            CountDownLatch latch = new CountDownLatch(messageCount);
            List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

            Dispatcher dispatcher = nc.createDispatcher((msg) -> {
                long sendTime = Long.parseLong(new String(msg.getData()));
                long latency = System.nanoTime() - sendTime;
                latencies.add(latency);
                latch.countDown();
            });
            dispatcher.subscribe(subject);

            System.out.println("Starting benchmark: " + messageCount + " messages");
            long startTime = System.nanoTime();

            for (int i = 0; i < messageCount; i++) {
                String timestamp = String.valueOf(System.nanoTime());
                nc.publish(subject, timestamp.getBytes());
            }

            nc.flush(java.time.Duration.ofSeconds(10));
            latch.await();

            long endTime = System.nanoTime();
            long totalTime = endTime - startTime;

            double throughput = (messageCount / (totalTime / 1_000_000_000.0));
            System.out.println("\nResults:");
            System.out.println("  Total time: " + (totalTime / 1_000_000) + " ms");
            System.out.println("  Throughput: " + String.format("%.2f", throughput) + " msgs/sec");

            Collections.sort(latencies);
            System.out.println("  Latency p50: " + (latencies.get(messageCount / 2) / 1_000) + " μs");
            System.out.println("  Latency p95: " + (latencies.get((int)(messageCount * 0.95)) / 1_000) + " μs");
            System.out.println("  Latency p99: " + (latencies.get((int)(messageCount * 0.99)) / 1_000) + " μs");
        }
    }
}
