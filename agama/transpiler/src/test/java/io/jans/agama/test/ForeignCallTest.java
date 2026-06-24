package io.jans.agama.test;

import io.jans.agama.dsl.TranspilationResult;
import io.jans.agama.dsl.Transpiler;

import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Code-generation conformance for foreign calls (`Call`) and their exception-capture
 * form, per the "Foreign routines" section of docs/agama/language-reference.md. Asserts
 * that a static `Call` generates an action call with the target class/method, and that
 * the `result | error = Call ...` form declares the capture variable.
 */
public class ForeignCallTest {

    @Test
    public void transpile_foreignCallAndExceptionCapture_generateExpectedJs() throws Exception {
        String source = String.join("\n",
                "Flow com.acme.calls",
                "    Basepath \"\"",
                "x256 = Call java.lang.Math#incrementExact 255",
                "n | E = Call java.lang.Integer#parseInt \"AGA\" 16",
                "Finish true",
                "");

        TranspilationResult result = Transpiler.transpile("com.acme.calls", source);
        assertNotNull(result);
        String code = result.getCode();
        assertNotNull(code);

        assertTrue(code.contains("_actionCall("), "Call should generate an action call");
        assertTrue(code.contains("\"java.lang.Math\""), "static call target class should appear");
        assertTrue(code.contains("\"incrementExact\""), "static call method name should appear");
        assertTrue(code.contains("var E"), "exception-capture variable should be declared");
    }
}
