package com.nats;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;

public class QueueWorker {
    public static void main(String[] args) throws Exception {
        String natsUrl = System.getenv().getOrDefault("NATS_URL", "nats://localhost:4222");
        String workerName = args.length > 0 ? args[0] : "Worker-" + System.currentTimeMillis();

        try (Connection nc = Nats.connect(natsUrl)) {
            String subject = "tasks.process";
            String queueGroup = "task-workers";

            Dispatcher dispatcher = nc.createDispatcher((msg) -> {
                String task = new String(msg.getData());
                System.out.println("[" + workerName + "] Processing: " + task);
            });

            dispatcher.subscribe(subject, queueGroup);

            System.out.println("[" + workerName + "] Listening on subject: " + subject + " (queue: " + queueGroup + ")");
            Thread.sleep(60000);
        }
    }
}
