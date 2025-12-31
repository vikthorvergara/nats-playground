# NATS Messaging
```text
PROS (+)
  * Performance: Extremely low latency and high throughput for pub/sub and request-reply.
  * Simplicity: Lightweight operational footprint (small binaries, fast startup, easy local dev).
  * Topology Flexibility: Supports clustering, leaf nodes, and superclusters for geo/distributed routing.
  * Messaging Patterns: Native pub/sub, queue groups (work distribution), and request-reply without extra components.
  * JetStream Durability: Optional persistence, replay, and at-least-once delivery for streams/consumers.
  * Resilience: Built-in reconnect, backpressure handling, and fanout semantics that work well in unreliable networks.
  * Security: TLS, nkeys, account-based multi-tenancy, and fine-grained subject-based authorization.
  * Cloud/Edge Fit: Works well for microservices, IoT, and edge deployments where Kafka-like overhead is too heavy.

CONS (-)
  * Operational Complexity (at scale): Multi-region setups (superclusters/leaf nodes), JetStream tuning, and failure modes require expertise.
  * Persistence Tradeoffs: JetStream is strong, but long retention, very large histories, and heavy replay workloads can be harder than Kafka-style logs.
  * Delivery Semantics: Typically at-most-once (core) or at-least-once (JetStream); exactly-once workflows still require application-level idempotency.
  * Ecosystem & Tooling: Smaller ecosystem compared to Kafka/RabbitMQ (connectors, managed integrations, off-the-shelf monitoring dashboards).
  * Observability: Message tracing and consumer lag visibility often need extra instrumentation and conventions to match “enterprise bus” expectations.
  * Ordering Guarantees: Ordering is per subject/stream and can be impacted by scaling patterns; strict global ordering is not a default expectation.
  * Payload/Use-case Fit: Very large messages or heavy ETL/event-sourcing archives may push you toward object storage + references or a log-centric platform.
  * Managed Offering Constraints: If using managed NATS, you may face limits around topology, storage, or upgrades that affect deep customization.
```