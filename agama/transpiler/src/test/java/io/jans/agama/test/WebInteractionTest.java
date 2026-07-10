package io.jans.agama.test;

import io.jans.agama.dsl.TranspilationResult;
import io.jans.agama.dsl.Transpiler;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Code-generation conformance for subflow invocation and web-interaction constructs
 * ({@code Trigger}, {@code RRF}, {@code RFAC}). Transpiles the existing pass flows and
 * asserts the generated JavaScript emits the corresponding runtime calls:
 * {@code triggers_calls.txt} exercises {@code Trigger} (subflow invocation with bubbled-up
 * result), and {@code structure.txt} exercises {@code RRF} (render-reply-fetch) and
 * {@code RFAC} (redirect-fetch-at-callback). {@link ValidFlowsTest} only checks that these
 * flows parse.
 */
public class WebInteractionTest {

    private String passFlow(String fileName) throws Exception {
        return Files.readString(Paths.get("target/test-classes/pass", fileName));
    }

    @Test
    public void transpile_triggersFlow_generatesSubflowCall() throws Exception {
        TranspilationResult result = Transpiler.transpile("flow", passFlow("triggers_calls.txt"));
        assertNotNull(result);
        String code = result.getCode();
        assertNotNull(code);

        assertTrue(code.contains("_flowCall("), "Trigger should generate a subflow call");
        assertTrue(code.contains("_it.bubbleUp"), "an unassigned Trigger should bubble up the subflow result");
    }

    @Test
    public void transpile_structureFlow_generatesWebInteractionCalls() throws Exception {
        TranspilationResult result = Transpiler.transpile("flow", passFlow("structure.txt"));
        assertNotNull(result);
        String code = result.getCode();
        assertNotNull(code);

        assertTrue(code.contains("_renderReplyFetch("), "RRF should generate a render-reply-fetch call");
        assertTrue(code.contains("_redirectFetchAtCallback("), "RFAC should generate a redirect-fetch-at-callback call");
    }
}
