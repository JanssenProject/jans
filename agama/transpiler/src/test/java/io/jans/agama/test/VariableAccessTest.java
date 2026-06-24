package io.jans.agama.test;

import io.jans.agama.dsl.TranspilationResult;
import io.jans.agama.dsl.Transpiler;
import io.jans.agama.dsl.TranspilerException;
import io.jans.agama.dsl.error.SyntaxException;

import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

/**
 * Spec-conformance checks for variable naming and data access, per the "Variables" and
 * "Accessing and mutating data in variables" sections of docs/agama/language-reference.md.
 * Positive: valid names and access forms (dot, numeric index, length, quoted and
 * $-prefixed property access) transpile. Negative: a name that does not match
 * {@code [a-zA-Z](_|[a-zA-Z]|[0-9])*} and expression-based indexing are rejected.
 */
public class VariableAccessTest {

    @Test
    public void transpile_validNamesAndAccessForms_succeeds() throws Exception {
        String source = String.join("\n",
                "Flow com.acme.vars",
                "    Basepath \"\"",
                "colors = [ \"red\", \"blue\" ]",
                "firstColor = colors[0]",
                "size = colors.length",
                "person = { name: \"Jo\", age: 30 }",
                "who = person.name",
                "firstChar = who[0]",
                "weird = person.\"- wow!\"",
                "dollar = person.$who",
                "Finish true",
                "");

        TranspilationResult result = Transpiler.transpile("com.acme.vars", source);
        assertNotNull(result);
        assertNotNull(result.getCode());
    }

    @Test
    public void transpile_invalidNameAndExpressionIndexing_rejected() {
        // Leading underscore violates the variable-name pattern; expression indexing is
        // explicitly disallowed by the language reference.
        String[] invalidBodies = {
                "_a = 1",          // invalid variable name (leading underscore)
                "x = a[b.c]",      // expression indexing: x[person.age]
                "x = a[b[0]]"      // expression indexing: x[y[0]]
        };
        for (String body : invalidBodies) {
            String source = String.join("\n",
                    "Flow com.acme.badvar",
                    "    Basepath \"\"",
                    body,
                    "Finish true",
                    "");
            try {
                Transpiler.runSyntaxCheck(source);
                fail("Source should be rejected per the language reference: " + body);
            } catch (SyntaxException | TranspilerException e) {
                // expected
            }
        }
    }
}
