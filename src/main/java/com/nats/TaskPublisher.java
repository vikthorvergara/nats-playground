package com.nats;

import io.nats.client.Connection;
import io.nats.client.Nats;

public class TaskPublisher {
    public static void main(String[] args) throws Exception {
        String natsUrl = System.getenv().getOrDefault("NATS_URL", "nats://localhost:4222");

        try (Connection nc = Nats.connect(natsUrl)) {
            String subject = "tasks.process";

            for (int i = 1; i <= 20; i++) {
                String task = "Task #" + i;
                nc.publish(subject, task.getBytes());
                System.out.println("Published: " + task);
                Thread.sleep(500);
            }

            nc.flush(java.time.Duration.ofSeconds(5));
        }
    }
}
