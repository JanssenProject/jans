package io.jans.agama.test;

import io.jans.agama.dsl.TranspilationResult;
import io.jans.agama.dsl.Transpiler;

import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Code-generation conformance for the control-flow constructs described in
 * docs/agama/language-reference.md (Conditionals, Match, loops). Asserts that flows
 * using {@code When}/{@code Otherwise}, {@code Match}, {@code Repeat} (with
 * {@code Quit When}) and {@code Iterate} transpile and the generated JavaScript contains
 * the expected control structures. Each construct is covered by a focused test so a
 * code-generation regression isolates to the affected construct. The existing pass-flow
 * tests only check syntax.
 */
public class ControlFlowTest {

    private static String transpile(String qname, String... lines) throws Exception {
        String source = String.join("\n", lines) + "\n";
        TranspilationResult result = Transpiler.transpile(qname, source);
        assertNotNull(result, "transpilation result should not be null");
        String code = result.getCode();
        assertNotNull(code, "generated code should not be null");
        return code;
    }

    @Test
    public void transpile_whenOtherwise_generatesIfElse() throws Exception {
        String code = transpile("com.acme.cond",
                "Flow com.acme.cond",
                "    Basepath \"\"",
                "When flag is true",
                "    Log \"yes\"",
                "Otherwise",
                "    Log \"no\"",
                "Finish true");

        assertTrue(code.contains("if ("), "When should generate an if statement");
        assertTrue(code.contains("else {"), "Otherwise should generate an else block");
    }

    @Test
    public void transpile_match_generatesEqualityBranches() throws Exception {
        String code = transpile("com.acme.match",
                "Flow com.acme.match",
                "    Basepath \"\"",
                "Match color to",
                "    \"red\"",
                "        Log \"r\"",
                "    \"blue\"",
                "        Log \"b\"",
                "Finish true");

        assertTrue(code.contains("if ("), "Match should generate an if statement");
        assertTrue(code.contains("_equals("), "Match should generate equality comparisons");
    }

    @Test
    public void transpile_repeatQuitWhen_generatesCountedLoopWithBreak() throws Exception {
        String code = transpile("com.acme.repeat",
                "Flow com.acme.repeat",
                "    Basepath \"\"",
                "Repeat count times max",
                "    Log \"loop\"",
                "    Quit When done is true",
                "Finish true");

        assertTrue(code.contains("for (let _times"), "Repeat should generate a counted loop");
        assertTrue(code.contains("break"), "Quit When should generate a loop break");
    }

    @Test
    public void transpile_iterate_generatesIterationLoop() throws Exception {
        String code = transpile("com.acme.iterate",
                "Flow com.acme.iterate",
                "    Basepath \"\"",
                "Iterate over items using it",
                "    Log \"each\"",
                "Finish true");

        assertTrue(code.contains("for (let _item of"), "Iterate should generate an iteration loop");
    }
}
