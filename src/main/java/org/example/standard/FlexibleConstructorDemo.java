package org.example.standard;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  JEP 513: Flexible Constructor Bodies                                      ║
 * ║  Status: FINAL in JDK 26                                                   ║
 * ║  Spec: https://openjdk.org/jeps/513                                        ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * WHAT THIS FEATURE DOES
 * ──────────────────────
 * Flexible Constructor Bodies allow statements to appear BEFORE the explicit
 * constructor invocation (`super(...)` or `this(...)`). Previously, Java
 * required that `super()` or `this()` be the very first statement in a
 * constructor. This was a significant limitation.
 *
 * WHY IT MATTERS
 * ──────────────
 * Before JDK 26, if you needed to validate or transform arguments before
 * passing them to the superclass constructor, you had to use awkward
 * workarounds:
 *
 *   // Workaround 1: Static helper method
 *   class PositiveCounter extends Counter {
 *       PositiveCounter(int initial) {
 *           super(validatePositive(initial));   // must be first!
 *       }
 *       private static int validatePositive(int v) {
 *           if (v < 0) throw new IllegalArgumentException();
 *           return v;
 *       }
 *   }
 *
 * With JDK 26, you can write it naturally:
 *
 *   class PositiveCounter extends Counter {
 *       PositiveCounter(int initial) {
 *           if (initial < 0) throw new IllegalArgumentException("Must be >= 0");
 *           super(initial);   // now super() can appear after statements!
 *       }
 *   }
 *
 * RULES
 * ─────
 * - Statements before `super()`/`this()` are called the "prologue".
 * - In the prologue, you CANNOT:
 *   • Access `this` (the instance being constructed)
 *   • Read instance fields
 *   • Call instance methods
 * - You CAN:
 *   • Validate and transform constructor parameters
 *   • Declare and use local variables
 *   • Throw exceptions
 *   • Call static methods
 *   • Access `this` in certain limited contexts (like passing it as an argument)
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.standard.FlexibleConstructorDemo
 */
public class FlexibleConstructorDemo {

    public static void main(String[] args) {
        IO.println("╔═══════════════════════════════════════════════════╗");
        IO.println("║  JEP 513 — Flexible Constructor Bodies           ║");
        IO.println("╚═══════════════════════════════════════════════════╝");
        IO.println();

        // ─── Example 1: Argument validation before super() ───
        IO.println("1️⃣  Argument Validation Before super()");
        IO.println("   Creating PositiveCounter(10)...");
        var counter = new PositiveCounter(10);
        IO.println("   ✅ Created: " + counter);

        IO.println("   Creating PositiveCounter(-5)...");
        try {
            new PositiveCounter(-5);
        } catch (IllegalArgumentException e) {
            IO.println("   ✅ Caught: " + e.getMessage());
        }
        IO.println();

        // ─── Example 2: Argument transformation before super() ───
        IO.println("2️⃣  Argument Transformation Before super()");
        var user = new NormalizedUser("  John DOE  ");
        IO.println("   ✅ Created user: '" + user.name() + "'");
        IO.println();

        // ─── Example 3: Complex validation with local variables ───
        IO.println("3️⃣  Complex Validation with Local Variables");
        var range = new ValidatedRange(1, 10);
        IO.println("   ✅ Range: " + range);

        try {
            new ValidatedRange(10, 1);
        } catch (IllegalArgumentException e) {
            IO.println("   ✅ Caught: " + e.getMessage());
        }
        IO.println();

        // ─── Example 4: Logging/side-effects before super() ───
        IO.println("4️⃣  Logging Before super()");
        var logged = new LoggedWidget("gear", 42);
        IO.println("   ✅ Created: " + logged);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Supporting classes for the demos
    // ═══════════════════════════════════════════════════════════════

    /**
     * Base class: a simple counter with a starting value.
     */
    static class Counter {
        private final int value;
        Counter(int value) { this.value = value; }
        @Override public String toString() { return "Counter[value=" + value + "]"; }
    }

    /**
     * DEMO 1: Validates that the initial value is non-negative
     * BEFORE calling super().
     *
     * In pre-JDK 26 Java, the `if` check could NOT appear before `super(initial)`.
     */
    static class PositiveCounter extends Counter {
        PositiveCounter(int initial) {
            // ✨ These statements execute BEFORE super() — new in JDK 26!
            if (initial < 0) {
                throw new IllegalArgumentException(
                    "Counter value must be non-negative, got: " + initial);
            }
            super(initial);  // Now we know `initial` is valid
        }
    }

    /**
     * Base class for a user with a name.
     */
    static class UserBase {
        final String name;
        UserBase(String name) { this.name = name; }
        String name() { return name; }
    }

    /**
     * DEMO 2: Normalizes the name (trim + proper case) BEFORE passing
     * it to the superclass constructor.
     *
     * In pre-JDK 26 Java, the transformation logic could NOT appear before super().
     */
    static class NormalizedUser extends UserBase {
        NormalizedUser(String rawName) {
            // ✨ Transform the argument before calling super()
            String trimmed = rawName.strip();
            String normalized = trimmed.substring(0, 1).toUpperCase()
                              + trimmed.substring(1).toLowerCase();
            super(normalized);
        }
    }

    /**
     * Base class for a numeric range.
     */
    static class Range {
        final int low, high;
        Range(int low, int high) { this.low = low; this.high = high; }
        @Override public String toString() { return "Range[" + low + ".." + high + "]"; }
    }

    /**
     * DEMO 3: Uses local variables in the prologue to perform multi-step
     * validation before super().
     */
    static class ValidatedRange extends Range {
        ValidatedRange(int a, int b) {
            // ✨ Multiple statements with local variables before super()
            int lo = Math.min(a, b);
            int hi = Math.max(a, b);
            if (a > b) {
                throw new IllegalArgumentException(
                    "First arg (" + a + ") must be <= second arg (" + b + "). "
                    + "Hint: did you mean (" + lo + ", " + hi + ")?");
            }
            super(lo, hi);
        }
    }

    /**
     * Base class for a named widget with an ID.
     */
    static class Widget {
        final String name;
        final int id;
        Widget(String name, int id) { this.name = name; this.id = id; }
        @Override public String toString() { return "Widget[" + name + "#" + id + "]"; }
    }

    /**
     * DEMO 4: Performs logging as a side-effect before super().
     */
    static class LoggedWidget extends Widget {
        LoggedWidget(String name, int id) {
            // ✨ Side-effect (logging) before super()
            IO.println("   [LOG] Constructing widget: name=" + name + ", id=" + id);
            super(name, id);
        }
    }
}


