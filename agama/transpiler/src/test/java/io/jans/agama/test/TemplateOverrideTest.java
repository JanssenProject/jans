package io.jans.agama.test;

import io.jans.agama.dsl.TranspilationResult;
import io.jans.agama.dsl.Transpiler;

import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Code-generation conformance for the {@code Override templates} form on a {@code Trigger},
 * per docs/janssen-server/developer/agama/advanced-usages.md. Asserts that a subflow call
 * with template overrides transpiles and the generated JavaScript carries the override
 * template paths into the subflow call.
 */
public class TemplateOverrideTest {

    @Test
    public void transpile_triggerWithTemplateOverrides_generateExpectedJs() throws Exception {
        String source = String.join("\n",
                "Flow com.acme.ovr",
                "    Basepath \"\"",
                "Trigger com.acme.sub",
                "    Override templates \"orig.ftl\" \"custom.ftl\"",
                "Finish true",
                "");

        TranspilationResult result = Transpiler.transpile("com.acme.ovr", source);
        assertNotNull(result);
        String code = result.getCode();
        assertNotNull(code);

        assertTrue(code.contains("_flowCall("), "Trigger should generate a subflow call");
        assertTrue(code.contains("\"orig.ftl\""), "override source template should appear in generated code");
        assertTrue(code.contains("\"custom.ftl\""), "override target template should appear in generated code");
    }
}
