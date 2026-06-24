package io.jans.agama.test;

import io.jans.agama.dsl.TranspilationResult;
import io.jans.agama.dsl.Transpiler;

import java.util.List;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Semantic / code-generation assertions on the transpiler output. Complements the
 * syntax-only checks in {@link ValidFlowsTest} / MalformedFlowsTest by verifying that a
 * successful transpilation produces the expected function name, declared inputs, timeout
 * and generated JavaScript.
 */
public class TranspilationResultTest {

    @Test
    public void transpile_withTimeoutAndInputs_populatesResult() throws Exception {
        String source = String.join("\n",
                "Flow com.acme.test",
                "    Basepath \"hello\"",
                "    Timeout 100 seconds",
                "    Inputs salutation askGender",
                "Log \"starting\"",
                "Finish \"john\"",
                "");

        TranspilationResult result = Transpiler.transpile("com.acme.test", source);

        assertNotNull(result);
        // funcName is "_" + qualified name with dots replaced by underscores.
        assertEquals(result.getFuncName(), "_com_acme_test");
        assertEquals(result.getInputs(), List.of("salutation", "askGender"));
        assertEquals(result.getTimeout(), Integer.valueOf(100));
        assertNotNull(result.getCode());
        assertTrue(result.getCode().contains("function _com_acme_test"),
                "generated code should declare the flow function");
    }

    @Test
    public void transpile_withoutTimeoutOrInputs_hasNoTimeoutAndEmptyInputs() throws Exception {
        String source = String.join("\n",
                "Flow com.acme.simple",
                "    Basepath \"\"",
                "Log \"hi\"",
                "Finish true",
                "");

        TranspilationResult result = Transpiler.transpile("com.acme.simple", source);

        assertNotNull(result);
        assertEquals(result.getFuncName(), "_com_acme_simple");
        assertNull(result.getTimeout());
        assertTrue(result.getInputs() == null || result.getInputs().isEmpty(),
                "a flow without Inputs should have no input parameters");
    }
}
