package com.nats;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.PushSubscribeOptions;
import io.nats.client.api.ConsumerConfiguration;

import java.time.Duration;

public class JetStreamConsumer {
    public static void main(String[] args) throws Exception {
        String natsUrl = System.getenv().getOrDefault("NATS_URL", "nats://localhost:4222");

        try (Connection nc = Nats.connect(natsUrl)) {
            JetStream js = nc.jetStream();

            ConsumerConfiguration consumerConfig = ConsumerConfiguration.builder()
                    .durable("orders-consumer")
                    .build();

            PushSubscribeOptions subscribeOptions = PushSubscribeOptions.builder()
                    .configuration(consumerConfig)
                    .build();

            JetStreamSubscription sub = js.subscribe("orders.*", subscribeOptions);

            System.out.println("Listening on JetStream stream: ORDERS");

            for (int i = 0; i < 20; i++) {
                Message msg = sub.nextMessage(Duration.ofSeconds(3));
                if (msg != null) {
                    System.out.println("Received: " + new String(msg.getData()));
                    msg.ack();
                } else {
                    System.out.println("No more messages");
                    break;
                }
            }
        }
    }
}
