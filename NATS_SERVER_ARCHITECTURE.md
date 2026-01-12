# NATS Server Architecture & Code Snippets

NATS is a high-performance, cloud-native messaging system from CNCF that provides pub/sub, request/reply, and JetStream (distributed persistence). It supports 40+ client languages and features clustering, multi-cluster gateways, and advanced authentication.

## Key Components

### Client Connection Pipeline
1. **Entry**: `client.go` handles incoming connections
2. **Parsing**: `parser.go` uses a state machine to parse NATS protocol
3. **Authentication**: `auth.go` validates credentials (basic auth, NKey, JWT)
4. **Routing**: `sublist.go` matches published messages to subscribers
5. **Delivery**: `client.go` writeLoop/readLoop manage I/O

### JetStream Architecture
- **RAFT Consensus**: Cluster leader election and replication
- **Persistence Options**: Memory or File storage with configurable sync
- **Consumer Delivery Modes**: Push/Pull, various replay options
- **Subject Transforms**: Transform incoming messages before storing
- **Mirroring/Sourcing**: Copy from other streams

### Multi-Cluster Support
- **Routes**: Direct cluster-to-cluster connections
- **Gateways**: Connect isolated clusters with reply subject mapping
- **Compression**: S2 compression for bandwidth optimization
- **Interest-based**: Only forward messages where there's interest

## Interesting Code Snippets

### 1. Subject Matching Algorithm (Core Routing)
**`server/sublist.go:758-783`**

The heart of NATS - a recursive trie traversal with wildcard support (`*` matches one token, `>` matches all remaining):

```go
func matchLevel(l *level, toks []string, results *SublistResult) {
    var pwc, n *node
    for i, t := range toks {
        if l == nil {
            return
        }
        if l.fwc != nil {
            addNodeToResults(l.fwc, results)
        }
        if pwc = l.pwc; pwc != nil {
            matchLevel(pwc.next, toks[i+1:], results)
        }
        n = l.nodes[t]
        if n != nil {
            l = n.next
        } else {
            l = nil
        }
    }
    if n != nil {
        addNodeToResults(n, results)
    }
    if pwc != nil {
        addNodeToResults(pwc, results)
    }
}
```

### 2. AVL-based SequenceSet (80-100x Memory Efficient)
**`server/avl/seqset.go:24-50`**

Stores sequence numbers 80-100x more efficiently than `map[uint64]struct{}` using AVL tree nodes with bitmasks:

```go
type SequenceSet struct {
    root    *node
    size    int
    nodes   int
    changed bool
}
```

### 3. Time Hash Wheel (O(1) TTL Management)
**`server/thw/thw.go:30-68`**

4096-slot timing wheel for efficient expiration scheduling:

```go
const (
    tickDuration = int64(time.Second)
    wheelBits    = 12
    wheelSize    = 1 << wheelBits  // 4096 slots
    wheelMask    = wheelSize - 1
)

type HashWheel struct {
    wheel  []*slot
    lowest int64
    count  uint64
}
```

### 4. Protocol Parser State Machine
**`server/parser.go:56-134`**

~40 states for efficient byte-by-byte parsing with minimal allocations:

```go
const (
    OP_START parserState = iota
    OP_CONNECT, CONNECT_ARG
    OP_PUB, PUB_ARG, MSG_PAYLOAD
    OP_SUB, SUB_ARG
    OP_UNSUB, UNSUB_ARG
    OP_MSG, MSG_ARG
    OP_PING, OP_PONG
)
```

### 5. Adaptive Radix Trie for Subjects
**`server/stree/stree.go:1-128`**

Memory-efficient ART with path compression for subject storage:

```go
type SubjectTree[T any] struct {
    root node
    size int
}
```

### 6. plist Optimization (Map to Slice at Scale)
**`server/sublist.go:356-446`**

Lazy conversion from map to slice when subscribers exceed 256 for faster iteration:

```go
if n.plist != nil {
    n.plist = append(n.plist, sub)
} else if len(n.psubs) > plistMin {  // plistMin = 256
    n.plist = make([]*subscription, 0, len(n.psubs))
    for psub := range n.psubs {
        n.plist = append(n.plist, psub)
    }
}
```

### 7. File Store with Encryption
**`server/filestore.go:55-101`**

Block-based storage with ChaCha20-Poly1305/AES-GCM encryption and S2 compression:

```go
type FileStoreConfig struct {
    BlockSize       uint64
    CacheExpire     time.Duration
    SyncInterval    time.Duration
    Cipher          StoreCipher       // ChaCha20-Poly1305 or AES-GCM
    Compression     StoreCompression  // S2 compression
}
```

### 8. Intra-Process Queue
**`server/ipqueue.go:25-82`**

Generic queue with backpressure support for internal messaging:

```go
type ipQueue[T any] struct {
    inprogress int64
    sync.Mutex
    ch   chan struct{}
    elts []T
    pos  int
    pool *sync.Pool
    sz   uint64
}
```

### 9. Ring Buffer for Closed Connections
**`server/ring.go:25-77`**

Fixed-size ring buffer for O(1) connection tracking:

```go
type closedRingBuffer struct {
    total uint64
    conns []*closedClient
}
```

## Key Files by Purpose

| Component | File | Size | Purpose |
|-----------|------|------|---------|
| JetStream Cluster | `jetstream_cluster.go` | 316KB | RAFT-based replication |
| File Store | `filestore.go` | 341KB | Persistent storage |
| Stream | `stream.go` | 253KB | Stream management |
| Consumer | `consumer.go` | 191KB | Delivery logic |
| Client | `client.go` | 200KB | Connection handling |
| RAFT | `raft.go` | ~5000 lines | Consensus algorithm |
| Sublist | `sublist.go` | ~1700 lines | Subject routing |

## Performance Patterns

1. **LRU Caching**: Subject match cache with 1024 entries and sweep-based eviction
2. **Object Pooling**: `sync.Pool` for message objects to reduce GC pressure
3. **Atomic Operations**: Lock-free counters for stats (matches, cache_hits)
4. **Ring Buffers**: O(1) fixed-size buffers for connection tracking
5. **Time Wheels**: O(1) TTL task scheduling
6. **Path Compression**: Adaptive Radix Trie for memory-efficient subject storage
7. **Batch Operations**: IP queue for efficient intra-process messaging
8. **Adaptive Compression**: S2 compression based on RTT measurement
9. **plist Conversion**: Lazy map-to-slice at 256+ elements for iteration speed

## Authentication & Authorization

**`server/auth.go`**

Multiple auth methods:
- Basic authentication (username/password with bcrypt)
- NKey-based authentication (cryptographic key pairs)
- JWT tokens with issuer validation
- External LDAP/HTTP callout authentication
- OCSP certificate validation for TLS

Fine-grained permissions:
- Subject-based allow/deny lists (supports wildcards)
- Separate pub/sub permissions
- Response permission with TTL and max message count
- Per-user allowed connection types

## Data Structures

| Structure | File | Purpose |
|-----------|------|---------|
| Sublist | `sublist.go` | Multi-level trie with wildcard support |
| SequenceSet | `avl/seqset.go` | AVL tree with bitmasks (80-100x memory efficient) |
| SubjectTree | `stree/stree.go` | Adaptive Radix Trie |
| GenericSublist | `gsl/gsl.go` | Type-generic subject list |
| HashWheel | `thw/thw.go` | Timing wheel for TTL |
| ipQueue | `ipqueue.go` | Intra-process message queue |
| closedRingBuffer | `ring.go` | Ring buffer for connections |

# References

https://github.com/nats-io/nats-server