package io.jans.agama.test;

import io.jans.agama.dsl.TranspilationResult;
import io.jans.agama.dsl.Transpiler;

import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Code-generation conformance for the {@code Log} and {@code Finish} statements, per
 * docs/agama/language-reference.md. Asserts a flow that logs and finishes with a userId
 * transpiles and the generated JavaScript contains the corresponding runtime calls and
 * the finish value.
 */
public class FinishAndLogTest {

    @Test
    public void transpile_logAndFinish_generateExpectedJs() throws Exception {
        String source = String.join("\n",
                "Flow com.acme.fin",
                "    Basepath \"\"",
                "Log \"starting\" \"up\"",
                "Finish \"john\"",
                "");

        TranspilationResult result = Transpiler.transpile("com.acme.fin", source);
        assertNotNull(result);
        String code = result.getCode();
        assertNotNull(code);

        assertTrue(code.contains("_log2("), "Log should generate a logging call");
        assertTrue(code.contains("\"starting\""), "Log argument should appear in generated code");
        assertTrue(code.contains("_finish("), "Finish should generate a flow-finish call");
        assertTrue(code.contains("\"john\""), "Finish value should appear in generated code");
    }
}
