package org.example.preview;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  Primitive Types in Patterns — Real-World Use Cases                        ║
 * ║  Practical examples where primitive patterns give you a real advantage      ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * These examples show REAL scenarios where primitive type patterns
 * (JEP 488) replace verbose if-else chains, manual casting, and fragile
 * range checks with clean, readable, and exhaustive pattern matching.
 *
 * REFERENCES
 * ──────────
 *   • JEP 488 — Primitive Types in Patterns, instanceof, and switch (Second Preview):
 *       https://openjdk.org/jeps/488
 *   • JEP 455 — Primitive Types in Patterns, instanceof, and switch (First Preview):
 *       https://openjdk.org/jeps/455
 *   • JEP 441 — Pattern Matching for switch (finalized in JDK 21):
 *       https://openjdk.org/jeps/441
 *   • JEP 440 — Record Patterns (finalized in JDK 21):
 *       https://openjdk.org/jeps/440
 *
 * PATTERNS DEMONSTRATED
 * ─────────────────────
 *   1. HTTP status classifier    — Guarded int ranges → action/retry logic (API gateways, HTTP clients)
 *   2. Tax bracket calculator    — Progressive numeric ranges → marginal rates (payroll, fintech)
 *   3. IoT sensor alerts         — Record destructuring + primitive guards (smart agriculture, HVAC)
 *   4. Dynamic config parser     — Object-typed values matched by runtime type (JSON/YAML, feature flags)
 *   5. Game achievement engine   — Score thresholds → ranks/rewards (gamification, loyalty programs)
 *   6. Network packet classifier — Multi-field record patterns → firewall verdicts (IDS/IPS, SDN)
 *   7. Financial risk scoring    — Credit score + DTI + amount → loan decisions (lending, insurance)
 *   8. Responsive layout         — Viewport width breakpoints + exhaustive boolean switch (SSR, PDF gen)
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.preview.PrimitivePatternsRealWorldExamples
 */
public class PrimitivePatternsRealWorldExamples {

    public static void main(String[] args) {
        IO.println("╔═══════════════════════════════════════════════════════╗");
        IO.println("║  Primitive Patterns — Real-World Use Cases           ║");
        IO.println("╚═══════════════════════════════════════════════════════╝");
        IO.println();

        example1_HttpStatusCodeClassifier();
        example2_TaxBracketCalculator();
        example3_IoTSensorAlertSystem();
        example4_DynamicConfigParser();
        example5_GameAchievementEngine();
        example6_NetworkPacketClassifier();
        example7_FinancialRiskScoring();
        example8_ResponsiveLayoutBreakpoints();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 1 — HTTP Status Code Classifier
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: You're building an HTTP client library or API gateway.
    //  You need to classify response status codes into categories and
    //  decide on retry logic, logging level, and user-facing messages.
    //
    //  BEFORE: Nested if-else chains with manual range checks:
    //    if (status >= 200 && status < 300) { ... }
    //    else if (status >= 400 && status < 500) { ... }
    //    else if (status == 429) { ... }   // must come before the range!
    //
    //  AFTER: Guarded primitive patterns — specific cases naturally
    //  come first, ranges are expressed declaratively, the compiler
    //  ensures exhaustiveness.
    //
    //  Real users: Spring WebClient, OkHttp interceptors, API gateways,
    //              microservice resilience layers.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example1_HttpStatusCodeClassifier() {
        IO.println("1️⃣  HTTP Status Code Classifier");
        IO.println("   Use case: API gateways, HTTP clients, retry logic");
        IO.println("   ────────────────────────────────────────");

        int[] statusCodes = { 200, 201, 301, 400, 401, 404, 429, 500, 502, 503 };

        for (int status : statusCodes) {
            var action = classifyHttpStatus(status);
            IO.println("   HTTP " + status + " → " + action);
        }
        IO.println();
    }

    record HttpAction(String category, String logLevel, boolean shouldRetry, String message) {}

    static HttpAction classifyHttpStatus(int status) {
        return switch ((Object) status) {
            case int s when s >= 200 && s < 300
                -> new HttpAction("SUCCESS", "DEBUG", false, "Request succeeded");
            case int s when s == 301 || s == 302
                -> new HttpAction("REDIRECT", "INFO", true, "Following redirect");
            case int s when s == 429
                -> new HttpAction("RATE_LIMITED", "WARN", true, "Back off and retry after delay");
            case int s when s == 401 || s == 403
                -> new HttpAction("AUTH_FAILURE", "WARN", false, "Authentication/authorization failed");
            case int s when s >= 400 && s < 500
                -> new HttpAction("CLIENT_ERROR", "WARN", false, "Client error — fix the request");
            case int s when s == 502 || s == 503 || s == 504
                -> new HttpAction("INFRA_ERROR", "ERROR", true, "Infrastructure issue — retry with backoff");
            case int s when s >= 500
                -> new HttpAction("SERVER_ERROR", "ERROR", true, "Server error — retry");
            default
                -> new HttpAction("UNKNOWN", "WARN", false, "Unexpected status code");
        };
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 2 — Progressive Tax Bracket Calculator
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Calculate income tax using progressive brackets. Each
    //  income range has a different marginal rate. This is the textbook
    //  case for guarded primitive patterns — ranges of numeric values
    //  mapping to different behaviors.
    //
    //  BEFORE: A wall of if-else-if with easy-to-misorder conditions:
    //    if (income <= 11600) return income * 0.10;
    //    else if (income <= 47150) return 1160 + (income - 11600) * 0.12;
    //    // Which bracket comes first? Easy to get wrong.
    //
    //  AFTER: Each bracket is a clear, self-contained case. The compiler
    //  verifies exhaustiveness.
    //
    //  Real users: Tax software, payroll systems, financial planning apps.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example2_TaxBracketCalculator() {
        IO.println("2️⃣  Progressive Tax Bracket Calculator (2024 US Federal)");
        IO.println("   Use case: Payroll systems, tax software, financial planning");
        IO.println("   ────────────────────────────────────────");

        double[] incomes = { 8_000, 25_000, 55_000, 110_000, 250_000, 500_000, 750_000 };

        for (double income : incomes) {
            var result = calculateTax(income);
            IO.println("   $" + String.format("%,.0f", income)
                + " → Tax: $" + String.format("%,.2f", result.tax())
                + "  (effective rate: " + String.format("%.1f%%", result.effectiveRate() * 100) + ")"
                + "  Bracket: " + result.bracket());
        }
        IO.println();
    }

    record TaxResult(double tax, double effectiveRate, String bracket) {}

    /**
     * 2024 US Federal single filer brackets (simplified).
     * Primitive patterns + guards make each bracket a clean, readable case.
     */
    static TaxResult calculateTax(double income) {
        // Marginal bracket boundaries and rates
        String bracket = switch ((Object) income) {
            case double d when d <= 11_600  -> "10%";
            case double d when d <= 47_150  -> "12%";
            case double d when d <= 100_525 -> "22%";
            case double d when d <= 191_950 -> "24%";
            case double d when d <= 243_725 -> "32%";
            case double d when d <= 609_350 -> "35%";
            case double d                   -> "37%";
            default                         -> "unknown";
        };

        // Progressive calculation
        double tax = 0;
        double remaining = income;
        double[][] brackets = {
            {11_600, 0.10}, {47_150 - 11_600, 0.12}, {100_525 - 47_150, 0.22},
            {191_950 - 100_525, 0.24}, {243_725 - 191_950, 0.32},
            {609_350 - 243_725, 0.35}, {Double.MAX_VALUE, 0.37}
        };
        for (double[] b : brackets) {
            double taxable = Math.min(remaining, b[0]);
            tax += taxable * b[1];
            remaining -= taxable;
            if (remaining <= 0) break;
        }
        return new TaxResult(tax, tax / income, bracket);
    }

    // ════════════════════════════════════════════════════════════════��══════════
    //  EXAMPLE 3 — IoT Sensor Alert System
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: You receive sensor readings from IoT devices. Each reading
    //  is a record with primitive fields (temperature, humidity, battery
    //  voltage). You need to classify alerts based on the values.
    //
    //  This showcases RECORD DESTRUCTURING with PRIMITIVE PATTERNS:
    //  you can match on the record type AND extract/constrain its
    //  primitive fields in one expression.
    //
    //  BEFORE: Extract fields, then if-else on each one. Easy to forget
    //  a field or mis-combine conditions:
    //    var temp = reading.temperature();
    //    var humidity = reading.humidity();
    //    if (temp > 80) { ... }
    //    else if (humidity > 90 && temp > 60) { ... }
    //
    //  AFTER: Record patterns with primitive guards make it a single
    //  declarative switch.
    //
    //  Real users: Smart agriculture, warehouse monitoring, HVAC systems,
    //              industrial IoT platforms.
    // ═══════════════════════════════════════════════════════════════════════════
    record SensorReading(String deviceId, double temperature, double humidity, double batteryVoltage) {}
    record Alert(String severity, String message) {}

    static void example3_IoTSensorAlertSystem() {
        IO.println("3️⃣  IoT Sensor Alert System");
        IO.println("   Use case: Smart agriculture, warehouse monitoring, HVAC");
        IO.println("   ────────────────────────────────────────");

        List<SensorReading> readings = List.of(
            new SensorReading("greenhouse-01", 42.5, 85.0, 3.7),   // hot + humid
            new SensorReading("warehouse-03",  -5.0, 30.0, 3.5),   // freezing
            new SensorReading("server-room-A", 28.0, 45.0, 2.8),   // low battery
            new SensorReading("farm-field-07", 22.0, 55.0, 3.6),   // all normal
            new SensorReading("cold-storage-2", 12.0, 92.0, 3.1),  // moisture risk
            new SensorReading("outdoor-unit-5", 55.0, 20.0, 3.4)   // extreme heat
        );

        for (SensorReading reading : readings) {
            Alert alert = classifySensorReading(reading);
            IO.println("   " + reading.deviceId()
                + " [" + reading.temperature() + "°C, " + reading.humidity() + "% RH, "
                + reading.batteryVoltage() + "V]");
            IO.println("      → [" + alert.severity() + "] " + alert.message());
        }
        IO.println();
    }

    static Alert classifySensorReading(SensorReading reading) {
        return switch (reading) {
            case SensorReading(var id, double t, double h, double bv)
                when bv < 3.0
                -> new Alert("🔴 CRITICAL", "Battery critically low (" + bv + "V) — replace immediately");

            case SensorReading(var id, double t, double h, double bv)
                when t > 50.0
                -> new Alert("🔴 CRITICAL", "Extreme temperature (" + t + "°C) — equipment at risk");

            case SensorReading(var id, double t, double h, double bv)
                when t < 0
                -> new Alert("🟠 WARNING", "Freezing conditions (" + t + "°C) — check for frost damage");

            case SensorReading(var id, double t, double h, double bv)
                when t > 35.0 && h > 70.0
                -> new Alert("🟠 WARNING", "Heat + humidity combo (" + t + "°C, " + h + "%) — heat stress risk");

            case SensorReading(var id, double t, double h, double bv)
                when h > 90.0
                -> new Alert("🟡 CAUTION", "Very high humidity (" + h + "%) — moisture/mold risk");

            case SensorReading(var id, double t, double h, double bv)
                when t > 35.0
                -> new Alert("🟡 CAUTION", "High temperature (" + t + "°C) — monitor closely");

            case SensorReading(var id, double t, double h, double bv)
                -> new Alert("🟢 OK", "All readings normal");
        };
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 4 — Dynamic Configuration / JSON-like Value Processing
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: You're reading configuration values that can be int,
    //  double, boolean, or String. Think JSON parsing, YAML config,
    //  feature flags, or dynamic database columns.
    //
    //  Primitive patterns let you handle Object-typed values by matching
    //  their actual runtime type, including primitives (via autoboxing).
    //
    //  BEFORE: instanceof checks + casts, often missing numeric types:
    //    if (value instanceof String s) { ... }
    //    else if (value instanceof Integer i) { ... }
    //    else if (value instanceof Boolean b) { ... }
    //    // Forgot Double! Forgot Long!
    //
    //  AFTER: A single exhaustive switch with clear precedence.
    //
    //  Real users: Config libraries, JSON/YAML parsers, NoSQL drivers,
    //              dynamic form builders, feature flag systems.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example4_DynamicConfigParser() {
        IO.println("4️⃣  Dynamic Configuration Value Parser");
        IO.println("   Use case: Feature flags, JSON parsing, config libraries");
        IO.println("   ────────────────────────────────────────");

        // Simulate config entries loaded from JSON/YAML/DB
        Map<String, Object> config = new HashMap<>();
        config.put("max_retries", 3);
        config.put("timeout_seconds", 30.5);
        config.put("feature_dark_mode", true);
        config.put("api_endpoint", "https://api.example.com/v2");
        config.put("cache_ttl_ms", 60_000L);
        config.put("verbose", false);

        for (var entry : config.entrySet()) {
            String validation = validateConfigValue(entry.getKey(), entry.getValue());
            IO.println("   " + entry.getKey() + " = " + entry.getValue() + " → " + validation);
        }
        IO.println();
    }

    static String validateConfigValue(String key, Object value) {
        return switch (value) {
            case Integer i when i < 0
                -> "❌ Invalid: negative integer not allowed for '" + key + "'";
            case Integer i
                -> "✅ int=" + i + " (will use as numeric setting)";
            case Long l when l < 0
                -> "❌ Invalid: negative long not allowed for '" + key + "'";
            case Long l
                -> "✅ long=" + l + " (will use as large numeric setting)";
            case Double d when d.isNaN() || d.isInfinite()
                -> "❌ Invalid: NaN/Infinite not allowed for '" + key + "'";
            case Double d when d < 0
                -> "❌ Invalid: negative double not allowed for '" + key + "'";
            case Double d
                -> "✅ double=" + d + " (will use as decimal setting)";
            case Boolean b
                -> "✅ boolean=" + b + " (feature flag)";
            case String s when s.isBlank()
                -> "❌ Invalid: blank string for '" + key + "'";
            case String s
                -> "✅ string=\"" + s + "\"";
            default
                -> "⚠️ Unknown type: " + value.getClass().getSimpleName();
        };
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 5 — Game Achievement / Scoring Engine
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: A game awards achievements based on the player's score.
    //  Score thresholds map to different titles and rewards. The score
    //  is a primitive int — perfect for guarded primitive patterns.
    //
    //  BEFORE: A chain of if-else that's hard to read and easy to mess up:
    //    if (score >= 10000) rank = "Legend";
    //    else if (score >= 5000) rank = "Master";
    //    // Are these >= or >? Did I get the order right?
    //
    //  AFTER: Declarative, top-to-bottom matching. Each tier is
    //  self-documenting.
    //
    //  Real users: Game engines, gamification platforms, loyalty programs,
    //              leaderboard services.
    // ═══════════════════════════════════════════════════════════════════════════
    record PlayerScore(String name, int score) {}
    record Achievement(String rank, String badge, int bonusCoins) {}

    static void example5_GameAchievementEngine() {
        IO.println("5️⃣  Game Achievement Engine");
        IO.println("   Use case: Game scoring, loyalty programs, gamification");
        IO.println("   ────────────────────────────────────────");

        List<PlayerScore> players = List.of(
            new PlayerScore("Alice",   15_230),
            new PlayerScore("Bob",      7_500),
            new PlayerScore("Charlie",  3_200),
            new PlayerScore("Diana",    1_100),
            new PlayerScore("Eve",        450),
            new PlayerScore("Frank",       80)
        );

        for (PlayerScore player : players) {
            Achievement ach = awardAchievement(player);
            IO.println("   " + player.name() + " (score: " + player.score() + ")"
                + " → " + ach.badge() + " " + ach.rank()
                + " (+" + ach.bonusCoins() + " coins)");
        }
        IO.println();
    }

    static Achievement awardAchievement(PlayerScore player) {
        return switch (player) {
            case PlayerScore(var name, int s) when s >= 10_000
                -> new Achievement("Legend", "🏆", 5000);
            case PlayerScore(var name, int s) when s >= 5_000
                -> new Achievement("Master", "⭐", 2000);
            case PlayerScore(var name, int s) when s >= 2_000
                -> new Achievement("Expert", "🥇", 1000);
            case PlayerScore(var name, int s) when s >= 500
                -> new Achievement("Skilled", "🥈", 500);
            case PlayerScore(var name, int s) when s >= 100
                -> new Achievement("Novice", "🥉", 100);
            case PlayerScore(var name, int s)
                -> new Achievement("Beginner", "🎮", 10);
        };
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 6 — Network Packet Classifier
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: A firewall or network monitor classifies packets by
    //  port number, protocol type, and payload size. All of these are
    //  primitive int fields in a record. Destructuring + guarded patterns
    //  make the rules declarative and auditable.
    //
    //  BEFORE: Deeply nested if-else with magic numbers:
    //    if (packet.port() == 443 && packet.protocol() == 6) { ... }
    //    else if (packet.port() >= 1 && packet.port() <= 1023) { ... }
    //
    //  AFTER: Each firewall rule is a readable case with named fields.
    //
    //  Real users: Network security appliances, cloud WAFs, SDN controllers,
    //              intrusion detection systems.
    // ═══════════════════════════════════════════════════════════════════════════
    record Packet(int sourcePort, int destPort, int protocol, int payloadBytes) {
        static final int TCP = 6;
        static final int UDP = 17;
    }

    record PacketVerdict(String action, String reason, String priority) {}

    static void example6_NetworkPacketClassifier() {
        IO.println("6️⃣  Network Packet Classifier");
        IO.println("   Use case: Firewalls, IDS/IPS, network monitoring, SDN");
        IO.println("   ────────────────────────────────────────");

        List<Packet> packets = List.of(
            new Packet(52000, 443, Packet.TCP, 1200),   // HTTPS
            new Packet(48000, 22,  Packet.TCP, 200),    // SSH
            new Packet(51000, 53,  Packet.UDP, 64),     // DNS
            new Packet(49000, 80,  Packet.TCP, 50_000), // Large HTTP
            new Packet(60000, 31337, Packet.TCP, 8000), // Suspicious port
            new Packet(55000, 3306, Packet.TCP, 500)    // MySQL
        );

        for (Packet pkt : packets) {
            PacketVerdict verdict = classifyPacket(pkt);
            IO.println("   :" + pkt.sourcePort() + " → :" + pkt.destPort()
                + " (" + (pkt.protocol() == Packet.TCP ? "TCP" : "UDP") + ", "
                + pkt.payloadBytes() + "B)");
            IO.println("      → [" + verdict.priority() + "] " + verdict.action() + ": " + verdict.reason());
        }
        IO.println();
    }

    static PacketVerdict classifyPacket(Packet pkt) {
        return switch (pkt) {
            // Large payload on any port — possible data exfiltration
            case Packet(int sp, int dp, int proto, int size)
                when size > 10_000
                -> new PacketVerdict("INSPECT", "Unusually large payload (" + size + " bytes)", "🟠 HIGH");

            // Known dangerous / unusual ports
            case Packet(int sp, int dp, int proto, int size)
                when dp == 31337 || dp == 12345 || dp == 6667
                -> new PacketVerdict("BLOCK", "Suspicious destination port " + dp, "🔴 CRITICAL");

            // Database ports should only come from internal sources
            case Packet(int sp, int dp, int proto, int size)
                when dp == 3306 || dp == 5432 || dp == 27017
                -> new PacketVerdict("FLAG", "Database port access (:" + dp + ") — verify source", "🟠 HIGH");

            // Standard HTTPS — allow
            case Packet(int sp, int dp, int proto, int size)
                when dp == 443 && proto == Packet.TCP
                -> new PacketVerdict("ALLOW", "Standard HTTPS traffic", "🟢 LOW");

            // SSH — allow but log
            case Packet(int sp, int dp, int proto, int size)
                when dp == 22 && proto == Packet.TCP
                -> new PacketVerdict("ALLOW+LOG", "SSH connection — logging for audit", "🟡 MEDIUM");

            // DNS — allow
            case Packet(int sp, int dp, int proto, int size)
                when dp == 53
                -> new PacketVerdict("ALLOW", "DNS query", "🟢 LOW");

            // HTTP — allow
            case Packet(int sp, int dp, int proto, int size)
                when dp == 80 && proto == Packet.TCP
                -> new PacketVerdict("ALLOW", "HTTP traffic", "🟢 LOW");

            // Anything else on well-known ports
            case Packet(int sp, int dp, int proto, int size)
                when dp >= 1 && dp <= 1023
                -> new PacketVerdict("ALLOW+LOG", "Well-known port " + dp, "🟡 MEDIUM");

            // Ephemeral ports — default allow
            case Packet(int sp, int dp, int proto, int size)
                -> new PacketVerdict("ALLOW", "Ephemeral port traffic", "🟢 LOW");
        };
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 7 — Financial Risk Scoring
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: A lending platform evaluates loan applications. Multiple
    //  numeric factors (credit score, debt-to-income ratio, loan amount)
    //  are combined into a risk assessment. Record destructuring with
    //  primitive patterns lets you express complex multi-field rules
    //  cleanly.
    //
    //  BEFORE: Nested ifs that are hard to audit:
    //    if (app.creditScore() < 580) { risk = "HIGH"; }
    //    else if (app.creditScore() < 670 && app.dti() > 0.43) { ... }
    //
    //  AFTER: Each risk rule is a single case — easy to audit, easy to
    //  hand to compliance for review.
    //
    //  Real users: Banks, fintech lenders, insurance underwriting,
    //              credit risk models.
    // ═══════════════════════════════════════════════════════════════════════════
    record LoanApplication(String applicant, int creditScore, double dti, double loanAmount) {}
    record RiskAssessment(String level, String decision, String reason) {}

    static void example7_FinancialRiskScoring() {
        IO.println("7️⃣  Financial Risk Scoring");
        IO.println("   Use case: Lending platforms, insurance underwriting, credit risk");
        IO.println("   ────────────────────────────────────────");

        List<LoanApplication> applications = List.of(
            new LoanApplication("Alice",   780, 0.25, 250_000),  // excellent
            new LoanApplication("Bob",     650, 0.38, 180_000),  // borderline
            new LoanApplication("Charlie", 550, 0.50, 300_000),  // high risk
            new LoanApplication("Diana",   720, 0.45, 500_000),  // good score but high DTI
            new LoanApplication("Eve",     690, 0.30, 50_000),   // decent, small loan
            new LoanApplication("Frank",   580, 0.55, 400_000)   // poor score + high DTI
        );

        for (LoanApplication app : applications) {
            RiskAssessment risk = assessRisk(app);
            IO.println("   " + app.applicant()
                + " (credit: " + app.creditScore()
                + ", DTI: " + String.format("%.0f%%", app.dti() * 100)
                + ", loan: $" + String.format("%,.0f", app.loanAmount()) + ")");
            IO.println("      → [" + risk.level() + "] " + risk.decision() + " — " + risk.reason());
        }
        IO.println();
    }

    static RiskAssessment assessRisk(LoanApplication app) {
        return switch (app) {
            // Sub-prime credit → automatic high risk
            case LoanApplication(var name, int cs, double dti, double amt)
                when cs < 580
                -> new RiskAssessment("🔴 HIGH", "DECLINE", "Credit score below minimum threshold");

            // Poor credit + high DTI → decline
            case LoanApplication(var name, int cs, double dti, double amt)
                when cs < 670 && dti > 0.43
                -> new RiskAssessment("🔴 HIGH", "DECLINE", "Low credit + high debt-to-income ratio");

            // Good credit but extreme DTI
            case LoanApplication(var name, int cs, double dti, double amt)
                when dti > 0.50
                -> new RiskAssessment("🟠 MEDIUM", "MANUAL REVIEW", "DTI exceeds 50% despite credit score");

            // Large loan + moderate credit → manual review
            case LoanApplication(var name, int cs, double dti, double amt)
                when cs < 700 && amt > 200_000
                -> new RiskAssessment("🟠 MEDIUM", "MANUAL REVIEW", "Large loan with moderate credit");

            // Good credit + reasonable DTI
            case LoanApplication(var name, int cs, double dti, double amt)
                when cs >= 700 && dti <= 0.43
                -> new RiskAssessment("🟢 LOW", "AUTO-APPROVE", "Strong credit profile");

            // Everything else → manual review
            case LoanApplication(var name, int cs, double dti, double amt)
                -> new RiskAssessment("🟡 MODERATE", "MANUAL REVIEW", "Does not fit standard criteria");
        };
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 8 — Responsive Layout Breakpoints
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: A server-side UI framework (like Vaadin or JSF) or a
    //  PDF/report generator needs to choose layout based on viewport
    //  width. Primitive patterns with guards replace the classic
    //  breakpoint if-else chain.
    //
    //  Also demonstrates EXHAUSTIVE BOOLEAN SWITCH: choosing layout
    //  direction based on an RTL flag — no default needed!
    //
    //  BEFORE:
    //    if (width < 576) layout = "mobile-single-col";
    //    else if (width < 768) layout = "tablet-stack";
    //    ...
    //
    //  AFTER: Declarative breakpoints as switch cases.
    //
    //  Real users: Server-side rendering, PDF generators, email template
    //              engines, responsive dashboard builders.
    // ═══════════════════════════════════════════════════════════════════════════
    record Viewport(int width, boolean isRtl) {}
    record LayoutConfig(String name, int columns, String direction, String description) {}

    static void example8_ResponsiveLayoutBreakpoints() {
        IO.println("8️⃣  Responsive Layout Breakpoints");
        IO.println("   Use case: Server-side rendering, PDF generators, dashboards");
        IO.println("   ────────────────────────────────────────");

        List<Viewport> viewports = List.of(
            new Viewport(320, false),    // Phone
            new Viewport(600, false),    // Large phone / small tablet
            new Viewport(768, true),     // Tablet (RTL)
            new Viewport(1024, false),   // Small desktop
            new Viewport(1440, false),   // Desktop
            new Viewport(1920, true)     // Wide desktop (RTL)
        );

        for (Viewport vp : viewports) {
            LayoutConfig layout = chooseLayout(vp);
            IO.println("   " + vp.width() + "px"
                + (vp.isRtl() ? " [RTL]" : "")
                + " → " + layout.name()
                + " (" + layout.columns() + " col, " + layout.direction() + ")"
                + " — " + layout.description());
        }
        IO.println();
    }

    static LayoutConfig chooseLayout(Viewport vp) {
        int cols = switch (vp) {
            case Viewport(int w, boolean rtl) when w < 576  -> 1;
            case Viewport(int w, boolean rtl) when w < 768  -> 2;
            case Viewport(int w, boolean rtl) when w < 1200 -> 3;
            case Viewport(int w, boolean rtl)               -> 4;
        };

        // Exhaustive boolean switch — no default needed!
        String direction = switch (vp.isRtl()) {
            case true  -> "right-to-left";
            case false -> "left-to-right";
        };

        String name = switch (vp) {
            case Viewport(int w, boolean rtl) when w < 576  -> "📱 Mobile";
            case Viewport(int w, boolean rtl) when w < 768  -> "📱 Phablet";
            case Viewport(int w, boolean rtl) when w < 1024 -> "📋 Tablet";
            case Viewport(int w, boolean rtl) when w < 1440 -> "🖥️ Desktop";
            case Viewport(int w, boolean rtl)               -> "🖥️ Wide Desktop";
        };

        String desc = switch ((Object) cols) {
            case int c when c == 1 -> "Single column, stacked layout";
            case int c when c == 2 -> "Side-by-side panels";
            case int c when c == 3 -> "Three-column content grid";
            case int c             -> "Full multi-column dashboard";
            default                -> "unknown";
        };

        return new LayoutConfig(name, cols, direction, desc);
    }
}

