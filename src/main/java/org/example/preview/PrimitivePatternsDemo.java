package org.example.preview;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  JEP 530: Primitive Types in Patterns, instanceof, and switch (Preview)    ║
 * ║  Status: PREVIEW in JDK 26 (Fourth Preview)                                ║
 * ║  Spec: https://openjdk.org/jeps/530                                        ║
 * ║  Requires: --enable-preview                                                ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * WHAT THIS FEATURE DOES
 * ──────────────────────
 * This JEP extends pattern matching to support primitive types in:
 *   - `instanceof` expressions
 *   - `switch` expressions and statements
 *   - Record patterns
 *
 * Before this feature, pattern matching only worked with reference types.
 * Now you can write patterns like `case int i`, `case double d`, etc.
 *
 * KEY CAPABILITIES
 * ────────────────
 *   1. Primitive type patterns in switch:
 *        switch (obj) {
 *            case int i    -> "it's an int: " + i;
 *            case double d -> "it's a double: " + d;
 *        }
 *
 *   2. Primitive instanceof:
 *        if (value instanceof int i) { ... }
 *
 *   3. Guard patterns with primitives:
 *        case int i when i > 0 -> "positive int"
 *
 *   4. Primitive patterns in record destructuring:
 *        case Point(int x, int y) -> x + y
 *
 *   5. Widening/narrowing in patterns:
 *        Matching a `long` against `int` checks if the value fits
 *
 * WHY IT MATTERS
 * ──────────────
 * Before this feature, switching on primitives was limited to `int`, `char`,
 * `byte`, and `short` with constant case labels. You couldn't use pattern
 * matching, guards, or mix primitive and reference patterns. This unification
 * makes the pattern matching feature complete.
 *
 * HOW TO RUN
 * ──────────
 *   javac --enable-preview --release 26 PrimitivePatternsDemo.java
 *   java --enable-preview PrimitivePatternsDemo
 *
 * Or with Maven (project is already configured with --enable-preview):
 *   mvn compile exec:exec -Dexec.mainClass=org.example.preview.PrimitivePatternsDemo
 */
public class PrimitivePatternsDemo {

    public static void main(String[] args) {
        IO.println("╔═══════════════════════════════════════════════════╗");
        IO.println("║  JEP 530 — Primitive Types in Patterns (Preview) ║");
        IO.println("╚═══════════════════════════════════════════════════╝");
        IO.println();

        demoPrimitiveSwitchOnObject();
        demoGuardedPrimitivePatterns();
        demoRecordWithPrimitivePatterns();
        demoPrimitiveNarrowingInSwitch();
        demoExhaustivePrimitiveSwitch();
    }

    /**
     * DEMO 1: Switching on Object and matching primitive types.
     *
     * When autoboxed values are stored in an Object, you can now pattern
     * match them back to their primitive types.
     */
    static void demoPrimitiveSwitchOnObject() {
        IO.println("1️⃣  Primitive Type Patterns in switch(Object)");

        Object[] values = { 42, 3.14, true, "hello", 'A', 100L };

        for (Object value : values) {
            String description = switch (value) {
                case Integer i  -> "int: " + i;
                case Double d   -> "double: " + d;
                case Boolean b  -> "boolean: " + b;
                case Long l     -> "long: " + l;
                case Character c -> "char: '" + c + "'";
                case String s   -> "String: \"" + s + "\"";
                default         -> "other: " + value;
            };
            IO.println("   " + value + " → " + description);
        }
        IO.println();
    }

    /**
     * DEMO 2: Guarded primitive patterns (with `when` clause).
     *
     * You can combine primitive patterns with guards to create
     * range-based matching — much cleaner than if-else chains.
     */
    static void demoGuardedPrimitivePatterns() {
        IO.println("2️⃣  Guarded Primitive Patterns");

        int[] temperatures = { -10, 0, 15, 25, 35, 45 };

        for (int temp : temperatures) {
            String description = classifyTemperature(temp);
            IO.println("   " + temp + "°C → " + description);
        }
        IO.println();
    }

    /**
     * Classify temperature using guarded patterns.
     * This is cleaner than a chain of if-else statements.
     *
     *   // Old way:
     *   if (temp < 0) return "Freezing";
     *   else if (temp < 10) return "Cold";
     *   else if (temp < 20) return "Cool";
     *   ...
     *
     *   // New way with primitive patterns + guards:
     *   case int t when t < 0 -> "Freezing"
     */
    static String classifyTemperature(int temp) {
        return switch ((Object) temp) {
            case int t when t < 0  -> "🥶 Freezing";
            case int t when t < 10 -> "❄️ Cold";
            case int t when t < 20 -> "🌤️ Cool";
            case int t when t < 30 -> "☀️ Warm";
            case int t when t < 40 -> "🔥 Hot";
            case int t             -> "🌋 Extreme heat!";
            default                -> "Unknown";
        };
    }

    /**
     * DEMO 3: Primitive patterns in record destructuring.
     *
     * Records with primitive fields can now be destructured with
     * primitive patterns, including guards.
     */
    record Point(int x, int y) {}
    record Circle(Point center, double radius) {}

    static void demoRecordWithPrimitivePatterns() {
        IO.println("3️⃣  Primitive Patterns in Record Destructuring");

        Object[] shapes = {
            new Point(0, 0),
            new Point(3, 4),
            new Circle(new Point(1, 1), 5.0),
            new Circle(new Point(0, 0), 0.5),
        };

        for (Object shape : shapes) {
            String desc = switch (shape) {
                case Point(int x, int y) when x == 0 && y == 0
                    -> "Origin point";
                case Point(int x, int y)
                    -> "Point at (" + x + ", " + y + "), distance from origin: "
                       + String.format("%.2f", Math.sqrt(x * x + y * y));
                case Circle(Point(int cx, int cy), double r) when r < 1.0
                    -> "Tiny circle at (" + cx + "," + cy + ") r=" + r;
                case Circle(Point(int cx, int cy), double r)
                    -> "Circle at (" + cx + "," + cy + ") r=" + r
                       + ", area=" + String.format("%.2f", Math.PI * r * r);
                default -> "Unknown shape";
            };
            IO.println("   " + shape + " → " + desc);
        }
        IO.println();
    }

    /**
     * DEMO 4: Primitive narrowing conversions in patterns.
     *
     * When switching on a wider type (e.g., long), you can match against
     * narrower types (e.g., int). The pattern only matches if the value
     * can be losslessly narrowed.
     */
    static void demoPrimitiveNarrowingInSwitch() {
        IO.println("4️⃣  Primitive Narrowing in Patterns");

        long[] values = { 42L, 100_000L, Long.MAX_VALUE };

        for (long value : values) {
            String desc = switch ((Object) value) {
                case int i  -> "Fits in int: " + i;
                case long l -> "Only fits in long: " + l;
                default     -> "other";
            };
            IO.println("   " + value + "L → " + desc);
        }
        IO.println();
    }

    /**
     * DEMO 5: Exhaustive switch on boolean.
     *
     * With primitive patterns, you can now have exhaustive switches on
     * boolean without a default clause.
     */
    static void demoExhaustivePrimitiveSwitch() {
        IO.println("5️⃣  Exhaustive Boolean Switch (no default needed!)");

        for (boolean b : new boolean[]{ true, false }) {
            String desc = switch (b) {
                case true  -> "✅ true — affirmative";
                case false -> "❌ false — negative";
                // No default needed! The compiler knows boolean is exhaustive.
            };
            IO.println("   " + b + " → " + desc);
        }
        IO.println();
    }
}


