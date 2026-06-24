package io.jans.agama.test;

import io.jans.agama.dsl.TranspilationResult;
import io.jans.agama.dsl.Transpiler;
import io.jans.agama.dsl.TranspilerException;
import io.jans.agama.dsl.error.SyntaxException;

import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Spec-conformance checks for literal handling, per the "Literals" section of
 * docs/agama/language-reference.md. Positive: all documented valid literal forms
 * (string, escapes, boolean, signed/decimal numbers, null, list, nested map) transpile.
 * Negative: documented invalid number forms are rejected.
 */
public class LiteralsTest {

    @Test
    public void transpile_allValidLiteralForms_succeeds() throws Exception {
        String source = String.join("\n",
                "Flow com.acme.literals",
                "    Basepath \"\"",
                "str = \"Agama\"",
                "esc = \"Hello\\nGluu\"",
                "empty = \"\"",
                "b1 = true",
                "b2 = false",
                "n1 = 0",
                "n2 = -1",
                "n3 = 2.0",
                "n4 = -3.000001",
                "nul = null",
                "lst = [ 1, 2, 3 ]",
                "mix = [ false, [ 0, 1 ], \"?\" ]",
                "mp = { brand: \"Ford\", color: null, model: 1963, overhaulsIn: [ 1979, 1999 ] }",
                "Finish true",
                "");

        TranspilationResult result = Transpiler.transpile("com.acme.literals", source);
        assertNotNull(result);
        assertNotNull(result.getCode());
        assertTrue(result.getCode().contains("\"Agama\""),
                "string literal should appear in generated code");
        assertTrue(result.getCode().contains("1963"),
                "number literal should appear in generated code");
    }

    @Test
    public void transpile_invalidNumberForms_rejected() {
        // Per the language reference: no exponential notation; ".1", "-.1", "+1" are invalid.
        String[] invalid = {"1E-05", ".1", "-.1", "+1"};
        for (String num : invalid) {
            String source = String.join("\n",
                    "Flow com.acme.badnum",
                    "    Basepath \"\"",
                    "x = " + num,
                    "Finish true",
                    "");
            try {
                Transpiler.runSyntaxCheck(source);
                fail("Number literal '" + num + "' should be rejected per the language reference");
            } catch (SyntaxException | TranspilerException e) {
                // expected
            }
        }
    }
}
