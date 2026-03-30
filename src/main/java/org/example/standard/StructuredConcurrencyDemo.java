package org.example.standard;

import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.ExecutionException;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  JEP 525: Structured Concurrency (Sixth Preview)                            ║
 * ║  Status: PREVIEW in JDK 26                                                 ║
 * ║  Spec: https://openjdk.org/jeps/525                                        ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * WHAT THIS FEATURE DOES
 * ──────────────────────
 * Structured Concurrency treats groups of related concurrent tasks as a single
 * unit of work. It ensures that when a task forks subtasks:
 *
 *   1. All subtasks complete before the parent scope exits
 *   2. If one subtask fails, the others are automatically cancelled
 *   3. The thread's lifecycle is bounded by the scope's lifetime
 *
 * This is done via `StructuredTaskScope`, which manages the fork/join of
 * virtual threads.
 *
 * KEY API
 * ───────
 *   StructuredTaskScope.open()           — Opens a new scope
 *   scope.fork(Callable)                 — Forks a subtask as a virtual thread
 *   scope.join()                         — Waits for all subtasks to complete
 *   subtask.get()                        — Gets the result (or throws)
 *   StructuredTaskScope.Joiner           — Strategy for how to join subtasks:
 *     .awaitAll()                        — Wait for all, regardless of outcome
 *     .awaitAllSuccessfulOrThrow()       — Wait for all, throw on first failure
 *     .allSuccessfulOrThrow()            — Wait for all, return List of results
 *     .anySuccessfulOrThrow()            — Return first success, cancel rest
 *     .allUntil(Predicate)              — Wait until predicate matches
 *
 * WHY IT MATTERS
 * ──────────────
 * Traditional concurrent code with ExecutorService has problems:
 *   - No automatic cancellation of other tasks when one fails
 *   - No structured lifetime (threads outlive the scope)
 *   - Hard to reason about error handling
 *   - Thread dumps don't show task relationships
 *
 * Structured Concurrency fixes all of these, making concurrent code
 * as safe and readable as sequential code.
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.standard.StructuredConcurrencyDemo
 */
public class StructuredConcurrencyDemo {

    public static void main(String[] args) throws Exception {
        IO.println("╔═══════════════════════════════════════════════════╗");
        IO.println("║  JEP 525 — Structured Concurrency (Preview)      ║");
        IO.println("╚═══════════════════════════════════════════════════╝");
        IO.println();

        demoForkJoinAll();
        demoRaceForFirstResult();
        demoFailureHandling();
    }

    /**
     * DEMO 1: Fork multiple tasks and wait for ALL to complete.
     *
     * Simulates fetching data from multiple "services" concurrently.
     * All tasks run as virtual threads and are joined together.
     */
    static void demoForkJoinAll() throws Exception {
        IO.println("1️⃣  Fork/Join All — Concurrent Data Fetching");
        IO.println("   Fetching user, order, and recommendations concurrently...");

        long start = System.currentTimeMillis();

        try (var scope = StructuredTaskScope.open()) {
            // Fork three concurrent tasks (each runs in a virtual thread)
            var userTask   = scope.fork(() -> fetchUser());
            var orderTask  = scope.fork(() -> fetchOrder());
            var recsTask   = scope.fork(() -> fetchRecommendations());

            // Wait for ALL tasks to complete
            scope.join();

            // All tasks are guaranteed to be done here
            String user = userTask.get();
            String order = orderTask.get();
            String recs = recsTask.get();

            long elapsed = System.currentTimeMillis() - start;
            IO.println("   User: " + user);
            IO.println("   Order: " + order);
            IO.println("   Recommendations: " + recs);
            IO.println("   ⏱️  Total time: ~" + elapsed + "ms (concurrent, not 3× sequential!)");
        }
        IO.println();
    }

    /**
     * DEMO 2: Race for the first successful result.
     *
     * Uses anySuccessfulResultOrThrow() to return as soon as one task
     * succeeds, automatically cancelling the remaining tasks.
     *
     * Useful for: querying multiple mirrors, failover endpoints, etc.
     */
    static void demoRaceForFirstResult() throws Exception {
        IO.println("2️⃣  Race — First Successful Result Wins");
        IO.println("   Querying 3 'mirror servers' concurrently...");

        long start = System.currentTimeMillis();

        try (var scope = StructuredTaskScope.open(
                StructuredTaskScope.Joiner.<String>anySuccessfulOrThrow())) {

            scope.fork(() -> queryMirror("US-East",  200));
            scope.fork(() -> queryMirror("EU-West",  100));  // Fastest!
            scope.fork(() -> queryMirror("AP-South", 300));

            // Returns as soon as the first task succeeds
            String result = scope.join();

            long elapsed = System.currentTimeMillis() - start;
            IO.println("   Winner: " + result);
            IO.println("   ⏱️  Time: ~" + elapsed + "ms (fastest mirror only)");
        }
        IO.println();
    }

    /**
     * DEMO 3: Failure handling — one task fails, others are cancelled.
     *
     * Uses awaitAllSuccessfulOrThrow() to demonstrate that when one subtask
     * throws an exception, the scope automatically cancels remaining tasks
     * and propagates the error.
     */
    static void demoFailureHandling() {
        IO.println("3️⃣  Failure Handling — Automatic Cancellation");
        IO.println("   Forking 3 tasks, one will fail...");

        try (var scope = StructuredTaskScope.open(
                StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow())) {

            scope.fork(() -> { Thread.sleep(100); return "Task A done"; });
            scope.fork(() -> { Thread.sleep(50);  throw new RuntimeException("Task B failed!"); });
            scope.fork(() -> { Thread.sleep(200); return "Task C done"; });

            scope.join();
            IO.println("   ❌ Should not reach here!");

        } catch (Exception e) {
            IO.println("   ✅ Caught failure: " + e.getMessage());
            IO.println("   All other tasks were automatically cancelled.");
        }
        IO.println();
    }

    // ─── Simulated service calls ───

    static String fetchUser() throws InterruptedException {
        Thread.sleep(150);
        return "User{id=42, name='Alice'}";
    }

    static String fetchOrder() throws InterruptedException {
        Thread.sleep(200);
        return "Order{id=1001, total=$59.99}";
    }

    static String fetchRecommendations() throws InterruptedException {
        Thread.sleep(100);
        return "Recommendations[Java 26 in Action, Effective Java, Modern Concurrency]";
    }

    static String queryMirror(String region, long latencyMs) throws InterruptedException {
        Thread.sleep(latencyMs);
        return "Data from " + region + " (latency: " + latencyMs + "ms)";
    }
}



