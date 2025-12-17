# NATS Messaging POC

Simple messaging patterns using NATS and Java 25:
- Pub/Sub Pattern
- Request/Reply Pattern

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

## Stop NATS

```bash
docker-compose down
```
