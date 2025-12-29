# NATS Messaging POC

Simple messaging patterns using NATS and Java 25:
- Pub/Sub Pattern
- Request/Reply Pattern
- Queue Groups Pattern
- JetStream Durability
- Performance Benchmark
- Resilience
- Operational Metrics
- Clustering
- Authorization
- TLS Encryption

## Prerequisites

- Java 25
- Maven
- Docker

## Setup

Start NATS server:
```bash
docker compose up -d
```

Compile the project:
```bash
mvn clean compile
```

## Pub/Sub Pattern

Run the subscriber (in one terminal):
```bash
mvn exec:java -Dexec.mainClass="com.nats.Subscriber"
```

Run the publisher (in another terminal):
```bash
mvn exec:java -Dexec.mainClass="com.nats.Publisher"
```

The publisher sends 10 messages to the `orders.created` subject, one per second.
The subscriber listens for 60 seconds and prints received messages.

## Request/Reply Pattern

Run the replier (in one terminal):
```bash
mvn exec:java -Dexec.mainClass="com.nats.Replier"
```

Run the requester (in another terminal):
```bash
mvn exec:java -Dexec.mainClass="com.nats.Requester"
```

The requester sends 5 requests to the `orders.get` subject and waits for responses.
The replier listens for requests and sends back responses.

## Queue Groups Pattern

Run multiple workers (in separate terminals):
```bash
mvn exec:java -Dexec.mainClass="com.nats.QueueWorker" -Dexec.args="Worker-1"
mvn exec:java -Dexec.mainClass="com.nats.QueueWorker" -Dexec.args="Worker-2"
mvn exec:java -Dexec.mainClass="com.nats.QueueWorker" -Dexec.args="Worker-3"
```

Run the task publisher (in another terminal):
```bash
mvn exec:java -Dexec.mainClass="com.nats.TaskPublisher"
```

The publisher sends 20 tasks to the `tasks.process` subject.
The workers share the workload automatically via queue groups, demonstrating load balancing.

## JetStream Durability

Run the publisher to create the stream and publish messages:
```bash
mvn exec:java -Dexec.mainClass="com.nats.JetStreamPublisher"
```

Run the consumer to receive and acknowledge messages:
```bash
mvn exec:java -Dexec.mainClass="com.nats.JetStreamConsumer"
```

The publisher creates a persistent stream called `ORDERS` and publishes 10 messages to `orders.new`.
The consumer uses a durable consumer to receive messages with at-least-once delivery.
Messages are persisted and can be replayed. Acknowledged messages are not redelivered.

## Performance Benchmark

Run the benchmark:
```bash
mvn exec:java -Dexec.mainClass="com.nats.PerformanceBenchmark"
```

Publishes and receives 100,000 messages to measure throughput and latency.
Displays messages per second and latency percentiles (p50, p95, p99).

## Resilience

Run the resilient client:
```bash
mvn exec:java -Dexec.mainClass="com.nats.ResilientClient"
```

While the client is running, stop and start NATS to see automatic reconnection:
```bash
docker-compose stop
docker-compose start
```

The client demonstrates automatic reconnection with connection event listeners.
Shows message buffering during disconnection and automatic resume on reconnection.

## Operational Metrics

Run the metrics viewer:
```bash
mvn exec:java -Dexec.mainClass="com.nats.OperationalMetrics"
```

Queries the NATS monitoring endpoint at `http://localhost:8222/varz`.
Displays server metrics including memory usage, uptime, connections, message counts, and CPU usage.
Demonstrates lightweight operational footprint.

## Clustering

The docker-compose configuration runs a 3-node NATS cluster with JetStream enabled.

Run the cluster client:
```bash
mvn exec:java -Dexec.mainClass="com.nats.ClusterClient"
```

The client connects to all cluster nodes and demonstrates message routing across the cluster.
Try stopping one node while the client is running to see automatic failover:
```bash
docker-compose stop nats-1
docker-compose start nats-1
```

## Authorization

The current docker-compose configuration uses `nats-config/auth.conf` for subject-based authorization.

Run the subscriber (in one terminal):
```bash
mvn exec:java -Dexec.mainClass="com.nats.SecureSubscriber"
```

Run the publisher (in another terminal):
```bash
mvn exec:java -Dexec.mainClass="com.nats.SecurePublisher"
```

The publisher has permission to publish to `secure.*` subjects.
The subscriber has permission to subscribe to `secure.*` subjects.
Attempting to publish to unauthorized subjects will result in a permission violation.

## TLS Encryption

The current docker-compose configuration uses TLS certificates from `nats-config/certs/`.

Certificates were generated using:
```bash
cd nats-config/certs
bash generate-certs.sh
```

Run the TLS client:
```bash
mvn exec:java -Dexec.mainClass="com.nats.TlsClient"
```

The client connects to NATS using TLS encryption.
All communication between client and server is encrypted.
Uses self-signed certificates for development purposes.

## Stop NATS

```bash
docker-compose down
```
