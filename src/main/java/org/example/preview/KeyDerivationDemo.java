package org.example.preview;

import javax.crypto.KDF;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.spec.HKDFParameterSpec;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  JEP 478: Key Derivation Function API (Preview)                            ║
 * ║  Status: PREVIEW in JDK 26                                                 ║
 * ║  Spec: https://openjdk.org/jeps/478                                        ║
 * ║  Requires: --enable-preview                                                ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * WHAT THIS FEATURE DOES
 * ──────────────────────
 * The Key Derivation Function (KDF) API introduces a new `javax.crypto.KDF`
 * class that provides a standard way to derive cryptographic keys from input
 * keying material (IKM). The first supported algorithm is HKDF (HMAC-based
 * Extract-and-Expand Key Derivation Function, RFC 5869).
 *
 * HKDF EXPLAINED
 * ──────────────
 * HKDF works in two phases:
 *
 *   1. EXTRACT — Takes raw keying material (which might be uneven or weak)
 *      and a salt, then produces a pseudorandom key (PRK).
 *
 *   2. EXPAND — Takes the PRK and optional context info, then produces
 *      one or more derived keys of the desired length.
 *
 * You can also do both in one step (Extract-then-Expand), or use just
 * the Expand phase if you already have a good PRK.
 *
 * USE CASES
 * ─────────
 *   - TLS 1.3 key derivation (TLS uses HKDF internally)
 *   - Deriving encryption + MAC keys from a single shared secret
 *   - Password-less key agreement post-processing
 *   - Deriving per-session keys from a master key
 *
 * WHY IT MATTERS
 * ──────────────
 * Before this API, Java had no built-in KDF support. Developers had to:
 *   - Implement HKDF manually using HMAC primitives
 *   - Use Bouncy Castle or other third-party libraries
 *   - Misuse existing APIs (like PBKDF2 for non-password inputs)
 *
 * The new KDF API provides a clean, pluggable, SPI-based approach that
 * follows the same pattern as Cipher, KeyGenerator, etc.
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.preview.KeyDerivationDemo
 */
public class KeyDerivationDemo {

    public static void main(String[] args) throws Exception {
        IO.println("╔═══════════════════════════════════════════════════╗");
        IO.println("║  JEP 478 — Key Derivation Function API (Preview) ║");
        IO.println("╚═══════════════════════════════════════════════════╝");
        IO.println();

        demoHkdfExtractExpand();
        demoHkdfExpandOnly();
        demoDeriveMultipleKeys();
    }

    /**
     * DEMO 1: HKDF Extract-then-Expand.
     *
     * Full HKDF workflow: extract a PRK from raw key material + salt,
     * then expand it into a derived key.
     */
    static void demoHkdfExtractExpand() throws Exception {
        IO.println("1️⃣  HKDF Extract-then-Expand");
        IO.println("   ────────────────────────────────────────");

        // Input keying material (e.g., from a key exchange)
        byte[] ikm = "raw-shared-secret-from-key-exchange".getBytes();

        // Salt — random value for extraction (improves security)
        byte[] salt = "unique-application-salt".getBytes();

        // Info — context string for expansion (binds key to purpose)
        byte[] info = "encryption-key-v1".getBytes();

        // Get HKDF-SHA256 instance via the new KDF API
        KDF hkdf = KDF.getInstance("HKDF-SHA256");

        // Build parameters for Extract-then-Expand in one step
        AlgorithmParameterSpec params = HKDFParameterSpec
            .ofExtract()
            .addIKM(new SecretKeySpec(ikm, "Generic"))
            .addSalt(salt)
            .thenExpand(info, 32);  // 32 bytes = 256-bit key

        // Derive the key
        SecretKey derivedKey = hkdf.deriveKey("AES", params);

        IO.println("   Input material:  " + new String(ikm));
        IO.println("   Salt:            " + new String(salt));
        IO.println("   Info:            " + new String(info));
        IO.println("   Derived key alg: " + derivedKey.getAlgorithm());
        IO.println("   Derived key len: " + derivedKey.getEncoded().length * 8 + " bits");
        IO.println("   Derived key hex: " + bytesToHex(derivedKey.getEncoded()));
        IO.println();
    }

    /**
     * DEMO 2: HKDF Expand-Only.
     *
     * If you already have a high-quality pseudorandom key (PRK), you can
     * skip the Extract phase and just do Expand. Useful when the input
     * is already uniformly random (e.g., from a hardware RNG or KEM).
     */
    static void demoHkdfExpandOnly() throws Exception {
        IO.println("2️⃣  HKDF Expand-Only (skip Extract)");
        IO.println("   ────────────────────────────────────────");

        // Pretend this is a high-quality PRK (e.g., from ML-KEM)
        byte[] prk = new byte[32];
        java.security.SecureRandom.getInstanceStrong().nextBytes(prk);

        byte[] info = "session-key-2026".getBytes();

        KDF hkdf = KDF.getInstance("HKDF-SHA256");

        // Expand-only: no extraction needed
        AlgorithmParameterSpec params = HKDFParameterSpec
            .expandOnly(new SecretKeySpec(prk, "Generic"), info, 16);  // 128-bit key

        SecretKey sessionKey = hkdf.deriveKey("AES", params);

        IO.println("   PRK (random):     " + bytesToHex(prk));
        IO.println("   Info:             " + new String(info));
        IO.println("   Session key alg:  " + sessionKey.getAlgorithm());
        IO.println("   Session key len:  " + sessionKey.getEncoded().length * 8 + " bits");
        IO.println("   Session key hex:  " + bytesToHex(sessionKey.getEncoded()));
        IO.println();
    }

    /**
     * DEMO 3: Derive multiple keys from the same material.
     *
     * A common pattern: derive separate encryption and MAC keys from
     * a single shared secret by varying the "info" parameter.
     */
    static void demoDeriveMultipleKeys() throws Exception {
        IO.println("3️⃣  Derive Multiple Keys (Encryption + MAC)");
        IO.println("   ────────────────────────────────────────");

        byte[] sharedSecret = "post-quantum-shared-secret".getBytes();
        byte[] salt = "my-app-salt-2026".getBytes();

        KDF hkdf = KDF.getInstance("HKDF-SHA256");

        // Derive encryption key (using "enc" context)
        SecretKey encKey = hkdf.deriveKey("AES",
            HKDFParameterSpec.ofExtract()
                .addIKM(new SecretKeySpec(sharedSecret, "Generic"))
                .addSalt(salt)
                .thenExpand("encryption".getBytes(), 32));

        // Derive MAC key (using "mac" context)
        SecretKey macKey = hkdf.deriveKey("HmacSHA256",
            HKDFParameterSpec.ofExtract()
                .addIKM(new SecretKeySpec(sharedSecret, "Generic"))
                .addSalt(salt)
                .thenExpand("authentication".getBytes(), 32));

        IO.println("   Shared secret: " + new String(sharedSecret));
        IO.println("   Encryption key (AES-256):   " + bytesToHex(encKey.getEncoded()));
        IO.println("   MAC key (HMAC-SHA256):       " + bytesToHex(macKey.getEncoded()));
        IO.println("   Keys are different: " + !java.util.Arrays.equals(
            encKey.getEncoded(), macKey.getEncoded()));
        IO.println();
    }

    /** Helper: convert bytes to hex string. */
    static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

