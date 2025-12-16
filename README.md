# NATS Pub/Sub POC

Simple publisher/subscriber pattern using NATS and Java 25.

## Prerequisites

- Java 25
- Maven
- Docker

## Running

Start NATS server:
```bash
docker compose up -d
```

Compile the project:
```bash
mvn clean compile
```

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

## Stop NATS

```bash
docker-compose down
```
