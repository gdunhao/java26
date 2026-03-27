package org.example.standard;

import java.security.*;
import javax.crypto.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  JEP 496: Quantum-Resistant Module-Lattice-Based Key Encapsulation         ║
 * ║           Mechanism (ML-KEM)                                               ║
 * ║  Status: FINAL in JDK 26                                                   ║
 * ║  Spec: https://openjdk.org/jeps/496                                        ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * WHAT THIS FEATURE DOES
 * ──────────────────────
 * ML-KEM (Module-Lattice-Based Key Encapsulation Mechanism) is a
 * quantum-resistant cryptographic algorithm standardized by NIST as FIPS 203.
 * It allows two parties to securely establish a shared secret key, even in the
 * presence of a quantum computer.
 *
 * KEY ENCAPSULATION MECHANISM (KEM) EXPLAINED
 * ────────────────────────────────────────────
 * A KEM is different from traditional key exchange (like Diffie-Hellman):
 *
 *   1. Key Generation:  Alice generates a keypair (public + private)
 *   2. Encapsulation:   Bob uses Alice's public key to produce:
 *                          - An "encapsulation" (ciphertext) to send to Alice
 *                          - A shared secret key (for Bob's use)
 *   3. Decapsulation:   Alice uses her private key + the encapsulation to
 *                        derive the SAME shared secret key
 *
 * Both parties now share a secret key without ever transmitting it directly.
 *
 * ML-KEM PARAMETER SETS
 * ─────────────────────
 *   - ML-KEM-512   — NIST Security Level 1 (≈ AES-128 equivalent)
 *   - ML-KEM-768   — NIST Security Level 3 (≈ AES-192 equivalent)
 *   - ML-KEM-1024  — NIST Security Level 5 (≈ AES-256 equivalent)
 *
 * WHY IT MATTERS
 * ──────────────
 * Quantum computers threaten RSA and elliptic-curve cryptography. ML-KEM is
 * based on lattice problems that are believed to be hard for both classical
 * and quantum computers. JDK 26 makes Java one of the first mainstream
 * platforms to include built-in quantum-resistant key exchange.
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.standard.QuantumKemDemo
 */
public class QuantumKemDemo {

    public static void main(String[] args) throws Exception {
        IO.println("╔═══════════════════════════════════════════════════╗");
        IO.println("║  JEP 496 — Quantum-Resistant ML-KEM             ║");
        IO.println("╚═══════════════════════════════════════════════════╝");
        IO.println();

        demoMlKem("ML-KEM-512");
        demoMlKem("ML-KEM-768");
        demoMlKem("ML-KEM-1024");
    }

    /**
     * Demonstrates the full KEM workflow:
     *   1. Alice generates a keypair
     *   2. Bob encapsulates using Alice's public key → gets ciphertext + shared secret
     *   3. Alice decapsulates using her private key + ciphertext → gets same shared secret
     *   4. Verify both shared secrets match
     */
    static void demoMlKem(String algorithm) throws Exception {
        IO.println("🔐 " + algorithm);
        IO.println("   ────────────────────────────────────────");

        // ─── Step 1: Alice generates her ML-KEM keypair ───
        IO.println("   Step 1: Alice generates keypair...");
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm);
        KeyPair aliceKeyPair = kpg.generateKeyPair();

        IO.println("   Public key size:  " + aliceKeyPair.getPublic().getEncoded().length + " bytes");
        IO.println("   Private key size: " + aliceKeyPair.getPrivate().getEncoded().length + " bytes");

        // ─── Step 2: Bob encapsulates a secret using Alice's public key ───
        IO.println("   Step 2: Bob encapsulates with Alice's public key...");
        KEM kemSender = KEM.getInstance(algorithm);
        KEM.Encapsulator encapsulator = kemSender.newEncapsulator(aliceKeyPair.getPublic());
        KEM.Encapsulated encapsulated = encapsulator.encapsulate();

        byte[] ciphertext = encapsulated.encapsulation();
        SecretKey bobSecret = encapsulated.key();

        IO.println("   Ciphertext size:    " + ciphertext.length + " bytes");
        IO.println("   Bob's shared secret: " + bytesToHex(bobSecret.getEncoded(), 16) + "...");

        // ─── Step 3: Alice decapsulates using her private key ───
        IO.println("   Step 3: Alice decapsulates with her private key...");
        KEM kemReceiver = KEM.getInstance(algorithm);
        KEM.Decapsulator decapsulator = kemReceiver.newDecapsulator(aliceKeyPair.getPrivate());
        SecretKey aliceSecret = decapsulator.decapsulate(ciphertext);

        IO.println("   Alice's shared secret: " + bytesToHex(aliceSecret.getEncoded(), 16) + "...");

        // ─── Step 4: Verify secrets match ───
        boolean match = java.util.Arrays.equals(
            bobSecret.getEncoded(), aliceSecret.getEncoded());
        IO.println("   ✅ Secrets match: " + match);
        IO.println("   Secret algorithm: " + aliceSecret.getAlgorithm());
        IO.println("   Secret size: " + aliceSecret.getEncoded().length * 8 + " bits");
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

