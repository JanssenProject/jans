package io.jans.agama.test;

import io.jans.agama.dsl.TranspilationResult;
import io.jans.agama.dsl.Transpiler;

import java.nio.file.Files;
import java.nio.file.Paths;
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
 * and generated JavaScript. Flow sources are read from files under
 * {@code src/test/resources/pass} rather than embedded in Java, matching the existing tests.
 */
public class TranspilationResultTest {

    private String passFlow(String fileName) throws Exception {
        return Files.readString(Paths.get("target/test-classes/pass", fileName));
    }

    @Test
    public void transpile_withTimeoutAndInputs_populatesResult() throws Exception {
        TranspilationResult result = Transpiler.transpile("io.jans.test.InputsTimeout",
                passFlow("inputs_timeout.txt"));

        assertNotNull(result);
        // funcName is "_" + qualified name with dots replaced by underscores.
        assertEquals(result.getFuncName(), "_io_jans_test_InputsTimeout");
        assertEquals(result.getInputs(), List.of("salutation", "askGender"));
        assertEquals(result.getTimeout(), Integer.valueOf(100));
        assertNotNull(result.getCode());
        assertTrue(result.getCode().contains("function _io_jans_test_InputsTimeout"),
                "generated code should declare the flow function");
    }

    @Test
    public void transpile_withoutTimeoutOrInputs_hasNoTimeoutAndEmptyInputs() throws Exception {
        // Reuses the existing pass flow 'variables.txt', which declares no Timeout and no Inputs.
        TranspilationResult result = Transpiler.transpile("flow", passFlow("variables.txt"));

        assertNotNull(result);
        assertEquals(result.getFuncName(), "_flow");
        assertNull(result.getTimeout());
        assertTrue(result.getInputs() == null || result.getInputs().isEmpty(),
                "a flow without Inputs should have no input parameters");
    }
}
