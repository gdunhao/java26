package org.example.standard;

import java.util.*;
import java.util.stream.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  Stream Gatherers — Real-World Use Cases                                    ║
 * ║  Practical examples where JEP 485 gives you a real advantage                ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * These examples show REAL scenarios where Stream Gatherers replace
 * broken pipelines, awkward collect-then-re-stream patterns, and
 * external library dependencies with clean, fluent stream operations.
 *
 * REFERENCES
 * ──────────
 *   • JEP 485 — Stream Gatherers:
 *       https://openjdk.org/jeps/485
 *   • Javadoc — java.util.stream.Gatherer:
 *       https://docs.oracle.com/en/java/javase/26/docs/api/java.base/java/util/stream/Gatherer.html
 *
 * PATTERNS DEMONSTRATED
 * ─────────────────────
 *   1. Log anomaly detection   — Sliding window to detect error bursts (SRE, monitoring)
 *   2. Stock price analysis    — Running min/max/average for time series (fintech, trading)
 *   3. Batch API calls         — Fixed-window batching for bulk API operations (ETL, migrations)
 *   4. Rate limiter simulation — Scan to track and enforce rate limits (API gateways)
 *   5. Event deduplication     — Custom gatherer to deduplicate events by key (event sourcing)
 *   6. Shopping cart discounts  — Sliding window for "buy N get 1 free" logic (e-commerce)
 *   7. CSV chunk processor     — Fixed windows for memory-bounded CSV processing (data pipelines)
 *   8. Running balance         — Scan for bank ledger running balance (accounting, fintech)
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.standard.StreamGatherersRealWorldExamples
 */
public class StreamGatherersRealWorldExamples {

    public static void main(String[] args) {
        IO.println("╔═══════════════════════════════════════════════════════╗");
        IO.println("║  Stream Gatherers — Real-World Use Cases             ║");
        IO.println("╚═══════════════════════════════════════════════════════╝");
        IO.println();

        example1_LogAnomalyDetection();
        example2_StockPriceAnalysis();
        example3_BatchApiCalls();
        example4_RateLimiterSimulation();
        example5_EventDeduplication();
        example6_ShoppingCartDiscounts();
        example7_CsvChunkProcessor();
        example8_RunningBalance();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 1 — Log Anomaly Detection (Sliding Window)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Your monitoring system receives log entries. You need to
    //  detect "error bursts" — when 3 or more errors occur within any
    //  window of 5 consecutive entries. Sliding windows make this trivial.
    //
    //  BEFORE: Collect to list, iterate with index math, track manually.
    //  AFTER: windowSliding(5) → filter by error count → done.
    //
    //  Real users: Datadog, Splunk, ELK Stack, PagerDuty, Grafana alerts.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example1_LogAnomalyDetection() {
        IO.println("1️⃣  Log Anomaly Detection (Error Burst Detection)");
        IO.println("   Use case: Datadog, Splunk, ELK Stack, PagerDuty alerts");
        IO.println("   ────────────────────────────────────────");

        record LogEntry(int seq, String level, String message) {}

        List<LogEntry> logs = List.of(
            new LogEntry(1,  "INFO",  "Request received"),
            new LogEntry(2,  "INFO",  "Processing payment"),
            new LogEntry(3,  "ERROR", "DB connection timeout"),
            new LogEntry(4,  "ERROR", "DB connection timeout"),
            new LogEntry(5,  "ERROR", "Cache miss fallback failed"),
            new LogEntry(6,  "ERROR", "Service unavailable"),
            new LogEntry(7,  "INFO",  "Retry succeeded"),
            new LogEntry(8,  "INFO",  "Request completed"),
            new LogEntry(9,  "ERROR", "Rate limit exceeded"),
            new LogEntry(10, "INFO",  "Healthcheck OK")
        );

        IO.println("   Log stream: " + logs.size() + " entries");

        // Sliding window of 5, flag windows with >= 3 errors
        var alerts = logs.stream()
            .gather(Gatherers.windowSliding(5))
            .filter(window -> window.stream()
                .filter(e -> e.level().equals("ERROR")).count() >= 3)
            .toList();

        IO.println("   Windows with ≥3 errors (burst detected):");
        for (var window : alerts) {
            String ids = window.stream().map(e -> "#" + e.seq()).collect(Collectors.joining(","));
            long errors = window.stream().filter(e -> e.level().equals("ERROR")).count();
            IO.println("     🔴 Window [" + ids + "] → " + errors + " errors");
        }
        IO.println("   ✅ " + alerts.size() + " alert(s) triggered");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 2 — Stock Price Analysis (Running Min/Max/Avg)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Compute a 3-period moving average, plus running high/low,
    //  for a stock price time series. Traders use this for technical analysis.
    //
    //  Real users: Bloomberg Terminal, TradingView, Robinhood, quantitative
    //              trading systems, financial data APIs.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example2_StockPriceAnalysis() {
        IO.println("2️⃣  Stock Price Analysis (Moving Average & Running High/Low)");
        IO.println("   Use case: Bloomberg, TradingView, quant trading, financial APIs");
        IO.println("   ────────────────────────────────────────");

        record PricePoint(String date, double close) {}

        List<PricePoint> prices = List.of(
            new PricePoint("Mon", 150.0), new PricePoint("Tue", 152.5),
            new PricePoint("Wed", 148.0), new PricePoint("Thu", 155.0),
            new PricePoint("Fri", 153.0), new PricePoint("Mon2", 158.0),
            new PricePoint("Tue2", 160.0)
        );

        // 3-period moving average using sliding windows
        IO.println("   Prices: " + prices.stream().map(p -> p.date() + "=$" + p.close())
            .collect(Collectors.joining(", ")));

        var movingAvgs = prices.stream()
            .map(PricePoint::close)
            .gather(Gatherers.windowSliding(3))
            .map(window -> window.stream().mapToDouble(d -> d).average().orElse(0))
            .toList();

        IO.println("   3-period MA: " + movingAvgs.stream()
            .map(d -> String.format("$%.1f", d)).collect(Collectors.joining(", ")));

        // Running high using scan
        var runningHighs = prices.stream()
            .map(PricePoint::close)
            .gather(Gatherers.scan(() -> Double.MIN_VALUE, Math::max))
            .toList();

        IO.println("   Running High: " + runningHighs.stream()
            .map(d -> String.format("$%.1f", d)).collect(Collectors.joining(", ")));
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 3 — Batch API Calls (Fixed Windows)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: You need to migrate 10,000 records to a new API that
    //  accepts batches of 3. Instead of managing index arithmetic,
    //  windowFixed(3) does it cleanly in the stream pipeline.
    //
    //  Real users: Database bulk inserts, Elasticsearch bulk API,
    //              AWS DynamoDB BatchWriteItem, Stripe batch operations.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example3_BatchApiCalls() {
        IO.println("3️⃣  Batch API Calls (Fixed-Window Batching)");
        IO.println("   Use case: Elasticsearch bulk, DynamoDB batch, Stripe batch ops");
        IO.println("   ────────────────────────────────────────");

        List<String> records = List.of(
            "user:alice", "user:bob", "user:carol", "user:dan",
            "user:eve", "user:frank", "user:grace", "user:heidi"
        );

        IO.println("   Records to migrate: " + records.size());

        var batches = records.stream()
            .gather(Gatherers.windowFixed(3))
            .toList();

        int batchNum = 1;
        for (var batch : batches) {
            IO.println("   Batch " + batchNum++ + ": " + batch
                + " (" + batch.size() + " records)");
        }
        IO.println("   ✅ " + batches.size() + " API calls instead of " + records.size());
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 4 — Rate Limiter Simulation (Scan)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Track cumulative request counts using scan to detect
    //  when a rate limit (10 requests) is exceeded. Each element is
    //  annotated with its cumulative count and whether it's allowed.
    //
    //  Real users: API gateways (Kong, Envoy), Cloudflare rate limiting,
    //              Spring Cloud Gateway, custom throttlers.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example4_RateLimiterSimulation() {
        IO.println("4️⃣  Rate Limiter Simulation (Running Count via Scan)");
        IO.println("   Use case: API gateways, Cloudflare rate limiting, throttlers");
        IO.println("   ────────────────────────────────────────");

        record ApiRequest(String clientId, String endpoint) {}

        List<ApiRequest> requests = List.of(
            new ApiRequest("client-A", "/api/users"),
            new ApiRequest("client-A", "/api/orders"),
            new ApiRequest("client-A", "/api/products"),
            new ApiRequest("client-A", "/api/search"),
            new ApiRequest("client-A", "/api/cart"),
            new ApiRequest("client-A", "/api/checkout"),
            new ApiRequest("client-A", "/api/shipping"),
            new ApiRequest("client-A", "/api/payment"),
            new ApiRequest("client-A", "/api/confirm"),
            new ApiRequest("client-A", "/api/receipt"),
            new ApiRequest("client-A", "/api/feedback"),     // #11 — exceeds limit!
            new ApiRequest("client-A", "/api/recommendations") // #12
        );

        int rateLimit = 10;

        // Use scan to track running request count
        var processed = requests.stream()
            .gather(Gatherers.scan(() -> 0, (count, req) -> count + 1))
            .toList();

        IO.println("   Rate limit: " + rateLimit + " requests");
        for (int i = 0; i < requests.size(); i++) {
            int count = processed.get(i);
            String status = count <= rateLimit ? "✅ ALLOWED" : "❌ RATE LIMITED (429)";
            IO.println("   #" + count + " " + requests.get(i).endpoint() + " → " + status);
        }
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 5 — Event Deduplication (Custom Gatherer)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Your event sourcing system receives duplicate events
    //  (e.g., from Kafka at-least-once delivery). You need to deduplicate
    //  by event ID while maintaining order — a custom gatherer.
    //
    //  Real users: Kafka consumers, SQS processors, event stores,
    //              webhook receivers, CQRS/ES systems.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example5_EventDeduplication() {
        IO.println("5️⃣  Event Deduplication (Custom Gatherer for Event Sourcing)");
        IO.println("   Use case: Kafka consumers, SQS, webhook receivers, CQRS");
        IO.println("   ────────────────────────────────────────");

        record DomainEvent(String eventId, String type, String payload) {}

        List<DomainEvent> events = List.of(
            new DomainEvent("evt-001", "ORDER_PLACED",   "order:1001"),
            new DomainEvent("evt-002", "PAYMENT_RECEIVED","payment:2001"),
            new DomainEvent("evt-001", "ORDER_PLACED",   "order:1001"),  // duplicate!
            new DomainEvent("evt-003", "ITEM_SHIPPED",   "shipment:3001"),
            new DomainEvent("evt-002", "PAYMENT_RECEIVED","payment:2001"),// duplicate!
            new DomainEvent("evt-004", "DELIVERY_CONFIRMED","delivery:4001")
        );

        IO.println("   Raw events: " + events.size() + " (with duplicates)");

        var deduplicated = events.stream()
            .gather(deduplicateBy(DomainEvent::eventId))
            .toList();

        IO.println("   After dedup: " + deduplicated.size() + " unique events");
        for (var event : deduplicated) {
            IO.println("     " + event.eventId() + " → " + event.type());
        }
        IO.println();
    }

    /** Custom gatherer: deduplicate by key extractor. */
    static <T, K> Gatherer<T, ?, T> deduplicateBy(
            java.util.function.Function<T, K> keyExtractor) {
        return Gatherer.ofSequential(
            HashSet<K>::new,
            (seen, element, downstream) -> {
                K key = keyExtractor.apply(element);
                if (seen.add(key)) {
                    return downstream.push(element);
                }
                return true;
            }
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 6 — Shopping Cart Discounts (Sliding Window)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: "Buy 2 get 1 free" promotion. Use sliding windows of 3
    //  to identify discount groups, then apply the discount to the
    //  cheapest item in each group.
    //
    //  Real users: Amazon, Shopify, Magento, any e-commerce checkout.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example6_ShoppingCartDiscounts() {
        IO.println("6️⃣  Shopping Cart Discounts (Buy 2 Get 1 Free)");
        IO.println("   Use case: Amazon, Shopify, any e-commerce checkout");
        IO.println("   ────────────────────────────────────────");

        record CartItem(String name, double price) {}

        // Items sorted by price descending (best deal for customer)
        List<CartItem> cart = List.of(
            new CartItem("Laptop Stand", 49.99),
            new CartItem("USB-C Hub",    39.99),
            new CartItem("Mouse Pad",    12.99),
            new CartItem("Webcam",       79.99),
            new CartItem("Headset",      59.99),
            new CartItem("Cable",         9.99)
        );

        // Sort by price descending, group into windows of 3
        var groups = cart.stream()
            .sorted(Comparator.comparingDouble(CartItem::price).reversed())
            .gather(Gatherers.windowFixed(3))
            .toList();

        double totalBefore = cart.stream().mapToDouble(CartItem::price).sum();
        double discount = 0;

        IO.println("   Cart items (" + cart.size() + "):");
        for (int g = 0; g < groups.size(); g++) {
            var group = groups.get(g);
            IO.println("   Group " + (g + 1) + ":");
            for (int i = 0; i < group.size(); i++) {
                CartItem item = group.get(i);
                boolean isFree = (i == group.size() - 1) && group.size() == 3;
                if (isFree) discount += item.price();
                IO.println("     " + item.name() + " $" + String.format("%.2f", item.price())
                    + (isFree ? " → FREE! 🎉" : ""));
            }
        }

        IO.println("   Subtotal: $" + String.format("%.2f", totalBefore));
        IO.println("   Discount: -$" + String.format("%.2f", discount));
        IO.println("   Total:    $" + String.format("%.2f", totalBefore - discount));
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 7 — CSV Chunk Processor (Memory-Bounded Processing)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Process a large CSV file in memory-bounded chunks.
    //  Each chunk is processed and written to the database, keeping
    //  only one chunk in memory at a time.
    //
    //  Real users: Spring Batch chunk processing, Apache Camel,
    //              ETL pipelines, data lake ingestion.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example7_CsvChunkProcessor() {
        IO.println("7️⃣  CSV Chunk Processor (Memory-Bounded Batching)");
        IO.println("   Use case: Spring Batch, Apache Camel, ETL pipelines");
        IO.println("   ────────────────────────────────────────");

        // Simulate CSV rows
        List<String> csvRows = List.of(
            "1,Alice,alice@test.com",
            "2,Bob,bob@test.com",
            "3,Carol,carol@test.com",
            "4,Dan,dan@test.com",
            "5,Eve,eve@test.com",
            "6,Frank,frank@test.com",
            "7,Grace,grace@test.com"
        );

        int chunkSize = 3;
        IO.println("   Total rows: " + csvRows.size() + ", chunk size: " + chunkSize);

        // Process in chunks using windowFixed
        var processedChunks = csvRows.stream()
            .gather(Gatherers.windowFixed(chunkSize))
            .map(chunk -> {
                // Simulate processing: parse and "insert" into database
                return "Inserted " + chunk.size() + " records: ["
                    + chunk.stream()
                        .map(row -> row.split(",")[1])
                        .collect(Collectors.joining(", "))
                    + "]";
            })
            .toList();

        int chunkNum = 1;
        for (String result : processedChunks) {
            IO.println("   Chunk " + chunkNum++ + ": " + result);
        }
        IO.println("   ✅ Processed " + csvRows.size() + " rows in " + processedChunks.size() + " chunks");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 8 — Running Balance (Bank Ledger)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Compute a running balance from a series of bank
    //  transactions. Scan is perfect for this — it emits the accumulated
    //  balance after each transaction.
    //
    //  Real users: Banking core systems, QuickBooks, Stripe balance
    //              transactions, cryptocurrency wallets.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example8_RunningBalance() {
        IO.println("8️⃣  Running Balance (Bank Ledger)");
        IO.println("   Use case: Banking core, QuickBooks, Stripe balance, crypto wallets");
        IO.println("   ────────────────────────────────────────");

        record Transaction(String description, double amount) {}

        List<Transaction> ledger = List.of(
            new Transaction("Opening deposit",      +1000.00),
            new Transaction("Rent payment",          -850.00),
            new Transaction("Salary",               +3500.00),
            new Transaction("Grocery store",          -67.50),
            new Transaction("Electric bill",          -95.00),
            new Transaction("Freelance payment",     +450.00),
            new Transaction("Restaurant",             -42.30)
        );

        // Running balance using scan
        var balances = ledger.stream()
            .map(Transaction::amount)
            .gather(Gatherers.scan(() -> 0.0, Double::sum))
            .toList();

        IO.println("   Ledger:");
        for (int i = 0; i < ledger.size(); i++) {
            Transaction tx = ledger.get(i);
            String sign = tx.amount() >= 0 ? "+" : "";
            IO.println("     " + String.format("%-25s %s$%8.2f  →  Balance: $%,.2f",
                tx.description(), sign, tx.amount(), balances.get(i)));
        }
        IO.println("   ✅ Running balance computed in a single stream pipeline");
        IO.println();
    }
}

