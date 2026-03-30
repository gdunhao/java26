package org.example.standard;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  JEP 517: HTTP/3 for the HTTP Client API                                   ║
 * ║  Status: FINAL in JDK 26                                                   ║
 * ║  Spec: https://openjdk.org/jeps/517                                        ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * WHAT THIS FEATURE DOES
 * ──────────────────────
 * The built-in `java.net.http.HttpClient` now supports HTTP/3 (RFC 9114),
 * which runs over QUIC (RFC 9000) instead of TCP. HTTP/3 is the third major
 * version of the Hypertext Transfer Protocol and provides significant
 * performance improvements over HTTP/2 and HTTP/1.1.
 *
 * HTTP/3 KEY IMPROVEMENTS
 * ───────────────────────
 *   - NO HEAD-OF-LINE BLOCKING — Each stream is independently delivered
 *     over QUIC, so a lost packet on one stream doesn't block others.
 *   - FASTER CONNECTION SETUP — QUIC combines TLS and transport handshakes,
 *     achieving 0-RTT or 1-RTT connection establishment.
 *   - CONNECTION MIGRATION — Connections survive network changes (e.g.,
 *     switching from Wi-Fi to cellular) without re-establishing.
 *   - BUILT-IN ENCRYPTION — QUIC mandates TLS 1.3; all data is encrypted.
 *
 * HOW IT WORKS IN JAVA
 * ────────────────────
 *   The HttpClient transparently upgrades to HTTP/3 when the server
 *   advertises support (via the Alt-Svc header or DNS HTTPS records).
 *   You can also explicitly request HTTP/3 via:
 *
 *     HttpClient.newBuilder()
 *         .version(HttpClient.Version.HTTP_3)
 *         .build();
 *
 *   The response's `version()` method tells you which protocol was used:
 *     response.version() → HttpClient.Version.HTTP_3
 *
 * WHY IT MATTERS
 * ──────────────
 * HTTP/3 is already used by ~30% of all web traffic (Google, Cloudflare,
 * Meta, etc.). Until now, Java developers needed third-party libraries
 * (Netty, Jetty) for HTTP/3 support. JDK 26 makes it a built-in,
 * zero-dependency feature.
 *
 * NOTE
 * ────
 * This demo requires network access. If no HTTP/3 server is reachable,
 * the demo will gracefully fall back and show the HTTP version actually used.
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.standard.Http3Demo
 */
public class Http3Demo {

    public static void main(String[] args) throws Exception {
        IO.println("╔═══════════════════════════════════════════════════╗");
        IO.println("║  JEP 517 — HTTP/3 for the HTTP Client API       ║");
        IO.println("╚═══════════════════════════════════════════════════╝");
        IO.println();

        demoVersionEnumeration();
        demoBasicHttp3Request();
        demoVersionNegotiation();
        demoAsyncHttp3Requests();
        demoConcurrentMultiplexedRequests();
    }

    /**
     * DEMO 1: Show the available HTTP version constants.
     *
     * JDK 26 adds HttpClient.Version.HTTP_3 alongside the existing
     * HTTP_1_1 and HTTP_2 constants.
     */
    static void demoVersionEnumeration() {
        IO.println("1️⃣  HTTP Version Constants");
        IO.println("   ────────────────────────────────────────");

        for (HttpClient.Version version : HttpClient.Version.values()) {
            IO.println("   Available: " + version);
        }
        IO.println("   ✅ HTTP_3 is now a first-class version in java.net.http");
        IO.println();
    }

    /**
     * DEMO 2: Basic HTTP/3 GET request.
     *
     * Creates an HttpClient configured for HTTP/3 and makes a simple GET.
     * The client will use HTTP/3 if the server supports it, otherwise
     * falls back to HTTP/2 or HTTP/1.1.
     */
    static void demoBasicHttp3Request() {
        IO.println("2️⃣  Basic HTTP/3 GET Request");
        IO.println("   ────────────────────────────────────────");

        try (var client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_3)
                .connectTimeout(Duration.ofSeconds(5))
                .build()) {

            var request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.google.com"))
                .GET()
                .build();

            IO.println("   Requesting: " + request.uri());
            IO.println("   Preferred version: HTTP/3");

            var response = client.send(request, HttpResponse.BodyHandlers.ofString());

            IO.println("   Status: " + response.statusCode());
            IO.println("   Protocol used: " + response.version());
            IO.println("   Body length: " + response.body().length() + " chars");
            IO.println("   ✅ Request completed over " + response.version());
        } catch (Exception e) {
            IO.println("   ⚠️ Could not connect: " + e.getMessage());
            IO.println("   (HTTP/3 requires network access and QUIC-capable server)");
        }
        IO.println();
    }

    /**
     * DEMO 3: Version negotiation comparison.
     *
     * Makes the same request with different preferred versions and
     * shows which protocol is actually negotiated each time.
     */
    static void demoVersionNegotiation() {
        IO.println("3️⃣  Version Negotiation Comparison");
        IO.println("   ────────────────────────────────────────");

        HttpClient.Version[] versions = {
            HttpClient.Version.HTTP_1_1,
            HttpClient.Version.HTTP_2,
            HttpClient.Version.HTTP_3
        };

        for (var preferredVersion : versions) {
            try (var client = HttpClient.newBuilder()
                    .version(preferredVersion)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build()) {

                var request = HttpRequest.newBuilder()
                    .uri(URI.create("https://cloudflare.com"))
                    .GET()
                    .build();

                var response = client.send(request, HttpResponse.BodyHandlers.discarding());

                IO.println("   Preferred: " + preferredVersion
                    + " → Actual: " + response.version()
                    + " (status: " + response.statusCode() + ")");
            } catch (Exception e) {
                IO.println("   Preferred: " + preferredVersion
                    + " → ⚠️ Failed: " + e.getClass().getSimpleName());
            }
        }
        IO.println();
    }

    /**
     * DEMO 4: Asynchronous HTTP/3 requests.
     *
     * HTTP/3 works seamlessly with the async API (sendAsync).
     * QUIC's multiplexing means multiple concurrent requests over
     * a single connection without head-of-line blocking.
     */
    static void demoAsyncHttp3Requests() {
        IO.println("4️⃣  Asynchronous HTTP/3 Requests");
        IO.println("   ────────────────────────────────────────");

        try (var client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_3)
                .connectTimeout(Duration.ofSeconds(5))
                .build()) {

            var request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.google.com"))
                .GET()
                .build();

            IO.println("   Sending async request over HTTP/3...");

            CompletableFuture<HttpResponse<String>> future =
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString());

            // Non-blocking: we can do other work here
            IO.println("   (doing other work while waiting...)");

            HttpResponse<String> response = future.join();

            IO.println("   Status: " + response.statusCode());
            IO.println("   Protocol: " + response.version());
            IO.println("   Body: " + response.body().length() + " chars");
            IO.println("   ✅ Async HTTP/3 request completed");
        } catch (Exception e) {
            IO.println("   ⚠️ Could not connect: " + e.getMessage());
        }
        IO.println();
    }

    /**
     * DEMO 5: Concurrent multiplexed requests.
     *
     * HTTP/3's QUIC transport eliminates head-of-line blocking,
     * so multiple concurrent requests over the same connection
     * don't interfere with each other. This demo fires multiple
     * requests simultaneously and tracks their completion.
     */
    static void demoConcurrentMultiplexedRequests() {
        IO.println("5️⃣  Concurrent Multiplexed Requests (No Head-of-Line Blocking)");
        IO.println("   ────────────────────────────────────────");

        try (var client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_3)
                .connectTimeout(Duration.ofSeconds(5))
                .build()) {

            List<String> urls = List.of(
                "https://www.google.com",
                "https://www.google.com/search?q=java",
                "https://www.google.com/search?q=quic",
                "https://www.google.com/search?q=http3"
            );

            IO.println("   Sending " + urls.size() + " concurrent requests...");
            long start = System.nanoTime();

            List<CompletableFuture<HttpResponse<String>>> futures = urls.stream()
                .map(url -> HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build())
                .map(req -> client.sendAsync(req, HttpResponse.BodyHandlers.ofString()))
                .toList();

            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            long elapsed = (System.nanoTime() - start) / 1_000_000;

            for (int i = 0; i < futures.size(); i++) {
                var resp = futures.get(i).join();
                IO.println("   [" + (i + 1) + "] " + urls.get(i));
                IO.println("       → " + resp.version() + " | "
                    + resp.statusCode() + " | "
                    + resp.body().length() + " chars");
            }

            IO.println("   Total time: " + elapsed + " ms (all concurrent, QUIC-multiplexed)");
            IO.println("   ✅ With HTTP/3, a stalled stream does NOT block others");
        } catch (Exception e) {
            IO.println("   ⚠️ Could not connect: " + e.getMessage());
        }
        IO.println();
    }
}

