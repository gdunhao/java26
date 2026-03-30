package org.example.incubator;

import jdk.incubator.vector.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  JEP 529: Vector API (Eleventh Incubator)                                  ║
 * ║  Status: INCUBATOR in JDK 26                                               ║
 * ║  Spec: https://openjdk.org/jeps/529                                        ║
 * ║  Requires: --add-modules jdk.incubator.vector                              ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * WHAT THIS FEATURE DOES
 * ──────────────────────
 * The Vector API expresses vector (SIMD) computations that reliably compile at
 * runtime to optimal hardware vector instructions on supported CPU architectures
 * (x64 SSE/AVX, ARM NEON/SVE, etc.).
 *
 * WHAT IS SIMD?
 * ─────────────
 * SIMD = Single Instruction, Multiple Data. Instead of processing one element
 * at a time, SIMD processes multiple elements simultaneously:
 *
 *   Scalar:  a[0]+b[0], a[1]+b[1], a[2]+b[2], a[3]+b[3]  (4 operations)
 *   SIMD:    a[0..3] + b[0..3]                             (1 operation!)
 *
 * This can provide 4x–16x speedups for data-parallel workloads.
 *
 * KEY API CONCEPTS
 * ────────────────
 *   - VectorSpecies   — Defines the element type + vector length
 *     Examples: FloatVector.SPECIES_256 (8 floats × 32-bit = 256 bits)
 *               IntVector.SPECIES_128   (4 ints × 32-bit = 128 bits)
 *
 *   - Vector<E>       — A fixed-size sequence of elements
 *     Subtypes: FloatVector, IntVector, DoubleVector, LongVector, etc.
 *
 *   - Operations: add, sub, mul, div, min, max, fma, abs, neg, and, or, etc.
 *   - Reductions: reduceLanes(ADD), reduceLanes(MAX), etc.
 *   - Masks: compare(), blend() for conditional operations
 *
 * USE CASES
 * ─────────
 *   - Scientific computing (matrix ops, physics simulations)
 *   - Image/audio processing (pixel operations, FFT)
 *   - Machine learning inference (dot products, activations)
 *   - Financial calculations (bulk pricing, risk analysis)
 *   - Compression / hashing algorithms
 *
 * WHY NOT AUTO-VECTORIZATION?
 * ───────────────────────────
 * The JIT compiler can sometimes auto-vectorize simple loops, but:
 *   - It's unpredictable: small code changes can disable auto-vectorization
 *   - It can't handle complex patterns (conditional, gather/scatter)
 *   - No developer control or visibility
 *
 * The Vector API gives you EXPLICIT, PORTABLE SIMD with guaranteed behavior.
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.incubator.VectorApiDemo
 *
 * Note: Requires --add-modules jdk.incubator.vector (configured in pom.xml).
 */
public class VectorApiDemo {

    // Choose the preferred species — the JVM picks the best for your CPU
    static final VectorSpecies<Float> FLOAT_SPECIES = FloatVector.SPECIES_PREFERRED;
    static final VectorSpecies<Integer> INT_SPECIES = IntVector.SPECIES_PREFERRED;

    public static void main(String[] args) {
        IO.println("╔═══════════════════════════════════════════════════╗");
        IO.println("║  JEP 529 — Vector API (Incubator)               ║");
        IO.println("╚═══════════════════════════════════════════════════╝");
        IO.println();

        IO.println("🖥️  Hardware Info:");
        IO.println("   Float vector species: " + FLOAT_SPECIES);
        IO.println("   Vector bit size: " + FLOAT_SPECIES.vectorBitSize() + " bits");
        IO.println("   Lanes (floats per vector): " + FLOAT_SPECIES.length());
        IO.println();

        demoElementWiseOps();
        demoReduction();
        demoConditionalOps();
        demoDotProduct();
        demoBenchmark();
    }

    /**
     * DEMO 1: Element-wise array operations.
     *
     * Multiply two float arrays element-by-element using SIMD.
     * The loop processes SPECIES.length() elements per iteration.
     */
    static void demoElementWiseOps() {
        IO.println("1️⃣  Element-wise Array Multiplication (SIMD)");

        float[] a = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
        float[] b = { 2, 2, 2, 2, 3, 3, 3, 3, 4, 4,  4,  4,  5,  5,  5,  5  };
        float[] result = new float[a.length];

        // Process in chunks of SPECIES.length()
        int i = 0;
        int upperBound = FLOAT_SPECIES.loopBound(a.length);

        // Main vectorized loop
        for (; i < upperBound; i += FLOAT_SPECIES.length()) {
            FloatVector va = FloatVector.fromArray(FLOAT_SPECIES, a, i);
            FloatVector vb = FloatVector.fromArray(FLOAT_SPECIES, b, i);
            FloatVector vr = va.mul(vb);  // SIMD multiply
            vr.intoArray(result, i);
        }

        // Scalar tail (for remaining elements)
        for (; i < a.length; i++) {
            result[i] = a[i] * b[i];
        }

        IO.println("   a =      " + formatArray(a));
        IO.println("   b =      " + formatArray(b));
        IO.println("   a × b =  " + formatArray(result));
        IO.println();
    }

    /**
     * DEMO 2: Reduction — sum all elements using SIMD.
     *
     * Instead of adding one element at a time, we load vectors and
     * accumulate them, then reduce the final vector to a scalar.
     */
    static void demoReduction() {
        IO.println("2️⃣  SIMD Reduction (Sum of Array)");

        float[] data = new float[64];
        for (int i = 0; i < data.length; i++) data[i] = i + 1;

        float sum = 0;
        int i = 0;
        int upperBound = FLOAT_SPECIES.loopBound(data.length);

        FloatVector vsum = FloatVector.zero(FLOAT_SPECIES);
        for (; i < upperBound; i += FLOAT_SPECIES.length()) {
            FloatVector v = FloatVector.fromArray(FLOAT_SPECIES, data, i);
            vsum = vsum.add(v);  // Accumulate vectors
        }
        sum = vsum.reduceLanes(VectorOperators.ADD);  // Reduce to scalar

        // Scalar tail
        for (; i < data.length; i++) sum += data[i];

        IO.println("   Sum of 1..64: " + sum + " (expected: " + (64 * 65 / 2) + ")");
        IO.println();
    }

    /**
     * DEMO 3: Conditional operations using masks.
     *
     * SIMD doesn't have traditional if-else branches. Instead, we use
     * masks to conditionally apply operations (predicated execution).
     *
     * Example: clamp all values to [0, 100].
     */
    static void demoConditionalOps() {
        IO.println("3️⃣  Conditional (Masked) Operations — Clamp to [0, 100]");

        float[] data = { -5, 10, 150, 42, -20, 100, 200, 75 };
        float[] result = new float[data.length];

        int i = 0;
        int upperBound = FLOAT_SPECIES.loopBound(data.length);

        for (; i < upperBound; i += FLOAT_SPECIES.length()) {
            FloatVector v = FloatVector.fromArray(FLOAT_SPECIES, data, i);
            // Clamp: max(0, min(100, v))
            v = v.max(0.0f).min(100.0f);
            v.intoArray(result, i);
        }
        for (; i < data.length; i++) {
            result[i] = Math.max(0, Math.min(100, data[i]));
        }

        IO.println("   Input:    " + formatArray(data));
        IO.println("   Clamped:  " + formatArray(result));
        IO.println();
    }

    /**
     * DEMO 4: Dot product — a fundamental ML/linear-algebra operation.
     *
     * dot(a, b) = Σ(a[i] × b[i]) — done entirely with SIMD.
     * This is the core operation in neural network inference.
     */
    static void demoDotProduct() {
        IO.println("4️⃣  SIMD Dot Product");

        float[] a = { 1, 2, 3, 4, 5, 6, 7, 8 };
        float[] b = { 8, 7, 6, 5, 4, 3, 2, 1 };

        float dot = 0;
        int i = 0;
        int upperBound = FLOAT_SPECIES.loopBound(a.length);

        FloatVector vsum = FloatVector.zero(FLOAT_SPECIES);
        for (; i < upperBound; i += FLOAT_SPECIES.length()) {
            FloatVector va = FloatVector.fromArray(FLOAT_SPECIES, a, i);
            FloatVector vb = FloatVector.fromArray(FLOAT_SPECIES, b, i);
            vsum = va.fma(vb, vsum);  // Fused multiply-add: vsum += va * vb
        }
        dot = vsum.reduceLanes(VectorOperators.ADD);
        for (; i < a.length; i++) dot += a[i] * b[i];

        IO.println("   a = " + formatArray(a));
        IO.println("   b = " + formatArray(b));
        IO.println("   dot(a, b) = " + dot + " (expected: 120.0)");
        IO.println();
    }

    /**
     * DEMO 5: Simple benchmark — scalar vs SIMD.
     *
     * ⚠️ This is NOT a proper JMH benchmark. It's just to illustrate the
     * concept. For real benchmarks, use JMH (Java Microbenchmark Harness).
     */
    static void demoBenchmark() {
        IO.println("5️⃣  Scalar vs SIMD Speed Comparison");
        IO.println("   ⚠️  Note: Not a proper benchmark — illustrative only!");

        int size = 10_000_000;
        float[] a = new float[size];
        float[] b = new float[size];
        float[] result = new float[size];

        for (int i = 0; i < size; i++) { a[i] = i; b[i] = i * 0.5f; }

        // Warm up
        for (int warmup = 0; warmup < 5; warmup++) {
            scalarMultiply(a, b, result);
            simdMultiply(a, b, result);
        }

        // Scalar timing
        long start = System.nanoTime();
        for (int rep = 0; rep < 10; rep++) scalarMultiply(a, b, result);
        long scalarTime = (System.nanoTime() - start) / 10;

        // SIMD timing
        start = System.nanoTime();
        for (int rep = 0; rep < 10; rep++) simdMultiply(a, b, result);
        long simdTime = (System.nanoTime() - start) / 10;

        IO.println("   Array size: " + size + " elements");
        IO.println("   Scalar: " + (scalarTime / 1_000_000.0) + " ms");
        IO.println("   SIMD:   " + (simdTime / 1_000_000.0) + " ms");
        IO.println("   Speedup: ~" + String.format("%.1fx", (double) scalarTime / simdTime));
        IO.println();
    }

    static void scalarMultiply(float[] a, float[] b, float[] r) {
        for (int i = 0; i < a.length; i++) r[i] = a[i] * b[i];
    }

    static void simdMultiply(float[] a, float[] b, float[] r) {
        int i = 0;
        int upperBound = FLOAT_SPECIES.loopBound(a.length);
        for (; i < upperBound; i += FLOAT_SPECIES.length()) {
            FloatVector va = FloatVector.fromArray(FLOAT_SPECIES, a, i);
            FloatVector vb = FloatVector.fromArray(FLOAT_SPECIES, b, i);
            va.mul(vb).intoArray(r, i);
        }
        for (; i < a.length; i++) r[i] = a[i] * b[i];
    }

    static String formatArray(float[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < Math.min(arr.length, 16); i++) {
            if (i > 0) sb.append(", ");
            sb.append(arr[i] % 1 == 0 ? String.valueOf((int) arr[i]) : String.valueOf(arr[i]));
        }
        if (arr.length > 16) sb.append(", ...");
        sb.append("]");
        return sb.toString();
    }
}

