package org.example.standard;

import java.security.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  JEP 497: Quantum-Resistant Module-Lattice-Based Digital Signature         ║
 * ║           Algorithm (ML-DSA)                                               ║
 * ║  Status: FINAL in JDK 26                                                   ║
 * ║  Spec: https://openjdk.org/jeps/497                                        ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * WHAT THIS FEATURE DOES
 * ──────────────────────
 * ML-DSA (Module-Lattice-Based Digital Signature Algorithm) is a
 * quantum-resistant digital signature scheme standardized by NIST as FIPS 204.
 * It allows you to sign and verify data with resistance to quantum attacks.
 *
 * DIGITAL SIGNATURES EXPLAINED
 * ────────────────────────────
 * A digital signature provides:
 *   - Authentication: proof that the signer owns the private key
 *   - Integrity: proof that the message hasn't been tampered with
 *   - Non-repudiation: the signer can't deny having signed
 *
 * The workflow:
 *   1. Signer generates a keypair (public + private)
 *   2. Signer signs data with their private key → produces a signature
 *   3. Verifier uses the signer's public key to verify the signature
 *
 * ML-DSA PARAMETER SETS
 * ─────────────────────
 *   - ML-DSA-44  — NIST Security Level 2 (≈ SHA-256/AES-128 collision)
 *   - ML-DSA-65  — NIST Security Level 3 (≈ AES-192 equivalent)
 *   - ML-DSA-87  — NIST Security Level 5 (≈ AES-256 equivalent)
 *
 * The numbers (44, 65, 87) refer to the matrix dimensions (k, l) used
 * in the lattice construction.
 *
 * COMPARISON TO CLASSICAL SIGNATURES
 * ───────────────────────────────────
 *   Algorithm     Key Size      Sig Size    Quantum-Safe?
 *   RSA-2048      256 bytes     256 bytes   ❌ No
 *   ECDSA P-256    64 bytes      64 bytes   ❌ No
 *   ML-DSA-44    1312 bytes    2420 bytes   ✅ Yes
 *   ML-DSA-65    1952 bytes    3309 bytes   ✅ Yes
 *   ML-DSA-87    2592 bytes    4627 bytes   ✅ Yes
 *
 * Trade-off: larger keys & signatures, but quantum resistance.
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.standard.QuantumDsaDemo
 */
public class QuantumDsaDemo {

    public static void main(String[] args) throws Exception {
        IO.println("╔═══════════════════════════════════════════════════╗");
        IO.println("║  JEP 497 — Quantum-Resistant ML-DSA             ║");
        IO.println("╚═══════════════════════════════════════════════════╝");
        IO.println();

        demoMlDsa("ML-DSA-44");
        demoMlDsa("ML-DSA-65");
        demoMlDsa("ML-DSA-87");
        demoTamperedSignature();
    }

    /**
     * Demonstrates the full digital signature workflow:
     *   1. Generate a keypair
     *   2. Sign a message
     *   3. Verify the signature
     */
    static void demoMlDsa(String algorithm) throws Exception {
        IO.println("✍️  " + algorithm);
        IO.println("   ────────────────────────────────────────");

        // ─── Step 1: Generate keypair ───
        IO.println("   Step 1: Generate keypair...");
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm);
        KeyPair keyPair = kpg.generateKeyPair();

        IO.println("   Public key size:  " + keyPair.getPublic().getEncoded().length + " bytes");
        IO.println("   Private key size: " + keyPair.getPrivate().getEncoded().length + " bytes");

        // ─── Step 2: Sign a message ───
        String message = "Hello from Java 26! This message is signed with " + algorithm;
        IO.println("   Step 2: Sign message: \"" + message + "\"");

        Signature signer = Signature.getInstance(algorithm);
        signer.initSign(keyPair.getPrivate());
        signer.update(message.getBytes());
        byte[] signature = signer.sign();

        IO.println("   Signature size: " + signature.length + " bytes");
        IO.println("   Signature (first 32 bytes): " + bytesToHex(signature, 32) + "...");

        // ─── Step 3: Verify the signature ───
        IO.println("   Step 3: Verify signature...");
        Signature verifier = Signature.getInstance(algorithm);
        verifier.initVerify(keyPair.getPublic());
        verifier.update(message.getBytes());
        boolean valid = verifier.verify(signature);

        IO.println("   ✅ Signature valid: " + valid);
        IO.println();
    }

    /**
     * DEMO: Verify that a tampered message fails verification.
     * This proves the integrity protection of digital signatures.
     */
    static void demoTamperedSignature() throws Exception {
        IO.println("🛡️  Tamper Detection Demo");
        IO.println("   ────────────────────────────────────────");

        String algorithm = "ML-DSA-44";
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm);
        KeyPair keyPair = kpg.generateKeyPair();

        // Sign original message
        String original = "Transfer $100 to Alice";
        Signature signer = Signature.getInstance(algorithm);
        signer.initSign(keyPair.getPrivate());
        signer.update(original.getBytes());
        byte[] signature = signer.sign();
        IO.println("   Signed: \"" + original + "\"");

        // Verify original — should succeed
        Signature verifier = Signature.getInstance(algorithm);
        verifier.initVerify(keyPair.getPublic());
        verifier.update(original.getBytes());
        IO.println("   Verify original: " + verifier.verify(signature));

        // Try to verify tampered message — should fail
        String tampered = "Transfer $1000000 to Eve";
        verifier.initVerify(keyPair.getPublic());
        verifier.update(tampered.getBytes());
        IO.println("   Verify tampered (\"" + tampered + "\"): " + verifier.verify(signature));
        IO.println("   ✅ Tampered message correctly rejected!");
        IO.println();
    }

    /** Helper: convert bytes to hex string, showing only first `maxBytes`. */
    static String bytesToHex(byte[] bytes, int maxBytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, maxBytes); i++) {
            sb.append(String.format("%02x", bytes[i]));
        }
        return sb.toString();
    }
}

