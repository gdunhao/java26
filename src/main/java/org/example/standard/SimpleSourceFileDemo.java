package org.example.standard;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  JEP 512: Compact Source Files and Instance Main Methods                    ║
 * ║  Status: FINAL in JDK 26                                                   ║
 * ║  Spec: https://openjdk.org/jeps/512                                        ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * WHAT THIS FEATURE DOES
 * ──────────────────────
 * This JEP simplifies the on-ramp to Java by allowing programs to be written
 * with less ceremony. It introduces three key relaxations:
 *
 *   1. Instance main methods — The `main` method no longer needs to be
 *      `public`, `static`, or accept a `String[]` parameter.
 *
 *   2. Implicit classes — A Java source file can omit the enclosing class
 *      declaration entirely. The compiler creates an "implicitly declared
 *      class" behind the scenes.
 *
 *   3. Automatic `IO` class — The new `java.io.IO` class provides convenient
 *      `println`, `print`, and `readln` methods that are auto-imported in
 *      compact source files.
 *
 * WHY IT MATTERS
 * ──────────────
 * Before JDK 26, even the simplest "Hello World" program required understanding
 * classes, access modifiers, static methods, and string arrays:
 *
 *   // Before JDK 26 — lots of boilerplate for beginners
 *   public class HelloWorld {
 *       public static void main(String[] args) {
 *           System.out.println("Hello, World!");
 *       }
 *   }
 *
 * With JDK 26, you can write it as simply as:
 *
 *   void main() {
 *       IO.println("Hello, World!");
 *   }
 *
 * This is a huge win for education, scripting, and rapid prototyping.
 *
 * NOTE ON THIS DEMO
 * ─────────────────
 * Since this file is inside a package (`org.example.standard`), it is NOT an
 * implicit class — implicit classes must be in the unnamed package. However,
 * this demo still shows the simplified `main` method (no `public`, no `static`,
 * no `String[] args`) and the use of `IO.println`.
 *
 * To see a true implicit class, create a file like `Hello.java` in the root
 * with no package declaration and no class declaration.
 *
 * HOW TO RUN
 * ──────────
 *   javac --enable-preview --release 26 SimpleSourceFileDemo.java
 *   java --enable-preview SimpleSourceFileDemo
 *
 * Or with Maven:
 *   mvn compile exec:exec -Dexec.mainClass=org.example.standard.SimpleSourceFileDemo
 */
public class SimpleSourceFileDemo {

    /**
     * Instance main method — no `static`, no `String[]`, no `public`.
     * The JVM launch protocol in JDK 26 recognizes this signature.
     *
     * Selection priority (from highest to lowest):
     *   1. static void main(String[] args)      — traditional
     *   2. static void main()                   — no-args static
     *   3. void main(String[] args)             — instance with args
     *   4. void main()                          — instance, no args  ← this one
     */
    void main() {
        // ─── Using IO.println (new in JDK 26) ───
        // java.io.IO is automatically available. It provides:
        //   IO.println(Object)  — prints with newline
        //   IO.print(Object)    — prints without newline
        //   IO.readln(String)   — reads a line with a prompt
        IO.println("╔═══════════════════════════════════════════════════╗");
        IO.println("║  JEP 512 — Compact Source Files & Instance Main  ║");
        IO.println("╚═══════════════════════════════════════════════════╝");
        IO.println();

        // ─── Demonstration: Instance state is accessible ───
        // Because `main` is an instance method, we can use `this` and
        // access instance fields/methods without static context.
        String greeting = buildGreeting("Java 26 Developer");
        IO.println("Instance method result: " + greeting);
        IO.println();

        // ─── Comparison: Old way vs New way ───
        IO.println("┌─────────────────────────────────────────┐");
        IO.println("│  BEFORE JDK 26:                         │");
        IO.println("│  public static void main(String[] args) │");
        IO.println("│      System.out.println(\"Hello\");        │");
        IO.println("│                                         │");
        IO.println("│  WITH JDK 26:                           │");
        IO.println("│  void main()                            │");
        IO.println("│      IO.println(\"Hello\");                │");
        IO.println("└─────────────────────────────────────────┘");
    }

    /**
     * Since main() is an instance method, we can call other instance methods
     * directly — no need for static helpers or creating objects.
     */
    private String buildGreeting(String name) {
        return "Hello, " + name + "! Welcome to simplified Java.";
    }
}

