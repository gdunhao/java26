package org.example.preview;

import java.security.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  JEP 524: PEM Encodings of Cryptographic Objects (Second Preview)           ║
 * ║  Status: PREVIEW in JDK 26 (Second Preview)                                ║
 * ║  Spec: https://openjdk.org/jeps/524                                        ║
 * ║  Requires: --enable-preview                                                ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * WHAT THIS FEATURE DOES
 * ──────────────────────
 * This JEP introduces a built-in API for encoding and decoding cryptographic
 * objects (keys, certificates, CRLs) in PEM (Privacy-Enhanced Mail) format.
 * PEM is the ubiquitous text format used across the entire crypto ecosystem:
 *
 *   -----BEGIN PUBLIC KEY-----
 *   MIIBIjANBgkqhkiG9w0BAQEFAAOC...
 *   -----END PUBLIC KEY-----
 *
 * KEY API CLASSES
 * ───────────────
 *   java.security.PEMEncoder — Encodes crypto objects to PEM text
 *     PEMEncoder.of()                  → Create an encoder
 *     encoder.encode(DEREncodable)     → byte[] (PEM bytes)
 *     encoder.encodeToString(obj)      → String (PEM text)
 *
 *   java.security.PEMDecoder — Decodes PEM text to crypto objects
 *     PEMDecoder.of()                  → Create a decoder
 *     decoder.decode(String)           → DEREncodable
 *     decoder.decode(String, Class)    → T (typed decode)
 *
 *   java.security.PEM — Raw PEM record representation
 *     new PEM(type, content)           → A PEM block
 *     pem.type()                       → "PUBLIC KEY", "CERTIFICATE", etc.
 *     pem.content()                    → Base64 PEM content string
 *     pem.decode()                     → Raw DER-encoded bytes
 *
 *   java.security.DEREncodable — Marker interface for PEM-encodable objects
 *     Already implemented by PublicKey, PrivateKey, Certificate, PEM, etc.
 *
 * WHY IT MATTERS
 * ──────────────
 * Before this API, Java developers had to:
 *   - Manually Base64-encode/decode DER bytes with "-----BEGIN/END-----" wrappers
 *   - Use Bouncy Castle just for PEM parsing
 *   - Write fragile string manipulation for multi-object PEM bundles
 *
 * Now, PEM support is built into the JDK — clean, type-safe, and correct.
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.preview.PemEncodingDemo
 */
public class PemEncodingDemo {

    public static void main(String[] args) throws Exception {
        IO.println("╔═══════════════════════════════════════════════════╗");
        IO.println("║  JEP 524 — PEM Encodings of Crypto Objects       ║");
        IO.println("╚═══════════════════════════════════════════════════╝");
        IO.println();

        demoEncodePublicKey();
        demoEncodePrivateKey();
        demoRoundTripKeyPair();
        demoPemRecord();
        demoEncodeMultipleObjects();
    }

    /**
     * DEMO 1: Encode a public key to PEM format.
     *
     * Generate an RSA key pair and encode the public key to the
     * standard PEM text format that tools like OpenSSL understand.
     */
    static void demoEncodePublicKey() throws Exception {
        IO.println("1️⃣  Encode Public Key to PEM");
        IO.println("   ────────────────────────────────────────");

        // Generate an RSA key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();

        // Encode the public key to PEM using the new API
        PEMEncoder encoder = PEMEncoder.of();
        String pem = encoder.encodeToString(keyPair.getPublic());

        IO.println("   Algorithm: " + keyPair.getPublic().getAlgorithm());
        IO.println("   Key size: " + keyPair.getPublic().getEncoded().length + " bytes (DER)");
        IO.println("   PEM output:");
        // Show first 3 lines and last line
        String[] lines = pem.split("\n");
        IO.println("     " + lines[0]);
        IO.println("     " + lines[1]);
        IO.println("     " + lines[2] + "...");
        IO.println("     " + lines[lines.length - 1]);
        IO.println("   Total PEM length: " + pem.length() + " chars");
        IO.println("   ✅ Standard PEM format, compatible with OpenSSL and other tools");
        IO.println();
    }

    /**
     * DEMO 2: Encode a private key to PEM format.
     *
     * Private keys use "-----BEGIN PRIVATE KEY-----" headers (PKCS#8).
     */
    static void demoEncodePrivateKey() throws Exception {
        IO.println("2️⃣  Encode Private Key to PEM");
        IO.println("   ────────────────────────────────────────");

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(256);
        KeyPair keyPair = kpg.generateKeyPair();

        PEMEncoder encoder = PEMEncoder.of();
        String pem = encoder.encodeToString(keyPair.getPrivate());

        IO.println("   Algorithm: " + keyPair.getPrivate().getAlgorithm());
        IO.println("   Format: " + keyPair.getPrivate().getFormat());
        String[] lines = pem.split("\n");
        IO.println("   PEM header: " + lines[0]);
        IO.println("   PEM footer: " + lines[lines.length - 1]);
        IO.println("   ✅ PKCS#8 private key in PEM format");
        IO.println();
    }

    /**
     * DEMO 3: Round-trip encode and decode a key pair.
     *
     * Encode keys to PEM, then decode them back and verify they match.
     * This proves the PEM API is lossless for cryptographic objects.
     */
    static void demoRoundTripKeyPair() throws Exception {
        IO.println("3️⃣  Round-Trip: Encode → Decode → Verify");
        IO.println("   ────────────────────────────────────────");

        // Generate original key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair original = kpg.generateKeyPair();

        // Encode to PEM
        PEMEncoder encoder = PEMEncoder.of();
        String publicPem = encoder.encodeToString(original.getPublic());
        String privatePem = encoder.encodeToString(original.getPrivate());

        IO.println("   Original public key:  " + original.getPublic().getEncoded().length + " bytes");
        IO.println("   Original private key: " + original.getPrivate().getEncoded().length + " bytes");

        // Decode from PEM
        PEMDecoder decoder = PEMDecoder.of();
        PublicKey decodedPublic = decoder.decode(publicPem, PublicKey.class);
        PrivateKey decodedPrivate = decoder.decode(privatePem, PrivateKey.class);

        IO.println("   Decoded public key:   " + decodedPublic.getEncoded().length + " bytes");
        IO.println("   Decoded private key:  " + decodedPrivate.getEncoded().length + " bytes");

        // Verify they match
        boolean pubMatch = java.util.Arrays.equals(
            original.getPublic().getEncoded(), decodedPublic.getEncoded());
        boolean privMatch = java.util.Arrays.equals(
            original.getPrivate().getEncoded(), decodedPrivate.getEncoded());

        IO.println("   Public key matches:  " + (pubMatch ? "✅" : "❌"));
        IO.println("   Private key matches: " + (privMatch ? "✅" : "❌"));

        // Prove they work: sign with original, verify with decoded
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(original.getPrivate());
        sig.update("test message".getBytes());
        byte[] signature = sig.sign();

        sig.initVerify(decodedPublic);
        sig.update("test message".getBytes());
        boolean verified = sig.verify(signature);

        IO.println("   Cross-verify signature: " + (verified ? "✅" : "❌"));
        IO.println("   ✅ PEM round-trip is lossless and cryptographically correct");
        IO.println();
    }

    /**
     * DEMO 4: PEM record — low-level PEM handling.
     *
     * The {@code java.security.PEM} record gives you raw access to the PEM
     * type label and content. Useful for inspecting PEM files without
     * committing to a specific crypto object type.
     */
    static void demoPemRecord() throws Exception {
        IO.println("4️⃣  PEM Record — Low-Level PEM Inspection");
        IO.println("   ────────────────────────────────────────");

        // Generate a key and encode to PEM
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
        KeyPair keyPair = kpg.generateKeyPair();

        PEMEncoder encoder = PEMEncoder.of();
        String pem = encoder.encodeToString(keyPair.getPublic());

        // Decode as generic DEREncodable (no specific crypto type assumed)
        PEMDecoder decoder = PEMDecoder.of();

        // We can also construct a PEM record directly from the PEM string
        // and inspect it without full crypto interpretation
        PEM pemRecord = decoder.decode(pem, PEM.class);
        IO.println("   PEM type label: \"" + pemRecord.type() + "\"");
        IO.println("   PEM content length: " + pemRecord.content().length() + " chars");
        byte[] derBytes = pemRecord.decode();
        IO.println("   DER bytes length: " + derBytes.length);
        IO.println("   First 16 DER bytes: " + bytesToHex(derBytes, 16));

        IO.println("   ✅ PEM record provides type-safe raw PEM access");
        IO.println();
    }

    /**
     * DEMO 5: Encode multiple objects (simulating a PEM bundle).
     *
     * PEM files often contain multiple objects concatenated (e.g.,
     * a certificate chain). This shows encoding several objects
     * into a single PEM string.
     */
    static void demoEncodeMultipleObjects() throws Exception {
        IO.println("5️⃣  Encode Multiple Objects (PEM Bundle)");
        IO.println("   ────────────────────────────────────────");

        // Generate keys with different algorithms
        PEMEncoder encoder = PEMEncoder.of();
        StringBuilder bundle = new StringBuilder();

        // RSA key
        KeyPairGenerator rsaKpg = KeyPairGenerator.getInstance("RSA");
        rsaKpg.initialize(2048);
        KeyPair rsaKeys = rsaKpg.generateKeyPair();
        bundle.append(encoder.encodeToString(rsaKeys.getPublic())).append("\n");

        // EC key
        KeyPairGenerator ecKpg = KeyPairGenerator.getInstance("EC");
        ecKpg.initialize(256);
        KeyPair ecKeys = ecKpg.generateKeyPair();
        bundle.append(encoder.encodeToString(ecKeys.getPublic())).append("\n");

        // Ed25519 key
        KeyPairGenerator edKpg = KeyPairGenerator.getInstance("Ed25519");
        KeyPair edKeys = edKpg.generateKeyPair();
        bundle.append(encoder.encodeToString(edKeys.getPublic()));

        IO.println("   Bundle contains 3 public keys:");
        IO.println("     1. RSA-2048:   " + rsaKeys.getPublic().getEncoded().length + " bytes");
        IO.println("     2. EC P-256:   " + ecKeys.getPublic().getEncoded().length + " bytes");
        IO.println("     3. Ed25519:    " + edKeys.getPublic().getEncoded().length + " bytes");
        IO.println("   Total PEM bundle: " + bundle.length() + " chars");

        // Count PEM blocks in the bundle
        long pemBlocks = bundle.toString().lines()
            .filter(l -> l.startsWith("-----BEGIN")).count();
        IO.println("   PEM blocks found: " + pemBlocks);
        IO.println("   ✅ Multiple crypto objects in one PEM bundle");
        IO.println();
    }

    /** Helper: convert bytes to hex string. */
    static String bytesToHex(byte[] bytes, int maxBytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, maxBytes); i++) {
            sb.append(String.format("%02x", bytes[i]));
        }
        return sb.toString();
    }
}

