package com.nats;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Options;

public class SecureSubscriber {
    public static void main(String[] args) throws Exception {
        String natsUrl = System.getenv().getOrDefault("NATS_URL", "nats://localhost:4222");

        Options options = new Options.Builder()
                .server(natsUrl)
                .userInfo("subscriber", "sub123")
                .build();

        try (Connection nc = Nats.connect(options)) {
            System.out.println("Connected as: subscriber");

            String allowedSubject = "secure.data";

            Dispatcher dispatcher = nc.createDispatcher((msg) -> {
                String message = new String(msg.getData());
                System.out.println("Received: " + message);
            });

            System.out.println("\nSubscribing to allowed subject: " + allowedSubject);
            try {
                dispatcher.subscribe(allowedSubject);
                System.out.println("Success: Subscribed to " + allowedSubject);
            } catch (Exception e) {
                System.out.println("Denied: Cannot subscribe to " + allowedSubject);
                System.out.println("Reason: " + e.getMessage());
            }

            System.out.println("\nListening for messages (60 seconds)...");
            Thread.sleep(60000);
        }
    }
}
