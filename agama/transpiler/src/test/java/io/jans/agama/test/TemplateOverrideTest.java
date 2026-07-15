package io.jans.agama.test;

import io.jans.agama.dsl.TranspilationResult;
import io.jans.agama.dsl.Transpiler;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Code-generation conformance for the {@code Override templates} form on a {@code Trigger}.
 * Transpiles the existing pass flow {@code triggers_calls.txt} (whose
 * {@code Trigger $bah.humbug} carries
 * {@code Override templates "pea/body.ftl" "" "pea/media.ftl" "fluff.ftl" "caravan.ftl" "../whoops.ftlh"})
 * and asserts the generated JavaScript emits the subflow call and carries the override
 * template paths into it. {@link ValidFlowsTest} only checks that this flow parses.
 */
public class TemplateOverrideTest {

    @Test
    public void transpile_triggersFlow_generatesSubflowCallWithTemplateOverrides() throws Exception {
        String source = Files.readString(Paths.get("target/test-classes/pass", "triggers_calls.txt"));

        TranspilationResult result = Transpiler.transpile("flow", source);
        assertNotNull(result);
        String code = result.getCode();
        assertNotNull(code);

        assertTrue(code.contains("_flowCall("), "Trigger should generate a subflow call");
        assertTrue(code.contains("\"pea/body.ftl\""), "override template path pea/body.ftl should appear in generated code");
        assertTrue(code.contains("\"pea/media.ftl\""), "override template path pea/media.ftl should appear in generated code");
        assertTrue(code.contains("\"fluff.ftl\""), "override template path fluff.ftl should appear in generated code");
        assertTrue(code.contains("\"caravan.ftl\""), "override template path caravan.ftl should appear in generated code");
        assertTrue(code.contains("\"../whoops.ftlh\""), "override template path ../whoops.ftlh should appear in generated code");
    }
}
