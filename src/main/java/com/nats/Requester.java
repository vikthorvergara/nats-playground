package com.nats;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Nats;

import java.time.Duration;

public class Requester {
    public static void main(String[] args) throws Exception {
        String natsUrl = System.getenv().getOrDefault("NATS_URL", "nats://localhost:4222");

        try (Connection nc = Nats.connect(natsUrl)) {
            String subject = "orders.get";

            for (int i = 1; i <= 5; i++) {
                String request = "ORDER-" + i;
                System.out.println("Sending request: " + request);

                Message reply = nc.request(subject, request.getBytes(), Duration.ofSeconds(5));

                if (reply != null) {
                    String response = new String(reply.getData());
                    System.out.println("Got response: " + response);
                } else {
                    System.out.println("No response received");
                }

                Thread.sleep(1000);
            }
        }
    }
}
