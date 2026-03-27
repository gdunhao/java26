package org.example.standard;

import java.security.*;
import java.security.spec.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  ML-DSA (Quantum-Resistant Digital Signatures) — Real-World Use Cases      ║
 * ║  Practical examples where JEP 497 gives you a real advantage                ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * These examples show REAL scenarios where quantum-resistant digital
 * signatures (ML-DSA / FIPS 204) protect real-world systems from future
 * quantum attacks.
 *
 * REFERENCES
 * ──────────
 *   • JEP 497 — Quantum-Resistant ML-DSA:
 *       https://openjdk.org/jeps/497
 *   • NIST FIPS 204 — Module-Lattice-Based Digital Signature Standard:
 *       https://csrc.nist.gov/pubs/fips/204/final
 *
 * PATTERNS DEMONSTRATED
 * ─────────────────────
 *   1. Software update signing   — Sign release artifacts, verify before install (app stores, OTA)
 *   2. Document signing          — Sign contracts/invoices with non-repudiation (legal, compliance)
 *   3. JWT-style token signing   — Sign auth tokens with quantum-safe algo (identity providers)
 *   4. Audit log integrity       — Sign log entries to detect tampering (SOX, HIPAA)
 *   5. Code commit signing       — Sign commits for provenance (supply chain security)
 *   6. Certificate chain         — Self-signed cert hierarchy using ML-DSA (PKI migration)
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.standard.QuantumDsaRealWorldExamples
 */
public class QuantumDsaRealWorldExamples {

    public static void main(String[] args) throws Exception {
        IO.println("╔═══════════════════════════════════════════════════════╗");
        IO.println("║  ML-DSA — Real-World Use Cases                       ║");
        IO.println("╚═══════════════════════════════════════════════════════╝");
        IO.println();

        example1_SoftwareUpdateSigning();
        example2_DocumentSigning();
        example3_TokenSigning();
        example4_AuditLogIntegrity();
        example5_CodeCommitSigning();
        example6_CertificateChain();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 1 — Software Update Signing
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Your CI/CD pipeline signs release binaries before pushing
    //  them to a distribution server. Client devices verify the signature
    //  before installing the update. Using ML-DSA ensures that even a
    //  future quantum computer can't forge updates.
    //
    //  Real users: Google Play, Apple App Store, Tesla OTA updates,
    //              Linux package managers (apt, rpm), firmware updates.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example1_SoftwareUpdateSigning() throws Exception {
        IO.println("1️⃣  Software Update Signing (Quantum-Safe Release Verification)");
        IO.println("   Use case: App stores, OTA firmware, Linux package managers");
        IO.println("   ────────────────────────────────────────");

        // Build server generates keypair (stored in HSM/vault)
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-DSA-65");
        KeyPair buildServerKeys = kpg.generateKeyPair();

        // Simulate a release artifact
        String artifactMetadata = "myapp-v2.3.1.jar|sha256:a1b2c3d4|size:15728640|date:2026-03-15";
        byte[] artifactHash = artifactMetadata.getBytes();

        // CI/CD signs the artifact
        Signature signer = Signature.getInstance("ML-DSA-65");
        signer.initSign(buildServerKeys.getPrivate());
        signer.update(artifactHash);
        byte[] releaseSignature = signer.sign();

        IO.println("   Artifact: myapp-v2.3.1.jar (15 MB)");
        IO.println("   Signature size: " + releaseSignature.length + " bytes");

        // Client device verifies before installing
        Signature verifier = Signature.getInstance("ML-DSA-65");
        verifier.initVerify(buildServerKeys.getPublic());
        verifier.update(artifactHash);
        boolean legitimate = verifier.verify(releaseSignature);
        IO.println("   Client verification: " + (legitimate ? "✅ TRUSTED — installing" : "❌ REJECTED"));

        // Attacker tries to push a malicious update
        byte[] maliciousArtifact = "malware-v1.0.jar|sha256:deadbeef|size:666".getBytes();
        verifier.initVerify(buildServerKeys.getPublic());
        verifier.update(maliciousArtifact);
        boolean forged = verifier.verify(releaseSignature);
        IO.println("   Malicious update:   " + (forged ? "❌ ACCEPTED (bad!)" : "✅ REJECTED — attack blocked"));
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 2 — Document Signing (Non-Repudiation)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: A legal document management system where contracts and
    //  invoices must be digitally signed for non-repudiation. Using
    //  quantum-resistant signatures ensures the signatures remain valid
    //  even decades from now when quantum computers may exist.
    //
    //  Real users: DocuSign, Adobe Sign, e-invoicing systems,
    //              government records, notarization platforms.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example2_DocumentSigning() throws Exception {
        IO.println("2️⃣  Document Signing (Legal Non-Repudiation)");
        IO.println("   Use case: DocuSign, e-invoicing, government records, contracts");
        IO.println("   ────────────────────────────────────────");

        // Each signatory has their own keypair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-DSA-87");  // highest security
        KeyPair aliceKeys = kpg.generateKeyPair();
        KeyPair bobKeys   = kpg.generateKeyPair();

        String contract = "CONTRACT: Alice agrees to sell Widget-X to Bob for $10,000. Date: 2026-03-15.";

        // Alice signs
        Signature signer = Signature.getInstance("ML-DSA-87");
        signer.initSign(aliceKeys.getPrivate());
        signer.update(contract.getBytes());
        byte[] aliceSig = signer.sign();

        // Bob signs
        signer.initSign(bobKeys.getPrivate());
        signer.update(contract.getBytes());
        byte[] bobSig = signer.sign();

        IO.println("   Contract: \"" + contract.substring(0, 50) + "...\"");
        IO.println("   Alice signature: " + aliceSig.length + " bytes (ML-DSA-87)");
        IO.println("   Bob signature:   " + bobSig.length + " bytes (ML-DSA-87)");

        // Verify both signatures
        Signature verifier = Signature.getInstance("ML-DSA-87");
        verifier.initVerify(aliceKeys.getPublic());
        verifier.update(contract.getBytes());
        IO.println("   Verify Alice: " + (verifier.verify(aliceSig) ? "✅ Valid" : "❌ Invalid"));

        verifier.initVerify(bobKeys.getPublic());
        verifier.update(contract.getBytes());
        IO.println("   Verify Bob:   " + (verifier.verify(bobSig) ? "✅ Valid" : "❌ Invalid"));

        // Try to tamper
        String tampered = contract.replace("$10,000", "$1");
        verifier.initVerify(aliceKeys.getPublic());
        verifier.update(tampered.getBytes());
        IO.println("   Tampered contract verification: "
            + (verifier.verify(aliceSig) ? "❌ Accepted (bad!)" : "✅ Rejected — integrity preserved"));
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 3 — JWT-Style Token Signing
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Your identity provider issues authentication tokens.
    //  Currently signed with RSA or ECDSA, but you want to future-proof
    //  by adopting ML-DSA. The workflow is identical — just swap the algo.
    //
    //  Real users: Keycloak, Auth0, Okta, Spring Security OAuth2,
    //              any custom JWT issuer.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example3_TokenSigning() throws Exception {
        IO.println("3️⃣  JWT-Style Token Signing (Quantum-Safe Auth Tokens)");
        IO.println("   Use case: Keycloak, Auth0, Spring Security, custom IdP");
        IO.println("   ────────────────────────────────────────");

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-DSA-44");
        KeyPair idpKeys = kpg.generateKeyPair();

        // Simulate JWT payload
        String header = "{\"alg\":\"ML-DSA-44\",\"typ\":\"JWT\"}";
        String payload = "{\"sub\":\"user-42\",\"name\":\"Alice\",\"role\":\"admin\","
            + "\"iat\":1742860800,\"exp\":1742864400}";

        // Base64 encode (simulating JWT structure)
        String headerB64 = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(header.getBytes());
        String payloadB64 = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.getBytes());
        String signingInput = headerB64 + "." + payloadB64;

        // Sign
        Signature signer = Signature.getInstance("ML-DSA-44");
        signer.initSign(idpKeys.getPrivate());
        signer.update(signingInput.getBytes());
        byte[] tokenSig = signer.sign();

        String signatureB64 = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(tokenSig);
        String fullToken = signingInput + "." + signatureB64;

        IO.println("   Token payload: " + payload);
        IO.println("   Token length: " + fullToken.length() + " chars");
        IO.println("   (Note: ML-DSA tokens are larger than ECDSA — trade-off for quantum safety)");

        // Resource server verifies
        Signature verifier = Signature.getInstance("ML-DSA-44");
        verifier.initVerify(idpKeys.getPublic());
        verifier.update(signingInput.getBytes());
        IO.println("   Resource server verification: "
            + (verifier.verify(tokenSig) ? "✅ Token valid — access granted" : "❌ Invalid token"));
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 4 — Audit Log Integrity
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: For regulatory compliance (SOX, HIPAA, PCI-DSS), you
    //  need to ensure audit log entries haven't been tampered with. Each
    //  batch of log entries is signed, creating an integrity chain.
    //
    //  Real users: Splunk, Elastic SIEM, AWS CloudTrail, custom audit
    //              systems, blockchain-alternative integrity proofs.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example4_AuditLogIntegrity() throws Exception {
        IO.println("4️⃣  Audit Log Integrity (Tamper-Proof Logging)");
        IO.println("   Use case: SIEM systems, CloudTrail, SOX/HIPAA compliance");
        IO.println("   ────────────────────────────────────────");

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-DSA-44");
        KeyPair auditKeys = kpg.generateKeyPair();

        String[] logEntries = {
            "2026-03-15T10:00:00Z | user:alice | action:LOGIN | ip:10.0.1.42",
            "2026-03-15T10:01:23Z | user:alice | action:QUERY_DB | table:customers",
            "2026-03-15T10:02:45Z | user:alice | action:EXPORT_CSV | rows:1500",
            "2026-03-15T10:05:00Z | user:alice | action:LOGOUT"
        };

        // Sign the entire batch
        Signature signer = Signature.getInstance("ML-DSA-44");
        signer.initSign(auditKeys.getPrivate());
        for (String entry : logEntries) {
            signer.update(entry.getBytes());
        }
        byte[] batchSignature = signer.sign();

        IO.println("   Log entries: " + logEntries.length);
        IO.println("   Batch signature: " + batchSignature.length + " bytes");

        // Verify integrity
        Signature verifier = Signature.getInstance("ML-DSA-44");
        verifier.initVerify(auditKeys.getPublic());
        for (String entry : logEntries) {
            verifier.update(entry.getBytes());
        }
        IO.println("   Integrity check: "
            + (verifier.verify(batchSignature) ? "✅ Logs untampered" : "❌ TAMPERING DETECTED"));

        // Simulate tampering (delete an entry)
        verifier.initVerify(auditKeys.getPublic());
        for (int i = 0; i < logEntries.length - 1; i++) { // "accidentally" skip last entry
            verifier.update(logEntries[i].getBytes());
        }
        IO.println("   After removing entry: "
            + (verifier.verify(batchSignature)
                ? "❌ Tampering not detected (bad!)"
                : "✅ TAMPERING DETECTED — entry was removed"));
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 5 — Code Commit Signing
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: In a supply-chain security model, developers sign their
    //  Git commits. With quantum computers threatening RSA/ECDSA signatures,
    //  migrating to ML-DSA ensures commit provenance remains verifiable.
    //
    //  Real users: GitHub verified commits, Sigstore/Cosign, SLSA
    //              provenance, enterprise Git policies.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example5_CodeCommitSigning() throws Exception {
        IO.println("5️⃣  Code Commit Signing (Supply Chain Security)");
        IO.println("   Use case: GitHub verified commits, Sigstore, SLSA provenance");
        IO.println("   ────────────────────────────────────────");

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-DSA-65");
        KeyPair devKeys = kpg.generateKeyPair();

        // Simulate a git commit object
        String commitData = "tree a1b2c3d4e5f6\n"
            + "parent 9876543210ab\n"
            + "author Alice <alice@example.com> 1742860800 +0000\n"
            + "committer Alice <alice@example.com> 1742860800 +0000\n"
            + "\n"
            + "feat: add quantum-resistant authentication module\n"
            + "\n"
            + "Implements ML-DSA-65 for all auth token signing.\n"
            + "Closes #1234";

        Signature signer = Signature.getInstance("ML-DSA-65");
        signer.initSign(devKeys.getPrivate());
        signer.update(commitData.getBytes());
        byte[] commitSig = signer.sign();

        IO.println("   Commit message: \"feat: add quantum-resistant authentication module\"");
        IO.println("   Author: Alice <alice@example.com>");
        IO.println("   Signature: " + commitSig.length + " bytes (ML-DSA-65)");

        // CI/CD pipeline verifies commit signature
        Signature verifier = Signature.getInstance("ML-DSA-65");
        verifier.initVerify(devKeys.getPublic());
        verifier.update(commitData.getBytes());
        IO.println("   CI/CD verification: "
            + (verifier.verify(commitSig)
                ? "✅ Commit signed by trusted developer"
                : "❌ Untrusted commit — blocking pipeline"));
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 6 — Certificate Chain Simulation
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Simulating a PKI certificate hierarchy where a root CA
    //  signs an intermediate CA's public key, and the intermediate CA
    //  signs an end-entity certificate. All using ML-DSA.
    //
    //  Real users: TLS certificate authorities, enterprise PKI,
    //              NIST PQC migration pilots.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example6_CertificateChain() throws Exception {
        IO.println("6️⃣  Certificate Chain Simulation (Quantum-Safe PKI)");
        IO.println("   Use case: TLS CAs, enterprise PKI, NIST PQC migration");
        IO.println("   ────────────────────────────────────────");

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-DSA-87");

        // Root CA
        KeyPair rootCaKeys = kpg.generateKeyPair();
        String rootCaName = "CN=QuantumRoot CA, O=Example Corp";

        // Intermediate CA
        KeyPair intermediateCaKeys = kpg.generateKeyPair();
        String intermediateCaData = "CN=Intermediate CA, O=Example Corp|pubkey-hash:"
            + bytesToHex(intermediateCaKeys.getPublic().getEncoded(), 16);

        // Root signs intermediate's certificate data
        Signature signer = Signature.getInstance("ML-DSA-87");
        signer.initSign(rootCaKeys.getPrivate());
        signer.update(intermediateCaData.getBytes());
        byte[] intermediateCert = signer.sign();

        // End entity (web server)
        KeyPair serverKeys = kpg.generateKeyPair();
        String serverCertData = "CN=api.example.com, O=Example Corp|pubkey-hash:"
            + bytesToHex(serverKeys.getPublic().getEncoded(), 16);

        // Intermediate CA signs server certificate
        signer.initSign(intermediateCaKeys.getPrivate());
        signer.update(serverCertData.getBytes());
        byte[] serverCert = signer.sign();

        IO.println("   Root CA: " + rootCaName);
        IO.println("   Intermediate CA cert signed by Root: " + intermediateCert.length + " bytes");
        IO.println("   Server cert signed by Intermediate: " + serverCert.length + " bytes");

        // Client verifies the chain
        Signature verifier = Signature.getInstance("ML-DSA-87");

        // Verify intermediate cert against root
        verifier.initVerify(rootCaKeys.getPublic());
        verifier.update(intermediateCaData.getBytes());
        boolean intermediateValid = verifier.verify(intermediateCert);

        // Verify server cert against intermediate
        verifier.initVerify(intermediateCaKeys.getPublic());
        verifier.update(serverCertData.getBytes());
        boolean serverValid = verifier.verify(serverCert);

        IO.println("   Chain verification:");
        IO.println("     Root → Intermediate: " + (intermediateValid ? "✅ Valid" : "❌ Invalid"));
        IO.println("     Intermediate → Server: " + (serverValid ? "✅ Valid" : "❌ Invalid"));
        IO.println("     Full chain: " + (intermediateValid && serverValid
            ? "✅ Trusted" : "❌ Broken chain"));
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

