package org.example.standard;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  Simple Source Files & Instance Main — Real-World Use Cases                  ║
 * ║  Practical examples where JEP 495 gives you a real advantage                ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * These examples show REAL scenarios where simplified Java programs
 * (instance main, reduced ceremony, IO class) lower the barrier for
 * scripting, education, prototyping, and operational tooling.
 *
 * REFERENCES
 * ──────────
 *   • JEP 495 — Simple Source Files and Instance Main Methods:
 *       https://openjdk.org/jeps/495
 *
 * PATTERNS DEMONSTRATED
 * ─────────────────────
 *   1. DevOps health checker    — Quick script to ping services (ops tooling)
 *   2. Data format converter    — One-off CSV→JSON conversion script (data eng)
 *   3. Teaching example         — Progressive complexity for beginners (education)
 *   4. Config validator         — Validate deployment configs before rollout (CI/CD)
 *   5. Log analyzer             — Quick ad-hoc log analysis script (incident response)
 *   6. Prototype / spike        — Rapid prototype of an algorithm (R&D, interviews)
 *
 * NOTE: This file is in a package, so it can't be a true implicit class.
 * The examples demonstrate the instance main method pattern and show
 * what WOULD be implicit classes if placed in the unnamed package.
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.standard.SimpleSourceFileRealWorldExamples
 */
public class SimpleSourceFileRealWorldExamples {

    private final java.util.List<String> services = java.util.List.of(
        "auth-service:8080", "payment-service:8081",
        "catalog-service:8082", "search-service:8083"
    );

    private int checksPerformed = 0;
    private int score = 0;
    private final java.util.List<String> achievements = new java.util.ArrayList<>();
    private final java.util.Map<String, Double> scores = new java.util.LinkedHashMap<>();

    void main() {
        IO.println("╔═══════════════════════════════════════════════════════╗");
        IO.println("║  Simple Source Files — Real-World Use Cases           ║");
        IO.println("╚═══════════════════════════════════════════════════════╝");
        IO.println();

        example1_DevOpsHealthChecker();
        example2_DataFormatConverter();
        example3_TeachingProgression();
        example4_ConfigValidator();
        example5_LogAnalyzer();
        example6_AlgorithmPrototype();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 1 — DevOps Health Checker Script
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: An SRE needs a quick script to check if services are
    //  reachable. With JDK 26, this is as simple as a Python script:
    //    void main() { IO.println("Checking..."); }
    //
    //  Real users: SRE teams, on-call scripts, smoke tests, Docker health checks.
    // ═══════════════════════════════════════════════════════════════════════════
    void example1_DevOpsHealthChecker() {
        IO.println("1️⃣  DevOps Health Checker Script");
        IO.println("   Use case: SRE scripts, smoke tests, Docker health checks");
        IO.println("   ────────────────────────────────────────");

        IO.println("   Checking " + services.size() + " services...");
        for (String service : services) {
            checksPerformed++;
            boolean healthy = !service.contains("search");
            IO.println("   " + service + " → " + (healthy ? "🟢 UP" : "🔴 DOWN"));
        }
        IO.println("   Checks performed: " + checksPerformed);
        IO.println("   ✅ No class declaration, no static, no String[] needed");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 2 — Data Format Converter (CSV → JSON)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Quick one-off script to convert CSV data to JSON format.
    //  Previously, data engineers would use Python for this.
    //
    //  Real users: Data engineers, ETL one-offs, format migration scripts.
    // ═══════════════════════════════════════════════════════════════════════════
    void example2_DataFormatConverter() {
        IO.println("2️⃣  Data Format Converter (CSV → JSON)");
        IO.println("   Use case: Data engineering one-offs, format migrations");
        IO.println("   ────────────────────────────────────────");

        String[] csvLines = {
            "name,age,city",
            "Alice,28,New York",
            "Bob,35,London",
            "Carol,42,Tokyo"
        };
        String[] headers = csvLines[0].split(",");

        IO.println("   CSV Input:");
        for (String line : csvLines) IO.println("     " + line);

        IO.println("   JSON Output:");
        IO.println("   [");
        for (int i = 1; i < csvLines.length; i++) {
            String[] values = csvLines[i].split(",");
            var json = new StringBuilder("     {");
            for (int j = 0; j < headers.length; j++) {
                if (j > 0) json.append(", ");
                json.append("\"").append(headers[j]).append("\": \"").append(values[j]).append("\"");
            }
            json.append("}");
            if (i < csvLines.length - 1) json.append(",");
            IO.println(json.toString());
        }
        IO.println("   ]");
        IO.println("   ✅ No imports needed — IO.println just works");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 3 — Teaching Progression
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: In a CS101 course, students learn Java progressively.
    //  Instance main lets them use state without understanding 'static'.
    //
    //  Real users: CS professors, bootcamps, online courses, Oracle Academy.
    // ═══════════════════════════════════════════════════════════════════════════
    void example3_TeachingProgression() {
        IO.println("3️⃣  Teaching Progression (CS101-Friendly)");
        IO.println("   Use case: CS education, bootcamps, Java tutorials");
        IO.println("   ────────────────────────────────────────");

        IO.println("   Lesson 1: Hello World");
        IO.println("     void main() { IO.println(\"Hello!\"); }");
        IO.println("     → Runs! No public class, no static, no String[]");
        IO.println();

        IO.println("   Lesson 2: Using instance state (no 'static')");
        score = 100;
        achievements.add("First Program");
        achievements.add("Variables");
        IO.println("     score = " + score);
        IO.println("     achievements = " + achievements);
        IO.println();

        IO.println("   Lesson 3: Instance methods (no 'static')");
        IO.println("     gradeScore(" + score + ") = " + gradeScore(score));
        IO.println("   ✅ Students learn logic first, ceremony later");
        IO.println();
    }

    String gradeScore(int s) {
        if (s >= 90) return "A";
        if (s >= 80) return "B";
        if (s >= 70) return "C";
        return "F";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 4 — Config Validator (CI/CD Pre-Check)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: A CI/CD script validates config before deploying.
    //
    //  Real users: GitHub Actions, GitLab CI, pre-deploy hooks.
    // ═══════════════════════════════════════════════════════════════════════════
    void example4_ConfigValidator() {
        IO.println("4️⃣  Config Validator (CI/CD Pre-Check Script)");
        IO.println("   Use case: GitHub Actions, GitLab CI, pre-deploy validation");
        IO.println("   ────────────────────────────────────────");

        var config = java.util.Map.of(
            "DATABASE_URL", "jdbc:postgresql://prod-db:5432/app",
            "REDIS_HOST", "redis-cluster.internal",
            "API_KEY", "ak-prod-xxxx-yyyy",
            "MAX_CONNECTIONS", "50",
            "LOG_LEVEL", "INFO"
        );

        String[] required = {"DATABASE_URL", "REDIS_HOST", "API_KEY", "SECRET_KEY"};
        var errors = new java.util.ArrayList<String>();

        for (String key : required) {
            String value = config.get(key);
            if (value == null || value.isBlank()) {
                errors.add("❌ Missing required config: " + key);
            } else {
                IO.println("   ✅ " + key + " = " + maskValue(value));
            }
        }

        if (!errors.isEmpty()) {
            IO.println("   Validation errors:");
            errors.forEach(e -> IO.println("     " + e));
            IO.println("   🚫 Deploy blocked — fix config and retry");
        }
        IO.println();
    }

    private String maskValue(String value) {
        if (value.length() <= 8) return "****";
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 5 — Log Analyzer (Incident Response)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: During an incident, an SRE quickly analyzes log entries.
    //  Just `java LogAnalyzer.java` — no project setup needed.
    //
    //  Real users: On-call engineers, incident response, post-mortem analysis.
    // ═══════════════════════════════════════════════════════════════════════════
    void example5_LogAnalyzer() {
        IO.println("5️⃣  Log Analyzer (Incident Response Script)");
        IO.println("   Use case: On-call SRE scripts, incident response, post-mortems");
        IO.println("   ────────────────────────────────────────");

        String[] logs = {
            "2026-03-15T10:00:01Z INFO  auth-service Request received",
            "2026-03-15T10:00:02Z ERROR payment-service Connection timeout",
            "2026-03-15T10:00:02Z ERROR payment-service Retry 1 failed",
            "2026-03-15T10:00:03Z ERROR payment-service Retry 2 failed",
            "2026-03-15T10:00:03Z WARN  catalog-service Cache miss rate high",
            "2026-03-15T10:00:04Z ERROR payment-service Circuit breaker OPEN",
            "2026-03-15T10:00:05Z INFO  auth-service Request completed",
            "2026-03-15T10:00:07Z ERROR payment-service Connection timeout"
        };

        var levelCounts = new java.util.LinkedHashMap<String, Integer>();
        var serviceCounts = new java.util.LinkedHashMap<String, Integer>();

        for (String log : logs) {
            String[] parts = log.split("\\s+", 4);
            String level = parts[1];
            String service = parts[2];
            levelCounts.merge(level, 1, Integer::sum);
            if (level.equals("ERROR")) serviceCounts.merge(service, 1, Integer::sum);
        }

        IO.println("   Log entries: " + logs.length);
        IO.println("   By level:");
        levelCounts.forEach((level, count) -> IO.println("     " + level + ": " + count));
        IO.println("   Errors by service:");
        serviceCounts.forEach((svc, count) -> IO.println("     " + svc + ": " + count));
        IO.println("   🔍 Root cause likely: payment-service DB connectivity");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 6 — Algorithm Prototype / Spike
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Prototyping a ranking algorithm. Just run
    //  `java Ranker.java` — no project setup, no build file.
    //
    //  Real users: R&D spikes, interview problems, algorithm exploration.
    // ═══════════════════════════════════════════════════════════════════════════
    void example6_AlgorithmPrototype() {
        IO.println("6️⃣  Algorithm Prototype (Quick Ranking Spike)");
        IO.println("   Use case: R&D spikes, interviews, algorithm exploration");
        IO.println("   ────────────────────────────────────────");

        record Item(String name, double relevance, double freshness, double popularity) {}

        var items = java.util.List.of(
            new Item("Java 26 Release Notes",    0.95, 0.9,  0.7),
            new Item("Java 21 Migration Guide",  0.8,  0.4,  0.9),
            new Item("Hello World Tutorial",     0.3,  0.2,  0.95),
            new Item("Virtual Threads Deep Dive",0.9,  0.7,  0.6),
            new Item("Spring Boot 4 Upgrade",    0.85, 0.85, 0.8)
        );

        double wR = 0.5, wF = 0.3, wP = 0.2;

        for (var item : items) {
            scores.put(item.name(), wR * item.relevance() + wF * item.freshness() + wP * item.popularity());
        }

        IO.println("   Ranking (relevance=0.5, freshness=0.3, popularity=0.2):");
        scores.entrySet().stream()
            .sorted(java.util.Map.Entry.<String, Double>comparingByValue().reversed())
            .forEach(e -> IO.println("     " + String.format("%.3f", e.getValue()) + " — " + e.getKey()));
        IO.println("   ✅ Prototype done — promote to real project when proven");
        IO.println();
    }
}

