# ☕ Java 26 Feature Demos

A collection of runnable demos showcasing **new features** in **JDK 26** (released March 2026). Each demo is a self-contained Java class with extensive Javadoc documentation explaining the feature, its motivation, and usage patterns.

## 📋 Prerequisites

| Requirement | Version |
|-------------|---------|
| **JDK**     | 26 (EA or GA) |
| **Maven**   | 3.9+ |

Verify your setup:
```bash
java -version   # Should show "26" or "26-ea"
mvn --version   # Should show 3.9+
```

## 🔨 Build

```bash
mvn compile
```

The project is pre-configured with `--enable-preview` and `--add-modules jdk.incubator.vector` in `pom.xml`.

## 🚀 Running Demos

Run any demo with:

```bash
java --enable-preview --add-modules jdk.incubator.vector \
  -cp target/classes <fully.qualified.ClassName>
```

Or using Maven:
```bash
mvn compile exec:exec -Dexec.mainClass=<fully.qualified.ClassName>
```

---

## 📚 Feature Index

### ✅ Standard Features (Final)

| # | JEP | Feature | Demo Class | Description |
|---|-----|---------|------------|-------------|
| 1 | [JEP 517](https://openjdk.org/jeps/517) | **HTTP/3 for the HTTP Client API** | [`o.e.standard.Http3Demo`](#1-jep-517--http3-for-the-http-client-api) | `HttpClient` now supports HTTP/3 (QUIC) |
| 6 | [JEP 500](https://openjdk.org/jeps/500) | **Prepare to Make Final Mean Final** | [`o.e.standard.FinalFieldWarningsDemo`](#6-jep-500--prepare-to-make-final-mean-final) | Runtime warnings when reflection mutates final fields |

### 🔬 Preview Features (require `--enable-preview`)

| # | JEP | Feature | Demo Class | Description |
|---|-----|---------|------------|-------------|
| 2 | [JEP 530](https://openjdk.org/jeps/530) | **Primitive Types in Patterns** | [`o.e.preview.PrimitivePatternsDemo`](#2-jep-530--primitive-types-in-patterns) | Pattern match on `int`, `double`, `boolean` in switch |
| 3 | [JEP 525](https://openjdk.org/jeps/525) | **Structured Concurrency** | [`o.e.standard.StructuredConcurrencyDemo`](#3-jep-525--structured-concurrency) | Fork/join concurrent tasks as a unit of work |
| 4 | [JEP 524](https://openjdk.org/jeps/524) | **PEM Encodings of Crypto Objects** | [`o.e.preview.PemEncodingDemo`](#4-jep-524--pem-encodings-of-cryptographic-objects) | Read/write PEM-encoded keys and certificates natively |

### 🧪 Incubator Features (require `--add-modules`)

| # | JEP | Feature | Demo Class | Description |
|---|-----|---------|------------|-------------|
| 5 | [JEP 529](https://openjdk.org/jeps/529) | **Vector API** | [`o.e.incubator.VectorApiDemo`](#5-jep-529--vector-api) | SIMD computations via `jdk.incubator.vector` |

---

## 🔍 Detailed Feature Descriptions

### 1. JEP 517 — HTTP/3 for the HTTP Client API

```bash
java --enable-preview --add-modules jdk.incubator.vector \
  -cp target/classes org.example.standard.Http3Demo
```

**What changed:** The built-in `java.net.http.HttpClient` now supports HTTP/3 (RFC 9114), which runs over QUIC (RFC 9000) instead of TCP. HTTP/3 eliminates head-of-line blocking, achieves faster connection setup (0-RTT/1-RTT), and supports connection migration across networks.

The client transparently upgrades to HTTP/3 when a server supports it:

```java
HttpClient client = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_3)
    .build();
HttpResponse<String> resp = client.send(request, BodyHandlers.ofString());
resp.version()  // → HTTP_3
```

The demo covers: version enumeration, basic HTTP/3 requests, version negotiation comparison, async requests, and concurrent multiplexed requests.

**Best for:** High-performance API clients, CDN integration, mobile backends, microservice communication.

---

### 2. JEP 530 — Primitive Types in Patterns

```bash
java --enable-preview --add-modules jdk.incubator.vector \
  -cp target/classes org.example.preview.PrimitivePatternsDemo
```

**What changed:** Pattern matching now supports primitive types in `switch`, `instanceof`, and record patterns:

```java
switch (value) {
    case int i when i > 0  -> "positive int"
    case int i when i == 0 -> "zero"
    case int i             -> "negative int"
}
```

The demo covers: primitive switch on Object, guarded primitive patterns, record destructuring with primitives, narrowing conversions, and exhaustive boolean switch.

**Status:** Fourth Preview — requires `--enable-preview`.

---

### 3. JEP 525 — Structured Concurrency

```bash
java --enable-preview --add-modules jdk.incubator.vector \
  -cp target/classes org.example.standard.StructuredConcurrencyDemo
```

**What changed:** `StructuredTaskScope` treats groups of concurrent tasks as a single unit:

```java
try (var scope = StructuredTaskScope.open()) {
    var user  = scope.fork(() -> fetchUser());
    var order = scope.fork(() -> fetchOrder());
    scope.join();         // Wait for all
    use(user.get(), order.get());
}   // All tasks guaranteed complete here
```

Key joiners: `awaitAll()`, `awaitAllSuccessfulOrThrow()`, `allSuccessfulOrThrow()`, `anySuccessfulOrThrow()`, `allUntil(Predicate)`.

**Status:** Sixth Preview — requires `--enable-preview`.

**Best for:** Concurrent service calls, fan-out/fan-in, replacing `ExecutorService`.

---

### 4. JEP 524 — PEM Encodings of Cryptographic Objects

```bash
java --enable-preview --add-modules jdk.incubator.vector \
  -cp target/classes org.example.preview.PemEncodingDemo
```

**What changed:** Java now has a built-in API for encoding and decoding cryptographic objects in PEM format — the ubiquitous text format used by OpenSSL, SSH, TLS, and every crypto tool. No more manual Base64 wrapping or Bouncy Castle dependency.

```java
PEMEncoder encoder = PEMEncoder.of();
String pem = encoder.encodeToString(publicKey);
// → "-----BEGIN PUBLIC KEY-----\nMIIBI..."

PEMDecoder decoder = PEMDecoder.of();
PublicKey key = decoder.decode(pem, PublicKey.class);
```

The demo covers: encoding public/private keys to PEM, round-trip verification, PEM record for raw access, and multi-object PEM bundles.

**Status:** Second Preview — requires `--enable-preview`.

**Best for:** Key export/import, certificate management, cross-platform crypto, CI/CD signing.

---

### 5. JEP 529 — Vector API

```bash
java --enable-preview --add-modules jdk.incubator.vector \
  -cp target/classes org.example.incubator.VectorApiDemo
```

**What changed:** The Vector API enables SIMD (Single Instruction, Multiple Data) parallel computation on arrays. Process 4–16 elements per CPU instruction instead of one.

The demo covers: element-wise operations, reductions, conditional/masked operations, dot products, and a scalar-vs-SIMD comparison.

**Status:** Incubator (11th round) — requires `--add-modules jdk.incubator.vector`.

---

### 6. JEP 500 — Prepare to Make Final Mean Final

```bash
java --enable-preview --add-modules jdk.incubator.vector \
  -cp target/classes org.example.standard.FinalFieldWarningsDemo
```

**What changed:** JDK 26 now issues runtime **warnings** when code uses reflection (`java.lang.reflect`) to mutate fields declared as `final`. This is the first step toward making `final` truly immutable — in a future JDK, these mutations will be blocked with an `IllegalAccessException`.

```java
class UserProfile {
    private final String name = "Alice";
}

// This now triggers a WARNING in JDK 26:
Field f = UserProfile.class.getDeclaredField("name");
f.setAccessible(true);
f.set(profile, "Bob");  // ⚠️ WARNING: final field was set via reflection
```

The demo covers: final instance field mutation warnings, final static field warnings, reading final fields (still safe), non-final fields (unaffected), and proper migration alternatives (builders, records, constructor injection).

**Best for:** Understanding migration paths for frameworks (Jackson, Spring, Gson) that mutate final fields via reflection.

---

## 📝 Non-Codeable JEPs in JDK 26

These JEPs are important but don't have runnable demo code in this project:

| JEP | Feature | Why Not Codeable |
|-----|---------|------------------|
| [JEP 504](https://openjdk.org/jeps/504) | **Remove the Applet API** | The `java.applet` package is removed entirely — there is no API left to call or demonstrate. |
| [JEP 516](https://openjdk.org/jeps/516) | **Ahead-of-Time Object Caching with Any GC** | Controlled via JVM flags (`-XX:AOTCache`), not Java code. No programmatic API. |
| [JEP 522](https://openjdk.org/jeps/522) | **G1 GC: Improve Throughput by Reducing Synchronization** | Internal JVM optimization — zero API surface, only observable via benchmarks. |
| [JEP 526](https://openjdk.org/jeps/526) | **Stable Values (Second Preview)** | `StableValue` API is codeable in principle (real preview API), but **not yet available** in JDK 26 EA builds. Demos will be added once the API ships in a later EA or GA build. |

---

## 📁 Project Structure

```
java26/
├── pom.xml                                    # Maven config (Java 26 + preview + incubator)
├── README.md                                  # This file
└── src/main/java/org/example/
    ├── Main.java                              # Original hello-world
    ├── standard/                              # Final/standard features
    │   ├── Http3Demo.java                     # JEP 517
    │   ├── Http3RealWorldExamples.java        # JEP 517 real-world
    │   ├── FinalFieldWarningsDemo.java        # JEP 500
    │   ├── FinalFieldWarningsRealWorldExamples.java  # JEP 500 real-world
    │   ├── StructuredConcurrencyDemo.java     # JEP 525
    │   └── StructuredConcurrencyRealWorldExamples.java  # JEP 525 real-world
    ├── preview/                               # Preview features
    │   ├── PrimitivePatternsDemo.java         # JEP 530
    │   ├── PrimitivePatternsRealWorldExamples.java  # JEP 530 real-world
    │   ├── PemEncodingDemo.java               # JEP 524
    │   └── PemEncodingRealWorldExamples.java  # JEP 524 real-world
    └── incubator/                             # Incubator features
        ├── VectorApiDemo.java                 # JEP 529
        └── VectorApiRealWorldExamples.java    # JEP 529 real-world
```

---

## 📜 License

This project is for educational purposes. Feel free to use, modify, and share.

