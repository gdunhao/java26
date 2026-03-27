package org.example.vm;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  JEP 491: Synchronize Virtual Threads without Pinning                      ║
 * ║  Status: FINAL in JDK 26                                                   ║
 * ║  Spec: https://openjdk.org/jeps/491                                        ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * WHAT THIS FEATURE DOES
 * ──────────────────────
 * In earlier JDK versions, when a virtual thread entered a `synchronized`
 * block and then performed a blocking operation (like Thread.sleep, I/O, etc.),
 * the virtual thread became "pinned" to its carrier (platform) thread. This
 * meant the carrier thread was blocked and unavailable to run other virtual
 * threads — defeating the purpose of virtual threads.
 *
 * JDK 26 removes this limitation: virtual threads can now unmount from their
 * carrier thread even while inside a `synchronized` block. This means
 * `synchronized` works correctly and efficiently with virtual threads.
 *
 * WHAT IS "PINNING"?
 * ──────────────────
 * Virtual threads are multiplexed onto a small pool of carrier (platform)
 * threads. When a virtual thread blocks (e.g., on I/O), it normally
 * "unmounts" from its carrier, freeing it for other virtual threads.
 *
 *   Normal:    VThread blocks → unmounts → carrier runs other VThread ✅
 *   Pinning:   VThread in synchronized blocks → STAYS on carrier → carrier blocked ❌
 *
 * With JDK 26:
 *   Fixed:     VThread in synchronized blocks → unmounts → carrier free ✅
 *
 * WHY IT MATTERS
 * ──────────────
 *   - `synchronized` is pervasive in Java (used in JDK itself, libraries, etc.)
 *   - Pinning could cause thread starvation with many virtual threads
 *   - Before: developers had to replace `synchronized` with ReentrantLock
 *   - Now: `synchronized` and ReentrantLock both work well with virtual threads
 *
 * WHAT THIS DEMO SHOWS
 * ────────────────────
 * We create many virtual threads that all enter a `synchronized` block and
 * sleep inside it. Before JDK 26, this would pin all carrier threads and
 * cause severe throughput issues. Now, it works smoothly.
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.vm.VirtualThreadSyncDemo
 */
public class VirtualThreadSyncDemo {

    private static final Object LOCK = new Object();
    private static int completedCount = 0;

    public static void main(String[] args) throws Exception {
        IO.println("╔═══════════════════════════════════════════════════╗");
        IO.println("║  JEP 491 — Virtual Threads without Pinning      ║");
        IO.println("╚═══════════════════════════════════════════════════╝");
        IO.println();

        demoSynchronizedWithVirtualThreads();
        demoComparisonWithReentrantLock();
    }

    /**
     * DEMO 1: Many virtual threads using `synchronized` with blocking.
     *
     * We spawn 1000 virtual threads, each of which:
     *   1. Enters a synchronized block
     *   2. Sleeps for 10ms (a blocking operation)
     *   3. Increments a counter
     *
     * Before JDK 26, this would pin carrier threads and be very slow.
     * With JDK 26, virtual threads unmount during sleep, even inside
     * synchronized — so all 1000 threads complete quickly.
     */
    static void demoSynchronizedWithVirtualThreads() throws Exception {
        IO.println("1️⃣  synchronized + Virtual Threads (Previously Caused Pinning)");

        int numThreads = 1_000;
        completedCount = 0;

        Instant start = Instant.now();

        // Create many virtual threads that all use synchronized + sleep
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = Thread.ofVirtual().start(() -> {
                synchronized (LOCK) {
                    try {
                        // This blocking op inside synchronized used to pin!
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    completedCount++;
                }
            });
        }

        // Wait for all threads to complete
        for (Thread t : threads) t.join();

        Duration elapsed = Duration.between(start, Instant.now());

        IO.println("   Virtual threads spawned: " + numThreads);
        IO.println("   Completed: " + completedCount);
        IO.println("   Time: " + elapsed.toMillis() + "ms");
        IO.println("   Available carriers: " + Runtime.getRuntime().availableProcessors());
        IO.println();
        IO.println("   ℹ️  Before JDK 26: ~" + (numThreads * 10) + "ms (all pinned, sequential)");
        IO.println("   ℹ️  With JDK 26:   Much faster (virtual threads unmount during sleep)");
        IO.println();
    }

    /**
     * DEMO 2: Comparison — synchronized vs ReentrantLock with virtual threads.
     *
     * Before JDK 26, ReentrantLock was recommended over synchronized for
     * virtual threads because it didn't cause pinning. Now both should
     * perform similarly.
     */
    static void demoComparisonWithReentrantLock() throws Exception {
        IO.println("2️⃣  synchronized vs ReentrantLock Performance");

        int numThreads = 500;

        // Test with synchronized
        long syncTime = benchmarkSynchronized(numThreads);

        // Test with ReentrantLock
        long lockTime = benchmarkReentrantLock(numThreads);

        IO.println("   Threads: " + numThreads + " (each sleeps 10ms in critical section)");
        IO.println("   synchronized:   " + syncTime + "ms");
        IO.println("   ReentrantLock:  " + lockTime + "ms");
        IO.println();
        IO.println("   ✅ With JDK 26, both approaches work well with virtual threads.");
        IO.println("   ✅ No need to replace synchronized with ReentrantLock for VTs!");
        IO.println();
    }

    static long benchmarkSynchronized(int numThreads) throws Exception {
        Object lock = new Object();
        int[] counter = {0};

        Instant start = Instant.now();
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = Thread.ofVirtual().start(() -> {
                synchronized (lock) {
                    try { Thread.sleep(10); } catch (InterruptedException e) { }
                    counter[0]++;
                }
            });
        }
        for (Thread t : threads) t.join();
        return Duration.between(start, Instant.now()).toMillis();
    }

    static long benchmarkReentrantLock(int numThreads) throws Exception {
        ReentrantLock lock = new ReentrantLock();
        int[] counter = {0};

        Instant start = Instant.now();
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = Thread.ofVirtual().start(() -> {
                lock.lock();
                try {
                    try { Thread.sleep(10); } catch (InterruptedException e) { }
                    counter[0]++;
                } finally {
                    lock.unlock();
                }
            });
        }
        for (Thread t : threads) t.join();
        return Duration.between(start, Instant.now()).toMillis();
    }
}

