package com.nats;

import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.Nats;
import io.nats.client.Options;

public class ResilientClient {
    public static void main(String[] args) throws Exception {
        String natsUrl = System.getenv().getOrDefault("NATS_URL", "nats://localhost:4222");

        Options options = new Options.Builder()
                .server(natsUrl)
                .maxReconnects(-1)
                .reconnectWait(java.time.Duration.ofSeconds(1))
                .connectionListener(new ConnectionListener() {
                    public void connectionEvent(Connection conn, Events event) {
                        switch (event) {
                            case CONNECTED:
                                System.out.println("[Event] Connected to NATS");
                                break;
                            case DISCONNECTED:
                                System.out.println("[Event] Disconnected from NATS");
                                break;
                            case RECONNECTED:
                                System.out.println("[Event] Reconnected to NATS");
                                break;
                            case RESUBSCRIBED:
                                System.out.println("[Event] Resubscribed to subjects");
                                break;
                            case CLOSED:
                                System.out.println("[Event] Connection closed");
                                break;
                        }
                    }
                })
                .build();

        try (Connection nc = Nats.connect(options)) {
            System.out.println("Client started. Try stopping and starting NATS to see reconnection:");
            System.out.println("  docker-compose stop");
            System.out.println("  docker-compose start");
            System.out.println("\nWaiting for connection events...\n");

            String subject = "resilience.test";

            for (int i = 1; i <= 60; i++) {
                if (nc.getStatus() == Connection.Status.CONNECTED) {
                    String message = "Message #" + i;
                    nc.publish(subject, message.getBytes());
                    System.out.println("Published: " + message);
                } else {
                    System.out.println("Not connected, buffering message #" + i);
                }
                Thread.sleep(1000);
            }
        }
    }
}
