// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

package io.jans.trace;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.testng.annotations.Test;

/**
 * Runs the Java example from {@code README.md} as a test, the way {@code cargo test} runs the
 * Rust example in the core README as a doctest. Java has no doctests, so this test does the same
 * by hand: it takes the {@code ```java} block from the README, compiles it against this module
 * and runs its {@code main}.
 *
 * <p>Why: the README example is what Lock developers copy. Without this test, renaming a method
 * or changing a signature keeps every other test green while the documented code stops compiling
 * or stops working. To change the example, edit the README; this test needs no change.
 */
public class ReadmeExampleTest {

    private static final Pattern JAVA_BLOCK = Pattern.compile("```java\\n(.*?)```", Pattern.DOTALL);
    private static final Pattern CLASS_NAME = Pattern.compile("public class (\\w+)");

    @Test
    public void readmeExampleCompilesAndRuns() throws Exception {
        // 1. Take the example out of the README. Exactly one block, so a second example cannot
        //    go untested and a renamed fence cannot make this test check nothing.
        String readme = new String(Files.readAllBytes(Paths.get("README.md")), StandardCharsets.UTF_8);
        List<String> blocks = new ArrayList<>();
        Matcher block = JAVA_BLOCK.matcher(readme);
        while (block.find()) {
            blocks.add(block.group(1));
        }
        assertEquals(blocks.size(), 1, "the README has exactly one Java example, and this test runs it");
        String source = blocks.get(0);
        Matcher className = CLASS_NAME.matcher(source);
        assertTrue(className.find(), "the example declares a public class");

        // 2. Write it to target/, so `mvn clean` removes it; javac needs the file named after
        //    its public class.
        Path dir = Files.createDirectories(Paths.get("target", "readme-example"));
        Path file = dir.resolve(className.group(1) + ".java");
        Files.write(file, source.getBytes(StandardCharsets.UTF_8));

        // 3. Compile it against the module's classes, as a Lock developer would. -Werror also fails
        //    on deprecated calls, so the example never shows an API on its way out.
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "tests run on a JDK");
        // Surefire may start the JVM from a manifest-only jar; this property holds the real path.
        String classPath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        int status = compiler.run(null, null, null,
                "-classpath", classPath, "-d", dir.toString(), "-Xlint:all", "-Werror", file.toString());
        assertEquals(status, 0, "the README example compiles without warnings");

        // 4. Run it. A thrown exception fails the test. The example catches a rejected record and
        //    prints it to stderr, as Lock would answer it, so stderr must stay empty.
        URL[] exampleDir = {dir.toUri().toURL()};
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        PrintStream stderr = System.err;
        // The test's own loader is the parent, so the example uses the classes, and the native
        // library, already loaded for this test.
        try (URLClassLoader loader = new URLClassLoader(exampleDir, getClass().getClassLoader())) {
            Method main = loader.loadClass(className.group(1)).getMethod("main", String[].class);
            System.setErr(new PrintStream(errors, true, "UTF-8"));
            main.invoke(null, (Object) new String[0]);
        } finally {
            System.setErr(stderr);
        }
        assertEquals(errors.toString("UTF-8"), "", "the example's record is accepted");
    }
}
