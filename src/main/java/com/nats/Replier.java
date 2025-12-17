package com.nats;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;

public class Replier {
    public static void main(String[] args) throws Exception {
        String natsUrl = System.getenv().getOrDefault("NATS_URL", "nats://localhost:4222");

        try (Connection nc = Nats.connect(natsUrl)) {
            String subject = "orders.get";

            Dispatcher dispatcher = nc.createDispatcher((msg) -> {
                String request = new String(msg.getData());
                System.out.println("Received request: " + request);

                String response = "Order details for: " + request;
                nc.publish(msg.getReplyTo(), response.getBytes());
                System.out.println("Sent response: " + response);
            });

            dispatcher.subscribe(subject);

            System.out.println("Replier listening on subject: " + subject);
            Thread.sleep(60000);
        }
    }
}
