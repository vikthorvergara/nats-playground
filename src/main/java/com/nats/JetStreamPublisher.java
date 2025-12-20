package com.nats;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;

public class JetStreamPublisher {
    public static void main(String[] args) throws Exception {
        String natsUrl = System.getenv().getOrDefault("NATS_URL", "nats://localhost:4222");

        try (Connection nc = Nats.connect(natsUrl)) {
            JetStreamManagement jsm = nc.jetStreamManagement();

            try {
                jsm.getStreamInfo("ORDERS");
            } catch (Exception e) {
                StreamConfiguration streamConfig = StreamConfiguration.builder()
                        .name("ORDERS")
                        .subjects("orders.*")
                        .storageType(StorageType.File)
                        .build();
                jsm.addStream(streamConfig);
                System.out.println("Created stream: ORDERS");
            }

            JetStream js = nc.jetStream();

            for (int i = 1; i <= 10; i++) {
                String subject = "orders.new";
                String message = "Order #" + i;
                js.publish(subject, message.getBytes());
                System.out.println("Published to JetStream: " + message);
                Thread.sleep(500);
            }
        }
    }
}
