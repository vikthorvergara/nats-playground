package com.nats;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Options;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ClusterClient {
    public static void main(String[] args) throws Exception {
        String[] servers = {
            "nats://localhost:4222",
            "nats://localhost:4223",
            "nats://localhost:4224"
        };

        Options options = new Options.Builder()
                .servers(servers)
                .maxReconnects(-1)
                .build();

        try (Connection nc = Nats.connect(options)) {
            System.out.println("Connected to NATS cluster");
            System.out.println("Current server: " + nc.getConnectedUrl());

            String subject = "cluster.test";
            CountDownLatch latch = new CountDownLatch(5);

            Dispatcher dispatcher = nc.createDispatcher((msg) -> {
                String message = new String(msg.getData());
                System.out.println("Received: " + message + " (from " + nc.getConnectedUrl() + ")");
                latch.countDown();
            });
            dispatcher.subscribe(subject);

            for (int i = 1; i <= 5; i++) {
                String message = "Message #" + i;
                nc.publish(subject, message.getBytes());
                System.out.println("Published: " + message + " (to " + nc.getConnectedUrl() + ")");
                Thread.sleep(1000);
            }

            latch.await(10, TimeUnit.SECONDS);

            System.out.println("\nCluster demonstration complete.");
            System.out.println("All messages routed through the cluster.");
            System.out.println("Try stopping one node to see automatic failover:");
            System.out.println("  docker-compose stop nats-1");
        }
    }
}
