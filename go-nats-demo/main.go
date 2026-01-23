package main

import (
	"bufio"
	"context"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/nats-io/nats-server/v2/server"
	"github.com/nats-io/nats.go"
	"github.com/nats-io/nats.go/jetstream"
)

func main() {
	srv, err := startEmbeddedServer()
	if err != nil {
		fmt.Printf("Failed to start embedded server: %v\n", err)
		os.Exit(1)
	}
	defer srv.Shutdown()

	fmt.Println("╔════════════════════════════════════════════════════════════╗")
	fmt.Println("║           NATS Demo with Embedded Server                   ║")
	fmt.Println("║                                                            ║")
	fmt.Println("║  Server running at: nats://localhost:4222                  ║")
	fmt.Println("║  Debug/Trace: ENABLED (check server logs)                  ║")
	fmt.Println("║  JetStream: ENABLED                                        ║")
	fmt.Println("╚════════════════════════════════════════════════════════════╝")
	fmt.Println()

	reader := bufio.NewReader(os.Stdin)
	for {
		printMenu()
		fmt.Print("Select option: ")
		input, _ := reader.ReadString('\n')
		input = strings.TrimSpace(input)

		switch input {
		case "1":
			demoPubSub()
		case "2":
			demoJetStream()
		case "3":
			fmt.Println("Shutting down...")
			return
		default:
			fmt.Println("Invalid option, try again.")
		}
		fmt.Println()
	}
}

func printMenu() {
	fmt.Println("┌────────────────────────────────────────┐")
	fmt.Println("│  Demo Menu                             │")
	fmt.Println("├────────────────────────────────────────┤")
	fmt.Println("│  1. Pub/Sub (Core NATS + Wildcards)    │")
	fmt.Println("│  2. JetStream (Persistence)            │")
	fmt.Println("│  3. Exit                               │")
	fmt.Println("└────────────────────────────────────────┘")
}

func startEmbeddedServer() (*server.Server, error) {
	opts := &server.Options{
		Host:      "localhost",
		Port:      4222,
		Debug:     true,
		Trace:     true,
		NoLog:     false,
		NoSigs:    true,
		JetStream: true,
		StoreDir:  filepath.Join(os.TempDir(), "nats-demo-jetstream"),
	}

	srv, err := server.NewServer(opts)
	if err != nil {
		return nil, fmt.Errorf("creating server: %w", err)
	}

	srv.ConfigureLogger()

	go srv.Start()

	if !srv.ReadyForConnections(5 * time.Second) {
		return nil, fmt.Errorf("server not ready after 5 seconds")
	}

	fmt.Println("[Server] NATS server started successfully")
	return srv, nil
}

func demoPubSub() {
	fmt.Println("\n=== Demo 1: Pub/Sub with Wildcards ===")
	fmt.Println("Demonstrating subject wildcard matching in NATS")
	fmt.Println()

	nc, err := nats.Connect("nats://localhost:4222",
		nats.Name("PubSub-Demo-Client"),
	)
	if err != nil {
		fmt.Printf("Connection failed: %v\n", err)
		return
	}
	defer nc.Close()

	fmt.Println("[Client] Connected to NATS server")

	received := make(chan *nats.Msg, 10)

	sub1, err := nc.Subscribe("orders.*", func(msg *nats.Msg) {
		fmt.Printf("[Sub orders.*] Received on %s: %s\n", msg.Subject, string(msg.Data))
		received <- msg
	})
	if err != nil {
		fmt.Printf("Subscribe failed: %v\n", err)
		return
	}
	defer sub1.Unsubscribe()

	sub2, err := nc.Subscribe("orders.>", func(msg *nats.Msg) {
		fmt.Printf("[Sub orders.>] Received on %s: %s\n", msg.Subject, string(msg.Data))
		received <- msg
	})
	if err != nil {
		fmt.Printf("Subscribe failed: %v\n", err)
		return
	}
	defer sub2.Unsubscribe()

	fmt.Println("[Client] Subscribed to: orders.* and orders.>")
	fmt.Println()

	testCases := []struct {
		subject string
		data    string
		desc    string
	}{
		{"orders.created", "Order #1001 created", "Matches orders.* only"},
		{"orders.updated", "Order #1001 updated", "Matches orders.* only"},
		{"orders.region.us", "US region order", "Matches orders.> only (2 tokens after orders)"},
		{"orders.region.eu.priority", "EU priority order", "Matches orders.> only (3 tokens after orders)"},
	}

	for _, tc := range testCases {
		fmt.Printf("[Publish] Subject: %-25s | %s\n", tc.subject, tc.desc)

		err := nc.Publish(tc.subject, []byte(tc.data))
		if err != nil {
			fmt.Printf("Publish failed: %v\n", err)
			continue
		}
	}

	nc.Flush()

	fmt.Println("\n[Waiting for messages...]")
	timeout := time.After(2 * time.Second)
	count := 0
	expectedCount := 6

loop:
	for count < expectedCount {
		select {
		case <-received:
			count++
		case <-timeout:
			fmt.Println("[Timeout] Some messages may not have been received")
			break loop
		}
	}

	fmt.Printf("\n[Complete] Received %d messages total\n", count)
	fmt.Println("\nKey takeaways:")
	fmt.Println("  - '*' matches exactly one token")
	fmt.Println("  - '>' matches one or more tokens (must be last)")
	fmt.Println("  - Subject matching uses a trie structure for efficiency")
}

func demoJetStream() {
	fmt.Println("\n=== Demo 2: JetStream Persistence ===")
	fmt.Println("Demonstrating stream storage and consumer fetch")
	fmt.Println()

	nc, err := nats.Connect("nats://localhost:4222",
		nats.Name("JetStream-Demo-Client"),
	)
	if err != nil {
		fmt.Printf("Connection failed: %v\n", err)
		return
	}
	defer nc.Close()

	fmt.Println("[Client] Connected to NATS server")

	js, err := jetstream.New(nc)
	if err != nil {
		fmt.Printf("JetStream context failed: %v\n", err)
		return
	}

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	streamName := "EVENTS"
	stream, err := js.CreateOrUpdateStream(ctx, jetstream.StreamConfig{
		Name:        streamName,
		Description: "Demo event stream",
		Subjects:    []string{"events.>"},
		Storage:     jetstream.MemoryStorage,
		Retention:   jetstream.LimitsPolicy,
		MaxMsgs:     1000,
	})
	if err != nil {
		fmt.Printf("Stream creation failed: %v\n", err)
		return
	}

	fmt.Printf("[Stream] Created/updated stream: %s\n", streamName)
	fmt.Printf("[Stream] Subjects: events.>\n")
	fmt.Println()

	fmt.Println("[Publishing events to stream...]")
	events := []struct {
		subject string
		data    string
	}{
		{"events.user.signup", `{"user": "alice", "plan": "premium"}`},
		{"events.user.login", `{"user": "alice", "ip": "192.168.1.1"}`},
		{"events.order.created", `{"order": "ORD-001", "amount": 99.99}`},
		{"events.order.paid", `{"order": "ORD-001", "method": "card"}`},
		{"events.user.logout", `{"user": "alice"}`},
	}

	for _, e := range events {
		ack, err := js.Publish(ctx, e.subject, []byte(e.data))
		if err != nil {
			fmt.Printf("Publish failed: %v\n", err)
			continue
		}
		fmt.Printf("  Published to %-20s | Seq: %d\n", e.subject, ack.Sequence)
	}

	info, _ := stream.Info(ctx)
	fmt.Printf("\n[Stream Info] Messages: %d, Bytes: %d\n", info.State.Msgs, info.State.Bytes)

	consumerName := "demo-consumer"
	consumer, err := stream.CreateOrUpdateConsumer(ctx, jetstream.ConsumerConfig{
		Name:          consumerName,
		Durable:       consumerName,
		AckPolicy:     jetstream.AckExplicitPolicy,
		DeliverPolicy: jetstream.DeliverAllPolicy,
	})
	if err != nil {
		fmt.Printf("Consumer creation failed: %v\n", err)
		return
	}

	fmt.Printf("\n[Consumer] Created consumer: %s\n", consumerName)
	fmt.Println()

	fmt.Println("[Fetching messages from stream...]")
	msgs, err := consumer.Fetch(5, jetstream.FetchMaxWait(2*time.Second))
	if err != nil {
		fmt.Printf("Fetch failed: %v\n", err)
		return
	}

	count := 0
	for msg := range msgs.Messages() {
		meta, _ := msg.Metadata()
		fmt.Printf("  Seq %d | %-20s | %s\n",
			meta.Sequence.Stream,
			msg.Subject(),
			string(msg.Data()),
		)

		msg.Ack()
		count++
	}

	fmt.Printf("\n[Complete] Fetched and acked %d messages\n", count)

	consInfo, _ := consumer.Info(ctx)
	fmt.Printf("[Consumer State] Delivered: %d, Ack Floor: %d, Pending: %d\n",
		consInfo.Delivered.Stream,
		consInfo.AckFloor.Stream,
		consInfo.NumPending,
	)

	js.DeleteStream(ctx, streamName)
	fmt.Println("\n[Cleanup] Stream deleted")

	fmt.Println("\nKey takeaways:")
	fmt.Println("  - JetStream persists messages to stream storage")
	fmt.Println("  - Consumers track position independently")
	fmt.Println("  - Explicit acks advance the consumer cursor")
	fmt.Println("  - Messages remain until retention limits/policy")
}
