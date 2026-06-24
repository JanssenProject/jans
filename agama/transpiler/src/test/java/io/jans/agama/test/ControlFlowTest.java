package io.jans.agama.test;

import io.jans.agama.dsl.TranspilationResult;
import io.jans.agama.dsl.Transpiler;

import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Code-generation conformance for the control-flow constructs described in
 * docs/agama/language-reference.md (Conditionals, Match, loops). Asserts that a flow
 * using {@code When}/{@code Otherwise}, {@code Match}, {@code Repeat} (with
 * {@code Quit When}) and {@code Iterate} transpiles and the generated JavaScript contains
 * the expected control structures. The existing pass-flow tests only check syntax.
 */
public class ControlFlowTest {

    @Test
    public void transpile_controlFlowConstructs_generateExpectedJs() throws Exception {
        String source = String.join("\n",
                "Flow com.acme.control",
                "    Basepath \"\"",
                "When flag is true",
                "    Log \"yes\"",
                "Otherwise",
                "    Log \"no\"",
                "Match color to",
                "    \"red\"",
                "        Log \"r\"",
                "    \"blue\"",
                "        Log \"b\"",
                "Repeat count times max",
                "    Log \"loop\"",
                "    Quit When done is true",
                "Iterate over items using it",
                "    Log \"each\"",
                "Finish true",
                "");

        TranspilationResult result = Transpiler.transpile("com.acme.control", source);
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
