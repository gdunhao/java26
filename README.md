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

## 📝 Non-Codeable JEPs in JDK 26

These JEPs are important but don't have direct demo code:

| JEP | Feature | Notes |
|-----|---------|-------|
| [JEP 500](https://openjdk.org/jeps/500) | **Prepare to Make Final Mean Final** | Warns at compile time about reflective access to final fields. |
| [JEP 504](https://openjdk.org/jeps/504) | **Remove the Applet API** | The long-deprecated `java.applet` package is removed. |
| [JEP 516](https://openjdk.org/jeps/516) | **Ahead-of-Time Object Caching with Any GC** | Extends AOT caching to work with all GC algorithms. |
| [JEP 522](https://openjdk.org/jeps/522) | **G1 GC: Improve Throughput by Reducing Synchronization** | G1 garbage collector performance improvements. |
| [JEP 526](https://openjdk.org/jeps/526) | **Stable Values (Second Preview)** | Lazily-initialized constant values; `StableValue` API not yet available in JDK 26 EA. |

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
    │   └── StructuredConcurrencyDemo.java     # JEP 525
    ├── preview/                               # Preview features
    │   ├── PrimitivePatternsDemo.java         # JEP 530
    │   └── PemEncodingDemo.java               # JEP 524
    └── incubator/                             # Incubator features
        └── VectorApiDemo.java                 # JEP 529
```

---

## 📜 License

This project is for educational purposes. Feel free to use, modify, and share.

