package io.jans.service.custom.script.test.java;

import net.openhft.compiler.CompilerUtils;

import java.lang.reflect.InvocationTargetException;

/**
 * @author Yuriy Zabrovarnyy
 */
public class SimpleJavaCompileTest {

    public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        String className = "MyClass";
        String javaCode =
                "public class MyClass implements Runnable {\n" +
                "    public void run() {\n" +
                "        System.out.println(\"Hello World\");\n" +
                "    }\n" +
                "}\n";
        Class aClass = CompilerUtils.CACHED_COMPILER.loadFromJava(className, javaCode);
        Runnable runner = (Runnable) aClass.getDeclaredConstructor().newInstance();
        runner.run();
    }
}
