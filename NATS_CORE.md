# NATS Core

The easiest way to start using NATS is by CLI and we're gonna test it through it.

Install it via `go install` itself or choose a OS specific approach.
Then start NATS server with `nats server run`, it also outputs connection info. (It's possible to `nats context select nats_connection_name` to use that connection whenever calling the CLI again)

## Request/Reply

With the proper setup we can test Request/Reply pattern.
In one terminal we can listen for a specific topic, for example:

Terminal 1
```bash
nats reply hero.topic "replying"
```

And in another terminal we can request from it (as many times we want) and we should get the `replying` defined above:

Terminal 2
```bash
nats req hero.topic ""
```

## Stream

We can test the Steam pattern with pub and sub commands:

Terminal 1
```bash
nats sub another.topic
```

Terminal 2
```bash
nats pub another.topic "hi"
```

### Fan-in

For the Fan-in pattern we could have the subscriber as well:

Terminal 1
```bash
nats sub hero.topic
```

But now we have multiple publishers keep calling the subscription indefenetly every second:

Terminal 2
```bash
nats pub hero.topic "pub 1" --count=-1 --sleep=1s
```

Terminal 3
```bash
nats pub hero.topic "pub 2" --count=-1 --sleep=1s
```

### Fan-out

Very simillar to Fan-in but in other way around. Here we have multiple subscribers and one publisher:

Terminal 1
```bash
nats sub hero.topic
```

Terminal 2
```bash
nats sub hero.topic
```

Terminal 3
```bash
nats pub hero.topic "hello world"
```

#### Queue group

Using Fan-out example we can apply a queue group to horizontally scale and load balance your subscribers:

Terminal 1
```bash
nats sub hero.topic --queue heroes
```

Terminal 2
```bash
nats sub hero.topic --queue heroes
```

Terminal 3
```bash
nats pub hero.topic "hello world" --count 1000
```

## Wildcards

The `.` is used to define hierarchy. For wildcards NATS uses `*` to match a single **token** (not substring). For example, to listen for any outcomes in a bank account could be `account.*.outcome`.

The second wildcard is `>` and can only be used at the end. It matches one or more tokens, for example `account.>` matches `account.transfer.income`
(It's possible to combine both `*.transfer.>` would recive `account.transfer.outcome.etc`)

Let's test them with group of services that replies with a greeting to any request in a wildcard:

Terminal 1
```bash
nats reply "hello.*" --command "echo 'hello, {{1}} from A'" --sleep=2s
```

Terminal 2
```bash
nats reply "hello.*" --command "echo 'hello, {{1}} from B'" --sleep=2s
```

Terminal 3
```bash
nats req hello.world "hello?" --count=-1
```

We can also observe the request and responses:

Terminal 4
```bash
nats sub ">"
```
