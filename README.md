# ☕ Java 26 Feature Demos

A comprehensive collection of runnable demos showcasing **every major feature** in **JDK 26** (released September 2025). Each demo is a self-contained Java class with extensive Javadoc documentation explaining the feature, its motivation, and usage patterns.

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
| 1 | [JEP 495](https://openjdk.org/jeps/495) | **Simple Source Files & Instance Main** | [`o.e.standard.SimpleSourceFileDemo`](#1-jep-495--simple-source-files--instance-main-methods) | Write `void main()` — no `public`, `static`, or `String[]` needed |
| 2 | [JEP 494](https://openjdk.org/jeps/494) | **Module Import Declarations** | [`o.e.standard.ModuleImportDemo`](#2-jep-494--module-import-declarations) | `import module java.base;` replaces dozens of imports |
| 3 | [JEP 492](https://openjdk.org/jeps/492) | **Flexible Constructor Bodies** | [`o.e.standard.FlexibleConstructorDemo`](#3-jep-492--flexible-constructor-bodies) | Statements before `super()` and `this()` |
| 4 | [JEP 485](https://openjdk.org/jeps/485) | **Stream Gatherers** | [`o.e.standard.StreamGatherersDemo`](#4-jep-485--stream-gatherers) | Custom intermediate stream operations |
| 5 | [JEP 484](https://openjdk.org/jeps/484) | **Class-File API** | [`o.e.standard.ClassFileApiDemo`](#5-jep-484--class-file-api) | Read, write, and transform `.class` files natively |
| 6 | [JEP 499](https://openjdk.org/jeps/499) | **Structured Concurrency** | [`o.e.standard.StructuredConcurrencyDemo`](#6-jep-499--structured-concurrency) | Fork/join concurrent tasks as a unit of work |
| 7 | [JEP 487](https://openjdk.org/jeps/487) | **Scoped Values** | [`o.e.standard.ScopedValuesDemo`](#7-jep-487--scoped-values) | Thread-safe, immutable alternative to `ThreadLocal` |
| 8 | [JEP 496](https://openjdk.org/jeps/496) | **Quantum-Resistant ML-KEM** | [`o.e.standard.QuantumKemDemo`](#8-jep-496--quantum-resistant-ml-kem) | Post-quantum key encapsulation (FIPS 203) |
| 9 | [JEP 497](https://openjdk.org/jeps/497) | **Quantum-Resistant ML-DSA** | [`o.e.standard.QuantumDsaDemo`](#9-jep-497--quantum-resistant-ml-dsa) | Post-quantum digital signatures (FIPS 204) |

### 🔬 Preview Features (require `--enable-preview`)

| # | JEP | Feature | Demo Class | Description |
|---|-----|---------|------------|-------------|
| 10 | [JEP 488](https://openjdk.org/jeps/488) | **Primitive Types in Patterns** | [`o.e.preview.PrimitivePatternsDemo`](#10-jep-488--primitive-types-in-patterns) | Pattern match on `int`, `double`, `boolean` in switch |
| 11 | [JEP 478](https://openjdk.org/jeps/478) | **Key Derivation Function API** | [`o.e.preview.KeyDerivationDemo`](#11-jep-478--key-derivation-function-api) | HKDF key derivation via `javax.crypto.KDF` |

### 🧪 Incubator Features (require `--add-modules`)

| # | JEP | Feature | Demo Class | Description |
|---|-----|---------|------------|-------------|
| 12 | [JEP 489](https://openjdk.org/jeps/489) | **Vector API** | [`o.e.incubator.VectorApiDemo`](#12-jep-489--vector-api) | SIMD computations via `jdk.incubator.vector` |

### ⚙️ VM/Runtime Improvements

| # | JEP | Feature | Demo Class | Description |
|---|-----|---------|------------|-------------|
| 13 | [JEP 491](https://openjdk.org/jeps/491) | **Virtual Threads without Pinning** | [`o.e.vm.VirtualThreadSyncDemo`](#13-jep-491--virtual-threads-without-pinning) | `synchronized` no longer pins virtual threads |

---

## 🔍 Detailed Feature Descriptions

### 1. JEP 495 — Simple Source Files & Instance Main Methods

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

### 2. JEP 494 — Module Import Declarations

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

### 3. JEP 492 — Flexible Constructor Bodies

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

### 6. JEP 499 — Structured Concurrency

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

Key joiners: `awaitAll()`, `awaitAllSuccessfulOrThrow()`, `anySuccessfulOrThrow()`.

**Best for:** Concurrent service calls, fan-out/fan-in, replacing `ExecutorService`.

---

### 7. JEP 487 — Scoped Values

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

### 8. JEP 496 — Quantum-Resistant ML-KEM

```bash
java --enable-preview --add-modules jdk.incubator.vector \
  -cp target/classes org.example.standard.QuantumKemDemo
```

**What changed:** Java now includes ML-KEM (FIPS 203), a quantum-resistant Key Encapsulation Mechanism based on lattice cryptography. Supports ML-KEM-512, ML-KEM-768, and ML-KEM-1024.

The demo shows the complete workflow: key generation → encapsulation → decapsulation → shared secret verification.

**Best for:** Post-quantum secure key exchange, future-proofing TLS, hybrid crypto systems.

---

### 9. JEP 497 — Quantum-Resistant ML-DSA

```bash
java --enable-preview --add-modules jdk.incubator.vector \
  -cp target/classes org.example.standard.QuantumDsaDemo
```

**What changed:** Java now includes ML-DSA (FIPS 204), a quantum-resistant digital signature algorithm. Supports ML-DSA-44, ML-DSA-65, and ML-DSA-87. The demo includes a tamper detection scenario proving signature integrity.

**Best for:** Post-quantum code signing, document signing, authentication.

---

### 10. JEP 488 — Primitive Types in Patterns

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

**Status:** Preview — requires `--enable-preview`.

---

### 11. JEP 478 — Key Derivation Function API

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

**Status:** Preview — requires `--enable-preview`.

---

### 12. JEP 489 — Vector API

```bash
java --enable-preview --add-modules jdk.incubator.vector \
  -cp target/classes org.example.incubator.VectorApiDemo
```

**What changed:** The Vector API enables SIMD (Single Instruction, Multiple Data) parallel computation on arrays. Process 4–16 elements per CPU instruction instead of one.

The demo covers: element-wise operations, reductions, conditional/masked operations, dot products, and a scalar-vs-SIMD comparison.

**Status:** Incubator (9th round) — requires `--add-modules jdk.incubator.vector`.

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
| [JEP 483](https://openjdk.org/jeps/483) | **Ahead-of-Time Class Loading & Linking** | JVM startup optimization. Use `java -XX:AOTCache` flags. |
| [JEP 490](https://openjdk.org/jeps/490) | **ZGC: Remove Non-Generational Mode** | ZGC is now always generational. Old `-XX:-ZGenerational` flag removed. |
| [JEP 486](https://openjdk.org/jeps/486) | **Permanently Disable Security Manager** | `System.setSecurityManager()` throws `UnsupportedOperationException`. |
| [JEP 472](https://openjdk.org/jeps/472) | **Prepare to Restrict JNI** | Warns on JNI usage without `--enable-native-access`. |
| [JEP 498](https://openjdk.org/jeps/498) | **Warn on sun.misc.Unsafe Memory Access** | Warns when using deprecated `Unsafe` memory methods. |
| [JEP 493](https://openjdk.org/jeps/493) | **Linking Run-Time Images without JMODs** | `jlink` can now create images from just the run-time image. |

---

## 📁 Project Structure

```
java26/
├── pom.xml                                    # Maven config (Java 26 + preview + incubator)
├── README.md                                  # This file
└── src/main/java/org/example/
    ├── Main.java                              # Original hello-world
    ├── standard/                              # Final/standard features
    │   ├── SimpleSourceFileDemo.java          # JEP 495
    │   ├── ModuleImportDemo.java              # JEP 494
    │   ├── FlexibleConstructorDemo.java       # JEP 492
    │   ├── StreamGatherersDemo.java           # JEP 485
    │   ├── ClassFileApiDemo.java              # JEP 484
    │   ├── StructuredConcurrencyDemo.java     # JEP 499
    │   ├── ScopedValuesDemo.java              # JEP 487
    │   ├── QuantumKemDemo.java                # JEP 496
    │   └── QuantumDsaDemo.java                # JEP 497
    ├── preview/                               # Preview features
    │   ├── PrimitivePatternsDemo.java         # JEP 488
    │   └── KeyDerivationDemo.java             # JEP 478
    ├── incubator/                             # Incubator features
    │   └── VectorApiDemo.java                 # JEP 489
    └── vm/                                    # VM/runtime improvements
        └── VirtualThreadSyncDemo.java         # JEP 491
```

---

## 📜 License

This project is for educational purposes. Feel free to use, modify, and share.

