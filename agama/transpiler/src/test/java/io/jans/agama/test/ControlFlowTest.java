package io.jans.agama.test;

import io.jans.agama.dsl.TranspilationResult;
import io.jans.agama.dsl.Transpiler;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Code-generation conformance for the control-flow constructs. Transpiles the existing
 * pass flow {@code structure.txt} (which exercises {@code When}/{@code Otherwise},
 * {@code Match}, {@code Repeat} with {@code Quit When} and {@code Iterate}) and asserts
 * the generated JavaScript contains the expected control structures. {@link ValidFlowsTest}
 * only checks that this flow parses; this test additionally pins its code generation.
 */
public class ControlFlowTest {

    @Test
    public void transpile_structureFlow_generatesExpectedControlFlowJs() throws Exception {
        String source = Files.readString(Paths.get("target/test-classes/pass", "structure.txt"));

        TranspilationResult result = Transpiler.transpile("flow", source);
        assertNotNull(result);
        String code = result.getCode();
        assertNotNull(code);

        assertTrue(code.contains("if ("), "When/Match should generate an if statement");
        assertTrue(code.contains("else {"), "Otherwise should generate an else block");
        assertTrue(code.contains("_equals("), "Match should generate equality comparisons");
        assertTrue(code.contains("for (let _times"), "Repeat should generate a counted loop");
        assertTrue(code.contains("for (let _item of"), "Iterate should generate an iteration loop");
        assertTrue(code.contains("break"), "Quit When should generate a loop break");
    }
}
