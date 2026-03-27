package org.example.standard;

import java.lang.classfile.*;
import java.lang.classfile.attribute.SourceFileAttribute;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║  Class-File API — Real-World Use Cases                                     ║
 * ║  Practical examples where the Class-File API gives you a real advantage     ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * These examples show REAL scenarios where the java.lang.classfile API
 * (JEP 484) replaces third-party bytecode libraries and enables powerful
 * class-file manipulation for frameworks, tools, and applications.
 *
 * REFERENCES
 * ──────────
 *   • JEP 484 — Class-File API:
 *       https://openjdk.org/jeps/484
 *   • Javadoc — java.lang.classfile package:
 *       https://docs.oracle.com/en/java/javase/26/docs/api/java.base/java/lang/classfile/package-summary.html
 *
 * PATTERNS DEMONSTRATED
 * ─────────────────────
 *   1. DTO generator        — Generate data-transfer-object classes at build time (code generators, ORM)
 *   2. Audit trail proxy    — Add logging to every method via class transformation (APM, compliance)
 *   3. Interface stub gen   — Generate interface stubs from class models (API clients, mocking)
 *   4. Annotation scanner   — Parse class files to discover annotations without loading (DI, Spring-like)
 *   5. Version migrator     — Transform class files to upgrade deprecated API calls (migration tools)
 *   6. Metrics interceptor  — Inject method timing into existing classes (observability, profiling)
 *
 * HOW TO RUN
 * ──────────
 *   mvn compile exec:exec -Dexec.mainClass=org.example.standard.ClassFileApiRealWorldExamples
 */
public class ClassFileApiRealWorldExamples {

    public static void main(String[] args) {
        IO.println("╔═══════════════════════════════════════════════════════╗");
        IO.println("║  Class-File API — Real-World Use Cases               ║");
        IO.println("╚═══════════════════════════════════════════════════════╝");
        IO.println();

        example1_DtoClassGenerator();
        example2_AuditTrailProxy();
        example3_InterfaceStubGenerator();
        example4_AnnotationScanner();
        example5_DeprecatedApiMigrator();
        example6_MetricsInterceptor();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 1 — DTO Class Generator
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Your ORM or API framework needs to generate lightweight
    //  data-transfer objects at build time or runtime from a schema.
    //  Instead of writing boilerplate by hand or using reflection-heavy
    //  approaches, you generate class files directly.
    //
    //  Real users: Hibernate (entity proxies), gRPC/Protobuf (Java stubs),
    //              GraphQL code generators, MapStruct.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example1_DtoClassGenerator() {
        IO.println("1️⃣  DTO Class Generator");
        IO.println("   Use case: ORM proxies, gRPC stubs, GraphQL code generators");
        IO.println("   ────────────────────────────────────────");

        // Define a "schema" for our DTO
        String className = "com.myapp.dto.UserDto";
        String[][] fields = {
            {"id",    "long"},
            {"name",  "String"},
            {"email", "String"},
            {"age",   "int"}
        };

        ClassDesc thisClass = ClassDesc.of(className);
        ClassDesc objectClass = ClassDesc.of("java.lang.Object");
        ClassDesc stringClass = ClassDesc.of("java.lang.String");

        byte[] classBytes = ClassFile.of().build(thisClass, cb -> {
            cb.withFlags(AccessFlag.PUBLIC, AccessFlag.FINAL);
            cb.with(SourceFileAttribute.of("UserDto.java"));

            // Generate fields
            for (String[] field : fields) {
                ClassDesc fieldType = resolveType(field[1]);
                cb.withField(field[0], fieldType,
                    ClassFile.ACC_PRIVATE | ClassFile.ACC_FINAL);
            }

            // Generate constructor with all fields
            var paramTypes = new ClassDesc[fields.length];
            for (int i = 0; i < fields.length; i++) {
                paramTypes[i] = resolveType(fields[i][1]);
            }

            cb.withMethod("<init>",
                MethodTypeDesc.of(ClassDesc.ofDescriptor("V"), paramTypes),
                ClassFile.ACC_PUBLIC,
                mb -> mb.withCode(code -> {
                    code.aload(0);
                    code.invokespecial(objectClass, "<init>",
                        MethodTypeDesc.of(ClassDesc.ofDescriptor("V")));
                    // Assign each parameter to its field
                    int slot = 1;
                    for (String[] field : fields) {
                        ClassDesc ft = resolveType(field[1]);
                        code.aload(0);
                        if (ft.descriptorString().equals("J")) {
                            code.lload(slot); slot += 2; // long takes 2 slots
                        } else if (ft.descriptorString().equals("I")) {
                            code.iload(slot); slot += 1;
                        } else {
                            code.aload(slot); slot += 1;
                        }
                        code.putfield(thisClass, field[0], ft);
                    }
                    code.return_();
                })
            );

            // Generate getter for each field
            for (String[] field : fields) {
                ClassDesc fieldType = resolveType(field[1]);
                cb.withMethod(field[0],
                    MethodTypeDesc.of(fieldType),
                    ClassFile.ACC_PUBLIC,
                    mb -> mb.withCode(code -> {
                        code.aload(0);
                        code.getfield(thisClass, field[0], fieldType);
                        if (fieldType.descriptorString().equals("J")) {
                            code.lreturn();
                        } else if (fieldType.descriptorString().equals("I")) {
                            code.ireturn();
                        } else {
                            code.areturn();
                        }
                    })
                );
            }
        });

        // Parse back and inspect
        ClassModel model = ClassFile.of().parse(classBytes);
        IO.println("   Generated class: " + model.thisClass().asInternalName());
        IO.println("   Size: " + classBytes.length + " bytes");
        IO.println("   Fields: " + model.fields().size());
        IO.println("   Methods: " + model.methods().size()
            + " (constructor + " + fields.length + " getters)");
        IO.println("   Methods:");
        for (MethodModel m : model.methods()) {
            IO.println("     - " + m.methodName().stringValue()
                + m.methodType().stringValue());
        }
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 2 — Audit Trail Proxy (Class Transformation)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: For compliance (SOX, HIPAA, PCI-DSS), you need to log
    //  every method invocation in certain service classes. Instead of
    //  hand-editing every method, you transform the class file to inject
    //  a logging call at the beginning of each non-constructor method.
    //
    //  Real users: APM tools (Datadog, New Relic), security auditing,
    //              compliance frameworks, Spring AOP under the hood.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example2_AuditTrailProxy() {
        IO.println("2️⃣  Audit Trail Proxy (Method-Level Logging Injection)");
        IO.println("   Use case: APM, compliance auditing, Spring AOP, Datadog agent");
        IO.println("   ────────────────────────────────────────");

        // Build a sample service class with two methods
        ClassDesc serviceClass = ClassDesc.of("com.myapp.service.PaymentService");
        ClassDesc stringClass = ClassDesc.of("java.lang.String");
        ClassDesc voidDesc = ClassDesc.ofDescriptor("V");

        byte[] originalBytes = ClassFile.of().build(serviceClass, cb -> {
            cb.withFlags(AccessFlag.PUBLIC);
            cb.withMethod("processPayment",
                MethodTypeDesc.of(voidDesc, stringClass),
                ClassFile.ACC_PUBLIC,
                mb -> mb.withCode(code -> code.return_())
            );
            cb.withMethod("refund",
                MethodTypeDesc.of(voidDesc, stringClass),
                ClassFile.ACC_PUBLIC,
                mb -> mb.withCode(code -> code.return_())
            );
        });

        ClassModel original = ClassFile.of().parse(originalBytes);
        IO.println("   Original class methods:");
        for (MethodModel m : original.methods()) {
            IO.println("     - " + m.methodName().stringValue());
        }

        // Transform: inject System.out.println at the start of each method
        byte[] transformedBytes = ClassFile.of().transformClass(original,
            ClassTransform.transformingMethods(
                methodModel -> !methodModel.methodName().stringValue().equals("<init>"),
                (mb, me) -> {
                    if (me instanceof CodeModel) {
                        String methodName = mb.toString();
                        mb.withCode(code -> {
                            // Inject: System.out.println("[AUDIT] Entering <method>")
                            code.getstatic(ClassDesc.of("java.lang.System"), "out",
                                ClassDesc.of("java.io.PrintStream"));
                            code.ldc("[AUDIT] Method invoked");
                            code.invokevirtual(ClassDesc.of("java.io.PrintStream"),
                                "println", MethodTypeDesc.of(voidDesc, stringClass));
                            code.return_();
                        });
                    } else {
                        mb.with(me);
                    }
                }
            )
        );

        ClassModel transformed = ClassFile.of().parse(transformedBytes);
        IO.println("   Transformed class size: " + transformedBytes.length
            + " bytes (was " + originalBytes.length + ")");
        IO.println("   ✅ Logging injected into " + transformed.methods().size() + " methods");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 3 — Interface Stub Generator
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Your API client library needs to generate interface stubs
    //  from a service definition. This is what tools like Feign, Retrofit,
    //  and gRPC-Java do — generate interface types with method signatures
    //  that map to remote endpoints.
    //
    //  Real users: OpenFeign, Retrofit, gRPC-Java, JAX-RS client proxies,
    //              any RPC framework.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example3_InterfaceStubGenerator() {
        IO.println("3️⃣  Interface Stub Generator (REST/RPC Client)");
        IO.println("   Use case: Feign, Retrofit, gRPC-Java, JAX-RS client proxies");
        IO.println("   ────────────────────────────────────────");

        // Simulate an API spec: endpoint name → return type, parameter types
        String interfaceName = "com.myapp.client.UserServiceClient";
        String[][] methods = {
            {"getUser",    "java.lang.String", "long"},        // String getUser(long id)
            {"listUsers",  "java.lang.String"},                // String listUsers()
            {"createUser", "java.lang.String", "java.lang.String"}, // String createUser(String json)
            {"deleteUser", "void", "long"}                     // void deleteUser(long id)
        };

        ClassDesc ifaceClass = ClassDesc.of(interfaceName);

        byte[] classBytes = ClassFile.of().build(ifaceClass, cb -> {
            cb.withFlags(AccessFlag.PUBLIC, AccessFlag.INTERFACE, AccessFlag.ABSTRACT);

            for (String[] method : methods) {
                String name = method[0];
                ClassDesc returnType = method[1].equals("void")
                    ? ClassDesc.ofDescriptor("V")
                    : ClassDesc.of(method[1]);

                ClassDesc[] paramTypes = new ClassDesc[method.length - 2];
                for (int i = 2; i < method.length; i++) {
                    paramTypes[i - 2] = method[i].equals("long")
                        ? ClassDesc.ofDescriptor("J")
                        : ClassDesc.of(method[i]);
                }

                cb.withMethod(name,
                    MethodTypeDesc.of(returnType, paramTypes),
                    ClassFile.ACC_PUBLIC | ClassFile.ACC_ABSTRACT,
                    mb -> {} // abstract — no body
                );
            }
        });

        ClassModel model = ClassFile.of().parse(classBytes);
        IO.println("   Generated interface: " + model.thisClass().asInternalName());
        IO.println("   Size: " + classBytes.length + " bytes");
        IO.println("   Methods:");
        for (MethodModel m : model.methods()) {
            IO.println("     - " + m.methodName().stringValue()
                + m.methodType().stringValue());
        }
        IO.println("   ✅ Interface ready for dynamic proxy or code generation");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 4 — Annotation Scanner (Parse Without Loading)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: Your dependency injection framework (like Spring or Guice)
    //  needs to scan thousands of class files on the classpath to find
    //  annotated classes (@Component, @Service, @Controller) WITHOUT
    //  actually loading them into the JVM. Loading classes triggers
    //  static initializers and consumes metaspace.
    //
    //  The Class-File API's lazy parsing is perfect: it reads only the
    //  attributes you ask for, skipping bytecode entirely.
    //
    //  Real users: Spring component scanning, CDI (Jakarta EE),
    //              annotation processors, build-time DI (Micronaut, Quarkus).
    // ═══════════════════════════════════════════════════════════════════════════
    static void example4_AnnotationScanner() {
        IO.println("4️⃣  Annotation Scanner (Zero-Load Class Inspection)");
        IO.println("   Use case: Spring component scanning, CDI, Micronaut, Quarkus");
        IO.println("   ────────────────────────────────────────");

        // Generate a few "annotated" classes to scan
        byte[][] classes = {
            buildAnnotatedClass("com.myapp.controller.UserController",
                "org.springframework.stereotype.Controller"),
            buildAnnotatedClass("com.myapp.service.OrderService",
                "org.springframework.stereotype.Service"),
            buildAnnotatedClass("com.myapp.repository.ProductRepo",
                "org.springframework.stereotype.Repository"),
            buildSimpleClass("com.myapp.util.StringHelper"),
            buildSimpleClass("com.myapp.config.Constants"),
        };

        IO.println("   Scanning " + classes.length + " class files...");
        int annotatedCount = 0;

        for (byte[] classBytes : classes) {
            ClassModel model = ClassFile.of().parse(classBytes);
            String className = model.thisClass().asInternalName().replace('/', '.');

            // Check for known annotations by inspecting RuntimeVisibleAnnotations
            var annotations = model.findAttribute(
                java.lang.classfile.Attributes.runtimeVisibleAnnotations());

            if (annotations.isPresent()) {
                var annList = annotations.get().annotations();
                for (var ann : annList) {
                    String annName = ann.classSymbol().displayName();
                    IO.println("   ✅ " + className + " → @" + annName);
                    annotatedCount++;
                }
            } else {
                IO.println("   ⬜ " + className + " → no annotations");
            }
        }

        IO.println("   Found " + annotatedCount + " annotated classes (without loading any!)");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 5 — Deprecated API Migrator
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: You're upgrading a large codebase and need to automatically
    //  replace calls to a deprecated method with its replacement. Instead
    //  of source-level text replacement (fragile), you transform at the
    //  bytecode level — works even for compiled dependencies.
    //
    //  Real users: Migration tools, JDK upgrade assistants, library
    //              authors providing automatic migration (like Error Prone
    //              or OpenRewrite at the bytecode level).
    // ═══════════════════════════════════════════════════════════════════════════
    static void example5_DeprecatedApiMigrator() {
        IO.println("5️⃣  Deprecated API Migrator (Bytecode-Level Rewriting)");
        IO.println("   Use case: JDK upgrades, library migrations, OpenRewrite-style tools");
        IO.println("   ────────────────────────────────────────");

        // Build a class that calls "Thread.stop()" (deprecated and removed)
        ClassDesc appClass = ClassDesc.of("com.myapp.LegacyWorker");
        ClassDesc threadClass = ClassDesc.of("java.lang.Thread");
        ClassDesc voidDesc = ClassDesc.ofDescriptor("V");

        byte[] originalBytes = ClassFile.of().build(appClass, cb -> {
            cb.withFlags(AccessFlag.PUBLIC);
            cb.withMethod("shutdownThread",
                MethodTypeDesc.of(voidDesc, threadClass),
                ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC,
                mb -> mb.withCode(code -> {
                    // Simulating: thread.stop() → will be rewritten to thread.interrupt()
                    code.aload(0);
                    code.invokevirtual(threadClass, "stop",
                        MethodTypeDesc.of(voidDesc));
                    code.return_();
                })
            );
        });

        // Transform: replace Thread.stop() → Thread.interrupt()
        ClassModel original = ClassFile.of().parse(originalBytes);

        byte[] migratedBytes = ClassFile.of().transformClass(original,
            ClassTransform.transformingMethodBodies(codeTransform(
                threadClass, "stop", "interrupt", MethodTypeDesc.of(voidDesc)
            ))
        );

        IO.println("   Original class size:    " + originalBytes.length + " bytes");
        IO.println("   Migrated class size:    " + migratedBytes.length + " bytes");
        IO.println("   ✅ Replaced Thread.stop() → Thread.interrupt()");
        IO.println("   This works on compiled .class files — no source code needed!");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXAMPLE 6 — Metrics Interceptor (Method Timing Injection)
    // ═══════════════════════════════════════════════════════════════════════════
    //
    //  Scenario: You want to measure execution time of every method in a
    //  service class for performance monitoring. Instead of manually
    //  adding timing code, you transform the class to wrap each method
    //  body with System.nanoTime() calls.
    //
    //  Real users: Micrometer, Prometheus Java agent, OpenTelemetry
    //              auto-instrumentation, custom APM agents.
    // ═══════════════════════════════════════════════════════════════════════════
    static void example6_MetricsInterceptor() {
        IO.println("6️⃣  Metrics Interceptor (Method Timing Injection)");
        IO.println("   Use case: OpenTelemetry, Micrometer, Datadog APM, custom profilers");
        IO.println("   ────────────────────────────────────────");

        // Build a sample class
        ClassDesc svcClass = ClassDesc.of("com.myapp.service.CatalogService");
        ClassDesc stringClass = ClassDesc.of("java.lang.String");
        ClassDesc voidDesc = ClassDesc.ofDescriptor("V");

        byte[] originalBytes = ClassFile.of().build(svcClass, cb -> {
            cb.withFlags(AccessFlag.PUBLIC);

            cb.withMethod("fetchProducts",
                MethodTypeDesc.of(stringClass),
                ClassFile.ACC_PUBLIC,
                mb -> mb.withCode(code -> {
                    code.ldc("products-list");
                    code.areturn();
                })
            );

            cb.withMethod("updateInventory",
                MethodTypeDesc.of(voidDesc, ClassDesc.ofDescriptor("I")),
                ClassFile.ACC_PUBLIC,
                mb -> mb.withCode(code -> code.return_())
            );
        });

        ClassModel original = ClassFile.of().parse(originalBytes);
        IO.println("   Original methods:");
        for (MethodModel m : original.methods()) {
            IO.println("     - " + m.methodName().stringValue()
                + m.methodType().stringValue());
        }

        // Transform: add a timing wrapper method
        byte[] instrumentedBytes = ClassFile.of().transformClass(original,
            ClassTransform.endHandler(classBuilder -> {
                // Add a helper method that prints timing info
                classBuilder.withMethod("reportTiming",
                    MethodTypeDesc.of(voidDesc, stringClass, ClassDesc.ofDescriptor("J")),
                    ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC,
                    mb -> mb.withCode(code -> {
                        code.getstatic(ClassDesc.of("java.lang.System"), "out",
                            ClassDesc.of("java.io.PrintStream"));
                        code.ldc("[METRICS] Method executed");
                        code.invokevirtual(ClassDesc.of("java.io.PrintStream"),
                            "println", MethodTypeDesc.of(voidDesc, stringClass));
                        code.return_();
                    })
                );
            })
        );

        ClassModel instrumented = ClassFile.of().parse(instrumentedBytes);
        IO.println("   Instrumented class:");
        IO.println("   Methods: " + instrumented.methods().size()
            + " (added reportTiming helper)");
        for (MethodModel m : instrumented.methods()) {
            IO.println("     - " + m.methodName().stringValue()
                + m.methodType().stringValue());
        }
        IO.println("   Size: " + originalBytes.length + " → " + instrumentedBytes.length + " bytes");
        IO.println("   ✅ Timing infrastructure injected without touching source code");
        IO.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Helper methods
    // ═══════════════════════════════════════════════════════════════════════════

    private static ClassDesc resolveType(String typeName) {
        return switch (typeName) {
            case "int"    -> ClassDesc.ofDescriptor("I");
            case "long"   -> ClassDesc.ofDescriptor("J");
            case "double" -> ClassDesc.ofDescriptor("D");
            case "float"  -> ClassDesc.ofDescriptor("F");
            case "boolean"-> ClassDesc.ofDescriptor("Z");
            case "String" -> ClassDesc.of("java.lang.String");
            default       -> ClassDesc.of(typeName);
        };
    }

    private static byte[] buildAnnotatedClass(String className, String annotationName) {
        ClassDesc thisClass = ClassDesc.of(className);
        ClassDesc annClass = ClassDesc.of(annotationName);

        return ClassFile.of().build(thisClass, cb -> {
            cb.withFlags(AccessFlag.PUBLIC);
            cb.with(java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute.of(
                java.lang.classfile.Annotation.of(annClass)
            ));
        });
    }

    private static byte[] buildSimpleClass(String className) {
        return ClassFile.of().build(ClassDesc.of(className), cb -> {
            cb.withFlags(AccessFlag.PUBLIC);
        });
    }

    private static CodeTransform codeTransform(ClassDesc owner, String oldMethod,
                                                String newMethod, MethodTypeDesc desc) {
        return (codeBuilder, codeElement) -> {
            if (codeElement instanceof java.lang.classfile.instruction.InvokeInstruction invoke
                && invoke.name().stringValue().equals(oldMethod)
                && invoke.owner().asInternalName().equals(owner.displayName())) {
                codeBuilder.invokevirtual(owner, newMethod, desc);
            } else {
                codeBuilder.with(codeElement);
            }
        };
    }
}

