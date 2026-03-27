package org.example.standard;

import java.lang.classfile.*;
import java.lang.classfile.attribute.SourceFileAttribute;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  JEP 484: Class-File API                                                   ║
 * ║  Status: FINAL in JDK 26                                                   ║
 * ║  Spec: https://openjdk.org/jeps/484                                        ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * WHAT THIS FEATURE DOES
 * ──────────────────────
 * The Class-File API provides a standard, built-in API for reading, writing,
 * and transforming Java class files. It lives in the `java.lang.classfile`
 * package and replaces the need for third-party libraries like ASM, BCEL, or
 * Byte Buddy for class file manipulation.
 *
 * KEY CONCEPTS
 * ────────────
 *   - ClassFile       — Entry point for parsing, building, and transforming
 *   - ClassModel      — Immutable representation of a parsed class file
 *   - ClassBuilder    — Fluent builder for constructing class files
 *   - MethodModel     — Represents a method in a class file
 *   - CodeBuilder     — Builder for method bytecode instructions
 *   - ClassDesc       — Nominal descriptor for a class (e.g., "Ljava/lang/String;")
 *   - MethodTypeDesc  — Nominal descriptor for a method type
 *
 * WHY IT MATTERS
 * ──────────────
 *   1. No more third-party bytecode libraries needed in the JDK toolchain
 *   2. Always in sync with the latest class file format version
 *   3. Immutable models with lazy parsing — efficient and thread-safe
 *   4. Designed for transformation pipelines (read → modify → write)
 *   5. Used internally by the JDK (e.g., for lambda metafactory, Proxy, etc.)
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.standard.ClassFileApiDemo
 */
public class ClassFileApiDemo {

    static void main(String[] args) {
        IO.println("╔═══════════════════════════════════════════════════╗");
        IO.println("║  JEP 484 — Class-File API                       ║");
        IO.println("╚═══════════════════════════════════════════════════╝");
        IO.println();

        demoGenerateClass();
        demoParseClass();
        demoTransformClass();
    }

    /**
     * DEMO 1: Generate a class file from scratch.
     *
     * We'll build the equivalent of:
     *
     *   public class GeneratedGreeter {
     *       public static String greet(String name) {
     *           return "Hello, " + name + "!";
     *       }
     *   }
     */
    static byte[] demoGenerateClass() {
        IO.println("1️⃣  Generate a Class File from Scratch");

        ClassDesc thisClass = ClassDesc.of("GeneratedGreeter");
        ClassDesc stringClass = ClassDesc.of("java.lang.String");

        byte[] classBytes = ClassFile.of().build(thisClass, classBuilder -> {
            // Set class flags
            classBuilder.withFlags(AccessFlag.PUBLIC, AccessFlag.FINAL);

            // Add a SourceFile attribute
            classBuilder.with(SourceFileAttribute.of("GeneratedGreeter.java"));

            // Add: public static String greet(String name)
            classBuilder.withMethod("greet",
                MethodTypeDesc.of(stringClass, stringClass),
                ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC,
                methodBuilder -> methodBuilder.withCode(codeBuilder -> {
                    // Build: return "Hello, " + name + "!"
                    // Using invokedynamic for string concatenation (like javac does)
                    codeBuilder
                        .new_(ClassDesc.of("java.lang.StringBuilder"))
                        .dup()
                        .invokespecial(ClassDesc.of("java.lang.StringBuilder"),
                            "<init>", MethodTypeDesc.of(ClassDesc.ofDescriptor("V")))
                        .ldc("Hello, ")
                        .invokevirtual(ClassDesc.of("java.lang.StringBuilder"),
                            "append", MethodTypeDesc.of(ClassDesc.of("java.lang.StringBuilder"), stringClass))
                        .aload(0)  // load the `name` parameter
                        .invokevirtual(ClassDesc.of("java.lang.StringBuilder"),
                            "append", MethodTypeDesc.of(ClassDesc.of("java.lang.StringBuilder"), stringClass))
                        .ldc("!")
                        .invokevirtual(ClassDesc.of("java.lang.StringBuilder"),
                            "append", MethodTypeDesc.of(ClassDesc.of("java.lang.StringBuilder"), stringClass))
                        .invokevirtual(ClassDesc.of("java.lang.StringBuilder"),
                            "toString", MethodTypeDesc.of(stringClass))
                        .areturn();
                })
            );
        });

        IO.println("   ✅ Generated class 'GeneratedGreeter' (" + classBytes.length + " bytes)");
        IO.println("   Contains method: public static String greet(String name)");
        IO.println();
        return classBytes;
    }

    /**
     * DEMO 2: Parse an existing class file and inspect its structure.
     *
     * We'll parse the class we just generated and print its methods,
     * fields, and attributes.
     */
    static void demoParseClass() {
        IO.println("2️⃣  Parse and Inspect a Class File");

        // Parse our own class file from the classpath
        byte[] classBytes = demoGenerateClassBytes();
        ClassModel classModel = ClassFile.of().parse(classBytes);

        IO.println("   Class: " + classModel.thisClass().asInternalName());
        IO.println("   Major version: " + classModel.majorVersion());
        IO.println("   Flags: " + classModel.flags().flagsMask());
        IO.println("   Methods:");

        for (MethodModel method : classModel.methods()) {
            IO.println("     - " + method.methodName().stringValue()
                + method.methodType().stringValue());
        }
        IO.println();
    }

    /**
     * DEMO 3: Transform a class file — add a new method to an existing class.
     *
     * We'll take the GeneratedGreeter class and add a new method:
     *   public static String farewell(String name)
     */
    static void demoTransformClass() {
        IO.println("3️⃣  Transform a Class File (Add a Method)");

        byte[] originalBytes = demoGenerateClassBytes();
        ClassModel originalModel = ClassFile.of().parse(originalBytes);

        IO.println("   Original method count: " + originalModel.methods().size());

        // Transform: add a new method
        ClassDesc stringClass = ClassDesc.of("java.lang.String");

        byte[] transformedBytes = ClassFile.of().transformClass(originalModel,
            ClassTransform.endHandler(classBuilder -> {
                classBuilder.withMethod("farewell",
                    MethodTypeDesc.of(stringClass, stringClass),
                    ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC,
                    methodBuilder -> methodBuilder.withCode(codeBuilder -> {
                        codeBuilder
                            .new_(ClassDesc.of("java.lang.StringBuilder"))
                            .dup()
                            .invokespecial(ClassDesc.of("java.lang.StringBuilder"),
                                "<init>", MethodTypeDesc.of(ClassDesc.ofDescriptor("V")))
                            .ldc("Goodbye, ")
                            .invokevirtual(ClassDesc.of("java.lang.StringBuilder"),
                                "append", MethodTypeDesc.of(ClassDesc.of("java.lang.StringBuilder"), stringClass))
                            .aload(0)
                            .invokevirtual(ClassDesc.of("java.lang.StringBuilder"),
                                "append", MethodTypeDesc.of(ClassDesc.of("java.lang.StringBuilder"), stringClass))
                            .ldc("!")
                            .invokevirtual(ClassDesc.of("java.lang.StringBuilder"),
                                "append", MethodTypeDesc.of(ClassDesc.of("java.lang.StringBuilder"), stringClass))
                            .invokevirtual(ClassDesc.of("java.lang.StringBuilder"),
                                "toString", MethodTypeDesc.of(stringClass))
                            .areturn();
                    })
                );
            })
        );

        ClassModel transformedModel = ClassFile.of().parse(transformedBytes);
        IO.println("   Transformed method count: " + transformedModel.methods().size());
        IO.println("   Methods after transformation:");
        for (MethodModel method : transformedModel.methods()) {
            IO.println("     - " + method.methodName().stringValue()
                + method.methodType().stringValue());
        }
        IO.println("   ✅ Successfully added 'farewell' method via transformation");
        IO.println();
    }

    /** Helper: generate the greeter class bytes. */
    private static byte[] demoGenerateClassBytes() {
        ClassDesc thisClass = ClassDesc.of("GeneratedGreeter");
        ClassDesc stringClass = ClassDesc.of("java.lang.String");

        return ClassFile.of().build(thisClass, classBuilder -> {
            classBuilder.withFlags(AccessFlag.PUBLIC, AccessFlag.FINAL);
            classBuilder.withMethod("greet",
                MethodTypeDesc.of(stringClass, stringClass),
                ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC,
                mb -> mb.withCode(cb -> {
                    cb.new_(ClassDesc.of("java.lang.StringBuilder"))
                      .dup()
                      .invokespecial(ClassDesc.of("java.lang.StringBuilder"),
                          "<init>", MethodTypeDesc.of(ClassDesc.ofDescriptor("V")))
                      .ldc("Hello, ")
                      .invokevirtual(ClassDesc.of("java.lang.StringBuilder"),
                          "append", MethodTypeDesc.of(ClassDesc.of("java.lang.StringBuilder"), stringClass))
                      .aload(0)
                      .invokevirtual(ClassDesc.of("java.lang.StringBuilder"),
                          "append", MethodTypeDesc.of(ClassDesc.of("java.lang.StringBuilder"), stringClass))
                      .ldc("!")
                      .invokevirtual(ClassDesc.of("java.lang.StringBuilder"),
                          "append", MethodTypeDesc.of(ClassDesc.of("java.lang.StringBuilder"), stringClass))
                      .invokevirtual(ClassDesc.of("java.lang.StringBuilder"),
                          "toString", MethodTypeDesc.of(stringClass))
                      .areturn();
                })
            );
        });
    }
}

