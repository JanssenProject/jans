package io.jans.agama.test;

import io.jans.agama.dsl.TranspilationResult;
import io.jans.agama.dsl.Transpiler;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Code-generation conformance for foreign calls ({@code Call}) and their exception-capture
 * form. Transpiles the existing pass flow {@code triggers_calls.txt} (which includes the
 * static call {@code x256 = Call java.lang.Math#incrementExact 255} and the capture form
 * {@code n | E = Call java.lang.Integer#parseInt "AGA" 16}) and asserts the generated
 * JavaScript contains the action-call invocation with the target class/method and declares
 * the capture variable. {@link ValidFlowsTest} only checks that this flow parses.
 */
public class ForeignCallTest {

    @Test
    public void transpile_triggersCallsFlow_generatesActionCallAndCaptureVariable() throws Exception {
        String source = Files.readString(Paths.get("target/test-classes/pass", "triggers_calls.txt"));

        TranspilationResult result = Transpiler.transpile("flow", source);
        assertNotNull(result);
        String code = result.getCode();
        assertNotNull(code);

        assertTrue(code.contains("_actionCall("), "Call should generate an action call");
        assertTrue(code.contains("\"java.lang.Math\""), "static call target class should appear");
        assertTrue(code.contains("\"incrementExact\""), "static call method name should appear");
        assertTrue(code.contains("var E"), "exception-capture variable should be declared");
    }
}
