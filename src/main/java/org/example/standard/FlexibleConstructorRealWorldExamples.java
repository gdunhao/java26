package org.example.standard;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  Flexible Constructor Bodies — Real-World Use Cases                         ║
 * ║  Practical examples where JEP 513 gives you a real advantage                ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * These examples show REAL scenarios where placing statements before super()
 * or this() eliminates awkward workarounds and makes code cleaner, safer,
 * and more readable.
 *
 * REFERENCES
 * ──────────
 *   • JEP 513 — Flexible Constructor Bodies:
 *       https://openjdk.org/jeps/513
 *
 * PATTERNS DEMONSTRATED
 * ─────────────────────
 *   1. JDBC connection wrapper  — Validate URL format before calling super (DB libraries)
 *   2. Immutable money type     — Normalize currency + validate before super (fintech)
 *   3. Thread pool builder      — Compute derived config before super (concurrency frameworks)
 *   4. JSON config loader       — Parse + validate config before super (microservices)
 *   5. Secure credential store  — Sanitize inputs before super (security, vault clients)
 *   6. UI component framework   — Generate IDs + validate hierarchy before super (Swing, JavaFX, web)
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.standard.FlexibleConstructorRealWorldExamples
 */
public class FlexibleConstructorRealWorldExamples {

    public static void main(String[] args) {
        IO.println("╔═══════════════════════════════════════════════════════╗");
        IO.println("║  Flexible Constructor Bodies — Real-World Use Cases  ║");
        IO.println("╚═══════════════════════════════════════════════════════╝");
        IO.println();

        example1_JdbcConnectionWrapper();
        example2_ImmutableMoneyType();
        example3_ThreadPoolBuilder();
        example4_JsonConfigLoader();
        example5_SecureCredentialStore();
        example6_UiComponentFramework();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 1 — JDBC Connection Wrapper
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: You're building a connection pool wrapper that extends a
    //  base Connection class. Before establishing the connection, you need
    //  to validate the JDBC URL format and extract the host/port.
    //
    //  BEFORE JDK 26: Had to use a static helper to validate and transform
    //  the URL, then pass the result to super(). Ugly and scattered.
    //
    //  WITH JDK 26: Validate inline, right before super(). Clean and local.
    //
    //  Real users: HikariCP, DBCP, c3p0, custom JDBC wrappers.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example1_JdbcConnectionWrapper() {
        IO.println("1️⃣  JDBC Connection Wrapper (URL Validation Before super())");
        IO.println("   Use case: Connection pools, JDBC wrappers, database libraries");
        IO.println("   ────────────────────────────────────────");

        try {
            var conn = new ValidatedConnection("jdbc:postgresql://localhost:5432/mydb");
            IO.println("   ✅ Created: " + conn);
        } catch (Exception e) {
            IO.println("   ❌ Error: " + e.getMessage());
        }

        try {
            var conn = new ValidatedConnection("not-a-valid-url");
            IO.println("   ❌ Should not reach here: " + conn);
        } catch (IllegalArgumentException e) {
            IO.println("   ✅ Caught invalid URL: " + e.getMessage());
        }

        try {
            var conn = new ValidatedConnection("");
            IO.println("   ❌ Should not reach here: " + conn);
        } catch (IllegalArgumentException e) {
            IO.println("   ✅ Caught empty URL: " + e.getMessage());
        }
        IO.println();
    }

    static class BaseConnection {
        final String host;
        final int port;
        final String database;
        BaseConnection(String host, int port, String database) {
            this.host = host;
            this.port = port;
            this.database = database;
        }
        @Override public String toString() {
            return "Connection[" + host + ":" + port + "/" + database + "]";
        }
    }

    static class ValidatedConnection extends BaseConnection {
        ValidatedConnection(String jdbcUrl) {
            // ✨ All validation and parsing BEFORE super() — JDK 26!
            if (jdbcUrl == null || jdbcUrl.isBlank()) {
                throw new IllegalArgumentException("JDBC URL cannot be null or empty");
            }
            if (!jdbcUrl.startsWith("jdbc:")) {
                throw new IllegalArgumentException(
                    "Invalid JDBC URL format (must start with 'jdbc:'): " + jdbcUrl);
            }

            // Extract host, port, database from URL
            String withoutPrefix = jdbcUrl.substring(jdbcUrl.indexOf("//") + 2);
            String[] hostPort = withoutPrefix.split("/")[0].split(":");
            String host = hostPort[0];
            int port = hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 5432;
            String database = withoutPrefix.contains("/")
                ? withoutPrefix.substring(withoutPrefix.indexOf("/") + 1)
                : "default";

            super(host, port, database);  // Now we know everything is valid
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 2 — Immutable Money Type
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Your fintech application has a Money value type that
    //  extends a base Amount class. Money values must be non-negative,
    //  and the currency code must be a valid ISO 4217 code. You want to
    //  normalize and validate BEFORE creating the object.
    //
    //  Real users: Joda-Money, JavaMoney (JSR 354), banking applications,
    //              payment gateways (Stripe, Square).
    // ═══════════════════════════════════════════════════════════════════════════
    static void example2_ImmutableMoneyType() {
        IO.println("2️⃣  Immutable Money Type (Normalize + Validate Before super())");
        IO.println("   Use case: Banking apps, payment gateways, Joda-Money, JSR 354");
        IO.println("   ────────────────────────────────────────");

        var usd = new Money(99.999, "usd");
        IO.println("   ✅ " + usd);

        var eur = new Money(1500.00, " EUR ");
        IO.println("   ✅ " + eur);

        try {
            new Money(-50.0, "USD");
        } catch (IllegalArgumentException e) {
            IO.println("   ✅ Caught: " + e.getMessage());
        }

        try {
            new Money(100.0, "INVALID");
        } catch (IllegalArgumentException e) {
            IO.println("   ✅ Caught: " + e.getMessage());
        }
        IO.println();
    }

    static class Amount {
        final long cents;
        final String currency;
        Amount(long cents, String currency) {
            this.cents = cents;
            this.currency = currency;
        }
    }

    static class Money extends Amount {
        private static final java.util.Set<String> VALID_CURRENCIES =
            java.util.Set.of("USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "BRL");

        Money(double amount, String currencyCode) {
            // ✨ Normalize and validate BEFORE super() — JDK 26!
            if (amount < 0) {
                throw new IllegalArgumentException(
                    "Money amount cannot be negative: " + amount);
            }
            String normalized = currencyCode.strip().toUpperCase();
            if (!VALID_CURRENCIES.contains(normalized)) {
                throw new IllegalArgumentException(
                    "Unknown currency code: '" + currencyCode + "'. Valid: " + VALID_CURRENCIES);
            }
            long cents = Math.round(amount * 100);

            super(cents, normalized);
        }

        @Override public String toString() {
            return currency + " " + (cents / 100) + "." + String.format("%02d", cents % 100);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 3 — Thread Pool Builder
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Your framework has a base ThreadPool class. A
    //  SmartThreadPool subclass needs to compute derived configuration
    //  (core size = CPU cores, max size = 2× core, queue capacity
    //  based on expected task duration) BEFORE initializing the pool.
    //
    //  Real users: Custom thread pool implementations, Tomcat/Jetty
    //              connector pools, Spring TaskExecutor configuration.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example3_ThreadPoolBuilder() {
        IO.println("3️⃣  Thread Pool Builder (Derived Config Before super())");
        IO.println("   Use case: Tomcat/Jetty pools, Spring TaskExecutor, custom executors");
        IO.println("   ────────────────────────────────────────");

        var pool = new SmartThreadPool("io-heavy", 50);
        IO.println("   ✅ " + pool);

        var cpuPool = new SmartThreadPool("cpu-bound", 2);
        IO.println("   ✅ " + cpuPool);

        try {
            new SmartThreadPool("", 10);
        } catch (IllegalArgumentException e) {
            IO.println("   ✅ Caught: " + e.getMessage());
        }
        IO.println();
    }

    static class ThreadPoolConfig {
        final String name;
        final int coreSize;
        final int maxSize;
        final int queueCapacity;
        ThreadPoolConfig(String name, int coreSize, int maxSize, int queueCapacity) {
            this.name = name;
            this.coreSize = coreSize;
            this.maxSize = maxSize;
            this.queueCapacity = queueCapacity;
        }
        @Override public String toString() {
            return "ThreadPool[" + name + ": core=" + coreSize
                + ", max=" + maxSize + ", queue=" + queueCapacity + "]";
        }
    }

    static class SmartThreadPool extends ThreadPoolConfig {
        SmartThreadPool(String name, int expectedConcurrentTasks) {
            // ✨ Compute derived config BEFORE super() — JDK 26!
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Pool name cannot be empty");
            }

            int cpuCores = Runtime.getRuntime().availableProcessors();
            int coreSize = Math.max(cpuCores, 2);
            int maxSize = Math.max(coreSize * 2, expectedConcurrentTasks);
            int queueCapacity = maxSize * 10;

            // Apply safety caps
            maxSize = Math.min(maxSize, 200);
            queueCapacity = Math.min(queueCapacity, 10_000);

            super(name, coreSize, maxSize, queueCapacity);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 4 — JSON Config Loader
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: A microservice's configuration class extends a base Config.
    //  The subclass receives a raw JSON-like string and must parse/validate
    //  individual fields before calling super(). This is common in
    //  Spring Boot auto-configuration and cloud-native apps.
    //
    //  Real users: Spring Boot @ConfigurationProperties, Quarkus config,
    //              Micronaut configuration, Kubernetes ConfigMap parsing.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example4_JsonConfigLoader() {
        IO.println("4️⃣  Config Loader (Parse + Validate Before super())");
        IO.println("   Use case: Spring Boot config, Quarkus, Kubernetes ConfigMaps");
        IO.println("   ────────────────────────────────────────");

        var config = new ServiceConfig("host=api.example.com;port=8443;timeout=30;retries=3");
        IO.println("   ✅ " + config);

        var defaultConfig = new ServiceConfig("host=localhost");
        IO.println("   ✅ " + defaultConfig);

        try {
            new ServiceConfig("port=8080");  // missing required 'host'
        } catch (IllegalArgumentException e) {
            IO.println("   ✅ Caught: " + e.getMessage());
        }
        IO.println();
    }

    static class BaseConfig {
        final String host;
        final int port;
        final int timeoutSeconds;
        final int retries;
        BaseConfig(String host, int port, int timeoutSeconds, int retries) {
            this.host = host;
            this.port = port;
            this.timeoutSeconds = timeoutSeconds;
            this.retries = retries;
        }
        @Override public String toString() {
            return "Config[host=" + host + ", port=" + port
                + ", timeout=" + timeoutSeconds + "s, retries=" + retries + "]";
        }
    }

    static class ServiceConfig extends BaseConfig {
        ServiceConfig(String rawConfig) {
            // ✨ Parse and validate config string BEFORE super() — JDK 26!
            var props = new java.util.HashMap<String, String>();
            for (String pair : rawConfig.split(";")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    props.put(kv[0].strip(), kv[1].strip());
                }
            }

            String host = props.get("host");
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException(
                    "Missing required config key: 'host'. Got: " + rawConfig);
            }
            int port = Integer.parseInt(props.getOrDefault("port", "443"));
            int timeout = Integer.parseInt(props.getOrDefault("timeout", "10"));
            int retries = Integer.parseInt(props.getOrDefault("retries", "3"));

            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Invalid port: " + port);
            }
            if (timeout < 1) {
                throw new IllegalArgumentException("Timeout must be >= 1s");
            }

            super(host, port, timeout, retries);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 5 — Secure Credential Store
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: A credential holder extends a base SecureEntry class.
    //  Before storing the credential, you must sanitize the key name,
    //  validate the value isn't empty, and compute a fingerprint —
    //  all BEFORE the immutable parent is constructed.
    //
    //  Real users: HashiCorp Vault clients, AWS Secrets Manager SDK,
    //              Spring Cloud Vault, password managers.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example5_SecureCredentialStore() {
        IO.println("5️⃣  Secure Credential Store (Sanitize Before super())");
        IO.println("   Use case: Vault clients, Secrets Manager, password managers");
        IO.println("   ────────────────────────────────────────");

        var cred1 = new SecureCredential(" DB_PASSWORD ", "s3cret!123");
        IO.println("   ✅ " + cred1);

        var cred2 = new SecureCredential("api/key/production", "ak-12345-abcde");
        IO.println("   ✅ " + cred2);

        try {
            new SecureCredential("key", "");
        } catch (IllegalArgumentException e) {
            IO.println("   ✅ Caught: " + e.getMessage());
        }

        try {
            new SecureCredential("", "value");
        } catch (IllegalArgumentException e) {
            IO.println("   ✅ Caught: " + e.getMessage());
        }
        IO.println();
    }

    static class SecureEntry {
        final String key;
        final byte[] encryptedValue;
        final String fingerprint;
        SecureEntry(String key, byte[] encryptedValue, String fingerprint) {
            this.key = key;
            this.encryptedValue = encryptedValue;
            this.fingerprint = fingerprint;
        }
    }

    static class SecureCredential extends SecureEntry {
        SecureCredential(String rawKey, String rawValue) {
            // ✨ Sanitize, validate, and transform BEFORE super() — JDK 26!
            if (rawKey == null || rawKey.isBlank()) {
                throw new IllegalArgumentException("Credential key cannot be empty");
            }
            if (rawValue == null || rawValue.isEmpty()) {
                throw new IllegalArgumentException("Credential value cannot be empty");
            }

            // Normalize key: trim, lowercase, replace special chars
            String sanitizedKey = rawKey.strip().toLowerCase()
                .replaceAll("[^a-z0-9/_-]", "_");

            // "Encrypt" the value (simplified — real code would use AES)
            byte[] encrypted = new byte[rawValue.length()];
            for (int i = 0; i < rawValue.length(); i++) {
                encrypted[i] = (byte) (rawValue.charAt(i) ^ 0x42);
            }

            // Compute fingerprint (first 8 chars of hex hash)
            int hash = rawValue.hashCode();
            String fingerprint = String.format("%08x", hash);

            super(sanitizedKey, encrypted, fingerprint);
        }

        @Override public String toString() {
            return "SecureCredential[key=" + key
                + ", fingerprint=" + fingerprint
                + ", encryptedBytes=" + encryptedValue.length + "]";
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 6 — UI Component Framework
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: A UI framework where components extend a base Component.
    //  Each component must have a unique auto-generated ID and validated
    //  layout constraints before the base class initializes the rendering
    //  pipeline.
    //
    //  Real users: JavaFX custom components, Vaadin, Swing L&F, Android
    //              View subclasses, web component frameworks (GWT, J2CL).
    // ═══════════════════════════════════════════════════════════════════════════
    static void example6_UiComponentFramework() {
        IO.println("6️⃣  UI Component Framework (Auto-ID + Validation Before super())");
        IO.println("   Use case: JavaFX, Vaadin, Swing, Android Views, web components");
        IO.println("   ────────────────────────────────────────");

        var btn = new Button("Submit", 120, 40);
        IO.println("   ✅ " + btn);

        var panel = new Button("Cancel", 80, 36);
        IO.println("   ✅ " + panel);

        try {
            new Button("", 100, 30);
        } catch (IllegalArgumentException e) {
            IO.println("   ✅ Caught: " + e.getMessage());
        }

        try {
            new Button("OK", -10, 30);
        } catch (IllegalArgumentException e) {
            IO.println("   ✅ Caught: " + e.getMessage());
        }
        IO.println();
    }

    static class Component {
        final String id;
        final String label;
        final int width;
        final int height;
        Component(String id, String label, int width, int height) {
            this.id = id;
            this.label = label;
            this.width = width;
            this.height = height;
        }
    }

    private static final java.util.concurrent.atomic.AtomicInteger ID_SEQ =
        new java.util.concurrent.atomic.AtomicInteger(0);

    static class Button extends Component {
        Button(String label, int width, int height) {
            // ✨ Generate ID + validate BEFORE super() — JDK 26!
            if (label == null || label.isBlank()) {
                throw new IllegalArgumentException("Button label cannot be empty");
            }
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException(
                    "Dimensions must be positive: " + width + "x" + height);
            }

            String id = "btn-" + label.strip().toLowerCase().replaceAll("\\s+", "-")
                + "-" + ID_SEQ.incrementAndGet();

            super(id, label.strip(), width, height);
        }

        @Override public String toString() {
            return "Button[id=" + id + ", label='" + label
                + "', size=" + width + "x" + height + "]";
        }
    }
}




