package org.example.standard;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  JEP 506: Scoped Values                                                    ║
 * ║  Status: FINAL in JDK 26                                                   ║
 * ║  Spec: https://openjdk.org/jeps/506                                        ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * WHAT THIS FEATURE DOES
 * ──────────────────────
 * Scoped Values provide a way to share IMMUTABLE data within a thread and its
 * child threads, without passing that data as method parameters. They are the
 * modern, thread-safe replacement for `ThreadLocal`.
 *
 * KEY API
 * ───────
 *   ScopedValue.newInstance()           — Create a new scoped value
 *   ScopedValue.where(sv, value)       — Bind a value in a scope
 *                .run(Runnable)         — Execute with that binding
 *                .call(Callable)        — Execute and return a result
 *   sv.get()                           — Read the current binding
 *   sv.isBound()                       — Check if bound in this scope
 *
 * WHY NOT ThreadLocal?
 * ────────────────────
 * ThreadLocal has several problems:
 *
 *   1. MUTABLE — Any code can call set() at any time, making data flow hard
 *      to reason about.
 *
 *   2. UNBOUNDED LIFETIME — Values persist until explicitly removed. Forgetting
 *      to call remove() causes memory leaks, especially with thread pools.
 *
 *   3. EXPENSIVE WITH VIRTUAL THREADS — Each virtual thread gets its own copy.
 *      With millions of virtual threads, this wastes a lot of memory.
 *
 *   4. INHERITANCE IS COPYING — InheritableThreadLocal copies values to child
 *      threads, which is expensive and can cause stale data issues.
 *
 * ScopedValue solves all of these:
 *   ✅ Immutable — once bound, the value cannot be changed in that scope
 *   ✅ Bounded lifetime — automatically unbound when the scope exits
 *   ✅ Cheap with virtual threads — sharing, not copying
 *   ✅ Safe inheritance — child threads see the parent's binding naturally
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.standard.ScopedValuesDemo
 */
public class ScopedValuesDemo {

    // ─── Define scoped values (like declaring ThreadLocals, but better) ───
    private static final ScopedValue<String> CURRENT_USER = ScopedValue.newInstance();
    private static final ScopedValue<String> REQUEST_ID   = ScopedValue.newInstance();
    private static final ScopedValue<String> TENANT       = ScopedValue.newInstance();

    public static void main(String[] args) throws Exception {
        IO.println("╔═══════════════════════════════════════════════════╗");
        IO.println("║  JEP 506 — Scoped Values                        ║");
        IO.println("╚═══════════════════════════════════════════════════╝");
        IO.println();

        demoBasicBinding();
        demoNestedRebinding();
        demoMultipleBindings();
        demoWithStructuredConcurrency();
    }

    /**
     * DEMO 1: Basic binding — set a value, then read it deep in the call stack.
     *
     * The scoped value is bound at the top and automatically available to all
     * methods called within that scope — no parameter passing needed.
     */
    static void demoBasicBinding() {
        IO.println("1️⃣  Basic Binding");

        ScopedValue.where(CURRENT_USER, "alice")
            .run(() -> {
                IO.println("   Bound CURRENT_USER = 'alice'");
                handleRequest();  // Deep in the call stack, we can read it
            });

        IO.println("   After scope: isBound = " + CURRENT_USER.isBound());
        IO.println();
    }

    static void handleRequest() {
        // No parameter needed — the scoped value is accessible here
        processData();
    }

    static void processData() {
        logAccess();
    }

    static void logAccess() {
        // Reading the scoped value deep in the call chain
        IO.println("   [logAccess] Current user: " + CURRENT_USER.get());
    }

    /**
     * DEMO 2: Nested rebinding — a scoped value can be temporarily rebound.
     *
     * The inner binding shadows the outer one for the duration of its scope.
     * When the inner scope exits, the outer binding is restored.
     */
    static void demoNestedRebinding() {
        IO.println("2️⃣  Nested Rebinding (Shadowing)");

        ScopedValue.where(CURRENT_USER, "alice")
            .run(() -> {
                IO.println("   Outer scope: " + CURRENT_USER.get());

                // Temporarily rebind for an inner operation (e.g., "sudo")
                ScopedValue.where(CURRENT_USER, "admin")
                    .run(() -> {
                        IO.println("   Inner scope (elevated): " + CURRENT_USER.get());
                    });

                // Back to the outer binding
                IO.println("   Back to outer scope: " + CURRENT_USER.get());
            });
        IO.println();
    }

    /**
     * DEMO 3: Binding multiple scoped values at once.
     *
     * You can chain .where() calls to bind several values together.
     */
    static void demoMultipleBindings() {
        IO.println("3️⃣  Multiple Bindings");

        ScopedValue.where(CURRENT_USER, "bob")
                   .where(REQUEST_ID, "req-42")
                   .where(TENANT, "acme-corp")
                   .run(() -> {
                       IO.println("   User:    " + CURRENT_USER.get());
                       IO.println("   Request: " + REQUEST_ID.get());
                       IO.println("   Tenant:  " + TENANT.get());
                   });
        IO.println();
    }

    /**
     * DEMO 4: Scoped Values with Structured Concurrency.
     *
     * When you fork virtual threads inside a StructuredTaskScope, they
     * automatically inherit the parent's scoped value bindings. This is
     * much cheaper than InheritableThreadLocal (sharing, not copying).
     */
    static void demoWithStructuredConcurrency() throws Exception {
        IO.println("4️⃣  Scoped Values + Structured Concurrency");

        ScopedValue.where(CURRENT_USER, "charlie")
                   .where(REQUEST_ID, "req-99")
                   .run(() -> {
                       IO.println("   Parent thread: user=" + CURRENT_USER.get()
                           + ", reqId=" + REQUEST_ID.get());

                       try (var scope = java.util.concurrent.StructuredTaskScope.open()) {
                           var task1 = scope.fork(() -> {
                               // Child virtual thread inherits scoped values!
                               return "Task1 sees user=" + CURRENT_USER.get();
                           });
                           var task2 = scope.fork(() -> {
                               return "Task2 sees reqId=" + REQUEST_ID.get();
                           });

                           scope.join();
                           IO.println("   " + task1.get());
                           IO.println("   " + task2.get());
                       } catch (Exception e) {
                           IO.println("   Error: " + e.getMessage());
                       }
                   });
        IO.println();
    }
}

