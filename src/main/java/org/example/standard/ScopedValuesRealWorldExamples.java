package org.example.standard;

import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.ExecutionException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  Scoped Values — Real-World Use Cases                                       ║
 * ║  Practical examples where JEP 506 gives you a real advantage                ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * These examples show REAL scenarios where ScopedValue replaces ThreadLocal
 * with cleaner, safer, and more performant implicit context propagation.
 *
 * REFERENCES
 * ──────────
 *   • JEP 506 — Scoped Values:
 *       https://openjdk.org/jeps/506
 *   • JEP 444 — Virtual Threads:
 *       https://openjdk.org/jeps/444
 *
 * PATTERNS DEMONSTRATED
 * ─────────────────────
 *   1. Request context propagation — Pass user/tenant/traceId through call stack (web frameworks)
 *   2. Multi-tenant isolation      — Tenant-scoped DB routing without parameter drilling (SaaS)
 *   3. Security context            — Propagate auth principal to deep service layers (Spring Security)
 *   4. Distributed tracing         — TraceId/SpanId propagation (OpenTelemetry, Zipkin, Jaeger)
 *   5. Feature flags               — Scope-bound feature toggles (LaunchDarkly, Unleash)
 *   6. Audit context               — Carry audit metadata through business logic (SOX, HIPAA)
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.standard.ScopedValuesRealWorldExamples
 */
public class ScopedValuesRealWorldExamples {

    public static void main(String[] args) throws Exception {
        IO.println("╔═══════════════════════════════════════════════════════╗");
        IO.println("║  Scoped Values — Real-World Use Cases                ║");
        IO.println("╚═══════════════════════════════════════════════════════╝");
        IO.println();

        example1_RequestContextPropagation();
        example2_MultiTenantDbRouting();
        example3_SecurityContext();
        example4_DistributedTracing();
        example5_FeatureFlags();
        example6_AuditContext();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 1 — Request Context Propagation (Web Framework)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: In a web framework, each HTTP request carries context
    //  (user, request ID, locale) that needs to be available deep in the
    //  call stack — in services, repositories, and logging — without
    //  passing them as parameters through every method.
    //
    //  BEFORE: ThreadLocal — works but leaks if not cleaned, expensive
    //  with virtual threads, mutable (any code can overwrite).
    //
    //  AFTER: ScopedValue — immutable, auto-cleaned, lightweight with
    //  virtual threads, inheritable by child threads.
    //
    //  Real users: Spring MVC RequestContextHolder, Jakarta Servlet,
    //              Quarkus/Micronaut request scoping.
    // ═══════════════════════════════════════════════════════════════════════════
    private static final ScopedValue<String> CURRENT_USER = ScopedValue.newInstance();
    private static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();
    private static final ScopedValue<String> LOCALE = ScopedValue.newInstance();

    static void example1_RequestContextPropagation() {
        IO.println("1️⃣  Request Context Propagation (Web Framework)");
        IO.println("   Use case: Spring MVC, Jakarta Servlet, Quarkus request scoping");
        IO.println("   ────────────────────────────────────────");

        // Simulate two concurrent HTTP requests
        ScopedValue.where(CURRENT_USER, "alice")
            .where(REQUEST_ID, "req-abc-001")
            .where(LOCALE, "en-US")
            .run(() -> {
                IO.println("   [Request 1] Processing...");
                handleHttpRequest("/api/orders");
            });

        ScopedValue.where(CURRENT_USER, "hiroshi")
            .where(REQUEST_ID, "req-def-002")
            .where(LOCALE, "ja-JP")
            .run(() -> {
                IO.println("   [Request 2] Processing...");
                handleHttpRequest("/api/products");
            });

        IO.println("   ✅ Both requests isolated — no ThreadLocal leaks possible");
        IO.println();
    }

    private static void handleHttpRequest(String path) {
        // Controller layer — reads context without parameter passing
        IO.println("     Controller: " + path + " for user=" + CURRENT_USER.get());
        callServiceLayer();
    }

    private static void callServiceLayer() {
        // Service layer — still has access to request context
        IO.println("     Service: processing for reqId=" + REQUEST_ID.get());
        callRepository();
    }

    private static void callRepository() {
        // Repository/DAO layer — deepest level still sees the context
        IO.println("     Repository: locale=" + LOCALE.get()
            + ", user=" + CURRENT_USER.get());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 2 — Multi-Tenant Database Routing
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: A SaaS application routes database queries to the correct
    //  tenant's schema/database. The tenant ID is set at the HTTP filter
    //  level and needs to be visible to the data access layer.
    //
    //  Real users: Spring AbstractRoutingDataSource, Hibernate multi-tenancy,
    //              Flyway tenant migrations, any SaaS platform.
    // ═══════════════════════════════════════════════════════════════════════════
    private static final ScopedValue<String> TENANT_ID = ScopedValue.newInstance();
    private static final ScopedValue<String> TENANT_DB = ScopedValue.newInstance();

    static void example2_MultiTenantDbRouting() {
        IO.println("2️⃣  Multi-Tenant Database Routing");
        IO.println("   Use case: SaaS platforms, Hibernate multi-tenancy, schema routing");
        IO.println("   ────────────────────────────────────────");

        // Simulate requests from different tenants
        String[][] tenants = {
            {"acme-corp", "db-us-east-acme"},
            {"globex-inc", "db-eu-west-globex"},
            {"initech", "db-ap-south-initech"}
        };

        for (String[] tenant : tenants) {
            ScopedValue.where(TENANT_ID, tenant[0])
                .where(TENANT_DB, tenant[1])
                .run(() -> {
                    String result = executeQuery("SELECT count(*) FROM orders");
                    IO.println("   Tenant: " + TENANT_ID.get()
                        + " → DB: " + TENANT_DB.get()
                        + " → " + result);
                });
        }
        IO.println("   ✅ Each tenant isolated — no cross-tenant data leaks");
        IO.println();
    }

    private static String executeQuery(String sql) {
        // In real code, this would use TENANT_DB.get() to pick the datasource
        return "Query[" + sql + "] on " + TENANT_DB.get();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 3 — Security Context (Principal Propagation)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: After authentication, the security principal (user, roles)
    //  must be accessible everywhere for authorization checks. With
    //  ScopedValue, the principal is immutable and automatically scoped
    //  to the request lifecycle.
    //
    //  Real users: Spring Security SecurityContextHolder, Apache Shiro,
    //              Jakarta Security, custom RBAC systems.
    // ═══════════════════════════════════════════════════════════════════════════
    record SecurityPrincipal(String username, List<String> roles, String authMethod) {}

    private static final ScopedValue<SecurityPrincipal> SECURITY_CONTEXT =
        ScopedValue.newInstance();

    static void example3_SecurityContext() {
        IO.println("3️⃣  Security Context (Principal Propagation)");
        IO.println("   Use case: Spring Security, Apache Shiro, Jakarta Security, RBAC");
        IO.println("   ────────────────────────────────────────");

        var adminPrincipal = new SecurityPrincipal("alice",
            List.of("ADMIN", "USER"), "OAuth2");
        var userPrincipal = new SecurityPrincipal("bob",
            List.of("USER"), "LDAP");

        // Admin request
        ScopedValue.where(SECURITY_CONTEXT, adminPrincipal)
            .run(() -> {
                IO.println("   " + checkPermission("DELETE /api/users/42"));
                IO.println("   " + checkPermission("GET /api/reports"));
            });

        // Regular user request
        ScopedValue.where(SECURITY_CONTEXT, userPrincipal)
            .run(() -> {
                IO.println("   " + checkPermission("DELETE /api/users/42"));
                IO.println("   " + checkPermission("GET /api/profile"));
            });

        IO.println("   ✅ Security context immutable within scope — no elevation attacks");
        IO.println();
    }

    private static String checkPermission(String action) {
        SecurityPrincipal principal = SECURITY_CONTEXT.get();
        boolean allowed = principal.roles().contains("ADMIN") || action.startsWith("GET");
        return "[" + principal.username() + "/" + principal.authMethod() + "] "
            + action + " → " + (allowed ? "✅ ALLOWED" : "❌ DENIED");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 4 — Distributed Tracing (Trace/Span Propagation)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Every request gets a trace ID that must propagate through
    //  all service calls, including forked virtual threads. ScopedValue
    //  inherits automatically to child threads in StructuredTaskScope.
    //
    //  Real users: OpenTelemetry, Zipkin, Jaeger, AWS X-Ray, Datadog APM.
    // ═══════════════════════════════════════════════════════════════════════════
    private static final ScopedValue<String> TRACE_ID = ScopedValue.newInstance();
    private static final ScopedValue<String> SPAN_ID = ScopedValue.newInstance();

    static void example4_DistributedTracing() throws Exception {
        IO.println("4️⃣  Distributed Tracing (Trace/Span Propagation)");
        IO.println("   Use case: OpenTelemetry, Zipkin, Jaeger, AWS X-Ray");
        IO.println("   ────────────────────────────────────────");

        String traceId = "trace-" + UUID.randomUUID().toString().substring(0, 8);

        ScopedValue.where(TRACE_ID, traceId)
            .where(SPAN_ID, "span-root")
            .run(() -> {
                IO.println("   Root span: trace=" + TRACE_ID.get()
                    + ", span=" + SPAN_ID.get());

                // Fork child tasks — they inherit scoped values automatically!
                try (var scope = StructuredTaskScope.open()) {
                    var t1 = scope.fork(() -> {
                        Thread.sleep(50);
                        return "  Child 1: trace=" + TRACE_ID.get()
                            + " (inherited ✅)";
                    });
                    var t2 = scope.fork(() -> {
                        Thread.sleep(30);
                        return "  Child 2: trace=" + TRACE_ID.get()
                            + " (inherited ✅)";
                    });

                    scope.join();
                    IO.println("   " + t1.get());
                    IO.println("   " + t2.get());
                } catch (Exception e) {
                    IO.println("   Error: " + e.getMessage());
                }
            });

        IO.println("   ✅ Trace ID propagated to child virtual threads automatically");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 5 — Feature Flags (Scoped Toggles)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Feature flags need to be evaluated once at the request
    //  boundary and then available throughout the processing pipeline.
    //  With ScopedValue, flags are immutable per-request — no risk of
    //  mid-request flag changes causing inconsistent behavior.
    //
    //  Real users: LaunchDarkly, Unleash, Flagsmith, Split.io,
    //              custom feature flag systems.
    // ═══════════════════════════════════════════════════════════════════════════
    record FeatureFlags(boolean newCheckoutFlow, boolean betaPricing, boolean darkMode) {}

    private static final ScopedValue<FeatureFlags> FEATURES = ScopedValue.newInstance();

    static void example5_FeatureFlags() {
        IO.println("5️⃣  Feature Flags (Scoped Toggles)");
        IO.println("   Use case: LaunchDarkly, Unleash, Flagsmith, custom toggles");
        IO.println("   ────────────────────────────────────────");

        // User in beta group
        ScopedValue.where(FEATURES, new FeatureFlags(true, true, false))
            .run(() -> {
                IO.println("   [Beta user]");
                renderCheckout();
                calculatePrice(100.0);
            });

        // Regular user
        ScopedValue.where(FEATURES, new FeatureFlags(false, false, true))
            .run(() -> {
                IO.println("   [Regular user]");
                renderCheckout();
                calculatePrice(100.0);
            });

        IO.println("   ✅ Flags immutable within request — no mid-request inconsistencies");
        IO.println();
    }

    private static void renderCheckout() {
        FeatureFlags flags = FEATURES.get();
        String flow = flags.newCheckoutFlow() ? "NEW single-page checkout" : "Classic multi-step checkout";
        String theme = flags.darkMode() ? "dark" : "light";
        IO.println("     Checkout: " + flow + " (" + theme + " theme)");
    }

    private static void calculatePrice(double basePrice) {
        FeatureFlags flags = FEATURES.get();
        double price = flags.betaPricing() ? basePrice * 0.9 : basePrice;
        IO.println("     Price: $" + String.format("%.2f", price)
            + (flags.betaPricing() ? " (10% beta discount)" : " (standard)"));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 6 — Audit Context (Compliance Metadata)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: For regulatory compliance, every data modification must
    //  carry audit metadata (who, when, from where, why). This context
    //  is set at the API boundary and must be available in the persistence
    //  layer to write audit log entries.
    //
    //  Real users: SOX-compliant financial systems, HIPAA healthcare apps,
    //              GDPR data processors, PCI-DSS payment systems.
    // ═══════════════════════════════════════════════════════════════════════════
    record AuditContext(String operator, String sourceIp, String reason, Instant timestamp) {}

    private static final ScopedValue<AuditContext> AUDIT = ScopedValue.newInstance();

    static void example6_AuditContext() {
        IO.println("6️⃣  Audit Context (Compliance Metadata Propagation)");
        IO.println("   Use case: SOX, HIPAA, GDPR, PCI-DSS compliance systems");
        IO.println("   ────────────────────────────────────────");

        // API controller sets audit context from the authenticated request
        var ctx = new AuditContext("admin@acme.com", "10.0.1.42",
            "Customer data export request #EXP-2026-001", Instant.now());

        ScopedValue.where(AUDIT, ctx)
            .run(() -> {
                IO.println("   API: Processing data export...");
                exportCustomerData(List.of("customer-001", "customer-002", "customer-003"));
            });

        IO.println("   ✅ Audit trail complete — every DB write logged with operator context");
        IO.println();
    }

    private static void exportCustomerData(List<String> customerIds) {
        // Service layer — no audit parameters needed
        for (String id : customerIds) {
            writeAuditLog("EXPORT", id);
        }
    }

    private static void writeAuditLog(String action, String entityId) {
        // Persistence layer — reads audit context from ScopedValue
        AuditContext ctx = AUDIT.get();
        IO.println("     AUDIT: " + action + " " + entityId
            + " by=" + ctx.operator()
            + " from=" + ctx.sourceIp()
            + " reason=\"" + ctx.reason().substring(0, Math.min(30, ctx.reason().length())) + "...\"");
    }
}

