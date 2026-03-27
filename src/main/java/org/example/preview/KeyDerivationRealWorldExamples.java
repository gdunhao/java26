package org.example.preview;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  KDF API — Real-World Use Cases                                            ║
 * ║  Practical examples where Key Derivation gives you a real advantage         ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * These examples show REAL scenarios where the new javax.crypto.KDF API
 * replaces fragile hand-rolled code or third-party dependencies with a
 * clean, standard Java solution.
 *
 * REFERENCES
 * ──────────
 *   • JEP 478 — Key Derivation Function API (Preview):
 *       https://openjdk.org/jeps/478
 *   • HKDF — RFC 5869 (HMAC-based Extract-and-Expand KDF):
 *       https://datatracker.ietf.org/doc/html/rfc5869
 *   • NIST SP 800-56C Rev. 2 — Key-Derivation Methods:
 *       https://csrc.nist.gov/publications/detail/sp/800-56c/rev-2/final
 *   • Javadoc — javax.crypto.KDF (JDK 24):
 *       https://docs.oracle.com/en/java/javase/24/docs/api/java.base/javax/crypto/KDF.html
 *
 * PATTERNS DEMONSTRATED
 * ─────────────────────
 *   1. Encrypt-then-MAC    — Derive independent ENC + MAC keys from one shared secret (TLS, Signal, WireGuard)
 *   2. Per-session keys    — HKDF Expand-Only from a vault master key (web apps, API gateways)
 *   3. API token generation — Deterministic, rotatable tokens from server secret + customer ID (Stripe-style)
 *   4. Per-file encryption — Derive per-file AES key from master + path (E2EE cloud storage, HIPAA)
 *   5. Post-quantum hybrid — Combine ECDH + ML-KEM secrets via Extract (NIST hybrid KE pattern)
 *   6. Multi-tenant isolation — One root key → per-tenant derived keys (SaaS, multi-tenant DBs)
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.preview.KeyDerivationRealWorldExamples
 */
public class KeyDerivationRealWorldExamples {

    public static void main(String[] args) throws Exception {
        IO.println("╔═══════════════════════════════════════════════════════╗");
        IO.println("║  KDF API — Real-World Use Cases                      ║");
        IO.println("╚═══════════════════════════════════════════════════════╝");
        IO.println();

        example1_EncryptThenMacFromSharedSecret();
        example2_PerSessionKeysFromMasterKey();
        example3_SecureTokenGeneration();
        example4_FileEncryptionKeyPerFile();
        example5_PostQuantumKeyExchangePostProcessing();
        example6_MultiTenantKeyIsolation();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 1 — Encrypt-then-MAC from a single shared secret
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Two parties (e.g., client and server) complete a key
    //  exchange (DH, ECDH, or ML-KEM) and end up with ONE shared secret.
    //  But you need TWO independent keys:
    //    • An AES-256 key for encryption
    //    • An HMAC-SHA256 key for message authentication
    //
    //  Using the same key for both encryption and MAC is a well-known
    //  cryptographic anti-pattern. HKDF solves this by deriving separate
    //  keys from the same input, bound to different "info" contexts.
    //
    //  Real users: Any TLS-like protocol, Signal protocol, WireGuard,
    //              custom encrypted messaging, VPN implementations.
    //
    //  BEFORE this API: you'd hand-roll HMAC-based extraction, or pull
    //  in Bouncy Castle just for HKDF — fragile and error-prone.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example1_EncryptThenMacFromSharedSecret() throws Exception {
        IO.println("1️⃣  Encrypt-then-MAC: Two keys from one shared secret");
        IO.println("   Use case: Secure messaging, VPNs, any protocol needing ENC + MAC");
        IO.println("   ────────────────────────────────────────");

        // Simulate a Diffie-Hellman shared secret
        byte[] dhSharedSecret = new byte[32];
        SecureRandom.getInstanceStrong().nextBytes(dhSharedSecret);

        byte[] salt = "myapp-session-salt-2026".getBytes(StandardCharsets.UTF_8);

        KDF hkdf = KDF.getInstance("HKDF-SHA256");

        // Derive encryption key
        SecretKey encKey = hkdf.deriveKey("AES",
            HKDFParameterSpec.ofExtract()
                .addIKM(new SecretKeySpec(dhSharedSecret, "Generic"))
                .addSalt(salt)
                .thenExpand("encrypt".getBytes(StandardCharsets.UTF_8), 32));

        // Derive MAC key (different "info" → completely independent key)
        SecretKey macKey = hkdf.deriveKey("HmacSHA256",
            HKDFParameterSpec.ofExtract()
                .addIKM(new SecretKeySpec(dhSharedSecret, "Generic"))
                .addSalt(salt)
                .thenExpand("authenticate".getBytes(StandardCharsets.UTF_8), 32));

        // Now encrypt a message with AES-GCM
        String plaintext = "Transfer $500 to Alice";
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        SecureRandom.getInstanceStrong().nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, encKey, new GCMParameterSpec(128, iv));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // Compute MAC over (IV || ciphertext)
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(macKey);
        mac.update(iv);
        byte[] tag = mac.doFinal(ciphertext);

        IO.println("   Shared secret:   " + bytesToHex(dhSharedSecret).substring(0, 32) + "...");
        IO.println("   Enc key (AES):   " + bytesToHex(encKey.getEncoded()).substring(0, 32) + "...");
        IO.println("   MAC key (HMAC):  " + bytesToHex(macKey.getEncoded()).substring(0, 32) + "...");
        IO.println("   Keys differ:     " + !Arrays.equals(encKey.getEncoded(), macKey.getEncoded()));
        IO.println("   Plaintext:       " + plaintext);
        IO.println("   Ciphertext:      " + Base64.getEncoder().encodeToString(ciphertext));
        IO.println("   MAC tag:         " + bytesToHex(tag).substring(0, 32) + "...");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 2 — Per-session keys from a long-lived master key
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Your application stores a single master key in a vault
    //  (e.g., AWS KMS, HashiCorp Vault). For each user session, you need
    //  a unique session key so that compromising one session doesn't
    //  expose others.
    //
    //  HKDF Expand-Only is perfect here: the master key is already
    //  high-quality (no extraction needed), and the session ID provides
    //  uniqueness.
    //
    //  Real users: Web applications, API gateways, session-based
    //              encryption (e.g., encrypting session cookies).
    //
    //  BEFORE this API: developers often used SHA-256(masterKey + sessionId)
    //  which is NOT a proper KDF and can leak information.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example2_PerSessionKeysFromMasterKey() throws Exception {
        IO.println("2️⃣  Per-Session Keys from a Master Key");
        IO.println("   Use case: Web apps, API gateways, session cookie encryption");
        IO.println("   ────────────────────────────────────────");

        // Master key (stored securely in a vault)
        byte[] masterKeyBytes = new byte[32];
        SecureRandom.getInstanceStrong().nextBytes(masterKeyBytes);
        SecretKey masterKey = new SecretKeySpec(masterKeyBytes, "Generic");

        KDF hkdf = KDF.getInstance("HKDF-SHA256");

        // Derive unique keys for different sessions
        String[] sessionIds = {"sess-abc-001", "sess-def-002", "sess-ghi-003"};
        IO.println("   Master key: " + bytesToHex(masterKeyBytes).substring(0, 32) + "...");
        IO.println();

        for (String sessionId : sessionIds) {
            // Each session gets its own key via Expand-Only
            // (master key is already strong — no extract needed)
            AlgorithmParameterSpec params = HKDFParameterSpec.expandOnly(
                masterKey,
                sessionId.getBytes(StandardCharsets.UTF_8),
                32  // 256-bit AES key
            );

            SecretKey sessionKey = hkdf.deriveKey("AES", params);
            IO.println("   Session: " + sessionId
                + " → key: " + bytesToHex(sessionKey.getEncoded()).substring(0, 24) + "...");
        }

        IO.println();
        IO.println("   ✓ Each session gets a unique key");
        IO.println("   ✓ Compromising one session key doesn't reveal the master key");
        IO.println("   ✓ No need to call the vault per-session (derive locally)");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 3 — Secure API token / secret generation
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Your SaaS platform needs to issue unique API tokens for
    //  each customer. Tokens must be:
    //    • Deterministically reproducible (given the same seed + customer)
    //    • Cryptographically independent (can't derive one from another)
    //    • Revocable (tied to a version counter)
    //
    //  HKDF lets you derive tokens from a server secret + customer ID +
    //  version, so you can re-derive them without storing them, and
    //  rotate by bumping the version.
    //
    //  Real users: Stripe-style API keys, webhook signing secrets,
    //              OAuth client secrets, service-to-service auth.
    //
    //  BEFORE this API: developers often used UUID.randomUUID() (not
    //  reproducible) or SHA-256 hashing (not a proper KDF).
    // ═══════════════════════════════════════════════════════════════════════════
    static void example3_SecureTokenGeneration() throws Exception {
        IO.println("3️⃣  Deterministic API Token Generation");
        IO.println("   Use case: SaaS API keys, webhook secrets, service auth");
        IO.println("   ────────────────────────────────────────");

        // Server-side secret (rotated periodically, stored in vault)
        byte[] serverSecret = "server-root-secret-v3-2026".getBytes(StandardCharsets.UTF_8);
        byte[] salt = "api-token-derivation".getBytes(StandardCharsets.UTF_8);

        KDF hkdf = KDF.getInstance("HKDF-SHA256");

        record Customer(String id, String name) {}
        var customers = new Customer[]{
            new Customer("cust_001", "Acme Corp"),
            new Customer("cust_002", "Globex Inc"),
            new Customer("cust_003", "Initech LLC"),
        };

        for (var customer : customers) {
            // Info = customer ID + version → deterministic, rotatable
            String info = "api-token:" + customer.id() + ":v1";

            SecretKey tokenKey = hkdf.deriveKey("Generic",
                HKDFParameterSpec.ofExtract()
                    .addIKM(new SecretKeySpec(serverSecret, "Generic"))
                    .addSalt(salt)
                    .thenExpand(info.getBytes(StandardCharsets.UTF_8), 32));

            String token = "sk_live_" + bytesToHex(tokenKey.getEncoded());
            IO.println("   " + customer.name() + " → " + token.substring(0, 32) + "...");
        }

        IO.println();
        IO.println("   ✓ Tokens are deterministic (re-derivable without storage)");
        IO.println("   ✓ Changing 'v1' to 'v2' rotates all tokens instantly");
        IO.println("   ✓ Customer tokens are cryptographically independent");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 4 — Per-file encryption key (client-side encryption)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: A cloud storage app (like Tresorit, Boxcryptor) encrypts
    //  each file with a unique key. Storing millions of keys is impractical.
    //  Instead, derive each file's key from a master key + file path.
    //
    //  Benefits:
    //    • No key database needed (keys are derived on-the-fly)
    //    • Deleting a file = just forget its path
    //    • Master key rotation: re-derive all keys with new master
    //
    //  Real users: End-to-end encrypted cloud storage, encrypted backups,
    //              HIPAA-compliant file handling.
    //
    //  BEFORE this API: hand-rolling HMAC chains, or storing a key per
    //  file in a database (scaling nightmare).
    // ═══════════════════════════════════════════════════════════════════════════
    static void example4_FileEncryptionKeyPerFile() throws Exception {
        IO.println("4️⃣  Per-File Encryption Keys (Client-Side Encryption)");
        IO.println("   Use case: Encrypted cloud storage, HIPAA backups, E2EE file sharing");
        IO.println("   ────────────────────────────────────────");

        // User's master key (derived from password or stored in secure element)
        byte[] userMasterKey = new byte[32];
        SecureRandom.getInstanceStrong().nextBytes(userMasterKey);

        KDF hkdf = KDF.getInstance("HKDF-SHA256");

        String[] filePaths = {
            "/documents/tax-return-2025.pdf",
            "/photos/vacation/IMG_0042.jpg",
            "/medical/lab-results-march.pdf",
        };

        IO.println("   Master key: " + bytesToHex(userMasterKey).substring(0, 32) + "...");
        IO.println();

        for (String filePath : filePaths) {
            // Derive a unique AES key for each file
            AlgorithmParameterSpec params = HKDFParameterSpec.expandOnly(
                new SecretKeySpec(userMasterKey, "Generic"),
                ("file-key:" + filePath).getBytes(StandardCharsets.UTF_8),
                32  // AES-256
            );

            SecretKey fileKey = hkdf.deriveKey("AES", params);

            // Encrypt a small "file" to prove it works
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[12];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, fileKey, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(("Content of " + filePath).getBytes(StandardCharsets.UTF_8));

            IO.println("   " + filePath);
            IO.println("     Key: " + bytesToHex(fileKey.getEncoded()).substring(0, 24) + "...");
            IO.println("     Encrypted size: " + encrypted.length + " bytes");
        }

        IO.println();
        IO.println("   ✓ No key database — keys derived on-the-fly from master + path");
        IO.println("   ✓ Each file has an independent key (compromise one ≠ compromise all)");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 5 — Post-quantum key exchange post-processing
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Your app uses ML-KEM (Kyber) for quantum-resistant key
    //  exchange. The raw shared secret from KEM encapsulation is good,
    //  but you still need to derive usable keys from it.
    //
    //  NIST recommends running the KEM output through a KDF to:
    //    • Bind the key to the transcript/context
    //    • Derive multiple keys (encryption, MAC, nonce, etc.)
    //    • Add domain separation
    //
    //  Real users: Post-quantum TLS implementations, Signal's PQXDH,
    //              hybrid key exchange (ECDH + ML-KEM).
    //
    //  This example shows the "hybrid" pattern: combining a classical
    //  ECDH secret with a post-quantum ML-KEM secret.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example5_PostQuantumKeyExchangePostProcessing() throws Exception {
        IO.println("5️⃣  Post-Quantum Hybrid Key Exchange Post-Processing");
        IO.println("   Use case: Quantum-safe TLS, Signal PQXDH, hybrid ECDH+ML-KEM");
        IO.println("   ────────────────────────────────────────");

        // Simulate two shared secrets from parallel key exchanges
        byte[] ecdhSecret = new byte[32];   // From classical ECDH
        byte[] mlkemSecret = new byte[32];  // From ML-KEM (Kyber)
        SecureRandom.getInstanceStrong().nextBytes(ecdhSecret);
        SecureRandom.getInstanceStrong().nextBytes(mlkemSecret);

        KDF hkdf = KDF.getInstance("HKDF-SHA256");

        // Combine both secrets as IKM (HKDF's Extract handles this safely)
        // This is the hybrid pattern recommended by NIST and used in TLS 1.3
        AlgorithmParameterSpec params = HKDFParameterSpec.ofExtract()
            .addIKM(new SecretKeySpec(ecdhSecret, "Generic"))
            .addIKM(new SecretKeySpec(mlkemSecret, "Generic"))   // Both fed into Extract
            .addSalt("hybrid-ke-v1-2026".getBytes(StandardCharsets.UTF_8))
            .thenExpand("handshake-traffic-key".getBytes(StandardCharsets.UTF_8), 32);

        SecretKey handshakeKey = hkdf.deriveKey("AES", params);

        IO.println("   ECDH secret:      " + bytesToHex(ecdhSecret).substring(0, 32) + "...");
        IO.println("   ML-KEM secret:    " + bytesToHex(mlkemSecret).substring(0, 32) + "...");
        IO.println("   Combined key:     " + bytesToHex(handshakeKey.getEncoded()).substring(0, 32) + "...");
        IO.println();
        IO.println("   ✓ Secure even if ML-KEM is broken (ECDH secret still protects)");
        IO.println("   ✓ Secure even if ECDH is broken by quantum (ML-KEM protects)");
        IO.println("   ✓ HKDF safely combines multiple IKM sources into one strong key");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 6 — Multi-tenant key isolation
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: You run a multi-tenant SaaS (e.g., a database service,
    //  document platform, or health-tech app). Each tenant's data must be
    //  encrypted with a completely independent key, but managing thousands
    //  of keys in a vault is expensive and slow.
    //
    //  Solution: Store ONE root key in the vault. Derive per-tenant keys
    //  using HKDF with the tenant ID as the "info" parameter. This gives
    //  you cryptographic isolation without the operational burden.
    //
    //  Real users: Multi-tenant SaaS platforms, database-as-a-service
    //              (e.g., CockroachDB, PlanetScale), healthcare platforms.
    //
    //  BEFORE this API: each tenant needed its own key in the vault,
    //  or devs used weak derivation like SHA-256(rootKey || tenantId).
    // ═══════════════════════════════════════════════════════════════════════════
    static void example6_MultiTenantKeyIsolation() throws Exception {
        IO.println("6️⃣  Multi-Tenant Key Isolation");
        IO.println("   Use case: SaaS platforms, multi-tenant DBs, healthcare data");
        IO.println("   ────────────────────────────────────────");

        // One root key in the vault
        byte[] rootKey = new byte[32];
        SecureRandom.getInstanceStrong().nextBytes(rootKey);

        KDF hkdf = KDF.getInstance("HKDF-SHA256");

        record Tenant(String id, String name, String sampleData) {}
        var tenants = new Tenant[]{
            new Tenant("tenant_healthcare_01", "MedVault Health", "Patient SSN: 123-45-6789"),
            new Tenant("tenant_fintech_02", "PayFlow Inc", "Account: 9876543210"),
            new Tenant("tenant_edtech_03", "LearnHub", "Student GPA: 3.85"),
        };

        IO.println("   Root key (vault): " + bytesToHex(rootKey).substring(0, 32) + "...");
        IO.println();

        for (var tenant : tenants) {
            // Derive per-tenant key
            AlgorithmParameterSpec params = HKDFParameterSpec.expandOnly(
                new SecretKeySpec(rootKey, "Generic"),
                ("tenant-data-key:" + tenant.id()).getBytes(StandardCharsets.UTF_8),
                32
            );
            SecretKey tenantKey = hkdf.deriveKey("AES", params);

            // Encrypt tenant data
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[12];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, tenantKey, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(tenant.sampleData().getBytes(StandardCharsets.UTF_8));

            // Decrypt to prove isolation works
            cipher.init(Cipher.DECRYPT_MODE, tenantKey, new GCMParameterSpec(128, iv));
            String decrypted = new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);

            IO.println("   " + tenant.name() + " (" + tenant.id() + ")");
            IO.println("     Tenant key:   " + bytesToHex(tenantKey.getEncoded()).substring(0, 24) + "...");
            IO.println("     Plaintext:    " + tenant.sampleData());
            IO.println("     Encrypted:    " + Base64.getEncoder().encodeToString(encrypted).substring(0, 32) + "...");
            IO.println("     Decrypted OK: " + decrypted.equals(tenant.sampleData()));
        }

        IO.println();
        IO.println("   ✓ One root key → thousands of independent tenant keys");
        IO.println("   ✓ Tenant keys are cryptographically isolated");
        IO.println("   ✓ No vault call per tenant (derive locally, instantly)");
        IO.println("   ✓ Root key rotation: re-derive + re-encrypt in batch");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Utility
    // ═══════════════════════════════════════════════════════════════════════════

    static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}

