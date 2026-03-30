package org.example.standard;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.time.*;
import java.util.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  Structured Concurrency — Real-World Use Cases                              ║
 * ║  Practical examples where JEP 525 gives you a real advantage                ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * These examples show REAL scenarios where StructuredTaskScope replaces
 * fragile ExecutorService patterns with safe, bounded, automatically-cancelling
 * concurrent workflows.
 *
 * REFERENCES
 * ──────────
 *   • JEP 525 — Structured Concurrency (Sixth Preview):
 *       https://openjdk.org/jeps/525
 *   • JEP 444 — Virtual Threads:
 *       https://openjdk.org/jeps/444
 *
 * PATTERNS DEMONSTRATED
 * ─────────────────────
 *   1. API aggregator (BFF)    — Fetch user + orders + recs concurrently (microservice gateway)
 *   2. Price comparison        — Race multiple suppliers, take cheapest (e-commerce, travel)
 *   3. Health check dashboard  — Fan-out to all services, report status (Kubernetes, monitoring)
 *   4. Payment with fallback   — Try primary processor, fallback on failure (Stripe→PayPal)
 *   5. Parallel search         — Search multiple backends, merge results (search engines)
 *   6. Timeout-bounded fetch   — Fetch with hard deadline, cancel stragglers (latency-sensitive)
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.standard.StructuredConcurrencyRealWorldExamples
 */
public class StructuredConcurrencyRealWorldExamples {

    public static void main(String[] args) throws Exception {
        IO.println("╔═══════════════════════════════════════════════════════╗");
        IO.println("║  Structured Concurrency — Real-World Use Cases       ║");
        IO.println("╚═══════════════════════════════════════════════════════╝");
        IO.println();

        example1_ApiAggregator();
        example2_PriceComparison();
        example3_HealthCheckDashboard();
        example4_PaymentWithFallback();
        example5_ParallelSearch();
        example6_TimeoutBoundedFetch();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 1 — API Aggregator (Backend-for-Frontend)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: A mobile app's BFF layer needs to assemble a dashboard
    //  from 4 microservices. All fetches run concurrently, and if ANY
    //  critical service fails, the others are cancelled immediately.
    //
    //  BEFORE: ExecutorService + Future — manual cancellation on failure,
    //  threads may outlive the scope, hard to handle partial failures.
    //
    //  AFTER: StructuredTaskScope — all tasks bounded to scope, automatic
    //  cancellation, clean error propagation.
    //
    //  Real users: Netflix Zuul/Gateway, Spring Cloud Gateway, BFF patterns,
    //              GraphQL resolvers (DGS, Apollo Federation).
    // ═══════════════════════════════════════════════════════════════════════════
    static void example1_ApiAggregator() throws Exception {
        IO.println("1️⃣  API Aggregator (BFF Dashboard)");
        IO.println("   Use case: Netflix Gateway, Spring Cloud, GraphQL resolvers");
        IO.println("   ────────────────────────────────────────");

        record DashboardData(String user, String orders, String notifications, String recommendations) {}

        long start = System.currentTimeMillis();

        try (var scope = StructuredTaskScope.open()) {
            var userTask    = scope.fork(() -> fetchService("UserService", 80, "User{Alice, premium}"));
            var ordersTask  = scope.fork(() -> fetchService("OrderService", 120, "Orders[3 pending]"));
            var notifTask   = scope.fork(() -> fetchService("NotificationService", 60, "Notifs[5 unread]"));
            var recsTask    = scope.fork(() -> fetchService("RecommendationService", 100, "Recs[10 items]"));

            scope.join();

            var dashboard = new DashboardData(
                userTask.get(), ordersTask.get(), notifTask.get(), recsTask.get());

            long elapsed = System.currentTimeMillis() - start;
            IO.println("   Dashboard assembled in ~" + elapsed + "ms (concurrent!):");
            IO.println("     User:          " + dashboard.user());
            IO.println("     Orders:        " + dashboard.orders());
            IO.println("     Notifications: " + dashboard.notifications());
            IO.println("     Recommendations:" + dashboard.recommendations());
            IO.println("   (Sequential would take ~360ms, we took ~" + elapsed + "ms)");
        }
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 2 — Price Comparison (Race for Best Price)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Query multiple suppliers for the same product. Return
    //  the first (fastest) response — cancel the slower ones. This is
    //  the "race" pattern.
    //
    //  Real users: Google Flights, Kayak, Trivago, Amazon marketplace,
    //              insurance quote aggregators.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example2_PriceComparison() throws Exception {
        IO.println("2️⃣  Price Comparison (Race for Fastest Supplier)");
        IO.println("   Use case: Google Flights, Kayak, Amazon marketplace, insurance quotes");
        IO.println("   ────────────────────────────────────────");

        record Quote(String supplier, double price, long latencyMs) {}

        long start = System.currentTimeMillis();

        try (var scope = StructuredTaskScope.open(
                StructuredTaskScope.Joiner.<Quote>anySuccessfulOrThrow())) {

            scope.fork(() -> { Thread.sleep(200); return new Quote("SupplierA", 29.99, 200); });
            scope.fork(() -> { Thread.sleep(80);  return new Quote("SupplierB", 31.99, 80); });
            scope.fork(() -> { Thread.sleep(150); return new Quote("SupplierC", 27.99, 150); });

            Quote fastest = scope.join();
            long elapsed = System.currentTimeMillis() - start;

            IO.println("   Fastest quote: " + fastest.supplier()
                + " at $" + fastest.price() + " (in ~" + elapsed + "ms)");
            IO.println("   Slower suppliers automatically cancelled ✅");
        }
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 3 — Health Check Dashboard (Fan-Out)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: A monitoring system pings all microservices concurrently
    //  and reports their status. Unlike the race pattern, here we want
    //  ALL results — even if some fail.
    //
    //  Real users: Kubernetes liveness/readiness probes, Spring Actuator
    //              health endpoints, Consul health checks, Nagios.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example3_HealthCheckDashboard() throws Exception {
        IO.println("3️⃣  Health Check Dashboard (Fan-Out to All Services)");
        IO.println("   Use case: Kubernetes probes, Spring Actuator, Consul, Nagios");
        IO.println("   ────────────────────────────────────────");

        long start = System.currentTimeMillis();

        try (var scope = StructuredTaskScope.open()) {
            var auth = scope.fork(() -> checkHealth("auth-service", 50, true));
            var payments = scope.fork(() -> checkHealth("payment-service", 80, true));
            var catalog = scope.fork(() -> checkHealth("catalog-service", 120, false)); // down!
            var search = scope.fork(() -> checkHealth("search-service", 40, true));
            var notify = scope.fork(() -> checkHealth("notification-service", 90, true));

            scope.join();
            long elapsed = System.currentTimeMillis() - start;

            List<HealthResult> results = List.of(
                auth.get(), payments.get(), catalog.get(), search.get(), notify.get());

            IO.println("   Health check completed in ~" + elapsed + "ms:");
            for (HealthResult status : results) {
                String icon = status.healthy() ? "🟢" : "🔴";
                IO.println("     " + icon + " " + status.service()
                    + " (" + status.latencyMs() + "ms) — " + status.message());
            }

            long healthy = results.stream().filter(HealthResult::healthy).count();
            IO.println("   Summary: " + healthy + "/" + results.size() + " services healthy");
        }
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 4 — Payment with Fallback
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Process a payment through the primary processor (Stripe).
    //  If it fails, automatically try the fallback (PayPal). Structured
    //  concurrency ensures clean error handling and no zombie threads.
    //
    //  Real users: Payment orchestration (Stripe, Adyen, PayPal),
    //              multi-provider SMS gateways, CDN failover.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example4_PaymentWithFallback() throws Exception {
        IO.println("4️⃣  Payment with Fallback (Primary → Fallback)");
        IO.println("   Use case: Stripe→PayPal failover, multi-provider gateways");
        IO.println("   ────────────────────────────────────────");

        String paymentResult = processPaymentWithFallback(99.99);
        IO.println("   Result: " + paymentResult);
        IO.println();
    }

    private static String processPaymentWithFallback(double amount) throws Exception {
        // Try primary (simulate failure)
        try (var scope = StructuredTaskScope.open(
                StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow())) {

            scope.fork(() -> {
                Thread.sleep(100);
                throw new RuntimeException("Stripe gateway timeout");
            });

            scope.join();
            return "Paid via Stripe"; // won't reach

        } catch (Exception primaryFailure) {
            IO.println("   Primary (Stripe) failed: " + primaryFailure.getMessage());
            IO.println("   Trying fallback (PayPal)...");

            // Fallback
            try (var fallbackScope = StructuredTaskScope.open()) {
                var fallback = fallbackScope.fork(() -> {
                    Thread.sleep(80);
                    return "PayPal txn-" + UUID.randomUUID().toString().substring(0, 8);
                });

                fallbackScope.join();
                return "✅ Paid $" + amount + " via PayPal (txn: " + fallback.get() + ")";
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 5 — Parallel Search (Merge Results)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Search for a product across multiple backends (database,
    //  Elasticsearch, external API) and merge the results. All searches
    //  run in parallel for minimum latency.
    //
    //  Real users: Google (web + images + news), Amazon (catalog + marketplace),
    //              unified search (Algolia, Elasticsearch multi-index).
    // ═══════════════════════════════════════════════════════════════════════════
    static void example5_ParallelSearch() throws Exception {
        IO.println("5️⃣  Parallel Search (Multi-Backend Merge)");
        IO.println("   Use case: Google federated search, Amazon catalog, Algolia");
        IO.println("   ────────────────────────────────────────");

        String query = "mechanical keyboard";
        long start = System.currentTimeMillis();

        try (var scope = StructuredTaskScope.open()) {
            var dbResults = scope.fork(() -> searchBackend("PostgreSQL", query, 100,
                List.of("Keychron K8", "Ducky One 3")));

            var esResults = scope.fork(() -> searchBackend("Elasticsearch", query, 50,
                List.of("Leopold FC660M", "HHKB Professional")));

            var apiResults = scope.fork(() -> searchBackend("Partner API", query, 150,
                List.of("Realforce R3", "Topre Type-S")));

            scope.join();

            List<String> merged = new ArrayList<>();
            merged.addAll(dbResults.get());
            merged.addAll(esResults.get());
            merged.addAll(apiResults.get());

            long elapsed = System.currentTimeMillis() - start;
            IO.println("   Query: \"" + query + "\"");
            IO.println("   Results merged from 3 backends in ~" + elapsed + "ms:");
            for (int i = 0; i < merged.size(); i++) {
                IO.println("     " + (i + 1) + ". " + merged.get(i));
            }
        }
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 6 — Timeout-Bounded Fetch
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Fetch data from a slow service but enforce a hard deadline.
    //  If the service doesn't respond within the budget, cancel it and
    //  return a fallback. No leaked threads, no forgotten futures.
    //
    //  Real users: Netflix Hystrix/Resilience4j, circuit breakers,
    //              SLA-bounded API calls, latency-sensitive pipelines.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example6_TimeoutBoundedFetch() throws Exception {
        IO.println("6️⃣  Timeout-Bounded Fetch (Hard Deadline)");
        IO.println("   Use case: Circuit breakers, SLA-bounded APIs, latency budgets");
        IO.println("   ────────────────────────────────────────");

        // Fast case — service responds within deadline
        String fast = fetchWithDeadline("FastService", 50, 500);
        IO.println("   Fast service: " + fast);

        // Slow case — service exceeds deadline, gets cancelled
        String slow = fetchWithDeadline("SlowService", 2000, 200);
        IO.println("   Slow service: " + slow);

        IO.println("   ✅ No leaked threads — structured concurrency guarantees cleanup");
        IO.println();
    }

    private static String fetchWithDeadline(String service, long serviceLatencyMs,
                                             long deadlineMs) throws Exception {
        try (var scope = StructuredTaskScope.open(
                StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow(),
                cf -> cf.withTimeout(Duration.ofMillis(deadlineMs)))) {

            var task = scope.fork(() -> {
                Thread.sleep(serviceLatencyMs);
                return service + " responded with data";
            });

            scope.join();
            return "✅ " + task.get();

        } catch (Exception e) {
            return "⏱️ " + service + " timed out after " + deadlineMs + "ms → using fallback";
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    private static String fetchService(String name, long latencyMs, String data)
            throws InterruptedException {
        Thread.sleep(latencyMs);
        return data;
    }

    record HealthResult(String service, boolean healthy, long latencyMs, String message) {}

    private static HealthResult checkHealth(String service, long latencyMs, boolean healthy)
            throws InterruptedException {
        Thread.sleep(latencyMs);
        return new HealthResult(service, healthy, latencyMs,
            healthy ? "OK" : "Connection refused");
    }

    private static List<String> searchBackend(String backend, String query,
                                               long latencyMs, List<String> results)
            throws InterruptedException {
        Thread.sleep(latencyMs);
        return results;
    }
}


