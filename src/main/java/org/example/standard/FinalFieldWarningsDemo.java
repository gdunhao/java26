package org.example.standard;

import java.lang.reflect.Field;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  JEP 500: Prepare to Make Final Mean Final                                  ║
 * ║  Status: FINAL in JDK 26                                                   ║
 * ║  Spec: https://openjdk.org/jeps/500                                        ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * WHAT THIS FEATURE DOES
 * ──────────────────────
 * JDK 26 issues runtime WARNINGS when code uses reflection (java.lang.reflect)
 * to mutate fields declared as {@code final}. This is the first step toward
 * making {@code final} truly immutable — in a future JDK, these modifications
 * will be blocked entirely with an {@link IllegalAccessException}.
 *
 * Today, despite the {@code final} keyword, frameworks can still do:
 *
 *   Field f = obj.getClass().getDeclaredField("name");
 *   f.setAccessible(true);
 *   f.set(obj, "hacked");   // ← Mutates a "final" field!
 *
 * In JDK 26, this still succeeds but prints a WARNING to {@code System.err}:
 *
 *   WARNING: java.lang.reflect: final field MyClass.name was set via
 *   reflection. This will be blocked in a future release.
 *
 * WHY IT MATTERS
 * ──────────────
 * The JVM and JIT compiler want to trust that final fields never change.
 * This enables powerful optimizations:
 *   - Constant folding (inline the value, eliminate the read)
 *   - Thread-safe publication (no need for volatile semantics)
 *   - Escape analysis (prove the object is truly immutable)
 *
 * Reflective mutation of final fields breaks these guarantees, causing
 * subtle concurrency bugs and defeating optimizations. This JEP closes
 * that loophole gradually: warn in JDK 26, block in a future JDK.
 *
 * WHAT TRIGGERS A WARNING
 * ───────────────────────
 *   • {@code Field.set*()} on a final instance field
 *   • {@code Field.set*()} on a final static field (non-record, non-hidden)
 *   • VarHandle / MethodHandle write access to final fields
 *
 * WHAT DOES NOT TRIGGER A WARNING
 * ────────────────────────────────
 *   • Reading final fields via reflection ({@code Field.get()})
 *   • Setting non-final fields via reflection
 *   • Normal (non-reflective) use of final fields
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.standard.FinalFieldWarningsDemo
 *
 * Watch the console — warnings appear on stderr alongside normal output.
 */
public class FinalFieldWarningsDemo {

    public static void main(String[] args) throws Exception {
        IO.println("╔═══════════════════════════════════════════════════╗");
        IO.println("║  JEP 500 — Prepare to Make Final Mean Final      ║");
        IO.println("╚═══════════════════════════════════════════════════╝");
        IO.println();

        demoFinalInstanceFieldWarning();
        demoFinalStaticFieldWarning();
        demoReadingFinalFieldIsStillFine();
        demoNonFinalFieldNoWarning();
        demoProperAlternatives();
    }

    // ───────────────────────────────────────────────────────────────
    //  Helper classes with final fields
    // ───────────────────────────────────────────────────────────────

    /** A simple class with a final instance field. */
    static class UserProfile {
        private final String name;
        private final int age;

        UserProfile(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public String toString() {
            return "UserProfile[name=" + name + ", age=" + age + "]";
        }
    }

    /** A class with a final static field (e.g., a "constant" or singleton). */
    static class AppConfig {
        private static final String DEFAULT_LOCALE = "en_US";
        private final String environment;

        AppConfig(String environment) {
            this.environment = environment;
        }

        static String getDefaultLocale() { return DEFAULT_LOCALE; }

        @Override
        public String toString() {
            return "AppConfig[env=" + environment + ", locale=" + DEFAULT_LOCALE + "]";
        }
    }

    /** A mutable class (non-final fields) for comparison. */
    static class MutableSettings {
        private String theme = "light";
        private int fontSize = 14;

        @Override
        public String toString() {
            return "MutableSettings[theme=" + theme + ", fontSize=" + fontSize + "]";
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  DEMO 1 — Modify final INSTANCE field via reflection → WARNING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Demonstrates the JDK 26 warning when setting a final instance field.
     *
     * Before JDK 26: silently succeeds (dangerous — breaks immutability).
     * JDK 26:        succeeds BUT prints a deprecation warning to stderr.
     * Future JDK:    will throw IllegalAccessException.
     */
    static void demoFinalInstanceFieldWarning() throws Exception {
        IO.println("1️⃣  Modify Final Instance Field via Reflection → WARNING");
        IO.println("   ────────────────────────────────────────");

        var profile = new UserProfile("Alice", 30);
        IO.println("   Before: " + profile);

        // Get the final field and make it accessible
        Field nameField = UserProfile.class.getDeclaredField("name");
        nameField.setAccessible(true);

        IO.println("   ⚠️  Calling Field.set() on final field 'name'...");
        IO.println("   (Watch stderr for the JDK 26 warning!)");
        IO.println();

        // This triggers the JDK 26 warning on stderr
        nameField.set(profile, "Bob");

        IO.println("   After:  " + profile);
        IO.println("   The field WAS modified, but JDK 26 warns this will be blocked.");
        IO.println("   ✅ In a future JDK, this line will throw IllegalAccessException.");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  DEMO 2 — Modify final STATIC field via reflection → WARNING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Final static fields (like "constants") are even more dangerous to mutate
     * because the JIT compiler may have already inlined their values.
     */
    static void demoFinalStaticFieldWarning() throws Exception {
        IO.println("2️⃣  Modify Final Static Field via Reflection → WARNING");
        IO.println("   ────────────────────────────────────────");

        IO.println("   Before: AppConfig.DEFAULT_LOCALE = " + AppConfig.getDefaultLocale());

        Field localeField = AppConfig.class.getDeclaredField("DEFAULT_LOCALE");
        localeField.setAccessible(true);

        IO.println("   ⚠️  Calling Field.set() on final static field 'DEFAULT_LOCALE'...");
        IO.println("   (Watch stderr for the JDK 26 warning!)");
        IO.println();

        try {
            localeField.set(null, "fr_FR");
            IO.println("   After:  AppConfig.DEFAULT_LOCALE = " + AppConfig.getDefaultLocale());
            IO.println("   ✅ JDK 26 warns: this reflective mutation of a final static will be blocked.");
        } catch (IllegalAccessException e) {
            IO.println("   ✅ Blocked! " + e.getMessage());
            IO.println("   Some final statics may already be protected.");
        }
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  DEMO 3 — READING final fields via reflection is still perfectly fine
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Only WRITES to final fields produce warnings.
     * Reading via reflection is safe and produces no warning.
     */
    static void demoReadingFinalFieldIsStillFine() throws Exception {
        IO.println("3️⃣  Reading Final Fields via Reflection — No Warning");
        IO.println("   ────────────────────────────────────────");

        var profile = new UserProfile("Charlie", 25);

        Field nameField = UserProfile.class.getDeclaredField("name");
        nameField.setAccessible(true);
        String value = (String) nameField.get(profile);

        Field ageField = UserProfile.class.getDeclaredField("age");
        ageField.setAccessible(true);
        int age = ageField.getInt(profile);

        IO.println("   Read via reflection: name=" + value + ", age=" + age);
        IO.println("   ✅ No warning — reading final fields is safe and supported.");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  DEMO 4 — Setting NON-FINAL fields via reflection → no warning
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * The warning only applies to FINAL fields. Non-final fields can be
     * freely modified via reflection without any new warnings.
     */
    static void demoNonFinalFieldNoWarning() throws Exception {
        IO.println("4️⃣  Setting Non-Final Fields via Reflection — No Warning");
        IO.println("   ────────────────────────────────────────");

        var settings = new MutableSettings();
        IO.println("   Before: " + settings);

        Field themeField = MutableSettings.class.getDeclaredField("theme");
        themeField.setAccessible(true);
        themeField.set(settings, "dark");

        Field fontField = MutableSettings.class.getDeclaredField("fontSize");
        fontField.setAccessible(true);
        fontField.setInt(settings, 18);

        IO.println("   After:  " + settings);
        IO.println("   ✅ No warning — non-final fields are unaffected by JEP 500.");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  DEMO 5 — Proper alternatives to final field mutation
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Shows the recommended patterns that avoid reflective mutation of
     * final fields. These work today AND will continue to work in future JDKs.
     */
    static void demoProperAlternatives() {
        IO.println("5️⃣  Proper Alternatives to Final Field Mutation");
        IO.println("   ────────────────────────────────────────");
        IO.println();

        // Alternative 1: Builder pattern
        IO.println("   📐 Alternative 1: Builder Pattern");
        var user = new UserProfile("Alice", 30);
        IO.println("   Original: " + user);
        var updated = withName(user, "Bob");
        IO.println("   Updated:  " + updated);
        IO.println("   (Created a new instance instead of mutating the original)");
        IO.println();

        // Alternative 2: Record copy pattern (records are already immutable)
        IO.println("   📐 Alternative 2: Records (Immutable by Design)");
        record Config(String env, String locale) {}
        var config = new Config("prod", "en_US");
        IO.println("   Original: " + config);
        var newConfig = new Config(config.env(), "fr_FR");
        IO.println("   Updated:  " + newConfig);
        IO.println("   (Records enforce immutability — no reflection needed)");
        IO.println();

        // Alternative 3: Expose a non-final field if mutation is intended
        IO.println("   📐 Alternative 3: Use Non-Final Fields for Mutable State");
        IO.println("   If a field MUST change, don't declare it final.");
        IO.println("   The 'final' keyword should mean 'truly immutable'.");
        IO.println();

        IO.println("   ✅ Summary: Create new instances instead of mutating final fields.");
        IO.println("   This is safer, more thread-friendly, and future-proof.");
        IO.println();
    }

    /** Creates a new UserProfile with a different name (copy-with pattern). */
    private static UserProfile withName(UserProfile original, String newName) {
        try {
            Field ageField = UserProfile.class.getDeclaredField("age");
            ageField.setAccessible(true);
            return new UserProfile(newName, ageField.getInt(original));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

