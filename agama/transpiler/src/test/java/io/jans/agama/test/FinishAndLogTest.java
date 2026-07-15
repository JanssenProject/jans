package io.jans.agama.test;

import io.jans.agama.dsl.TranspilationResult;
import io.jans.agama.dsl.Transpiler;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Code-generation conformance for the {@code Log} and {@code Finish} statements. Transpiles
 * the existing pass flow {@code structure.txt} (which logs {@code Log "a" "A" "aha!" -10}
 * and finishes {@code Finish "you"}) and asserts the generated JavaScript contains the
 * logging call with its argument, the flow-finish call, and the finish value.
 * {@link ValidFlowsTest} only checks that this flow parses.
 */
public class FinishAndLogTest {

    @Test
    public void transpile_structureFlow_generatesLogAndFinishCalls() throws Exception {
        String source = Files.readString(Paths.get("target/test-classes/pass", "structure.txt"));

        TranspilationResult result = Transpiler.transpile("flow", source);
        assertNotNull(result);
        String code = result.getCode();
        assertNotNull(code);

        assertTrue(code.contains("_log2("), "Log should generate a logging call");
        assertTrue(code.contains("\"aha!\""), "Log argument should appear in generated code");
        assertTrue(code.contains("_finish("), "Finish should generate a flow-finish call");
        assertTrue(code.contains("\"you\""), "Finish value should appear in generated code");
    }
}
