# ☕ Java 26 Feature Demos

A comprehensive collection of runnable demos showcasing **every major feature** in **JDK 26** (released March 2026). Each demo is a self-contained Java class with extensive Javadoc documentation explaining the feature, its motivation, and usage patterns.

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
| 1 | [JEP 512](https://openjdk.org/jeps/512) | **Compact Source Files & Instance Main** | [`o.e.standard.SimpleSourceFileDemo`](#1-jep-512--compact-source-files--instance-main-methods) | Write `void main()` — no `public`, `static`, or `String[]` needed |
| 2 | [JEP 511](https://openjdk.org/jeps/511) | **Module Import Declarations** | [`o.e.standard.ModuleImportDemo`](#2-jep-511--module-import-declarations) | `import module java.base;` replaces dozens of imports |
| 3 | [JEP 513](https://openjdk.org/jeps/513) | **Flexible Constructor Bodies** | [`o.e.standard.FlexibleConstructorDemo`](#3-jep-513--flexible-constructor-bodies) | Statements before `super()` and `this()` |
| 4 | [JEP 485](https://openjdk.org/jeps/485) | **Stream Gatherers** | [`o.e.standard.StreamGatherersDemo`](#4-jep-485--stream-gatherers) | Custom intermediate stream operations |
| 5 | [JEP 484](https://openjdk.org/jeps/484) | **Class-File API** | [`o.e.standard.ClassFileApiDemo`](#5-jep-484--class-file-api) | Read, write, and transform `.class` files natively |
| 6 | [JEP 506](https://openjdk.org/jeps/506) | **Scoped Values** | [`o.e.standard.ScopedValuesDemo`](#6-jep-506--scoped-values) | Thread-safe, immutable alternative to `ThreadLocal` |
| 7 | [JEP 496](https://openjdk.org/jeps/496) | **Quantum-Resistant ML-KEM** | [`o.e.standard.QuantumKemDemo`](#7-jep-496--quantum-resistant-ml-kem) | Post-quantum key encapsulation (FIPS 203) |
| 8 | [JEP 497](https://openjdk.org/jeps/497) | **Quantum-Resistant ML-DSA** | [`o.e.standard.QuantumDsaDemo`](#8-jep-497--quantum-resistant-ml-dsa) | Post-quantum digital signatures (FIPS 204) |
| 9 | [JEP 510](https://openjdk.org/jeps/510) | **Key Derivation Function API** | [`o.e.preview.KeyDerivationDemo`](#9-jep-510--key-derivation-function-api) | HKDF key derivation via `javax.crypto.KDF` |

### 🔬 Preview Features (require `--enable-preview`)

| # | JEP | Feature | Demo Class | Description |
|---|-----|---------|------------|-------------|
| 10 | [JEP 530](https://openjdk.org/jeps/530) | **Primitive Types in Patterns** | [`o.e.preview.PrimitivePatternsDemo`](#10-jep-530--primitive-types-in-patterns) | Pattern match on `int`, `double`, `boolean` in switch |
| 11 | [JEP 525](https://openjdk.org/jeps/525) | **Structured Concurrency** | [`o.e.standard.StructuredConcurrencyDemo`](#11-jep-525--structured-concurrency) | Fork/join concurrent tasks as a unit of work |

### 🧪 Incubator Features (require `--add-modules`)

| # | JEP | Feature | Demo Class | Description |
|---|-----|---------|------------|-------------|
| 12 | [JEP 529](https://openjdk.org/jeps/529) | **Vector API** | [`o.e.incubator.VectorApiDemo`](#12-jep-529--vector-api) | SIMD computations via `jdk.incubator.vector` |

### ⚙️ VM/Runtime Improvements

| # | JEP | Feature | Demo Class | Description |
|---|-----|---------|------------|-------------|
| 13 | [JEP 491](https://openjdk.org/jeps/491) | **Virtual Threads without Pinning** | [`o.e.vm.VirtualThreadSyncDemo`](#13-jep-491--virtual-threads-without-pinning) | `synchronized` no longer pins virtual threads |

---

## 🔍 Detailed Feature Descriptions

### 1. JEP 512 — Compact Source Files & Instance Main Methods

```bash
java --enable-preview --add-modules jdk.incubator.vector \
  -cp target/classes org.example.standard.SimpleSourceFileDemo
```

**What changed:** The traditional `public static void main(String[] args)` is no longer the only entry point. You can now write:

```java
void main() {
    IO.println("Hello, World!");
}
```

The JVM launch protocol accepts instance `main()` methods with relaxed visibility and no arguments. The new `java.io.IO` class provides `println`, `print`, and `readln` for simple console interaction.

**Best for:** Education, scripting, prototyping.

---

### 2. JEP 511 — Module Import Declarations

```bash
java --enable-preview --add-modules jdk.incubator.vector \
  -cp target/classes org.example.standard.ModuleImportDemo
```

**What changed:** A single `import module java.base;` imports all public types from all packages exported by `java.base` — covering `java.util`, `java.io`, `java.time`, `java.math`, `java.nio`, `java.net`, `java.util.concurrent`, and more.

```java
import module java.base;  // Replaces 20+ import statements!
```

**Best for:** Reducing import boilerplate, scripting, implicit classes.

---

### 3. JEP 513 — Flexible Constructor Bodies

```bash
java --enable-preview --add-modules jdk.incubator.vector \
  -cp target/classes org.example.standard.FlexibleConstructorDemo
```

**What changed:** You can now write statements *before* `super()` or `this()` in constructors. Previously, `super()` had to be the very first statement.

```java
// Before JDK 26 — workaround needed
MyClass(int val) {
    super(validate(val));  // Must be first!
}

// With JDK 26 — natural validation
MyClass(int val) {
    if (val < 0) throw new IllegalArgumentException();
    super(val);  // Can appear after validation!
}
```

**Best for:** Argument validation, logging, transformation before delegation.

---

### 4. JEP 485 — Stream Gatherers

```bash
java --enable-preview --add-modules jdk.incubator.vector \
  -cp target/classes org.example.standard.StreamGatherersDemo
```

**What changed:** The new `Stream.gather(Gatherer)` method allows custom intermediate operations. Built-in gatherers include:

| Gatherer | Purpose | Example |
|----------|---------|---------|
| `windowFixed(n)` | Non-overlapping chunks | `[1,2,3,4,5]` → `[[1,2,3],[4,5]]` |
| `windowSliding(n)` | Overlapping windows | `[1,2,3,4]` → `[[1,2,3],[2,3,4]]` |
| `scan(init, op)` | Running accumulation | `[1,2,3]` → `[1,3,6]` |
| `fold(init, op)` | Reduce in pipeline | `[1,2,3]` → `[6]` |
| `mapConcurrent(n, fn)` | Parallel mapping | N concurrent mappers |

**Best for:** Windowed analytics, running totals, custom stream transformations.

---

### 5. JEP 484 — Class-File API

```bash
java --enable-preview --add-modules jdk.incubator.vector \
  -cp target/classes org.example.standard.ClassFileApiDemo
```

**What changed:** Java now has a built-in API for reading, writing, and transforming `.class` files in `java.lang.classfile`. No more need for ASM, BCEL, or Byte Buddy.

The demo shows: generating a class from scratch, parsing and inspecting it, and transforming it by adding a new method.

**Best for:** Bytecode tooling, instrumentation agents, annotation processors, code generation.

---

### 6. JEP 506 — Scoped Values

```bash
java --enable-preview --add-modules jdk.incubator.vector \
  -cp target/classes org.example.standard.ScopedValuesDemo
```

**What changed:** `ScopedValue` is the modern replacement for `ThreadLocal`:

| Feature | ThreadLocal | ScopedValue |
|---------|------------|-------------|
| Mutability | Mutable (set anytime) | Immutable per scope |
| Lifetime | Unbounded (leak risk) | Bounded to scope |
| Virtual threads | Expensive (copy per VT) | Cheap (sharing) |
| Inheritance | Copies values | Shares bindings |

```java
static final ScopedValue<String> USER = ScopedValue.newInstance();

ScopedValue.where(USER, "alice").run(() -> {
    // USER.get() returns "alice" anywhere in this scope
});
```

**Best for:** Request context, user identity, transaction IDs, replacing ThreadLocal.

---

### 7. JEP 496 — Quantum-Resistant ML-KEM

```bash
java --enable-preview --add-modules jdk.incubator.vector \
  -cp target/classes org.example.standard.QuantumKemDemo
```

**What changed:** Java now includes ML-KEM (FIPS 203), a quantum-resistant Key Encapsulation Mechanism based on lattice cryptography. Supports ML-KEM-512, ML-KEM-768, and ML-KEM-1024.

The demo shows the complete workflow: key generation → encapsulation → decapsulation → shared secret verification.

**Best for:** Post-quantum secure key exchange, future-proofing TLS, hybrid crypto systems.

---

### 8. JEP 497 — Quantum-Resistant ML-DSA

```bash
java --enable-preview --add-modules jdk.incubator.vector \
  -cp target/classes org.example.standard.QuantumDsaDemo
```

**What changed:** Java now includes ML-DSA (FIPS 204), a quantum-resistant digital signature algorithm. Supports ML-DSA-44, ML-DSA-65, and ML-DSA-87. The demo includes a tamper detection scenario proving signature integrity.

**Best for:** Post-quantum code signing, document signing, authentication.

---

### 9. JEP 510 — Key Derivation Function API

```bash
java --enable-preview --add-modules jdk.incubator.vector \
  -cp target/classes org.example.preview.KeyDerivationDemo
```

**What changed:** The new `javax.crypto.KDF` class provides HKDF (RFC 5869) key derivation:

```java
KDF hkdf = KDF.getInstance("HKDF-SHA256");
SecretKey key = hkdf.deriveKey("AES", HKDFParameterSpec
    .ofExtract().addIKM(ikm).addSalt(salt)
    .thenExpand(info, 32));
```

The demo shows: extract-then-expand, expand-only, and deriving multiple keys from one shared secret.

**Status:** Final (since JDK 25).

---

### 10. JEP 530 — Primitive Types in Patterns

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

### 11. JEP 525 — Structured Concurrency

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

### 12. JEP 529 — Vector API

```bash
java --enable-preview --add-modules jdk.incubator.vector \
  -cp target/classes org.example.incubator.VectorApiDemo
```

**What changed:** The Vector API enables SIMD (Single Instruction, Multiple Data) parallel computation on arrays. Process 4–16 elements per CPU instruction instead of one.

The demo covers: element-wise operations, reductions, conditional/masked operations, dot products, and a scalar-vs-SIMD comparison.

**Status:** Incubator (11th round) — requires `--add-modules jdk.incubator.vector`.

---

### 13. JEP 491 — Virtual Threads without Pinning

```bash
java --enable-preview --add-modules jdk.incubator.vector \
  -cp target/classes org.example.vm.VirtualThreadSyncDemo
```

**What changed:** Virtual threads no longer get "pinned" to carrier threads when they block inside `synchronized` blocks. This means `synchronized` now works correctly and efficiently with virtual threads — no need to replace it with `ReentrantLock`.

The demo spawns 1000 virtual threads that sleep inside `synchronized` blocks and compares performance with `ReentrantLock`.

---

## 📝 Non-Codeable JEPs in JDK 26

These JEPs are important but don't have direct demo code:

| JEP | Feature | Notes |
|-----|---------|-------|
| [JEP 500](https://openjdk.org/jeps/500) | **Prepare to Make Final Mean Final** | Warns at compile time about reflective access to final fields. |
| [JEP 504](https://openjdk.org/jeps/504) | **Remove the Applet API** | The long-deprecated `java.applet` package is removed. |
| [JEP 516](https://openjdk.org/jeps/516) | **Ahead-of-Time Object Caching with Any GC** | Extends AOT caching to work with all GC algorithms. |
| [JEP 517](https://openjdk.org/jeps/517) | **HTTP/3 for the HTTP Client API** | The `java.net.http.HttpClient` now supports HTTP/3 (QUIC). |
| [JEP 522](https://openjdk.org/jeps/522) | **G1 GC: Improve Throughput by Reducing Synchronization** | G1 garbage collector performance improvements. |
| [JEP 526](https://openjdk.org/jeps/526) | **Lazy Constants (Second Preview)** | Lazily-initialized constant values for performance. |
| [JEP 524](https://openjdk.org/jeps/524) | **PEM Encodings of Cryptographic Objects (Second Preview)** | Read/write PEM-encoded keys and certificates natively. |

---

## 📁 Project Structure

```
java26/
├── pom.xml                                    # Maven config (Java 26 + preview + incubator)
├── README.md                                  # This file
└── src/main/java/org/example/
    ├── Main.java                              # Original hello-world
    ├── standard/                              # Final/standard features
    │   ├── SimpleSourceFileDemo.java          # JEP 512
    │   ├── ModuleImportDemo.java              # JEP 511
    │   ├── FlexibleConstructorDemo.java       # JEP 513
    │   ├── StreamGatherersDemo.java           # JEP 485
    │   ├── ClassFileApiDemo.java              # JEP 484
    │   ├── StructuredConcurrencyDemo.java     # JEP 525
    │   ├── ScopedValuesDemo.java              # JEP 506
    │   ├── QuantumKemDemo.java                # JEP 496
    │   └── QuantumDsaDemo.java                # JEP 497
    ├── preview/                               # Preview features
    │   ├── PrimitivePatternsDemo.java         # JEP 530
    │   └── KeyDerivationDemo.java             # JEP 510
    ├── incubator/                             # Incubator features
    │   └── VectorApiDemo.java                 # JEP 529
    └── vm/                                    # VM/runtime improvements
        └── VirtualThreadSyncDemo.java         # JEP 491
```

---

## 📜 License

This project is for educational purposes. Feel free to use, modify, and share.

