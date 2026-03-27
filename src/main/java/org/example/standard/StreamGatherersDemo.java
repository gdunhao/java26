package org.example.standard;

import java.util.List;
import java.util.stream.Gatherer;
import java.util.stream.Gatherers;
import java.util.stream.Stream;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  JEP 485: Stream Gatherers                                                 ║
 * ║  Status: FINAL in JDK 26                                                   ║
 * ║  Spec: https://openjdk.org/jeps/485                                        ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * WHAT THIS FEATURE DOES
 * ──────────────────────
 * Stream Gatherers enhance the Stream API by allowing you to define CUSTOM
 * intermediate operations, similar to how Collectors allow custom terminal
 * operations. The new `Stream.gather(Gatherer)` method accepts a `Gatherer`
 * that can transform stream elements in ways not possible with the built-in
 * `map`, `filter`, `flatMap`, etc.
 *
 * THE GATHERER INTERFACE
 * ──────────────────────
 * A Gatherer<T, A, R> has four components:
 *
 *   1. initializer()  — Creates the mutable state (type A)
 *   2. integrator()   — Processes each input element T, optionally pushing
 *                        output elements R downstream
 *   3. combiner()     — Merges two states for parallel streams (optional)
 *   4. finisher()     — Called after all elements are processed, can push
 *                        final elements downstream (optional)
 *
 * BUILT-IN GATHERERS (java.util.stream.Gatherers)
 * ────────────────────────────────────────────────
 * JDK 26 ships with several ready-to-use gatherers:
 *
 *   - Gatherers.fold(initial, folder)          — Reduces to a single value
 *   - Gatherers.scan(initial, scanner)         — Running accumulation
 *   - Gatherers.windowFixed(size)              — Non-overlapping windows
 *   - Gatherers.windowSliding(size)            — Overlapping sliding windows
 *   - Gatherers.mapConcurrent(maxConcurrency, mapper) — Concurrent mapping
 *
 * WHY IT MATTERS
 * ──────────────
 * Before Gatherers, you had to:
 *   - Break the stream pipeline and collect to a list for custom logic
 *   - Write complex flatMap hacks for windowing
 *   - Use external libraries for running totals, deduplication, etc.
 *
 * Now you can keep a clean, fluent pipeline while expressing any intermediate
 * transformation.
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.standard.StreamGatherersDemo
 */
public class StreamGatherersDemo {

    public static void main(String[] args) {
        IO.println("╔═══════════════════════════════════════════════════╗");
        IO.println("║  JEP 485 — Stream Gatherers                     ║");
        IO.println("╚═══════════════════════════════════════════════════╝");
        IO.println();

        demoFixedWindow();
        demoSlidingWindow();
        demoScan();
        demoFold();
        demoCustomGatherer();
    }

    /**
     * DEMO 1: Fixed-size windows (non-overlapping).
     *
     * Gatherers.windowFixed(n) groups elements into lists of size n.
     * The last window may be smaller if elements don't divide evenly.
     *
     * Input:  [1, 2, 3, 4, 5, 6, 7]
     * Output: [[1,2,3], [4,5,6], [7]]
     */
    static void demoFixedWindow() {
        IO.println("1️⃣  Fixed-Size Windows (Gatherers.windowFixed)");
        IO.println("   Input: [1, 2, 3, 4, 5, 6, 7]");

        var windows = Stream.of(1, 2, 3, 4, 5, 6, 7)
            .gather(Gatherers.windowFixed(3))
            .toList();

        IO.println("   Windows of size 3: " + windows);
        IO.println();
    }

    /**
     * DEMO 2: Sliding windows (overlapping).
     *
     * Gatherers.windowSliding(n) produces windows that advance by one element.
     * Useful for moving averages, pattern detection, n-grams, etc.
     *
     * Input:  [1, 2, 3, 4, 5]
     * Output: [[1,2,3], [2,3,4], [3,4,5]]
     */
    static void demoSlidingWindow() {
        IO.println("2️⃣  Sliding Windows (Gatherers.windowSliding)");
        IO.println("   Input: [1, 2, 3, 4, 5]");

        var windows = Stream.of(1, 2, 3, 4, 5)
            .gather(Gatherers.windowSliding(3))
            .toList();

        IO.println("   Sliding windows of size 3: " + windows);

        // Practical use: compute moving average
        IO.println("   Moving averages: " +
            Stream.of(10.0, 20.0, 30.0, 40.0, 50.0)
                .gather(Gatherers.windowSliding(3))
                .map(win -> win.stream().mapToDouble(d -> d).average().orElse(0))
                .toList());
        IO.println();
    }

    /**
     * DEMO 3: Scan (running accumulation).
     *
     * Gatherers.scan(identity, accumulator) emits the accumulated value
     * after each element — like a "prefix sum" or "running total".
     *
     * Input:  [1, 2, 3, 4, 5]
     * Output: [1, 3, 6, 10, 15]  (running sum)
     */
    static void demoScan() {
        IO.println("3️⃣  Scan — Running Accumulation (Gatherers.scan)");
        IO.println("   Input: [1, 2, 3, 4, 5]");

        var runningSums = Stream.of(1, 2, 3, 4, 5)
            .gather(Gatherers.scan(() -> 0, Integer::sum))
            .toList();

        IO.println("   Running sums: " + runningSums);

        // Running product
        var runningProducts = Stream.of(1, 2, 3, 4, 5)
            .gather(Gatherers.scan(() -> 1, (a, b) -> a * b))
            .toList();
        IO.println("   Running products: " + runningProducts);
        IO.println();
    }

    /**
     * DEMO 4: Fold (reduce to a single value, emitted at end).
     *
     * Gatherers.fold(identity, folder) is like a terminal reduce, but
     * returns a stream of 0 or 1 elements — keeping you in the stream
     * pipeline for further processing.
     *
     * Input:  [1, 2, 3, 4, 5]
     * Output: [15]  (sum)
     */
    static void demoFold() {
        IO.println("4️⃣  Fold — Reduce in the Pipeline (Gatherers.fold)");
        IO.println("   Input: [1, 2, 3, 4, 5]");

        var folded = Stream.of(1, 2, 3, 4, 5)
            .gather(Gatherers.fold(() -> 0, Integer::sum))
            .toList();

        IO.println("   Folded (sum): " + folded);

        // Fold to concatenate strings
        var concatenated = Stream.of("Hello", " ", "Stream", " ", "Gatherers!")
            .gather(Gatherers.fold(() -> "", String::concat))
            .toList();
        IO.println("   Folded (concat): " + concatenated);
        IO.println();
    }

    /**
     * DEMO 5: Custom Gatherer — Distinct-By.
     *
     * This custom gatherer deduplicates elements based on a key extractor.
     * For example, keep only the first person with each name length.
     *
     * This demonstrates writing your own Gatherer from scratch using
     * Gatherer.ofSequential().
     */
    static void demoCustomGatherer() {
        IO.println("5️⃣  Custom Gatherer — Distinct by String Length");

        List<String> names = List.of("Ana", "Bob", "Carl", "Dan", "Eve", "Frank", "Gus");
        IO.println("   Input: " + names);

        var distinctByLength = names.stream()
            .gather(distinctBy(String::length))
            .toList();

        IO.println("   Distinct by length: " + distinctByLength);
        IO.println("   (Only the first name of each length is kept)");
        IO.println();
    }

    /**
     * Creates a custom Gatherer that keeps only the first element for each
     * unique key, as determined by the provided keyExtractor function.
     *
     * This is a "stateful many-to-fewer" gatherer:
     *   - State: a Set of seen keys
     *   - Integrator: if key is new, push element downstream; else skip
     *   - No finisher needed (all work done in integrator)
     */
    static <T, K> Gatherer<T, ?, T> distinctBy(java.util.function.Function<T, K> keyExtractor) {
        return Gatherer.ofSequential(
            // initializer: create the mutable state (a HashSet of seen keys)
            java.util.HashSet<K>::new,

            // integrator: for each element, check if its key is new
            (seenKeys, element, downstream) -> {
                K key = keyExtractor.apply(element);
                if (seenKeys.add(key)) {
                    // Key was new — push element downstream
                    return downstream.push(element);
                }
                // Key already seen — skip this element, continue processing
                return true;
            }
        );
    }
}

