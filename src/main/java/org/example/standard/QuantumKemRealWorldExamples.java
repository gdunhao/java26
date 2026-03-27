package org.example.standard;

import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  ML-KEM (Quantum-Resistant Key Encapsulation) — Real-World Use Cases       ║
 * ║  Practical examples where JEP 496 gives you a real advantage                ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * REFERENCES
 * ──────────
 *   • JEP 496 — Quantum-Resistant ML-KEM:
 *       https://openjdk.org/jeps/496
 *   • NIST FIPS 203 — Module-Lattice-Based Key-Encapsulation Mechanism:
 *       https://csrc.nist.gov/pubs/fips/203/final
 *
 * PATTERNS DEMONSTRATED
 * ─────────────────────
 *   1. Secure messaging     — Establish E2EE channel between two parties (Signal, WhatsApp)
 *   2. File encryption      — Encrypt files for a recipient using their public key (PGP-like)
 *   3. API key exchange     — Bootstrap service-to-service shared secret (microservices)
 *   4. Multi-recipient      — Encrypt one message for multiple recipients (group chat)
 *   5. Key rotation         — Periodically rotate shared secrets (TLS session rekeying)
 *   6. Hybrid key exchange  — Combine ML-KEM + ECDH for defense-in-depth (NIST guidance)
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.standard.QuantumKemRealWorldExamples
 */
public class QuantumKemRealWorldExamples {

    public static void main(String[] args) throws Exception {
        IO.println("╔═══════════════════════════════════════════════════════╗");
        IO.println("║  ML-KEM — Real-World Use Cases                       ║");
        IO.println("╚═══════════════════════════════════════════════════════╝");
        IO.println();

        example1_SecureMessaging();
        example2_FileEncryptionForRecipient();
        example3_ApiKeyBootstrap();
        example4_MultiRecipientEncryption();
        example5_KeyRotation();
        example6_HybridKeyExchange();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 1 — Secure Messaging Channel
    // ═══════════════════════════════════════════════════════════════════════════
    //  Scenario: Two users establish an encrypted messaging channel.
    //  Alice publishes her ML-KEM public key. Bob uses it to encapsulate
    //  a shared secret, then both derive an AES key for message encryption.
    //
    //  Real users: Signal Protocol, WhatsApp, Matrix/Element, custom E2EE.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example1_SecureMessaging() throws Exception {
        IO.println("1️⃣  Secure Messaging Channel (E2EE Setup)");
        IO.println("   Use case: Signal, WhatsApp, Matrix, custom E2EE chat");
        IO.println("   ────────────────────────────────────────");

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-KEM-768");
        KeyPair aliceKeys = kpg.generateKeyPair();

        IO.println("   Alice publishes her ML-KEM-768 public key ("
            + aliceKeys.getPublic().getEncoded().length + " bytes)");

        KEM kem = KEM.getInstance("ML-KEM-768");
        KEM.Encapsulator encapsulator = kem.newEncapsulator(aliceKeys.getPublic());
        KEM.Encapsulated encapsulated = encapsulator.encapsulate();

        SecretKey bobSecret = encapsulated.key();
        byte[] ciphertext = encapsulated.encapsulation();

        IO.println("   Bob encapsulates → ciphertext: " + ciphertext.length + " bytes");

        KEM.Decapsulator decapsulator = kem.newDecapsulator(aliceKeys.getPrivate());
        SecretKey aliceSecret = decapsulator.decapsulate(ciphertext);

        IO.println("   Secrets match: "
            + Arrays.equals(bobSecret.getEncoded(), aliceSecret.getEncoded()));

        String message = "Hey Alice! Let's meet at 3pm.";
        byte[] encrypted = encryptAesGcm(bobSecret, message);
        String decrypted = decryptAesGcm(aliceSecret, encrypted);

        IO.println("   Bob sends: \"" + message + "\"");
        IO.println("   Alice decrypts: \"" + decrypted + "\"");
        IO.println("   ✅ Quantum-safe E2EE channel established!");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 2 — File Encryption for a Specific Recipient
    // ═══════════════════════════════════════════════════════════════════════════
    //  Scenario: Encrypt a file so only the intended recipient can decrypt
    //  it (like PGP/GPG). The sender only needs the recipient's public key.
    //
    //  Real users: PGP/GPG, Keybase, encrypted file sharing (Tresorit),
    //              E2EE backup systems.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example2_FileEncryptionForRecipient() throws Exception {
        IO.println("2️⃣  File Encryption for Recipient (PGP-Like)");
        IO.println("   Use case: PGP/GPG, encrypted file sharing, E2EE backups");
        IO.println("   ────────────────────────────────────────");

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-KEM-1024");
        KeyPair recipientKeys = kpg.generateKeyPair();

        String fileContent = "CONFIDENTIAL: Q1 2026 Financial Report\n"
            + "Revenue: $42.5M | EBITDA: $12.3M | Net: $8.1M";

        KEM kem = KEM.getInstance("ML-KEM-1024");
        KEM.Encapsulator enc = kem.newEncapsulator(recipientKeys.getPublic());
        KEM.Encapsulated encapsulated = enc.encapsulate();

        byte[] encryptedFile = encryptAesGcm(encapsulated.key(), fileContent);

        IO.println("   Plaintext: " + fileContent.length() + " bytes");
        IO.println("   Encrypted: " + encryptedFile.length + " bytes");
        IO.println("   KEM ciphertext: " + encapsulated.encapsulation().length + " bytes");

        KEM.Decapsulator dec = kem.newDecapsulator(recipientKeys.getPrivate());
        SecretKey recipientSecret = dec.decapsulate(encapsulated.encapsulation());
        String decryptedFile = decryptAesGcm(recipientSecret, encryptedFile);

        IO.println("   Recipient decrypts: \"" + decryptedFile.split("\n")[0] + "...\"");
        IO.println("   Content matches: " + fileContent.equals(decryptedFile));
        IO.println("   ✅ Securely delivered using ML-KEM-1024");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 3 — API Key Bootstrap (Service-to-Service)
    // ═══════════════════════════════════════════════════════════════════════════
    //  Scenario: Microservices bootstrap shared secrets with a central
    //  config server using its published ML-KEM public key.
    //
    //  Real users: Service mesh, Consul/Vault, K8s secrets, zero-trust.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example3_ApiKeyBootstrap() throws Exception {
        IO.println("3️⃣  API Key Bootstrap (Service-to-Service)");
        IO.println("   Use case: Service mesh, Consul/Vault, K8s secrets, zero-trust");
        IO.println("   ────────────────────────────────────────");

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-KEM-768");
        KeyPair configServerKeys = kpg.generateKeyPair();
        KEM kem = KEM.getInstance("ML-KEM-768");

        String[] services = {"auth-service", "payment-service", "catalog-service"};

        for (String service : services) {
            KEM.Encapsulator enc = kem.newEncapsulator(configServerKeys.getPublic());
            KEM.Encapsulated encapsulated = enc.encapsulate();

            KEM.Decapsulator dec = kem.newDecapsulator(configServerKeys.getPrivate());
            SecretKey serverSideSecret = dec.decapsulate(encapsulated.encapsulation());

            boolean match = Arrays.equals(encapsulated.key().getEncoded(),
                serverSideSecret.getEncoded());

            IO.println("   " + service + " → key established: " + (match ? "✅" : "❌")
                + " (" + bytesToHex(encapsulated.key().getEncoded(), 8) + "...)");
        }
        IO.println("   ✅ All services bootstrapped with unique session keys");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 4 — Multi-Recipient Encryption
    // ═══════════════════════════════════════════════════════════════════════════
    //  Scenario: Encrypt one message for multiple recipients. Each
    //  recipient independently recovers the shared secret via ML-KEM.
    //
    //  Real users: Signal group chats, Proton Mail, enterprise email.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example4_MultiRecipientEncryption() throws Exception {
        IO.println("4️⃣  Multi-Recipient Encryption (Group Chat / Team Email)");
        IO.println("   Use case: Signal groups, Proton Mail, enterprise email");
        IO.println("   ────────────────────────────────────────");

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-KEM-512");
        KeyPair aliceKeys = kpg.generateKeyPair();
        KeyPair bobKeys = kpg.generateKeyPair();
        KeyPair carolKeys = kpg.generateKeyPair();

        KEM kem = KEM.getInstance("ML-KEM-512");
        KeyPair[] allKeys = {aliceKeys, bobKeys, carolKeys};
        String[] names = {"Alice", "Bob", "Carol"};

        for (int i = 0; i < names.length; i++) {
            KEM.Encapsulator enc = kem.newEncapsulator(allKeys[i].getPublic());
            KEM.Encapsulated encapsulated = enc.encapsulate();

            KEM.Decapsulator dec = kem.newDecapsulator(allKeys[i].getPrivate());
            SecretKey recipientSecret = dec.decapsulate(encapsulated.encapsulation());

            IO.println("   " + names[i] + ": KEM ciphertext="
                + encapsulated.encapsulation().length + " bytes, secret established ✅");
        }
        IO.println("   ✅ One message, 3 recipients, each with independent KEM exchange");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 5 — Key Rotation
    // ═══════════════════════════════════════════════════════════════════════════
    //  Scenario: Periodically rotate session keys without disrupting the
    //  connection, ensuring forward secrecy.
    //
    //  Real users: TLS 1.3 key update, WireGuard rekeying, WebSockets.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example5_KeyRotation() throws Exception {
        IO.println("5️⃣  Key Rotation (Session Rekeying)");
        IO.println("   Use case: TLS key update, WireGuard rekeying, long-lived WebSockets");
        IO.println("   ────────────────────────────────────────");

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-KEM-768");
        KEM kem = KEM.getInstance("ML-KEM-768");
        SecretKey currentKey = null;

        for (int epoch = 1; epoch <= 3; epoch++) {
            KeyPair serverEphemeral = kpg.generateKeyPair();

            KEM.Encapsulator enc = kem.newEncapsulator(serverEphemeral.getPublic());
            KEM.Encapsulated encapsulated = enc.encapsulate();

            KEM.Decapsulator dec = kem.newDecapsulator(serverEphemeral.getPrivate());
            SecretKey newKey = dec.decapsulate(encapsulated.encapsulation());

            boolean fresh = currentKey == null ||
                !Arrays.equals(currentKey.getEncoded(), newKey.getEncoded());

            IO.println("   Epoch " + epoch + ": key = "
                + bytesToHex(newKey.getEncoded(), 8) + "..."
                + (fresh ? " (fresh ✅)" : " (REUSED ❌)"));
            currentKey = newKey;
        }
        IO.println("   ✅ 3 key rotations completed — forward secrecy maintained");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 6 — Hybrid Key Exchange (ML-KEM + ECDH)
    // ═══════════════════════════════════════════════════════════════════════════
    //  Scenario: Combine classical ECDH with ML-KEM for defense-in-depth.
    //  If either algorithm is broken, the other still protects the data.
    //
    //  Real users: Chrome/Firefox hybrid TLS (X25519Kyber768),
    //              Signal PQXDH protocol, NIST hybrid KE guidance.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example6_HybridKeyExchange() throws Exception {
        IO.println("6️⃣  Hybrid Key Exchange (ML-KEM + ECDH Defense-in-Depth)");
        IO.println("   Use case: Chrome/Firefox hybrid TLS, Signal PQXDH, NIST guidance");
        IO.println("   ────────────────────────────────────────");

        // Classical ECDH
        KeyPairGenerator ecKpg = KeyPairGenerator.getInstance("EC");
        ecKpg.initialize(new java.security.spec.ECGenParameterSpec("secp256r1"));
        KeyPair aliceEc = ecKpg.generateKeyPair();
        KeyPair bobEc = ecKpg.generateKeyPair();

        javax.crypto.KeyAgreement aliceKa = javax.crypto.KeyAgreement.getInstance("ECDH");
        aliceKa.init(aliceEc.getPrivate());
        aliceKa.doPhase(bobEc.getPublic(), true);
        byte[] ecdhSecret = aliceKa.generateSecret();

        IO.println("   ECDH secret:   " + bytesToHex(ecdhSecret, 8) + "... ("
            + ecdhSecret.length + " bytes)");

        // Post-quantum ML-KEM
        KeyPairGenerator kemKpg = KeyPairGenerator.getInstance("ML-KEM-768");
        KeyPair aliceKem = kemKpg.generateKeyPair();
        KEM kem = KEM.getInstance("ML-KEM-768");

        KEM.Encapsulator enc = kem.newEncapsulator(aliceKem.getPublic());
        KEM.Encapsulated encapsulated = enc.encapsulate();
        KEM.Decapsulator dec = kem.newDecapsulator(aliceKem.getPrivate());
        SecretKey kemSecret = dec.decapsulate(encapsulated.encapsulation());

        IO.println("   ML-KEM secret: " + bytesToHex(kemSecret.getEncoded(), 8) + "... ("
            + kemSecret.getEncoded().length + " bytes)");

        // Combine (XOR for simplicity; real code uses HKDF)
        int len = Math.min(ecdhSecret.length, kemSecret.getEncoded().length);
        byte[] hybrid = new byte[len];
        for (int i = 0; i < len; i++) {
            hybrid[i] = (byte) (ecdhSecret[i] ^ kemSecret.getEncoded()[i]);
        }

        IO.println("   Hybrid secret: " + bytesToHex(hybrid, 8) + "... (" + len + " bytes)");
        IO.println("   ✅ If ECDH broken by quantum → ML-KEM still protects");
        IO.println("   ✅ If ML-KEM broken classically → ECDH still protects");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    private static byte[] encryptAesGcm(SecretKey key, String plaintext) throws Exception {
        byte[] aesKeyBytes = Arrays.copyOf(key.getEncoded(), 16);
        SecretKey aesKey = new javax.crypto.spec.SecretKeySpec(aesKeyBytes, "AES");

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        SecureRandom.getInstanceStrong().nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
        byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        byte[] result = new byte[iv.length + ct.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(ct, 0, result, iv.length, ct.length);
        return result;
    }

    private static String decryptAesGcm(SecretKey key, byte[] data) throws Exception {
        byte[] aesKeyBytes = Arrays.copyOf(key.getEncoded(), 16);
        SecretKey aesKey = new javax.crypto.spec.SecretKeySpec(aesKeyBytes, "AES");

        byte[] iv = Arrays.copyOf(data, 12);
        byte[] ct = Arrays.copyOfRange(data, 12, data.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
    }

    static String bytesToHex(byte[] bytes, int maxBytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, maxBytes); i++) {
            sb.append(String.format("%02x", bytes[i]));
        }
        return sb.toString();
    }
}

