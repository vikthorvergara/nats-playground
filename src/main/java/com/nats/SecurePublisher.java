package com.nats;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

public class SecurePublisher {
    public static void main(String[] args) throws Exception {
        String natsUrl = System.getenv().getOrDefault("NATS_URL", "nats://localhost:4222");

        Options options = new Options.Builder()
                .server(natsUrl)
                .userInfo("publisher", "pub123")
                .build();

        try (Connection nc = Nats.connect(options)) {
            System.out.println("Connected as: publisher");

            String allowedSubject = "secure.data";
            String deniedSubject = "admin.data";

            System.out.println("\nPublishing to allowed subject: " + allowedSubject);
            nc.publish(allowedSubject, "Authorized message".getBytes());
            System.out.println("Success: Published to " + allowedSubject);

            System.out.println("\nPublishing to denied subject: " + deniedSubject);
            try {
                nc.publish(deniedSubject, "Unauthorized message".getBytes());
                nc.flush(java.time.Duration.ofSeconds(1));
                System.out.println("Success: Published to " + deniedSubject);
            } catch (Exception e) {
                System.out.println("Denied: Cannot publish to " + deniedSubject);
                System.out.println("Reason: " + e.getMessage());
            }
        }
    }
}
