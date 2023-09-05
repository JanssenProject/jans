package io.jans.service.custom.script.test.java;

import io.jans.service.custom.script.jit.SimpleJavaCompiler;

/**
 * @author Yuriy Zabrovarnyy
 */
public class SimpleJavaCompileTest {

    public static void main(String[] args) throws Exception {
        String source = "import io.jans.service.custom.script.test.java.Testable; public class Test implements Testable { static { System.out.println(\"hello\"); } public Test() { System.out.println(\"world\"); } }";

        for (int i = 0; i < 10000; i++) {
            Class<Testable> cls = (Class<Testable>) SimpleJavaCompiler.compile(Testable.class, source);
            Object instance = cls.getDeclaredConstructor().newInstance();
            System.out.println(i);
        }
    }
}
