package org.example.incubator;

import jdk.incubator.vector.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  Vector API — Real-World Use Cases                                         ║
 * ║  Practical examples where SIMD gives you a real advantage                  ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * REFERENCES
 * ──────────
 *   • JEP 529 — Vector API (Eleventh Incubator):
 *       https://openjdk.org/jeps/529
 *   • Javadoc — jdk.incubator.vector package:
 *       https://docs.oracle.com/en/java/javase/26/docs/api/jdk.incubator.vector/jdk/incubator/vector/package-summary.html
 *   • JEP 426 — Vector API (Fourth Incubator, detailed design):
 *       https://openjdk.org/jeps/426
 *
 * ALGORITHMS USED
 * ───────────────
 *   1. Image brightness  — Pixel channel clamping (standard image processing)
 *   2. Audio gain         — Sample amplification + clamp (digital audio fundamentals)
 *   3. Cosine similarity  — dot(a,b)/(‖a‖·‖b‖) — used in Elasticsearch kNN, pgvector, FAISS
 *   4. Moving average     — Sliding-window sum (time-series analysis, finance, IoT)
 *   5. RGB→Grayscale      — ITU-R BT.601 luma: 0.299R + 0.587G + 0.114B
 *   6. Min-Max norm       — (x−min)/(max−min) — scikit-learn MinMaxScaler
 *   7. Hamming distance   — Count differing positions (BLAST, LSH, image dedup)
 *   8. Euclidean distance — √Σ(aᵢ−bᵢ)² — kNN, PostGIS, vector databases
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.incubator.VectorApiRealWorldExamples
 */
public class VectorApiRealWorldExamples {

    static final VectorSpecies<Float> F_SPECIES = FloatVector.SPECIES_PREFERRED;
    static final VectorSpecies<Integer> I_SPECIES = IntVector.SPECIES_PREFERRED;
    static final VectorSpecies<Byte> B_SPECIES = ByteVector.SPECIES_PREFERRED;

    public static void main(String[] args) {
        IO.println("╔═══════════════════════════════════════════════════════╗");
        IO.println("║  Vector API — Real-World Use Cases                   ║");
        IO.println("╚═══════════════════════════════════════════════════════╝");
        IO.println();

        example1_ImageBrightnessAdjustment();
        example2_AudioAmplification();
        example3_CosineSimilarityForSearch();
        example4_MovingAverageTimeSeries();
        example5_RgbToGrayscale();
        example6_MinMaxNormalization();
        example7_HammingDistance();
        example8_EuclideanDistance();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 1 — Image Brightness Adjustment
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: You're building an image editor or processing pipeline.
    //  Every pixel's RGB channels need to be increased by a fixed value,
    //  clamped to [0, 255]. Images can have millions of pixels —
    //  SIMD processes 8–16 channels per instruction instead of one.
    //
    //  Real users: Instagram-style filters, medical imaging, satellite imagery.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example1_ImageBrightnessAdjustment() {
        IO.println("1️⃣  Image Brightness Adjustment");
        IO.println("   Use case: Photo editors, medical imaging, satellite imagery");
        IO.println();

        // Simulate a row of pixel channel values (R, G, B, R, G, B, ...)
        float[] pixels = {
            120, 200, 50,  30,  180, 240, 100, 90,
             10, 255, 128, 64,  220, 170,  5,  45
        };
        float brightnessOffset = 40.0f;
        float[] result = new float[pixels.length];

        int i = 0;
        int bound = F_SPECIES.loopBound(pixels.length);
        FloatVector offsetVec = FloatVector.broadcast(F_SPECIES, brightnessOffset);
        FloatVector minVec = FloatVector.broadcast(F_SPECIES, 0.0f);
        FloatVector maxVec = FloatVector.broadcast(F_SPECIES, 255.0f);

        for (; i < bound; i += F_SPECIES.length()) {
            FloatVector v = FloatVector.fromArray(F_SPECIES, pixels, i);
            v = v.add(offsetVec).max(minVec).min(maxVec); // Add & clamp in one pass
            v.intoArray(result, i);
        }
        for (; i < pixels.length; i++) {
            result[i] = Math.max(0, Math.min(255, pixels[i] + brightnessOffset));
        }

        IO.println("   Original pixels: " + formatArray(pixels));
        IO.println("   +40 brightness:  " + formatArray(result));
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 2 — Audio Amplification (Gain)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: You're building a music player, podcast app, or audio
    //  processing tool. Each audio sample (float in [-1.0, 1.0]) must be
    //  multiplied by a gain factor and clamped to avoid distortion.
    //
    //  At 44.1 kHz stereo, that's 88,200 samples/second. SIMD makes
    //  this fast enough for real-time processing.
    //
    //  Real users: DAWs (Audacity, Ableton), VoIP (Zoom, Discord), game audio.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example2_AudioAmplification() {
        IO.println("2️⃣  Audio Amplification (Gain + Clamp)");
        IO.println("   Use case: Music players, VoIP, game engines, podcasts");
        IO.println();

        // Simulated audio samples in [-1.0, 1.0]
        float[] samples = { 0.3f, -0.5f, 0.8f, -0.1f, 0.95f, -0.7f, 0.2f, 0.6f,
                           -0.9f, 0.4f, 0.0f, -0.3f, 0.75f, -0.85f, 0.1f, 0.55f };
        float gain = 1.5f;
        float[] amplified = new float[samples.length];

        int i = 0;
        int bound = F_SPECIES.loopBound(samples.length);
        FloatVector gainVec = FloatVector.broadcast(F_SPECIES, gain);

        for (; i < bound; i += F_SPECIES.length()) {
            FloatVector v = FloatVector.fromArray(F_SPECIES, samples, i);
            v = v.mul(gainVec).max(-1.0f).min(1.0f); // Amplify & clamp
            v.intoArray(amplified, i);
        }
        for (; i < samples.length; i++) {
            amplified[i] = Math.max(-1.0f, Math.min(1.0f, samples[i] * gain));
        }

        IO.println("   Original samples: " + formatFloatArray(samples, 3));
        IO.println("   Amplified (×1.5): " + formatFloatArray(amplified, 3));
        IO.println("   Notice: 0.95×1.5=1.425 → clamped to 1.0 (prevents distortion)");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 3 — Cosine Similarity for Semantic Search / Recommendations
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: You have a search engine or recommendation system that
    //  stores items as high-dimensional vectors (embeddings). To find
    //  the most similar item, you compute cosine similarity:
    //
    //      cos(a, b) = dot(a, b) / (‖a‖ × ‖b‖)
    //
    //  With millions of embeddings (typical for RAG, product search,
    //  Spotify-style recommendations), this is the #1 bottleneck.
    //  SIMD makes each comparison 4–8× faster.
    //
    //  Real users: Elasticsearch kNN, pgvector, Pinecone, LangChain.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example3_CosineSimilarityForSearch() {
        IO.println("3️⃣  Cosine Similarity (Vector Embeddings / Semantic Search)");
        IO.println("   Use case: RAG pipelines, recommendation engines, kNN search");
        IO.println();

        // Two 16-dimensional "embeddings" (real ones are 384–1536 dims)
        float[] query     = { 0.1f, 0.9f, 0.3f, 0.7f, 0.5f, 0.2f, 0.8f, 0.4f,
                              0.6f, 0.1f, 0.3f, 0.9f, 0.2f, 0.7f, 0.5f, 0.8f };
        float[] document  = { 0.2f, 0.8f, 0.4f, 0.6f, 0.5f, 0.3f, 0.7f, 0.5f,
                              0.5f, 0.2f, 0.4f, 0.8f, 0.3f, 0.6f, 0.4f, 0.9f };

        float dotProduct = 0, normA = 0, normB = 0;
        int i = 0;
        int bound = F_SPECIES.loopBound(query.length);

        FloatVector vDot = FloatVector.zero(F_SPECIES);
        FloatVector vNormA = FloatVector.zero(F_SPECIES);
        FloatVector vNormB = FloatVector.zero(F_SPECIES);

        for (; i < bound; i += F_SPECIES.length()) {
            FloatVector va = FloatVector.fromArray(F_SPECIES, query, i);
            FloatVector vb = FloatVector.fromArray(F_SPECIES, document, i);
            vDot = va.fma(vb, vDot);       // dot += a * b
            vNormA = va.fma(va, vNormA);    // normA += a * a
            vNormB = vb.fma(vb, vNormB);    // normB += b * b
        }
        dotProduct = vDot.reduceLanes(VectorOperators.ADD);
        normA = vNormA.reduceLanes(VectorOperators.ADD);
        normB = vNormB.reduceLanes(VectorOperators.ADD);

        for (; i < query.length; i++) {
            dotProduct += query[i] * document[i];
            normA += query[i] * query[i];
            normB += document[i] * document[i];
        }

        double similarity = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
        IO.println("   Query embedding:    [0.1, 0.9, 0.3, ... 16 dims]");
        IO.println("   Document embedding: [0.2, 0.8, 0.4, ... 16 dims]");
        IO.println("   Cosine similarity:  " + String.format("%.4f", similarity));
        IO.println("   (1.0 = identical, 0.0 = unrelated, -1.0 = opposite)");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 4 — Simple Moving Average (Time-Series / Finance)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: You're computing a moving average over stock prices,
    //  sensor data, or server metrics. The window slides over the data,
    //  and at each position you sum the window elements.
    //
    //  SIMD accelerates the summation within each window.
    //
    //  Real users: Trading platforms, IoT dashboards, Prometheus/Grafana.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example4_MovingAverageTimeSeries() {
        IO.println("4️⃣  Simple Moving Average (Time Series / Finance)");
        IO.println("   Use case: Stock analysis, IoT sensors, server monitoring");
        IO.println();

        // Daily closing prices (simulated)
        float[] prices = { 100, 102, 101, 105, 108, 107, 110, 112,
                           109, 111, 115, 118, 117, 120, 122, 121 };
        int window = 4;
        float[] sma = new float[prices.length - window + 1];

        for (int start = 0; start < sma.length; start++) {
            // SIMD sum of the window
            float sum = 0;
            int j = 0;
            int bound = F_SPECIES.loopBound(window);
            FloatVector vsum = FloatVector.zero(F_SPECIES);
            for (; j < bound; j += F_SPECIES.length()) {
                FloatVector v = FloatVector.fromArray(F_SPECIES, prices, start + j);
                vsum = vsum.add(v);
            }
            sum = vsum.reduceLanes(VectorOperators.ADD);
            for (; j < window; j++) sum += prices[start + j];

            sma[start] = sum / window;
        }

        IO.println("   Prices:             " + formatArray(prices));
        IO.println("   SMA(" + window + "):             " + formatArray(sma));
        IO.println("   Each value = average of 4 consecutive days");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 5 — RGB to Grayscale Conversion
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Converting color images to grayscale is one of the most
    //  common image processing operations. The standard formula is:
    //
    //      gray = 0.299 × R + 0.587 × G + 0.114 × B
    //
    //  With SIMD, we process entire batches of R, G, B channels in parallel.
    //  A 4K image has 8.3 million pixels — SIMD shines here.
    //
    //  Real users: OpenCV preprocessing, ML data augmentation, thumbnails.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example5_RgbToGrayscale() {
        IO.println("5️⃣  RGB to Grayscale Conversion");
        IO.println("   Use case: Image preprocessing, ML pipelines, thumbnail generation");
        IO.println();

        // Separate R, G, B channel arrays (Structure of Arrays layout)
        float[] r = { 255, 128,  0,  64, 200, 100, 50, 180 };
        float[] g = { 0,   128, 255, 64, 100, 200, 50, 180 };
        float[] b = { 0,   128,  0,  64,  50, 100, 200, 180 };
        float[] gray = new float[r.length];

        int i = 0;
        int bound = F_SPECIES.loopBound(r.length);
        FloatVector wR = FloatVector.broadcast(F_SPECIES, 0.299f);
        FloatVector wG = FloatVector.broadcast(F_SPECIES, 0.587f);
        FloatVector wB = FloatVector.broadcast(F_SPECIES, 0.114f);

        for (; i < bound; i += F_SPECIES.length()) {
            FloatVector vr = FloatVector.fromArray(F_SPECIES, r, i);
            FloatVector vg = FloatVector.fromArray(F_SPECIES, g, i);
            FloatVector vb = FloatVector.fromArray(F_SPECIES, b, i);

            // gray = 0.299*R + 0.587*G + 0.114*B  (all lanes in parallel)
            FloatVector vGray = vr.mul(wR).add(vg.mul(wG)).add(vb.mul(wB));
            vGray.intoArray(gray, i);
        }
        for (; i < r.length; i++) {
            gray[i] = 0.299f * r[i] + 0.587f * g[i] + 0.114f * b[i];
        }

        IO.println("   R:         " + formatArray(r));
        IO.println("   G:         " + formatArray(g));
        IO.println("   B:         " + formatArray(b));
        IO.println("   Grayscale: " + formatArray(gray));
        IO.println("   (Pure red=76.2, pure green=149.7, gray=128.0)");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 6 — Min-Max Normalization (ML Feature Scaling)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Before feeding data into a machine learning model, features
    //  must be normalized to [0, 1]:
    //
    //      normalized[i] = (data[i] - min) / (max - min)
    //
    //  With large datasets (millions of rows × hundreds of features),
    //  SIMD processes entire feature columns in bulk.
    //
    //  Real users: scikit-learn preprocessing, Spark ML, TensorFlow data pipelines.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example6_MinMaxNormalization() {
        IO.println("6️⃣  Min-Max Normalization (ML Feature Scaling)");
        IO.println("   Use case: ML preprocessing, data pipelines, analytics");
        IO.println();

        float[] data = { 10, 25, 50, 75, 100, 0, 30, 60, 90, 40, 80, 20, 55, 70, 15, 95 };
        float min = 0, max = 100;
        float[] normalized = new float[data.length];

        int i = 0;
        int bound = F_SPECIES.loopBound(data.length);
        FloatVector vMin = FloatVector.broadcast(F_SPECIES, min);
        FloatVector vRange = FloatVector.broadcast(F_SPECIES, max - min);

        for (; i < bound; i += F_SPECIES.length()) {
            FloatVector v = FloatVector.fromArray(F_SPECIES, data, i);
            v = v.sub(vMin).div(vRange); // (x - min) / (max - min)
            v.intoArray(normalized, i);
        }
        for (; i < data.length; i++) {
            normalized[i] = (data[i] - min) / (max - min);
        }

        IO.println("   Raw features:  " + formatArray(data));
        IO.println("   Normalized:    " + formatFloatArray(normalized, 2));
        IO.println("   All values now in [0.0, 1.0] — ready for ML model");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 7 — Hamming Distance (Bioinformatics / Error Detection)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Hamming distance counts the number of positions where two
    //  byte sequences differ. It's used in:
    //    - DNA sequence comparison (each base = 1 byte)
    //    - Error detection in network packets
    //    - Perceptual hashing (image deduplication)
    //    - Locality-Sensitive Hashing (LSH) for nearest neighbors
    //
    //  With genome sequences of billions of bases, SIMD processes 16–64
    //  comparisons per instruction.
    //
    //  Real users: BLAST (bioinformatics), Redis similarity, image dedup.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example7_HammingDistance() {
        IO.println("7️⃣  Hamming Distance (Bioinformatics / Error Detection)");
        IO.println("   Use case: DNA comparison, network error detection, image dedup");
        IO.println();

        // Simulated DNA sequences (A=65, C=67, G=71, T=84)
        byte[] seq1 = "ACGTACGTACGTACGTACGTACGTACGTACGT".getBytes();
        byte[] seq2 = "ACGTACGTATGTACGAACGTACGTACCTACGT".getBytes();
        //                       ^         ^              ^^
        // Differences at positions 8, 15, 26, 27

        int distance = 0;
        int i = 0;
        int bound = B_SPECIES.loopBound(seq1.length);

        for (; i < bound; i += B_SPECIES.length()) {
            ByteVector v1 = ByteVector.fromArray(B_SPECIES, seq1, i);
            ByteVector v2 = ByteVector.fromArray(B_SPECIES, seq2, i);
            // Compare all lanes simultaneously — true where they differ
            VectorMask<Byte> diff = v1.compare(VectorOperators.NE, v2);
            distance += diff.trueCount();
        }
        for (; i < seq1.length; i++) {
            if (seq1[i] != seq2[i]) distance++;
        }

        IO.println("   Seq 1: " + new String(seq1));
        IO.println("   Seq 2: " + new String(seq2));
        IO.println("   Hamming distance: " + distance + " (positions that differ)");
        IO.println("   → " + String.format("%.1f%%", (1 - distance / (double) seq1.length) * 100)
                   + " sequence identity");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 8 — Euclidean Distance (Geospatial / kNN)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Finding the nearest neighbor in N-dimensional space.
    //  Used in geospatial queries ("find the 5 closest restaurants"),
    //  kNN classifiers, and vector databases.
    //
    //      dist(a, b) = √( Σ (a[i] - b[i])² )
    //
    //  The inner subtraction + squaring loop is perfectly SIMD-friendly.
    //
    //  Real users: PostGIS, k-d trees, FAISS, Weaviate.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example8_EuclideanDistance() {
        IO.println("8️⃣  Euclidean Distance (Geospatial / kNN Classification)");
        IO.println("   Use case: Location search, nearest-neighbor, vector databases");
        IO.println();

        float[] pointA = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f };
        float[] pointB = { 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f };

        float sumSq = 0;
        int i = 0;
        int bound = F_SPECIES.loopBound(pointA.length);
        FloatVector vSumSq = FloatVector.zero(F_SPECIES);

        for (; i < bound; i += F_SPECIES.length()) {
            FloatVector va = FloatVector.fromArray(F_SPECIES, pointA, i);
            FloatVector vb = FloatVector.fromArray(F_SPECIES, pointB, i);
            FloatVector diff = va.sub(vb);
            vSumSq = diff.fma(diff, vSumSq); // sumSq += diff²
        }
        sumSq = vSumSq.reduceLanes(VectorOperators.ADD);
        for (; i < pointA.length; i++) {
            float d = pointA[i] - pointB[i];
            sumSq += d * d;
        }

        double distance = Math.sqrt(sumSq);

        IO.println("   Point A: " + formatArray(pointA));
        IO.println("   Point B: " + formatArray(pointB));
        IO.println("   Euclidean distance: " + String.format("%.4f", distance));
        IO.println("   (each dimension differs by 1.0 → √8 = " +
                   String.format("%.4f", Math.sqrt(8)) + ")");
        IO.println();
    }

    // ── Utility formatters ───────────────────────────────────────────────────

    static String formatArray(float[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < Math.min(arr.length, 16); i++) {
            if (i > 0) sb.append(", ");
            sb.append(arr[i] % 1 == 0 ? String.valueOf((int) arr[i]) : String.format("%.1f", arr[i]));
        }
        if (arr.length > 16) sb.append(", ...");
        sb.append("]");
        return sb.toString();
    }

    static String formatFloatArray(float[] arr, int decimals) {
        String fmt = "%." + decimals + "f";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < Math.min(arr.length, 16); i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.format(fmt, arr[i]));
        }
        if (arr.length > 16) sb.append(", ...");
        sb.append("]");
        return sb.toString();
    }
}

