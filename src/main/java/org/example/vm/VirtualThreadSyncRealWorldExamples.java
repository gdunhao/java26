package org.example.vm;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  Virtual Threads without Pinning — Real-World Use Cases                    ║
 * ║  Practical examples where JEP 491 gives you a real advantage               ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * These examples show REAL scenarios where virtual threads previously got
 * "pinned" to carrier threads inside `synchronized` blocks — wrecking
 * throughput. With JDK 26, all of these work smoothly because virtual
 * threads can unmount from their carrier even while holding a monitor.
 *
 * Each example simulates a common server-side pattern that was broken
 * (or severely degraded) before JDK 26 when using virtual threads.
 *
 * REFERENCES
 * ──────────
 *   • JEP 491 — Synchronize Virtual Threads without Pinning:
 *       https://openjdk.org/jeps/491
 *   • JEP 444 — Virtual Threads (finalized in JDK 21):
 *       https://openjdk.org/jeps/444
 *   • JEP 425 — Virtual Threads (First Preview, detailed design):
 *       https://openjdk.org/jeps/425
 *   • Inside Java — Virtual Threads: An Adoption Guide:
 *       https://inside.java/2024/02/04/sip097/
 *
 * PATTERNS DEMONSTRATED
 * ─────────────────────
 *   1. Connection pool       — synchronized checkout + blocking I/O (HikariCP, DBCP, any JDBC app)
 *   2. Shared cache          — synchronized lazy-load with remote fetch (product catalogs, config caches)
 *   3. Legacy logging        — synchronized file appender (java.util.logging, Log4j 1.x, audit trails)
 *   4. Rate limiter          — Token-bucket with wait() inside synchronized (API gateways, throttling)
 *   5. Write-Ahead Log       — synchronized sequential disk writes (databases, Kafka, event stores)
 *   6. Legacy library calls  — Third-party synchronized I/O you can't modify (XML parsers, SMTP, PDF)
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.vm.VirtualThreadSyncRealWorldExamples
 */
public class VirtualThreadSyncRealWorldExamples {

    public static void main(String[] args) throws Exception {
        IO.println("╔═══════════════════════════════════════════════════════╗");
        IO.println("║  Virtual Threads without Pinning — Real-World Cases  ║");
        IO.println("╚═══════════════════════════════════════════════════════╝");
        IO.println();

        example1_DatabaseConnectionPool();
        example2_SharedCacheWithLazyLoading();
        example3_LegacyLoggingFramework();
        example4_RateLimiter();
        example5_WriteAheadLog();
        example6_LegacyLibraryIntegration();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 1 — Database Connection Pool
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: You have a web server handling thousands of requests
    //  concurrently with virtual threads. Each request needs a database
    //  connection from a shared pool. Connection pools like HikariCP,
    //  C3P0, and even JDBC drivers themselves use `synchronized` internally
    //  for thread safety.
    //
    //  THE PROBLEM BEFORE JDK 26:
    //    1. Virtual thread acquires connection inside `synchronized`
    //    2. Executes a query → blocking I/O (network round-trip to DB)
    //    3. Virtual thread is PINNED to its carrier the entire time
    //    4. With only N carrier threads (= CPU cores), only N requests
    //       can make progress → throughput tanks
    //
    //  WITH JDK 26:
    //    1. Virtual thread acquires connection inside `synchronized`
    //    2. Executes a query → blocking I/O → virtual thread UNMOUNTS ✅
    //    3. Carrier thread runs OTHER virtual threads while waiting
    //    4. Thousands of concurrent DB operations work smoothly
    //
    //  Real users: Spring Boot + HikariCP, Quarkus + Agroal, any
    //              JDBC-based application migrating to virtual threads.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example1_DatabaseConnectionPool() throws Exception {
        IO.println("1️⃣  Database Connection Pool (synchronized checkout + I/O)");
        IO.println("   Use case: Web servers, microservices, any JDBC-based app");
        IO.println("   ────────────────────────────────────────");

        // Simulate a connection pool with 10 connections, 500 concurrent requests
        var pool = new SimulatedConnectionPool(10);
        int numRequests = 500;
        var completedRequests = new AtomicInteger(0);
        var totalQueryTime = new AtomicLong(0);

        Instant start = Instant.now();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < numRequests; i++) {
                final int requestId = i;
                futures.add(executor.submit(() -> {
                    // Each virtual thread: checkout → query (I/O) → return
                    var conn = pool.checkout();    // synchronized inside!
                    try {
                        long queryStart = System.nanoTime();
                        conn.executeQuery();       // simulated blocking I/O
                        totalQueryTime.addAndGet(System.nanoTime() - queryStart);
                    } finally {
                        pool.checkin(conn);         // synchronized inside!
                    }
                    completedRequests.incrementAndGet();
                }));
            }
            for (var f : futures) f.get();
        }

        long elapsed = Duration.between(start, Instant.now()).toMillis();

        IO.println("   Pool size:        10 connections");
        IO.println("   Concurrent reqs:  " + numRequests);
        IO.println("   Completed:        " + completedRequests.get());
        IO.println("   Wall-clock time:  " + elapsed + "ms");
        IO.println("   Carriers (cores): " + Runtime.getRuntime().availableProcessors());
        IO.println();
        IO.println("   ℹ️  Before JDK 26: With only " + Runtime.getRuntime().availableProcessors()
                + " carriers, pinning inside the pool's");
        IO.println("      synchronized blocks meant at most " + Runtime.getRuntime().availableProcessors()
                + " queries could run in parallel.");
        IO.println("   ✅ With JDK 26: All 10 pooled connections stay busy — virtual threads");
        IO.println("      unmount during I/O even inside synchronized.");
        IO.println();
    }

    /** Simulates a JDBC-like connection pool using synchronized for thread safety. */
    static class SimulatedConnectionPool {
        private final Deque<SimulatedConnection> available = new ArrayDeque<>();
        private final int maxSize;

        SimulatedConnectionPool(int size) {
            this.maxSize = size;
            for (int i = 0; i < size; i++) {
                available.push(new SimulatedConnection(i));
            }
        }

        /** Real pools (HikariCP, DBCP) use synchronized for checkout. */
        synchronized SimulatedConnection checkout() {
            while (available.isEmpty()) {
                try { wait(); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
            return available.pop();
        }

        synchronized void checkin(SimulatedConnection conn) {
            available.push(conn);
            notify();
        }
    }

    static class SimulatedConnection {
        final int id;
        SimulatedConnection(int id) { this.id = id; }

        /** Simulates a 5ms database query (network I/O). */
        void executeQuery() {
            try { Thread.sleep(5); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 2 — Shared Cache with Lazy Loading
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: You have an application-level cache (product catalog,
    //  user profiles, feature flags) protected by `synchronized` to
    //  ensure thread safety. On a cache miss, you load data from a
    //  remote service or database — a blocking I/O call.
    //
    //  THE PROBLEM BEFORE JDK 26:
    //    - Cache miss → load data (I/O) inside `synchronized`
    //    - Virtual thread pinned during the ENTIRE load
    //    - Other virtual threads waiting for the same lock are also stuck
    //    - Cold cache startup = massive pinning storm
    //
    //  WITH JDK 26:
    //    - Cache miss → load data (I/O) → virtual thread unmounts ✅
    //    - Carrier thread serves other virtual threads while data loads
    //    - Cold cache warm-up is fast despite heavy contention
    //
    //  Real users: E-commerce product catalogs, session stores, config
    //              caches, any compute-then-cache pattern.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example2_SharedCacheWithLazyLoading() throws Exception {
        IO.println("2️⃣  Shared Cache with Lazy Loading (synchronized + remote fetch)");
        IO.println("   Use case: Product catalogs, session stores, config caches");
        IO.println("   ────────────────────────────────────────");

        var cache = new SynchronizedCache();
        int numLookups = 200;
        var hits = new AtomicInteger(0);
        var misses = new AtomicInteger(0);

        Instant start = Instant.now();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < numLookups; i++) {
                // 50 unique keys → mix of hits and misses
                final String key = "product-" + (i % 50);
                futures.add(executor.submit(() -> {
                    String value = cache.get(key);
                    if (value.startsWith("LOADED")) misses.incrementAndGet();
                    else hits.incrementAndGet();
                    return value;
                }));
            }
            for (var f : futures) f.get();
        }

        long elapsed = Duration.between(start, Instant.now()).toMillis();

        IO.println("   Lookups:      " + numLookups + " (50 unique keys)");
        IO.println("   Cache misses: " + cache.loadCount.get() + " (triggered remote fetch)");
        IO.println("   Wall-clock:   " + elapsed + "ms");
        IO.println();
        IO.println("   ℹ️  Before JDK 26: Each cache miss pinned a carrier during the remote");
        IO.println("      fetch. With " + Runtime.getRuntime().availableProcessors()
                + " carriers, a cold cache was a bottleneck.");
        IO.println("   ✅ With JDK 26: Cache-miss fetches unmount — carriers stay productive.");
        IO.println();
    }

    /** A typical synchronized cache — exactly how many legacy apps protect shared state. */
    static class SynchronizedCache {
        private final Map<String, String> store = new HashMap<>();
        final AtomicInteger loadCount = new AtomicInteger(0);

        String get(String key) {
            synchronized (this) {
                String cached = store.get(key);
                if (cached != null) return cached;
            }
            // Cache miss → fetch from "remote service" (blocking I/O)
            String value = fetchFromRemoteService(key);
            synchronized (this) {
                store.putIfAbsent(key, value);
                return store.get(key);
            }
        }

        /** Simulates a 10ms remote service call. */
        String fetchFromRemoteService(String key) {
            loadCount.incrementAndGet();
            try { Thread.sleep(10); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "LOADED:" + key + "@" + Instant.now();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 3 — Legacy Logging Framework (synchronized file appender)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Your application uses a logging framework (or even
    //  `java.util.logging`) that synchronizes writes to ensure log lines
    //  don't interleave. Every time a virtual thread logs a message,
    //  it enters a `synchronized` block and performs file I/O.
    //
    //  THE PROBLEM BEFORE JDK 26:
    //    - Logging is EVERYWHERE (every request, every method boundary)
    //    - Each log call: synchronized { write to file (I/O) }
    //    - With 10,000 virtual threads logging frequently → pinning storm
    //    - Carrier threads spent blocked on file writes instead of running
    //      other virtual threads
    //
    //  WITH JDK 26:
    //    - Log writes still synchronized (correct!) but virtual threads
    //      unmount during the file I/O → carriers run other threads ✅
    //
    //  Real users: Any Java app using java.util.logging, Log4j 1.x
    //              synchronized appenders, custom file loggers, audit
    //              trail writers.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example3_LegacyLoggingFramework() throws Exception {
        IO.println("3️⃣  Legacy Logging Framework (synchronized file writes)");
        IO.println("   Use case: java.util.logging, Log4j 1.x appenders, audit logs");
        IO.println("   ────────────────────────────────────────");

        var logger = new SynchronizedFileLogger();
        int numThreads = 1_000;
        int logsPerThread = 5;

        Instant start = Instant.now();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < numThreads; t++) {
                final int threadId = t;
                futures.add(executor.submit(() -> {
                    for (int msg = 0; msg < logsPerThread; msg++) {
                        // Every log call enters synchronized + does I/O
                        logger.log("INFO", "VThread-" + threadId,
                                "Processing request step " + msg);
                    }
                }));
            }
            for (var f : futures) f.get();
        }

        long elapsed = Duration.between(start, Instant.now()).toMillis();

        IO.println("   Virtual threads:  " + numThreads);
        IO.println("   Log lines total:  " + logger.lineCount.get());
        IO.println("   Wall-clock time:  " + elapsed + "ms");
        IO.println();
        IO.println("   ℹ️  Before JDK 26: Each log write pinned the carrier during file I/O.");
        IO.println("      With " + numThreads + " threads × " + logsPerThread
                + " logs = " + (numThreads * logsPerThread) + " pinning events!");
        IO.println("   ✅ With JDK 26: synchronized file writes no longer pin carriers.");
        IO.println();
    }

    /** Mimics a traditional synchronized file appender (like Log4j 1.x or j.u.l). */
    static class SynchronizedFileLogger {
        final AtomicInteger lineCount = new AtomicInteger(0);

        /** Real loggers use synchronized to prevent interleaved output. */
        synchronized void log(String level, String source, String message) {
            // Format the log line
            String line = Instant.now() + " [" + level + "] " + source + ": " + message;
            // Simulate file I/O (write + flush) — the blocking part
            simulateFileWrite(line);
            lineCount.incrementAndGet();
        }

        void simulateFileWrite(String line) {
            try { Thread.sleep(1); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 4 — Token Bucket Rate Limiter
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Your API gateway enforces rate limits using a token-
    //  bucket algorithm. The bucket state (available tokens, last refill
    //  time) is protected by `synchronized`. When a virtual thread finds
    //  no tokens available, it waits (blocking) inside the synchronized
    //  block until tokens are refilled.
    //
    //  THE PROBLEM BEFORE JDK 26:
    //    - Virtual thread calls acquireToken() → enters `synchronized`
    //    - No tokens available → Thread.sleep() or wait() to back off
    //    - Virtual thread is PINNED during the entire wait
    //    - With burst traffic (1000+ VTs), all carriers get pinned waiting
    //      for tokens → zero throughput until tokens refill
    //
    //  WITH JDK 26:
    //    - wait()/sleep() inside `synchronized` unmounts the VT ✅
    //    - Carriers run other virtual threads while waiters wait
    //    - Rate limiting works correctly without stalling the server
    //
    //  Real users: API gateways (Kong, Zuul), microservice throttlers,
    //              cloud function rate limiters, SaaS tier enforcement.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example4_RateLimiter() throws Exception {
        IO.println("4️⃣  Token Bucket Rate Limiter (synchronized wait for tokens)");
        IO.println("   Use case: API gateways, throttling, SaaS tier enforcement");
        IO.println("   ────────────────────────────────────────");

        // 50 tokens/sec, bucket size 50 — simulating a rate-limited API
        var limiter = new TokenBucketRateLimiter(50, 50);
        int numRequests = 200;
        var allowed = new AtomicInteger(0);
        var throttled = new AtomicInteger(0);

        Instant start = Instant.now();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < numRequests; i++) {
                futures.add(executor.submit(() -> {
                    if (limiter.tryAcquire()) {
                        // Simulate handling the request (some I/O)
                        try { Thread.sleep(5); } catch (InterruptedException e) { }
                        allowed.incrementAndGet();
                    } else {
                        throttled.incrementAndGet();
                    }
                }));
            }
            for (var f : futures) f.get();
        }

        long elapsed = Duration.between(start, Instant.now()).toMillis();

        IO.println("   Requests:    " + numRequests);
        IO.println("   Allowed:     " + allowed.get());
        IO.println("   Throttled:   " + throttled.get());
        IO.println("   Wall-clock:  " + elapsed + "ms");
        IO.println();
        IO.println("   ℹ️  Before JDK 26: Virtual threads calling tryAcquire() pinned carriers");
        IO.println("      inside the synchronized token-check, stalling the whole server.");
        IO.println("   ✅ With JDK 26: Token checks inside synchronized don't pin — carriers");
        IO.println("      keep running other requests while tokens are checked.");
        IO.println();
    }

    /**
     * Classic token-bucket rate limiter — uses `synchronized` exactly
     * as you'd find in real-world implementations (Guava RateLimiter,
     * Resilience4j, custom implementations).
     */
    static class TokenBucketRateLimiter {
        private double tokens;
        private final double maxTokens;
        private final double refillRate; // tokens per second
        private long lastRefillNanos;

        TokenBucketRateLimiter(double maxTokens, double refillRate) {
            this.tokens = maxTokens;
            this.maxTokens = maxTokens;
            this.refillRate = refillRate;
            this.lastRefillNanos = System.nanoTime();
        }

        /** Synchronized — this is the pattern that previously caused pinning. */
        synchronized boolean tryAcquire() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            // Optionally wait for a token (blocking inside synchronized!)
            try {
                long waitMs = (long) (1000.0 / refillRate);
                wait(waitMs); // This wait() inside synchronized used to PIN!
                refill();
                if (tokens >= 1.0) {
                    tokens -= 1.0;
                    return true;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            double elapsed = (now - lastRefillNanos) / 1_000_000_000.0;
            tokens = Math.min(maxTokens, tokens + elapsed * refillRate);
            lastRefillNanos = now;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 5 — Write-Ahead Log (WAL)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: A database, message broker, or event-sourced system
    //  writes every mutation to a sequential Write-Ahead Log before
    //  applying it. The WAL uses `synchronized` to guarantee ordering
    //  and atomicity of log entries. Each write is file I/O.
    //
    //  THE PROBLEM BEFORE JDK 26:
    //    - Every transaction must: synchronized { assign LSN → write → fsync }
    //    - With virtual threads handling transactions, every write pins
    //    - High transaction rates → all carriers blocked on fsync
    //    - Throughput collapses despite having thousands of VTs
    //
    //  WITH JDK 26:
    //    - The file I/O inside synchronized causes the VT to unmount ✅
    //    - Carriers run other VTs while waiting for disk writes
    //    - WAL ordering is preserved (synchronized guarantees it)
    //    - Much higher transaction throughput with virtual threads
    //
    //  Real users: Embedded databases (H2, SQLite via JDBC), Kafka
    //              brokers, event stores, CQRS/ES frameworks.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example5_WriteAheadLog() throws Exception {
        IO.println("5️⃣  Write-Ahead Log (synchronized sequential writes)");
        IO.println("   Use case: Databases, message brokers, event-sourced systems");
        IO.println("   ────────────────────────────────────────");

        var wal = new WriteAheadLog();
        int numTransactions = 1_000;

        Instant start = Instant.now();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < numTransactions; i++) {
                final int txnId = i;
                futures.add(executor.submit(() -> {
                    // Each transaction writes to the WAL inside synchronized
                    wal.append("TXN-" + txnId, "UPDATE account SET balance = balance - 100");
                }));
            }
            for (var f : futures) f.get();
        }

        long elapsed = Duration.between(start, Instant.now()).toMillis();

        IO.println("   Transactions:    " + numTransactions);
        IO.println("   WAL entries:     " + wal.lsn.get());
        IO.println("   Wall-clock time: " + elapsed + "ms");
        IO.println();
        IO.println("   ℹ️  Before JDK 26: Each WAL append = synchronized { I/O } = pinned carrier.");
        IO.println("      With " + Runtime.getRuntime().availableProcessors()
                + " carriers, max " + Runtime.getRuntime().availableProcessors()
                + " concurrent disk writes.");
        IO.println("   ✅ With JDK 26: Disk I/O inside synchronized unmounts the VT —");
        IO.println("      carriers stay busy running other transactions.");
        IO.println();
    }

    /** A simplified Write-Ahead Log — the synchronized + I/O pattern is the key point. */
    static class WriteAheadLog {
        final AtomicLong lsn = new AtomicLong(0);

        /**
         * Append an entry to the log. Must be synchronized to guarantee
         * sequential LSN assignment and ordered writes — exactly as
         * real WAL implementations do.
         */
        synchronized void append(String txnId, String operation) {
            long sequence = lsn.incrementAndGet();
            String entry = sequence + "|" + txnId + "|" + operation;
            // Simulate disk write + fsync (~2ms)
            simulateDiskWrite(entry);
        }

        void simulateDiskWrite(String data) {
            try { Thread.sleep(2); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 6 — Legacy Library Integration (third-party synchronized code)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Your application calls a third-party library that you
    //  cannot modify. Internally, that library uses `synchronized` around
    //  I/O operations — a pattern extremely common in older Java libraries.
    //  Examples: XML parsers, PDF generators, SMTP clients, LDAP
    //  connectors, older HTTP clients, cryptographic providers.
    //
    //  THE PROBLEM BEFORE JDK 26:
    //    - You adopt virtual threads for your new microservice ✅
    //    - You call a legacy XML parsing library to process requests
    //    - Library internally: synchronized(parser) { read from stream }
    //    - Your virtual thread gets pinned — you can't fix the library!
    //    - Only workaround: wrap calls on platform threads (defeats VTs)
    //
    //  WITH JDK 26:
    //    - The library's internal synchronized blocks are no longer a
    //      problem — JDK handles unmounting transparently ✅
    //    - No need to audit third-party code for synchronized blocks
    //    - No need for platform-thread wrappers
    //
    //  Real users: ANY app migrating to virtual threads that depends on
    //              legacy libraries — which is essentially every Java app.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example6_LegacyLibraryIntegration() throws Exception {
        IO.println("6️⃣  Legacy Library Integration (third-party synchronized code)");
        IO.println("   Use case: XML parsers, PDF generators, SMTP/LDAP clients");
        IO.println("   ────────────────────────────────────────");

        // Simulate calling a legacy library that you CANNOT modify
        var legacyXmlParser = new LegacyXmlParser();
        var legacySmtpClient = new LegacySmtpClient();
        var legacyPdfRenderer = new LegacyPdfRenderer();

        int numRequests = 300;
        var results = new AtomicInteger(0);

        Instant start = Instant.now();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < numRequests; i++) {
                final int reqId = i;
                futures.add(executor.submit(() -> {
                    // A typical request pipeline calling multiple legacy libraries:
                    // 1. Parse incoming XML payload (legacy parser with synchronized)
                    String parsed = legacyXmlParser.parse("<order id='" + reqId + "'><item>Widget</item></order>");

                    // 2. Generate a PDF receipt (legacy renderer with synchronized)
                    byte[] pdf = legacyPdfRenderer.render("Receipt for " + parsed);

                    // 3. Send confirmation email (legacy SMTP with synchronized)
                    legacySmtpClient.send("customer@example.com", "Order " + reqId, "Your order is confirmed.");

                    results.incrementAndGet();
                }));
            }
            for (var f : futures) f.get();
        }

        long elapsed = Duration.between(start, Instant.now()).toMillis();

        IO.println("   Requests:    " + numRequests);
        IO.println("   Completed:   " + results.get());
        IO.println("   Wall-clock:  " + elapsed + "ms");
        IO.println("   Libraries:   3 (XML parser + PDF renderer + SMTP client)");
        IO.println();
        IO.println("   ℹ️  Before JDK 26: Each library's internal synchronized blocks pinned");
        IO.println("      the carrier. A single request hit 3 pinning points!");
        IO.println("   ✅ With JDK 26: Third-party synchronized code is transparent —");
        IO.println("      no auditing, no wrappers, no ReentrantLock rewrites needed.");
        IO.println();

        IO.println("   ┌────────────────────────────────────────────────────────────┐");
        IO.println("   │  BOTTOM LINE: Before JDK 26, adopting virtual threads      │");
        IO.println("   │  required auditing EVERY dependency for synchronized       │");
        IO.println("   │  blocks. With JDK 26, `synchronized` and virtual threads   │");
        IO.println("   │  simply work together — no migration effort required.      │");
        IO.println("   └────────────────────────────────────────────────────────────┘");
        IO.println();
    }

    // ── Legacy library simulations (you CANNOT change this code) ──────────

    /** Simulates a legacy XML parser that uses synchronized internally. */
    static class LegacyXmlParser {
        synchronized String parse(String xml) {
            // Real parsers: javax.xml.parsers, dom4j, JDOM — many use synchronized
            simulateIo(3); // SAX/DOM parsing reads from InputStream
            return "parsed[" + xml.length() + " chars]";
        }
    }

    /** Simulates a legacy SMTP client with synchronized send. */
    static class LegacySmtpClient {
        synchronized void send(String to, String subject, String body) {
            // Real: JavaMail / Jakarta Mail, Apache Commons Email
            simulateIo(5); // Network I/O to SMTP server
        }
    }

    /** Simulates a legacy PDF rendering library with synchronized render. */
    static class LegacyPdfRenderer {
        synchronized byte[] render(String content) {
            // Real: iText, Apache PDFBox, JasperReports — many use synchronized
            simulateIo(4); // File/memory I/O for font loading, image rendering
            return new byte[content.length() * 10]; // fake PDF bytes
        }
    }

    static void simulateIo(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

