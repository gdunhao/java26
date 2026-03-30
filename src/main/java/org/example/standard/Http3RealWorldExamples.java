package org.example.standard;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  HTTP/3 for the HTTP Client API — Real-World Use Cases                      ║
 * ║  Practical examples where JEP 517 gives you a real advantage                ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * REFERENCES
 * ──────────
 *   • JEP 517 — HTTP/3 for the HTTP Client API:
 *       https://openjdk.org/jeps/517
 *   • RFC 9114 — HTTP/3:
 *       https://www.rfc-editor.org/rfc/rfc9114
 *   • RFC 9000 — QUIC: A UDP-Based Multiplexed and Secure Transport:
 *       https://www.rfc-editor.org/rfc/rfc9000
 *
 * PATTERNS DEMONSTRATED
 * ─────────────────────
 *   1. REST API client          — Version-aware API calls with fallback (microservices)
 *   2. Health check service     — Monitor endpoint availability and protocol support
 *   3. Parallel API aggregator  — Fan-out to multiple services with QUIC multiplexing
 *   4. Protocol version audit   — Audit which services support HTTP/3 (compliance)
 *   5. Resilient downloader     — Download with version fallback and retry logic
 *   6. Latency benchmarking     — Compare HTTP/2 vs HTTP/3 performance
 *
 * NOTE: These examples require network access. If servers are unreachable,
 * results will show graceful fallback behavior.
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.standard.Http3RealWorldExamples
 */
public class Http3RealWorldExamples {

    public static void main(String[] args) throws Exception {
        IO.println("╔═══════════════════════════════════════════════════════╗");
        IO.println("║  HTTP/3 — Real-World Use Cases                       ║");
        IO.println("╚═══════════════════════════════════════════════════════╝");
        IO.println();

        example1_VersionAwareRestClient();
        example2_HealthCheckService();
        example3_ParallelApiAggregator();
        example4_ProtocolVersionAudit();
        example5_ResilientDownloader();
        example6_LatencyBenchmark();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 1 — Version-Aware REST API Client
    // ═══════════════════════════════════════════════════════════════════════════
    //  Scenario: A microservice client that prefers HTTP/3 for reduced
    //  latency but logs the actual negotiated protocol for diagnostics.
    //
    //  Real users: Spring WebClient, Micronaut HTTP client, API gateways.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example1_VersionAwareRestClient() {
        IO.println("1️⃣  Version-Aware REST API Client");
        IO.println("   Use case: Microservices, API gateways, backend-for-frontend");
        IO.println("   ────────────────────────────────────────");

        try (var client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_3)
                .connectTimeout(Duration.ofSeconds(5))
                .build()) {

            // Simulate calling a REST API endpoint
            var request = HttpRequest.newBuilder()
                .uri(URI.create("https://httpbin.org/get"))
                .header("Accept", "application/json")
                .header("X-Client-Version", "java26-http3")
                .GET()
                .build();

            IO.println("   GET " + request.uri());
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());

            IO.println("   Status: " + response.statusCode());
            IO.println("   Protocol: " + response.version());
            IO.println("   Content-Type: "
                + response.headers().firstValue("content-type").orElse("unknown"));
            IO.println("   Response body (first 200 chars): "
                + response.body().substring(0, Math.min(200, response.body().length())) + "...");
            IO.println("   ✅ REST call completed over " + response.version());
        } catch (Exception e) {
            IO.println("   ⚠️ " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 2 — Health Check Service
    // ═══════════════════════════════════════════════════════════════════════════
    //  Scenario: A health check system that monitors multiple endpoints
    //  and reports their HTTP version support alongside availability.
    //
    //  Real users: Kubernetes liveness probes, load balancer health checks,
    //              uptime monitoring (Datadog, Pingdom).
    // ═══════════════════════════════════════════════════════════════════════════
    static void example2_HealthCheckService() {
        IO.println("2️⃣  Health Check Service with Protocol Detection");
        IO.println("   Use case: K8s probes, load balancers, uptime monitoring");
        IO.println("   ────────────────────────────────────────");

        List<String> endpoints = List.of(
            "https://www.google.com",
            "https://cloudflare.com",
            "https://www.github.com"
        );

        try (var client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_3)
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()) {

            for (String endpoint : endpoints) {
                long start = System.nanoTime();
                try {
                    var request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .method("HEAD", HttpRequest.BodyPublishers.noBody())
                        .build();

                    var response = client.send(request, HttpResponse.BodyHandlers.discarding());
                    long latency = (System.nanoTime() - start) / 1_000_000;

                    String status = response.statusCode() < 400 ? "✅ UP" : "⚠️ DEGRADED";
                    IO.println("   " + status + " | " + endpoint
                        + " | " + response.version()
                        + " | " + response.statusCode()
                        + " | " + latency + "ms");
                } catch (Exception e) {
                    long latency = (System.nanoTime() - start) / 1_000_000;
                    IO.println("   ❌ DOWN | " + endpoint
                        + " | " + e.getClass().getSimpleName()
                        + " | " + latency + "ms");
                }
            }
        }
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 3 — Parallel API Aggregator (Fan-Out)
    // ═══════════════════════════════════════════════════════════════════════════
    //  Scenario: A BFF (Backend-For-Frontend) aggregates data from multiple
    //  upstream services in parallel. HTTP/3's QUIC multiplexing avoids
    //  head-of-line blocking, so one slow response doesn't stall others.
    //
    //  Real users: GraphQL resolvers, API gateways, dashboard backends.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example3_ParallelApiAggregator() {
        IO.println("3️⃣  Parallel API Aggregator (Fan-Out with QUIC Multiplexing)");
        IO.println("   Use case: GraphQL resolvers, API gateways, dashboard backends");
        IO.println("   ────────────────────────────────────────");

        // Simulate calling multiple upstream APIs
        Map<String, String> apiCalls = Map.of(
            "user-profile", "https://httpbin.org/delay/0",
            "recent-orders", "https://httpbin.org/delay/0",
            "notifications", "https://httpbin.org/delay/0",
            "recommendations", "https://httpbin.org/delay/0"
        );

        try (var client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_3)
                .connectTimeout(Duration.ofSeconds(10))
                .build()) {

            IO.println("   Fan-out: " + apiCalls.size() + " parallel requests...");
            long start = System.nanoTime();

            List<CompletableFuture<String>> futures = new ArrayList<>();
            List<String> names = new ArrayList<>();

            for (var entry : apiCalls.entrySet()) {
                names.add(entry.getKey());
                var request = HttpRequest.newBuilder()
                    .uri(URI.create(entry.getValue()))
                    .GET()
                    .build();

                futures.add(
                    client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenApply(resp -> entry.getKey()
                            + " → " + resp.version()
                            + " | " + resp.statusCode()
                            + " | " + resp.body().length() + " bytes")
                        .exceptionally(ex -> entry.getKey() + " → ⚠️ " + ex.getMessage())
                );
            }

            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            long elapsed = (System.nanoTime() - start) / 1_000_000;

            for (var future : futures) {
                IO.println("   " + future.join());
            }

            IO.println("   Total aggregation time: " + elapsed + "ms (parallel, not sequential!)");
            IO.println("   ✅ HTTP/3 multiplexing: no head-of-line blocking between streams");
        } catch (Exception e) {
            IO.println("   ⚠️ " + e.getMessage());
        }
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 4 — Protocol Version Audit
    // ═══════════════════════════════════════════════════════════════════════════
    //  Scenario: Security/compliance audit that checks which of your
    //  service endpoints support HTTP/3, HTTP/2, or are stuck on HTTP/1.1.
    //
    //  Real users: Security teams, SREs, compliance scanning tools.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example4_ProtocolVersionAudit() {
        IO.println("4️⃣  Protocol Version Audit (Compliance Scan)");
        IO.println("   Use case: Security audits, SRE tooling, compliance dashboards");
        IO.println("   ────────────────────────────────────────");

        List<String> endpoints = List.of(
            "https://www.google.com",
            "https://cloudflare.com",
            "https://www.github.com",
            "https://example.com"
        );

        Map<HttpClient.Version, AtomicInteger> versionCounts = new ConcurrentHashMap<>();
        for (var v : HttpClient.Version.values()) {
            versionCounts.put(v, new AtomicInteger(0));
        }

        try (var client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_3)
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()) {

            for (String endpoint : endpoints) {
                try {
                    var request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .method("HEAD", HttpRequest.BodyPublishers.noBody())
                        .build();

                    var response = client.send(request, HttpResponse.BodyHandlers.discarding());
                    var version = response.version();
                    versionCounts.get(version).incrementAndGet();

                    String badge = switch (version) {
                        case HTTP_3  -> "🟢 HTTP/3";
                        case HTTP_2  -> "🟡 HTTP/2";
                        case HTTP_1_1 -> "🔴 HTTP/1.1";
                    };
                    IO.println("   " + badge + " — " + endpoint);
                } catch (Exception e) {
                    IO.println("   ⚫ UNREACHABLE — " + endpoint + " (" + e.getClass().getSimpleName() + ")");
                }
            }

            IO.println("   ── Audit Summary ──");
            versionCounts.forEach((version, count) -> {
                if (count.get() > 0) {
                    IO.println("   " + version + ": " + count.get() + " endpoint(s)");
                }
            });
        }
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 5 — Resilient Downloader with Version Fallback
    // ═══════════════════════════════════════════════════════════════════════════
    //  Scenario: Download content preferring HTTP/3, but gracefully fall
    //  back through HTTP/2 → HTTP/1.1 if the preferred version fails.
    //
    //  Real users: Package managers, CI/CD artifact downloads, CDN clients.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example5_ResilientDownloader() {
        IO.println("5️⃣  Resilient Downloader with Version Fallback");
        IO.println("   Use case: Package managers, CI/CD, CDN artifact downloads");
        IO.println("   ────────────────────────────────────────");

        String url = "https://www.google.com/robots.txt";
        HttpClient.Version[] fallbackChain = {
            HttpClient.Version.HTTP_3,
            HttpClient.Version.HTTP_2,
            HttpClient.Version.HTTP_1_1
        };

        for (var version : fallbackChain) {
            IO.println("   Attempting download with " + version + "...");
            try (var client = HttpClient.newBuilder()
                    .version(version)
                    .connectTimeout(Duration.ofSeconds(3))
                    .build()) {

                var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

                var response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String body = response.body();
                    IO.println("   ✅ Downloaded via " + response.version()
                        + " (" + body.length() + " chars)");
                    IO.println("   First line: " + body.lines().findFirst().orElse("(empty)"));
                    break; // Success, no need to try next version
                }
            } catch (Exception e) {
                IO.println("   ⚠️ " + version + " failed: " + e.getClass().getSimpleName()
                    + " — trying next...");
            }
        }
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 6 — Latency Benchmark: HTTP/2 vs HTTP/3
    // ═══════════════════════════════════════════════════════════════════════════
    //  Scenario: Compare response latencies between HTTP/2 and HTTP/3 for
    //  the same endpoint, highlighting HTTP/3's faster connection setup.
    //
    //  Real users: Performance teams, capacity planning, protocol migration.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example6_LatencyBenchmark() {
        IO.println("6️⃣  Latency Benchmark: HTTP/2 vs HTTP/3");
        IO.println("   Use case: Performance testing, capacity planning, protocol migration");
        IO.println("   ────────────────────────────────────────");

        String url = "https://cloudflare.com";
        int warmupRounds = 1;
        int measureRounds = 3;

        HttpClient.Version[] versions = {
            HttpClient.Version.HTTP_2,
            HttpClient.Version.HTTP_3
        };

        for (var version : versions) {
            try (var client = HttpClient.newBuilder()
                    .version(version)
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build()) {

                var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

                // Warmup
                for (int i = 0; i < warmupRounds; i++) {
                    try {
                        client.send(request, HttpResponse.BodyHandlers.discarding());
                    } catch (Exception ignored) {}
                }

                // Measure
                long totalMs = 0;
                int successful = 0;
                HttpClient.Version actualVersion = null;

                for (int i = 0; i < measureRounds; i++) {
                    try {
                        long start = System.nanoTime();
                        var response = client.send(request, HttpResponse.BodyHandlers.discarding());
                        long elapsed = (System.nanoTime() - start) / 1_000_000;
                        totalMs += elapsed;
                        successful++;
                        actualVersion = response.version();
                    } catch (Exception e) {
                        // Skip failed rounds
                    }
                }

                if (successful > 0) {
                    long avgMs = totalMs / successful;
                    IO.println("   " + version + " (actual: " + actualVersion + ")"
                        + " → avg latency: " + avgMs + "ms"
                        + " (" + successful + "/" + measureRounds + " successful)");
                } else {
                    IO.println("   " + version + " → all " + measureRounds + " attempts failed");
                }
            }
        }

        IO.println("   Note: HTTP/3 benefits are most visible on high-latency or lossy networks");
        IO.println("   ✅ HTTP/3 (QUIC) achieves 0-RTT/1-RTT setup vs TCP's 2-3 RTT");
        IO.println();
    }
}


