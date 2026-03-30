package org.example.standard;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  Prepare to Make Final Mean Final — Real-World Use Cases                    ║
 * ║  Patterns that WILL break in future JDKs and how to migrate NOW             ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * REFERENCES
 * ──────────
 *   • JEP 500 — Prepare to Make Final Mean Final:
 *       https://openjdk.org/jeps/500
 *   • JEP 416 — Reimplement Core Reflection with Method Handles:
 *       https://openjdk.org/jeps/416
 *
 * PATTERNS DEMONSTRATED
 * ─────────────────────
 *   1. JSON deserializer      — Setting final fields during deserialization (Jackson/Gson pattern)
 *   2. Singleton reset        — Test utility that resets a final singleton instance
 *   3. Dependency injection    — Framework injecting into final fields (Spring @Autowired pattern)
 *   4. Configuration override  — Overriding final config constants for testing
 *   5. Immutable DTO copy     — "withX()" copy methods without reflection
 *   6. Cache warm-up          — Lazy init of final-looking cache fields
 *
 * Each example shows the OLD way (reflection on final fields → now warns)
 * and the NEW recommended way (future-proof, no reflection needed).
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.standard.FinalFieldWarningsRealWorldExamples
 */
public class FinalFieldWarningsRealWorldExamples {

    public static void main(String[] args) throws Exception {
        IO.println("╔═══════════════════════════════════════════════════════╗");
        IO.println("║  Final Mean Final — Real-World Use Cases             ║");
        IO.println("╚═══════════════════════════════════════════════════════╝");
        IO.println();

        example1_JsonDeserializer();
        example2_SingletonResetForTesting();
        example3_DependencyInjection();
        example4_ConfigOverrideForTesting();
        example5_ImmutableDtoCopyPattern();
        example6_LazyCacheInitialization();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 1 — JSON Deserializer Setting Final Fields
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: A JSON library (like Jackson or Gson) deserializes JSON into
    //  an object with final fields. The library uses reflection to set them.
    //
    //  OLD WAY: Reflection on final fields (now warns in JDK 26).
    //  NEW WAY: Use a constructor, builder, or factory method.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example1_JsonDeserializer() throws Exception {
        IO.println("1️⃣  JSON Deserializer — Setting Final Fields");
        IO.println("   Use case: Jackson, Gson, Moshi, any JSON/XML mapper");
        IO.println("   ────────────────────────────────────────");

        // --- OLD WAY: Reflection on final fields (WARNS in JDK 26) ---
        IO.println("   ❌ OLD WAY (reflection on final fields):");
        var user = new ImmutableUser("unknown", -1);
        Field nameField = ImmutableUser.class.getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(user, "Alice");  // ← JDK 26 WARNING on stderr
        Field ageField = ImmutableUser.class.getDeclaredField("age");
        ageField.setAccessible(true);
        ageField.setInt(user, 30);     // ← JDK 26 WARNING on stderr
        IO.println("      Deserialized (reflection): " + user);
        IO.println("      ⚠️  Two warnings emitted — will break in future JDK!");
        IO.println();

        // --- NEW WAY: Constructor-based deserialization ---
        IO.println("   ✅ NEW WAY (constructor-based deserialization):");
        // Frameworks should use constructor parameters (Jackson: @JsonCreator)
        var user2 = deserializeViaConstructor("Bob", 25);
        IO.println("      Deserialized (constructor): " + user2);
        IO.println("      No warnings, no reflection on final fields.");
        IO.println();
    }

    static class ImmutableUser {
        private final String name;
        private final int age;

        ImmutableUser(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public String toString() {
            return "ImmutableUser[name=" + name + ", age=" + age + "]";
        }
    }

    /** Simulates constructor-based deserialization (no reflection on final fields). */
    static ImmutableUser deserializeViaConstructor(String name, int age) {
        return new ImmutableUser(name, age);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 2 — Singleton Reset for Testing
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: A test needs to reset a singleton's final INSTANCE field
    //  between test runs to get a clean state.
    //
    //  OLD WAY: Reflection to null out the final static field.
    //  NEW WAY: Use a resettable holder or dependency injection.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example2_SingletonResetForTesting() throws Exception {
        IO.println("2️⃣  Singleton Reset for Testing");
        IO.println("   Use case: JUnit/TestNG tests, integration tests, test isolation");
        IO.println("   ────────────────────────────────────────");

        // --- OLD WAY: Reset singleton via reflection ---
        IO.println("   ❌ OLD WAY (reflection to reset final singleton):");
        IO.println("      DatabasePool.getInstance() = " + DatabasePool.getInstance());

        Field instanceField = DatabasePool.class.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        IO.println("      ⚠️  Attempting to set final static INSTANCE to null...");
        try {
            instanceField.set(null, null);  // ← JDK 26 WARNING
            IO.println("      Reset succeeded (with warning). INSTANCE = " + DatabasePool.getInstance());
        } catch (IllegalAccessException e) {
            IO.println("      ✅ Blocked: " + e.getMessage());
        }
        IO.println();

        // --- NEW WAY: Resettable holder ---
        IO.println("   ✅ NEW WAY (resettable holder pattern):");
        IO.println("      ResettableService.get() = " + ResettableService.get());
        ResettableService.resetForTesting();
        IO.println("      After reset:               " + ResettableService.get());
        IO.println("      No reflection needed — explicit reset method for tests.");
        IO.println();
    }

    static class DatabasePool {
        private static final DatabasePool INSTANCE = new DatabasePool();
        private final String id = "pool-" + System.identityHashCode(this);

        static DatabasePool getInstance() { return INSTANCE; }

        @Override
        public String toString() { return "DatabasePool(" + id + ")"; }
    }

    /** A testable service with an explicit reset mechanism (no reflection). */
    static class ResettableService {
        private static volatile ResettableService instance = new ResettableService();
        private final String id = "svc-" + System.identityHashCode(this);

        static ResettableService get() {
            if (instance == null) instance = new ResettableService();
            return instance;
        }

        /** Called by tests only — resets the service to a fresh instance. */
        static void resetForTesting() { instance = null; }

        @Override
        public String toString() { return "ResettableService(" + id + ")"; }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 3 — Dependency Injection into Final Fields
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: A DI framework (like Spring) injects dependencies into
    //  @Autowired final fields via reflection.
    //
    //  OLD WAY: Field injection on final fields.
    //  NEW WAY: Constructor injection (recommended by Spring since 4.3).
    // ═══════════════════════════════════════════════════════════════════════════
    static void example3_DependencyInjection() throws Exception {
        IO.println("3️⃣  Dependency Injection into Final Fields");
        IO.println("   Use case: Spring @Autowired, CDI @Inject, Guice @Inject");
        IO.println("   ────────────────────────────────────────");

        // --- OLD WAY: Field injection via reflection ---
        IO.println("   ❌ OLD WAY (field injection on final fields):");
        var controller = new OrderControllerOld();
        Field serviceField = OrderControllerOld.class.getDeclaredField("orderService");
        serviceField.setAccessible(true);
        serviceField.set(controller, new OrderService("injected-via-reflection"));  // ← WARNING
        IO.println("      " + controller.processOrder());
        IO.println("      ⚠️  Warning emitted — field injection on final fields!");
        IO.println();

        // --- NEW WAY: Constructor injection ---
        IO.println("   ✅ NEW WAY (constructor injection):");
        var service = new OrderService("constructor-injected");
        var controllerNew = new OrderControllerNew(service);
        IO.println("      " + controllerNew.processOrder());
        IO.println("      No reflection needed — constructor injection is the standard.");
        IO.println();
    }

    static class OrderService {
        private final String source;

        OrderService(String source) { this.source = source; }

        String findOrder(int id) { return "Order-" + id + " (from " + source + ")"; }
    }

    /** OLD pattern: final field set via reflection by DI framework. */
    static class OrderControllerOld {
        private final OrderService orderService = null; // Set by framework

        String processOrder() {
            return "Processed: " + orderService.findOrder(42);
        }
    }

    /** NEW pattern: constructor injection — framework passes dependency in. */
    static class OrderControllerNew {
        private final OrderService orderService;

        OrderControllerNew(OrderService orderService) {
            this.orderService = orderService;
        }

        String processOrder() {
            return "Processed: " + orderService.findOrder(42);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 4 — Configuration Override for Testing
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Tests need to override a final static constant like
    //  MAX_RETRIES or TIMEOUT_MS to speed up test execution.
    //
    //  OLD WAY: Reflection on final static fields.
    //  NEW WAY: Use a configuration object or system properties.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example4_ConfigOverrideForTesting() throws Exception {
        IO.println("4️⃣  Configuration Override for Testing");
        IO.println("   Use case: Unit tests, integration tests, environment-specific config");
        IO.println("   ────────────────────────────────────────");

        // --- OLD WAY: Override final static constant via reflection ---
        IO.println("   ❌ OLD WAY (reflection on final static constant):");
        IO.println("      RetryConfig.MAX_RETRIES = " + RetryConfig.MAX_RETRIES);

        Field maxRetriesField = RetryConfig.class.getDeclaredField("MAX_RETRIES");
        maxRetriesField.setAccessible(true);
        try {
            maxRetriesField.setInt(null, 1);  // ← JDK 26 WARNING
            IO.println("      After reflection: MAX_RETRIES = " + RetryConfig.MAX_RETRIES);
            IO.println("      ⚠️  JIT may have already inlined the old value!");
        } catch (IllegalAccessException e) {
            IO.println("      ✅ Blocked: " + e.getMessage());
        }
        IO.println();

        // --- NEW WAY: Configurable object ---
        IO.println("   ✅ NEW WAY (configurable settings object):");
        var prodConfig = new FlexibleRetryConfig(5, 1000);
        IO.println("      Production: " + prodConfig);
        var testConfig = new FlexibleRetryConfig(1, 10);
        IO.println("      Testing:    " + testConfig);
        IO.println("      No reflection needed — inject the config you want.");
        IO.println();
    }

    static class RetryConfig {
        static final int MAX_RETRIES = 5;
        static final long TIMEOUT_MS = 30_000;
    }

    record FlexibleRetryConfig(int maxRetries, long timeoutMs) {
        @Override
        public String toString() {
            return "RetryConfig[maxRetries=" + maxRetries + ", timeoutMs=" + timeoutMs + "ms]";
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 5 — Immutable DTO Copy Pattern (withX methods)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: You want to create a modified copy of an immutable object.
    //  Some codebases use reflection to "clone and change" final fields.
    //
    //  OLD WAY: Reflection to modify a cloned object's final fields.
    //  NEW WAY: Record wither methods or builder pattern.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example5_ImmutableDtoCopyPattern() {
        IO.println("5️⃣  Immutable DTO Copy Pattern");
        IO.println("   Use case: Domain models, value objects, API responses");
        IO.println("   ────────────────────────────────────────");

        // --- NEW WAY: Record with wither methods ---
        IO.println("   ✅ Records with wither (copy) methods:");
        record Product(String name, double price, boolean inStock) {
            Product withPrice(double newPrice) {
                return new Product(name, newPrice, inStock);
            }
            Product withInStock(boolean newInStock) {
                return new Product(name, price, newInStock);
            }
        }

        var original = new Product("Widget", 29.99, true);
        IO.println("      Original:    " + original);
        IO.println("      Discounted:  " + original.withPrice(19.99));
        IO.println("      Out of stock: " + original.withInStock(false));
        IO.println("      Original unchanged: " + original);
        IO.println("      ✅ Clean, immutable, no reflection needed.");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 6 — Lazy Cache Initialization (Without Final Field Hacks)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: A cache field is declared final and initialized to empty,
    //  then populated lazily via reflection. Better: use explicit lazy init.
    //
    //  OLD WAY: Declare final Map, then reflectively replace it.
    //  NEW WAY: Use a non-final volatile field with double-checked locking,
    //           or use StableValue (JEP 526) when available.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example6_LazyCacheInitialization() {
        IO.println("6️⃣  Lazy Cache Initialization");
        IO.println("   Use case: Service caches, lookup tables, computed constants");
        IO.println("   ────────────────────────────────────────");

        IO.println("   ✅ NEW WAY (explicit lazy initialization):");
        var cache = new LazyCache();
        IO.println("      Cache initialized: " + cache.isInitialized());
        IO.println("      First access:  " + cache.get("greeting"));
        IO.println("      Cache initialized: " + cache.isInitialized());
        IO.println("      Second access: " + cache.get("greeting") + " (cached)");
        IO.println();

        IO.println("   💡 Tip: In JDK 26, also consider StableValue (JEP 526) for");
        IO.println("      lazily-initialized constants that are truly set-once.");
        IO.println();
    }

    /** A lazy cache that initializes on first access — no reflection needed. */
    static class LazyCache {
        private volatile Map<String, String> data;

        boolean isInitialized() { return data != null; }

        String get(String key) {
            if (data == null) {
                synchronized (this) {
                    if (data == null) {
                        data = loadExpensiveData();
                    }
                }
            }
            return data.getOrDefault(key, "(not found)");
        }

        private Map<String, String> loadExpensiveData() {
            IO.println("      (Loading expensive data...)");
            return Map.of(
                "greeting", "Hello, World!",
                "farewell", "Goodbye!",
                "version", "26"
            );
        }
    }
}


