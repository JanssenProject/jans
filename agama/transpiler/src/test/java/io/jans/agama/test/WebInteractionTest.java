package io.jans.agama.test;

import io.jans.agama.dsl.TranspilationResult;
import io.jans.agama.dsl.Transpiler;

import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Code-generation conformance for subflow invocation and web-interaction constructs, per
 * docs/agama/language-reference.md ({@code Trigger}, {@code RRF}, {@code RFAC}). Asserts a
 * flow using these transpiles and the generated JavaScript contains the corresponding
 * runtime calls: the subflow call (with result propagation), render-reply-fetch, and
 * redirect-fetch-at-callback.
 */
public class WebInteractionTest {

    @Test
    public void transpile_triggerRrfRfac_generateExpectedJs() throws Exception {
        String source = String.join("\n",
                "Flow com.acme.web",
                "    Basepath \"\"",
                "sub = Trigger com.acme.other",
                "RRF \"index.ftl\"",
                "RFAC \"callback.ftl\"",
                "Finish true",
                "");

        TranspilationResult result = Transpiler.transpile("com.acme.web", source);
        assertNotNull(result);
        String code = result.getCode();
        assertNotNull(code);

        assertTrue(code.contains("_flowCall("), "Trigger should generate a subflow call");
        assertTrue(code.contains("_it.bubbleUp"), "Trigger should propagate a bubbled-up subflow result");
        assertTrue(code.contains("_renderReplyFetch("), "RRF should generate a render-reply-fetch call");
        assertTrue(code.contains("_redirectFetchAtCallback("), "RFAC should generate a redirect-fetch-at-callback call");
    }
}
