package com.nats;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;

public class Subscriber {
    public static void main(String[] args) throws Exception {
        String natsUrl = System.getenv().getOrDefault("NATS_URL", "nats://localhost:4222");

        try (Connection nc = Nats.connect(natsUrl)) {
            String subject = "orders.created";

            Dispatcher dispatcher = nc.createDispatcher((msg) -> {
                String message = new String(msg.getData());
                System.out.println("Received: " + message);
            });

            dispatcher.subscribe(subject);

            System.out.println("Listening on subject: " + subject);
            Thread.sleep(60000);
        }
    }
}
