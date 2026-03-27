package org.example.standard;

import module java.base;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  JEP 494: Module Import Declarations                                       ║
 * ║  Status: FINAL in JDK 26                                                   ║
 * ║  Spec: https://openjdk.org/jeps/494                                        ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * WHAT THIS FEATURE DOES
 * ──────────────────────
 * Module Import Declarations allow you to import all packages exported by a
 * module with a single statement:
 *
 *     import module java.base;
 *
 * This single line replaces dozens of individual import statements. It makes
 * ALL public top-level types from ALL packages exported by the module available
 * for use without qualification.
 *
 * WHAT GETS IMPORTED
 * ──────────────────
 * `import module java.base;` gives you access to types from packages including
 * (but not limited to):
 *   - java.lang (already auto-imported, but included)
 *   - java.util (List, Map, Set, Optional, stream, etc.)
 *   - java.io (File, InputStream, OutputStream, etc.)
 *   - java.nio (ByteBuffer, Path, Files, etc.)
 *   - java.time (LocalDate, Instant, Duration, etc.)
 *   - java.math (BigDecimal, BigInteger, etc.)
 *   - java.net (URI, URL, HttpURLConnection, etc.)
 *   - java.util.concurrent (ExecutorService, Future, etc.)
 *   - java.util.function (Predicate, Function, Consumer, etc.)
 *   - java.security, java.text, and many more
 *
 * AMBIGUITY RESOLUTION
 * ────────────────────
 * If two modules export classes with the same simple name (e.g., `List` from
 * `java.base` and a hypothetical `List` from another module), the compiler
 * will flag an ambiguity error. You resolve it with a specific single-type
 * import, which takes priority over the module import.
 *
 * WHY IT MATTERS
 * ──────────────
 * - Reduces boilerplate: No more long import lists at the top of every file.
 * - Great for learning: Beginners don't need to memorize package names.
 * - Scripting-friendly: Ideal for quick scripts and implicit classes.
 * - Production-ready: Unlike wildcard imports, module imports follow the
 *   module system's encapsulation — only exported packages are imported.
 *
 * COMPARISON
 * ──────────
 *   // Before JDK 26 — many individual imports
 *   import java.util.List;
 *   import java.util.Map;
 *   import java.util.stream.Collectors;
 *   import java.time.LocalDate;
 *   import java.math.BigDecimal;
 *   import java.nio.file.Files;
 *   import java.nio.file.Path;
 *
 *   // With JDK 26 — one line covers all of java.base
 *   import module java.base;
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.standard.ModuleImportDemo
 */
public class ModuleImportDemo {

    public static void main(String[] args) {
        IO.println("╔═══════════════════════════════════════════════════╗");
        IO.println("║  JEP 494 — Module Import Declarations            ║");
        IO.println("╚═══════════════════════════════════════════════════╝");
        IO.println();

        // ─── 1. Using java.util types (no explicit import needed) ───
        // List, Map, Set are all available from `import module java.base`
        List<String> languages = List.of("Java", "Kotlin", "Scala", "Groovy");
        IO.println("Languages: " + languages);

        Map<String, Integer> versions = Map.of(
            "Java", 26,
            "Kotlin", 2,
            "Scala", 3
        );
        IO.println("Versions: " + versions);
        IO.println();

        // ─── 2. Using java.time types ───
        LocalDate today = LocalDate.now();
        LocalDate jdk26Release = LocalDate.of(2025, 9, 16);
        IO.println("Today: " + today);
        IO.println("JDK 26 GA target: " + jdk26Release);
        IO.println();

        // ─── 3. Using java.math types ───
        BigDecimal price = new BigDecimal("19.99");
        BigDecimal tax = price.multiply(new BigDecimal("0.08"));
        BigDecimal total = price.add(tax).setScale(2, RoundingMode.HALF_UP);
        IO.println("Price: $" + price + " + tax: $" + tax + " = $" + total);
        IO.println();

        // ─── 4. Using java.util.stream types ───
        String result = languages.stream()
            .filter(lang -> lang.startsWith("J") || lang.startsWith("S"))
            .collect(Collectors.joining(", "));
        IO.println("JVM languages starting with J or S: " + result);
        IO.println();

        // ─── 5. Using java.nio.file types ───
        Path currentDir = Path.of(".");
        IO.println("Current directory: " + currentDir.toAbsolutePath().normalize());
        IO.println();

        // ─── 6. Using java.util.concurrent types ───
        ConcurrentHashMap<String, Integer> concurrentMap = new ConcurrentHashMap<>();
        concurrentMap.put("threads", Runtime.getRuntime().availableProcessors());
        IO.println("Available processors: " + concurrentMap.get("threads"));
        IO.println();

        // ─── 7. Using java.util.function types ───
        Predicate<String> isLong = s -> s.length() > 4;
        Function<String, String> toUpper = String::toUpperCase;
        languages.stream()
            .filter(isLong)
            .map(toUpper)
            .forEach(lang -> IO.println("  Long language (uppercased): " + lang));

        IO.println();
        IO.println("✅ All types above were available from a single:");
        IO.println("   import module java.base;");
    }
}

