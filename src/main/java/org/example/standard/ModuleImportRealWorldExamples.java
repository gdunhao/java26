package org.example.standard;

import module java.base;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  Module Import Declarations — Real-World Use Cases                          ║
 * ║  Practical examples where JEP 511 gives you a real advantage                ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * These examples show REAL scenarios where `import module java.base` replaces
 * dozens of individual imports and enables cleaner, more readable code in
 * production applications.
 *
 * REFERENCES
 * ──────────
 *   • JEP 511 — Module Import Declarations:
 *       https://openjdk.org/jeps/511
 *
 * PATTERNS DEMONSTRATED
 * ─────────────────────
 *   1. ETL pipeline          — Uses types from java.util, java.time, java.nio, java.math in one method
 *   2. Report generator      — Combines collections, formatting, streams, I/O without 15 imports
 *   3. REST response builder — Uses Map, List, Optional, Instant, URI — all from java.base
 *   4. Data validation       — regex, collections, optionals, math — all available with one import
 *   5. Concurrent aggregator — Uses CompletableFuture, ConcurrentHashMap, AtomicLong, Duration
 *   6. File system analyzer  — Path, Files, Stream, Map, Collectors — entire workflow, one import
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.standard.ModuleImportRealWorldExamples
 */
public class ModuleImportRealWorldExamples {

    public static void main(String[] args) {
        IO.println("╔═══════════════════════════════════════════════════════╗");
        IO.println("║  Module Import Declarations — Real-World Use Cases   ║");
        IO.println("╚═══════════════════════════════════════════════════════╝");
        IO.println();

        example1_EtlPipeline();
        example2_ReportGenerator();
        example3_RestResponseBuilder();
        example4_DataValidationSuite();
        example5_ConcurrentAggregator();
        example6_FileSystemAnalyzer();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 1 — ETL Pipeline
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: An ETL (Extract, Transform, Load) pipeline that processes
    //  sales records. In a single method, you need types from:
    //    java.util (List, Map, HashMap, stream.Collectors)
    //    java.time (LocalDate, Month, YearMonth)
    //    java.math (BigDecimal, RoundingMode)
    //
    //  Without `import module java.base`, this requires 10+ individual imports.
    //  With it, zero additional imports needed.
    //
    //  Real users: Apache Spark ETL jobs (Java), Spring Batch processors,
    //              data warehouse loaders, analytics pipelines.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example1_EtlPipeline() {
        IO.println("1️⃣  ETL Pipeline (Sales Data Aggregation)");
        IO.println("   Use case: Spring Batch, data warehouses, analytics");
        IO.println("   Types used: List, Map, BigDecimal, LocalDate, Collectors, RoundingMode");
        IO.println("   ────────────────────────────────────────");

        // All these types come from different java.base packages — one import covers all!
        record SalesRecord(LocalDate date, String product, BigDecimal amount) {}

        List<SalesRecord> rawData = List.of(
            new SalesRecord(LocalDate.of(2026, 1, 15), "Widget A", new BigDecimal("29.99")),
            new SalesRecord(LocalDate.of(2026, 1, 22), "Widget B", new BigDecimal("49.99")),
            new SalesRecord(LocalDate.of(2026, 1, 28), "Widget A", new BigDecimal("29.99")),
            new SalesRecord(LocalDate.of(2026, 2, 5),  "Widget C", new BigDecimal("99.99")),
            new SalesRecord(LocalDate.of(2026, 2, 14), "Widget A", new BigDecimal("29.99")),
            new SalesRecord(LocalDate.of(2026, 2, 20), "Widget B", new BigDecimal("49.99"))
        );

        // Transform: Group by month, compute total revenue
        Map<YearMonth, BigDecimal> monthlyRevenue = rawData.stream()
            .collect(Collectors.groupingBy(
                r -> YearMonth.from(r.date()),
                Collectors.reducing(BigDecimal.ZERO, SalesRecord::amount, BigDecimal::add)
            ));

        // Transform: Top product by total sales
        Map<String, BigDecimal> productTotals = rawData.stream()
            .collect(Collectors.groupingBy(
                SalesRecord::product,
                Collectors.reducing(BigDecimal.ZERO, SalesRecord::amount, BigDecimal::add)
            ));

        Optional<Map.Entry<String, BigDecimal>> topProduct = productTotals.entrySet().stream()
            .max(Map.Entry.comparingByValue());

        IO.println("   Monthly Revenue:");
        monthlyRevenue.forEach((month, total) ->
            IO.println("     " + month + " → $" + total.setScale(2, RoundingMode.HALF_UP)));
        IO.println("   Top Product: " + topProduct.map(e -> e.getKey() + " ($" + e.getValue() + ")").orElse("N/A"));
        IO.println("   (All done with a single: import module java.base)");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 2 — Report Generator
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Generate a formatted text report combining data from
    //  multiple sources. Needs: StringJoiner, Formatter, DecimalFormat,
    //  LocalDateTime, Duration, List, Map — all from different packages.
    //
    //  Real users: PDF report generators, email summary builders,
    //              Slack/Teams notification formatters, audit trail exports.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example2_ReportGenerator() {
        IO.println("2️⃣  Report Generator (Multi-Package Type Usage)");
        IO.println("   Use case: PDF reports, email summaries, Slack notifications");
        IO.println("   Types used: StringJoiner, LocalDateTime, Duration, List, Formatter");
        IO.println("   ────────────────────────────────────────");

        // Simulate report data
        LocalDateTime reportTime = LocalDateTime.now();
        Duration uptime = Duration.ofHours(72).plusMinutes(34);
        List<String> healthyServices = List.of("auth", "payments", "catalog", "search");
        List<String> degradedServices = List.of("recommendations");

        // Build report using StringJoiner (java.util)
        StringJoiner report = new StringJoiner("\n");
        report.add("   ┌── System Health Report ──────────────────────┐");
        report.add("   │ Generated: " + reportTime.format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        report.add("   │ Uptime: " + uptime.toHours() + "h " + (uptime.toMinutesPart()) + "m");
        report.add("   │ Healthy:  " + healthyServices.size() + " services " +
            healthyServices.stream().collect(Collectors.joining(", ", "[", "]")));
        report.add("   │ Degraded: " + degradedServices.size() + " service " +
            degradedServices.stream().collect(Collectors.joining(", ", "[", "]")));

        double availability = (double) healthyServices.size() /
            (healthyServices.size() + degradedServices.size()) * 100;
        report.add("   │ Availability: " + String.format("%.1f%%", availability));
        report.add("   └──────────────────────────────────────────────┘");

        IO.println(report.toString());
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 3 — REST API Response Builder
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Building a REST API response object that combines metadata
    //  (timestamps, URIs), data (maps, lists), and pagination info.
    //  A single handler method touches types from 5+ packages.
    //
    //  Real users: Spring MVC/WebFlux controllers, JAX-RS resources,
    //              any REST API handler.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example3_RestResponseBuilder() {
        IO.println("3️⃣  REST API Response Builder");
        IO.println("   Use case: Spring MVC, JAX-RS, any REST API handler");
        IO.println("   Types used: Map, List, Optional, Instant, URI, UUID, LinkedHashMap");
        IO.println("   ────────────────────────────────────────");

        // Simulate building a paginated API response
        UUID requestId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        URI selfLink = URI.create("https://api.example.com/users?page=1&size=3");
        URI nextLink = URI.create("https://api.example.com/users?page=2&size=3");

        // Use LinkedHashMap to preserve insertion order (java.util)
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("requestId", requestId.toString());
        response.put("timestamp", timestamp.toString());
        response.put("data", List.of(
            Map.of("id", 1, "name", "Alice", "role", "admin"),
            Map.of("id", 2, "name", "Bob",   "role", "user"),
            Map.of("id", 3, "name", "Carol", "role", "user")
        ));
        response.put("pagination", Map.of(
            "page", 1,
            "pageSize", 3,
            "totalElements", 42
        ));
        response.put("_links", Map.of(
            "self", selfLink.toString(),
            "next", nextLink.toString()
        ));

        IO.println("   Response:");
        response.forEach((key, value) ->
            IO.println("     " + key + ": " + value));
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 4 — Data Validation Suite
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Validating user registration data requires regex patterns,
    //  Optional chaining, collection operations, and math checks.
    //
    //  Real users: Spring Validation, Bean Validation (JSR 380), form
    //              handlers, any user input validation layer.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example4_DataValidationSuite() {
        IO.println("4️⃣  Data Validation Suite (Multi-Field User Registration)");
        IO.println("   Use case: Spring Validation, form handlers, registration flows");
        IO.println("   Types used: Pattern, Optional, List, Map, BigDecimal");
        IO.println("   ────────────────────────────────────────");

        // Validate several user registrations
        record Registration(String email, String username, int age, String salary) {}

        List<Registration> registrations = List.of(
            new Registration("alice@example.com", "alice_dev", 28, "75000.50"),
            new Registration("not-an-email",      "ab",        17, "50000"),
            new Registration("bob@test.org",       "bob_builder", 35, "-1000")
        );

        // Compiled regex pattern (java.util.regex — available from module import!)
        var emailPattern = java.util.regex.Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

        for (Registration reg : registrations) {
            List<String> errors = new ArrayList<>();

            // Email validation via regex
            if (!emailPattern.matcher(reg.email()).matches()) {
                errors.add("Invalid email format");
            }

            // Username length check
            Optional.of(reg.username())
                .filter(u -> u.length() >= 3)
                .ifPresentOrElse(
                    u -> {},  // valid
                    () -> errors.add("Username must be at least 3 characters")
                );

            // Age range check
            if (reg.age() < 18) {
                errors.add("Must be 18 or older (got " + reg.age() + ")");
            }

            // Salary validation with BigDecimal
            BigDecimal salary = new BigDecimal(reg.salary());
            if (salary.compareTo(BigDecimal.ZERO) < 0) {
                errors.add("Salary cannot be negative");
            }

            String status = errors.isEmpty() ? "✅ VALID" : "❌ INVALID";
            IO.println("   " + reg.email() + " → " + status);
            errors.forEach(e -> IO.println("      - " + e));
        }
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 5 — Concurrent Aggregator
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Aggregate results from multiple upstream services
    //  concurrently, with timeout and atomic counters for metrics.
    //
    //  Real users: API gateways, BFF (Backend-for-Frontend) services,
    //              dashboard data aggregators, monitoring systems.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example5_ConcurrentAggregator() {
        IO.println("5️⃣  Concurrent Aggregator (Multi-Service Data Fetch)");
        IO.println("   Use case: API gateways, BFF services, dashboard aggregators");
        IO.println("   Types used: CompletableFuture, ConcurrentHashMap, AtomicLong, Duration, Instant");
        IO.println("   ────────────────────────────────────────");

        ConcurrentHashMap<String, String> results = new ConcurrentHashMap<>();
        java.util.concurrent.atomic.AtomicLong totalLatency = new java.util.concurrent.atomic.AtomicLong(0);
        Instant start = Instant.now();

        // Fire concurrent requests to simulated services
        CompletableFuture<String> userFuture = CompletableFuture.supplyAsync(() -> {
            sleep(80);
            return "User{id=42, name='Alice'}";
        });

        CompletableFuture<String> orderFuture = CompletableFuture.supplyAsync(() -> {
            sleep(120);
            return "Orders[3 items, total=$149.97]";
        });

        CompletableFuture<String> recsFuture = CompletableFuture.supplyAsync(() -> {
            sleep(60);
            return "Recommendations[5 products]";
        });

        // Wait for all with timeout
        try {
            CompletableFuture.allOf(userFuture, orderFuture, recsFuture)
                .get(5, TimeUnit.SECONDS);

            results.put("user", userFuture.get());
            results.put("orders", orderFuture.get());
            results.put("recommendations", recsFuture.get());
        } catch (Exception e) {
            IO.println("   ❌ Timeout or error: " + e.getMessage());
        }

        Duration elapsed = Duration.between(start, Instant.now());

        IO.println("   Aggregated results:");
        results.forEach((svc, data) -> IO.println("     " + svc + " → " + data));
        IO.println("   Total wall-clock time: " + elapsed.toMillis() + "ms (concurrent!)");
        IO.println("   (All types from a single: import module java.base)");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 6 — File System Analyzer
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Analyze the current directory structure — count files
    //  by extension, compute total size, find the largest file. Uses
    //  Path, Files, Stream, Map, Collectors, Optional, Long all in one method.
    //
    //  Real users: Build tools, disk usage analyzers, backup tools,
    //              CI/CD artifact scanners, log rotation.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example6_FileSystemAnalyzer() {
        IO.println("6️⃣  File System Analyzer (Directory Scan & Stats)");
        IO.println("   Use case: Build tools, disk analyzers, CI artifact scanners");
        IO.println("   Types used: Path, Files, Stream, Map, Collectors, Optional");
        IO.println("   ────────────────────────────────────────");

        Path projectDir = Path.of("src/main/java");

        try (var fileStream = Files.walk(projectDir)) {
            List<Path> allFiles = fileStream
                .filter(Files::isRegularFile)
                .toList();

            // Group by extension
            Map<String, Long> byExtension = allFiles.stream()
                .collect(Collectors.groupingBy(
                    p -> {
                        String name = p.getFileName().toString();
                        int dot = name.lastIndexOf('.');
                        return dot >= 0 ? name.substring(dot) : "(no ext)";
                    },
                    Collectors.counting()
                ));

            // Total size
            long totalBytes = allFiles.stream()
                .mapToLong(p -> {
                    try { return Files.size(p); }
                    catch (Exception e) { return 0; }
                })
                .sum();

            // Largest file
            Optional<Path> largest = allFiles.stream()
                .max(Comparator.comparingLong(p -> {
                    try { return Files.size(p); }
                    catch (Exception e) { return 0; }
                }));

            IO.println("   Directory: " + projectDir.toAbsolutePath().normalize());
            IO.println("   Total files: " + allFiles.size());
            IO.println("   Total size: " + String.format("%,d", totalBytes) + " bytes");
            IO.println("   Files by extension:");
            byExtension.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> IO.println("     " + e.getKey() + " → " + e.getValue() + " files"));
            largest.ifPresent(p -> {
                try {
                    IO.println("   Largest file: " + p.getFileName()
                        + " (" + String.format("%,d", Files.size(p)) + " bytes)");
                } catch (Exception e) { /* ignore */ }
            });

        } catch (Exception e) {
            IO.println("   ⚠️  Could not scan directory: " + e.getMessage());
        }
        IO.println();
    }

    // ─── Helpers ───

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}

