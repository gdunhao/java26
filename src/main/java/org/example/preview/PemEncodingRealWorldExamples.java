package org.example.preview;

import java.security.*;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  PEM Encodings of Cryptographic Objects — Real-World Use Cases              ║
 * ║  Practical examples where JEP 524 gives you a real advantage                ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * REFERENCES
 * ──────────
 *   • JEP 524 — PEM Encodings of Cryptographic Objects (Second Preview):
 *       https://openjdk.org/jeps/524
 *   • RFC 7468 — Textual Encodings of PKIX, PKCS, and CMS Structures:
 *       https://www.rfc-editor.org/rfc/rfc7468
 *   • RFC 5280 — Internet X.509 PKI Certificate and CRL Profile:
 *       https://www.rfc-editor.org/rfc/rfc5280
 *
 * PATTERNS DEMONSTRATED
 * ─────────────────────
 *   1. Key export/import       — Export keys to PEM for cross-platform sharing (OpenSSL, curl)
 *   2. Public key pinning      — Extract and pin server public keys (HPKP-like, TOFU)
 *   3. Key pair backup/restore — Serialize keypairs for secure storage (Vault, HSM staging)
 *   4. PEM type routing        — Inspect PEM type and route to appropriate handler (PKI gateway)
 *   5. Signature with PEM keys — Sign data with PEM-stored keys (CI/CD code signing)
 *   6. Multi-algorithm keyring — Manage keys of different algorithms in PEM format (key mgmt)
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.preview.PemEncodingRealWorldExamples
 */
public class PemEncodingRealWorldExamples {

    public static void main(String[] args) throws Exception {
        IO.println("╔═══════════════════════════════════════════════════════╗");
        IO.println("║  PEM Encodings — Real-World Use Cases                ║");
        IO.println("╚═══════════════════════════════════════════════════════╝");
        IO.println();

        example1_CrossPlatformKeyExport();
        example2_PublicKeyPinning();
        example3_KeyPairBackupRestore();
        example4_PemTypeRouting();
        example5_CodeSigningWithPemKeys();
        example6_MultiAlgorithmKeyring();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 1 — Cross-Platform Key Export
    // ═══════════════════════════════════════════════════════════════════════════
    //  Scenario: Export a Java-generated public key in PEM format so it can
    //  be used by OpenSSL, curl, Python, Go, or any other tool.
    //
    //  Real users: Multi-language microservices, DevOps key distribution,
    //              API authentication setup, webhook verification.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example1_CrossPlatformKeyExport() throws Exception {
        IO.println("1️⃣  Cross-Platform Key Export (Java → OpenSSL/Python/Go)");
        IO.println("   Use case: Multi-language services, webhook verification, DevOps");
        IO.println("   ────────────────────────────────────────");

        // Generate an EC key pair in Java
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(256);
        KeyPair keyPair = kpg.generateKeyPair();

        // Export to PEM — universally understood by all crypto tools
        PEMEncoder encoder = PEMEncoder.of();
        String publicPem = encoder.encodeToString(keyPair.getPublic());
        String privatePem = encoder.encodeToString(keyPair.getPrivate());

        IO.println("   Generated: EC P-256 key pair");
        IO.println("   Public key PEM (share this with other services):");
        IO.println("     " + publicPem.lines().findFirst().orElse(""));
        IO.println("     ... (" + publicPem.lines().count() + " lines total)");
        IO.println("     " + publicPem.lines().reduce((a, b) -> b).orElse(""));
        IO.println("   Private key PEM (keep secret, store in Vault):");
        IO.println("     " + privatePem.lines().findFirst().orElse(""));
        IO.println("     ... (" + privatePem.lines().count() + " lines total)");

        IO.println("   Commands to verify with OpenSSL:");
        IO.println("     echo '<pem>' | openssl ec -pubin -text -noout");
        IO.println("   ✅ PEM format works with OpenSSL, Python cryptography, Go crypto, Node.js");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 2 — Public Key Pinning (TOFU)
    // ═══════════════════════════════════════════════════════════════════════════
    //  Scenario: Implement Trust-On-First-Use by recording a server's public
    //  key in PEM format and comparing on subsequent connections.
    //
    //  Real users: SSH known_hosts, certificate pinning, mobile apps,
    //              HPKP-like protection for API clients.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example2_PublicKeyPinning() throws Exception {
        IO.println("2️⃣  Public Key Pinning (Trust-On-First-Use)");
        IO.println("   Use case: SSH-like known_hosts, cert pinning, mobile API security");
        IO.println("   ────────────────────────────────────────");

        // Simulate a server key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair serverKey = kpg.generateKeyPair();

        // First connection: record the PEM
        PEMEncoder encoder = PEMEncoder.of();
        String pinnedPem = encoder.encodeToString(serverKey.getPublic());

        IO.println("   First connection: pinning server public key");
        IO.println("   Pin: " + pinnedPem.lines().findFirst().orElse("") + " ...");

        // Subsequent connection: verify the key matches
        String currentPem = encoder.encodeToString(serverKey.getPublic());
        boolean pinMatch = pinnedPem.equals(currentPem);
        IO.println("   Second connection: key matches pin? " + (pinMatch ? "✅ TRUSTED" : "❌ MISMATCH"));

        // Simulate a MITM attack with a different key
        KeyPair attackerKey = kpg.generateKeyPair();
        String attackerPem = encoder.encodeToString(attackerKey.getPublic());
        boolean attackDetected = !pinnedPem.equals(attackerPem);
        IO.println("   MITM attempt: key matches pin? " + (!attackDetected ? "✅ TRUSTED" : "🚨 REJECTED"));

        IO.println("   ✅ PEM-based pinning detects key substitution attacks");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 3 — Key Pair Backup and Restore
    // ═══════════════════════════════════════════════════════════════════════════
    //  Scenario: Serialize a key pair to PEM for secure storage (Vault,
    //  encrypted backup, HSM staging), then restore it later.
    //
    //  Real users: HashiCorp Vault, AWS KMS staging, disaster recovery,
    //              key ceremony workflows.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example3_KeyPairBackupRestore() throws Exception {
        IO.println("3️⃣  Key Pair Backup and Restore (Vault/HSM Staging)");
        IO.println("   Use case: HashiCorp Vault, AWS KMS, DR, key ceremony");
        IO.println("   ────────────────────────────────────────");

        // Generate original key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
        KeyPair original = kpg.generateKeyPair();

        // Backup: encode both keys to PEM
        PEMEncoder encoder = PEMEncoder.of();
        String publicPem = encoder.encodeToString(original.getPublic());
        String privatePem = encoder.encodeToString(original.getPrivate());

        IO.println("   Original key fingerprint: "
            + fingerprint(original.getPublic().getEncoded()));
        IO.println("   Backed up: public (" + publicPem.length() + " chars) + "
            + "private (" + privatePem.length() + " chars)");

        // Simulate storage and retrieval (in real life: Vault, encrypted S3, etc.)
        IO.println("   [Stored to secure vault]");

        // Restore: decode PEM back to keys
        PEMDecoder decoder = PEMDecoder.of();
        PublicKey restoredPub = decoder.decode(publicPem, PublicKey.class);
        PrivateKey restoredPriv = decoder.decode(privatePem, PrivateKey.class);

        IO.println("   Restored key fingerprint: "
            + fingerprint(restoredPub.getEncoded()));

        // Prove the restored keys work
        Signature sig = Signature.getInstance("Ed25519");
        sig.initSign(restoredPriv);
        sig.update("disaster recovery test".getBytes());
        byte[] signature = sig.sign();

        sig.initVerify(restoredPub);
        sig.update("disaster recovery test".getBytes());
        boolean verified = sig.verify(signature);

        IO.println("   Restored keys functional: " + (verified ? "✅" : "❌"));
        IO.println("   ✅ Full key pair round-trip through PEM backup");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 4 — PEM Type Routing (PKI Gateway)
    // ═══════════════════════════════════════════════════════════════════════════
    //  Scenario: A PKI gateway receives PEM-encoded objects and routes them
    //  to the appropriate handler based on the PEM type label.
    //
    //  Real users: Certificate management platforms (Venafi, DigiCert),
    //              PKI gateways, ACME clients, enterprise CA.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example4_PemTypeRouting() throws Exception {
        IO.println("4️⃣  PEM Type Routing (PKI Gateway)");
        IO.println("   Use case: Certificate management, PKI gateways, ACME clients");
        IO.println("   ────────────────────────────────────────");

        PEMEncoder encoder = PEMEncoder.of();
        PEMDecoder decoder = PEMDecoder.of();

        // Generate different types of crypto objects
        KeyPairGenerator rsaKpg = KeyPairGenerator.getInstance("RSA");
        rsaKpg.initialize(2048);
        KeyPair rsaKeys = rsaKpg.generateKeyPair();

        // Encode them all to PEM
        String[] pemObjects = {
            encoder.encodeToString(rsaKeys.getPublic()),
            encoder.encodeToString(rsaKeys.getPrivate())
        };

        IO.println("   Routing " + pemObjects.length + " PEM objects:");

        for (String pem : pemObjects) {
            DEREncodable decoded = decoder.decode(pem);

            String route = switch (decoded) {
                case PublicKey pk   -> "→ Public Key Store  [" + pk.getAlgorithm() + "]";
                case PrivateKey sk  -> "→ Secure Key Vault  [" + sk.getAlgorithm() + ", " + sk.getFormat() + "]";
                case PEM rec        -> "→ Raw PEM Archive   [type=" + rec.type() + "]";
                default             -> "→ Unknown Handler";
            };

            String type = pem.lines().findFirst().orElse("unknown");
            IO.println("   " + type);
            IO.println("     " + route);
        }

        IO.println("   ✅ Type-safe PEM routing without string parsing");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 5 — Code Signing with PEM Keys
    // ═══════════════════════════════════════════════════════════════════════════
    //  Scenario: A CI/CD pipeline reads signing keys from PEM-formatted
    //  secrets, signs build artifacts, and publishes PEM-encoded signatures.
    //
    //  Real users: GitHub Actions, GitLab CI, Sigstore/Cosign, Maven
    //              Central publishing, APK/IPA signing.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example5_CodeSigningWithPemKeys() throws Exception {
        IO.println("5️⃣  Code Signing with PEM Keys (CI/CD Pipeline)");
        IO.println("   Use case: GitHub Actions, Sigstore, Maven Central, APK signing");
        IO.println("   ────────────────────────────────────────");

        // Setup: generate signing key pair and export to PEM
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(256);
        KeyPair signingKeys = kpg.generateKeyPair();

        PEMEncoder encoder = PEMEncoder.of();
        String signingKeyPem = encoder.encodeToString(signingKeys.getPrivate());
        String verifyKeyPem = encoder.encodeToString(signingKeys.getPublic());

        // CI step 1: Read signing key from CI secret (PEM format)
        IO.println("   CI Step 1: Read signing key from secret store");
        PEMDecoder decoder = PEMDecoder.of();
        PrivateKey signingKey = decoder.decode(signingKeyPem, PrivateKey.class);
        IO.println("   Loaded: " + signingKey.getAlgorithm() + " signing key");

        // CI step 2: Sign the artifact
        byte[] artifact = "com.example:my-app:1.0.0:jar (SHA-256: abc123...)".getBytes();
        Signature sig = Signature.getInstance("SHA256withECDSA");
        sig.initSign(signingKey);
        sig.update(artifact);
        byte[] signature = sig.sign();

        IO.println("   CI Step 2: Signed artifact (" + signature.length + " byte signature)");

        // Verification step: anyone with the public PEM can verify
        IO.println("   Verification: Load public key from published PEM");
        PublicKey verifyKey = decoder.decode(verifyKeyPem, PublicKey.class);

        sig.initVerify(verifyKey);
        sig.update(artifact);
        boolean valid = sig.verify(signature);
        IO.println("   Artifact signature valid: " + (valid ? "✅" : "❌"));

        // Tamper detection
        byte[] tamperedArtifact = "com.example:my-app:1.0.0:jar (SHA-256: TAMPERED!)".getBytes();
        sig.initVerify(verifyKey);
        sig.update(tamperedArtifact);
        boolean tamperedValid = sig.verify(signature);
        IO.println("   Tampered artifact detected: " + (!tamperedValid ? "✅ REJECTED" : "❌ MISSED"));

        IO.println("   ✅ PEM-based CI/CD signing pipeline");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 6 — Multi-Algorithm Keyring
    // ═══════════════════════════════════════════════════════════════════════════
    //  Scenario: Manage a keyring containing keys of different algorithms
    //  (RSA, EC, Ed25519, ML-DSA), all stored in standard PEM format.
    //
    //  Real users: Key management systems, JWKS endpoints, SSH agent,
    //              crypto-agile applications preparing for PQC migration.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example6_MultiAlgorithmKeyring() throws Exception {
        IO.println("6️⃣  Multi-Algorithm Keyring (Crypto Agility)");
        IO.println("   Use case: Key management, JWKS, SSH agent, PQC migration");
        IO.println("   ────────────────────────────────────────");

        PEMEncoder encoder = PEMEncoder.of();
        PEMDecoder decoder = PEMDecoder.of();

        // Generate keys with different algorithms
        record KeyEntry(String name, String algorithm, String pemPublic, String pemPrivate) {}

        List<KeyEntry> keyring = new java.util.ArrayList<>();

        // RSA
        KeyPairGenerator rsaKpg = KeyPairGenerator.getInstance("RSA");
        rsaKpg.initialize(2048);
        KeyPair rsa = rsaKpg.generateKeyPair();
        keyring.add(new KeyEntry("signing-rsa", "RSA",
            encoder.encodeToString(rsa.getPublic()),
            encoder.encodeToString(rsa.getPrivate())));

        // EC
        KeyPairGenerator ecKpg = KeyPairGenerator.getInstance("EC");
        ecKpg.initialize(256);
        KeyPair ec = ecKpg.generateKeyPair();
        keyring.add(new KeyEntry("signing-ec", "EC",
            encoder.encodeToString(ec.getPublic()),
            encoder.encodeToString(ec.getPrivate())));

        // Ed25519
        KeyPairGenerator edKpg = KeyPairGenerator.getInstance("Ed25519");
        KeyPair ed = edKpg.generateKeyPair();
        keyring.add(new KeyEntry("signing-ed25519", "Ed25519",
            encoder.encodeToString(ed.getPublic()),
            encoder.encodeToString(ed.getPrivate())));

        // ML-DSA (post-quantum, JEP 497)
        KeyPairGenerator mlKpg = KeyPairGenerator.getInstance("ML-DSA-65");
        KeyPair ml = mlKpg.generateKeyPair();
        keyring.add(new KeyEntry("signing-pqc", "ML-DSA-65",
            encoder.encodeToString(ml.getPublic()),
            encoder.encodeToString(ml.getPrivate())));

        IO.println("   Keyring contents:");
        for (KeyEntry entry : keyring) {
            // Decode back to verify
            PublicKey pub = decoder.decode(entry.pemPublic(), PublicKey.class);
            IO.println("   🔑 " + entry.name()
                + " [" + entry.algorithm() + "]"
                + " pub=" + pub.getEncoded().length + "B"
                + " pemPub=" + entry.pemPublic().length() + " chars"
                + " pemPriv=" + entry.pemPrivate().length() + " chars");
        }

        IO.println("   Total keys: " + keyring.size() + " (across " + keyring.size() + " algorithms)");
        IO.println("   ✅ All keys stored in standard PEM — portable, inspectable, tool-compatible");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    private static String fingerprint(byte[] keyBytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(keyBytes);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                if (i > 0) sb.append(":");
                sb.append(String.format("%02x", hash[i]));
            }
            return "SHA256:" + sb + "...";
        } catch (Exception e) {
            return "error";
        }
    }
}



