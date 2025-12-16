package com.nats;

import io.nats.client.Connection;
import io.nats.client.Nats;

public class Publisher {
    public static void main(String[] args) throws Exception {
        String natsUrl = System.getenv().getOrDefault("NATS_URL", "nats://localhost:4222");

        try (Connection nc = Nats.connect(natsUrl)) {
            String subject = "orders.created";

            for (int i = 1; i <= 10; i++) {
                String message = "Order #" + i;
                nc.publish(subject, message.getBytes());
                System.out.println("Published: " + message);
                Thread.sleep(1000);
            }

            nc.flush(java.time.Duration.ofSeconds(5));
        }
    }
}
