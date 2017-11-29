package org.xdi.oxauth.model.uma;

import org.testng.Assert;
import org.testng.annotations.Test;

import javax.script.ScriptException;

/**
 * @author yuriyz
 */
public class JsonLogicTest {

    @Test
    public void testJsEngine() throws ScriptException, NoSuchMethodException {
        JsonLogic.eval("var fun1 = function(name) {\n" +
                "    print('Hi there from Javascript, ' + name);\n" +
                "    return \"greetings from javascript\";\n" +
                "};");
        Object result = JsonLogic.invokeFunction("fun1", "");
        Assert.assertEquals(result, "greetings from javascript");
    }

    @Test
    public void testJsonLogic() throws ScriptException, NoSuchMethodException {
        assertTrue("jsonLogic.apply( { \"==\" : [1, 1] } );");
        assertFalse("jsonLogic.apply( { \"==\" : [1, 0] } );");

        assertTrue("jsonLogic.apply(\n" +
                "  {\"and\" : [\n" +
                "    { \">\" : [3,1] },\n" +
                "    { \"<\" : [1,3] }\n" +
                "  ] }\n" +
                ");");
    }

    private static void assertResult(String script, Boolean expectedResult) throws ScriptException {
        Assert.assertEquals(JsonLogic.eval(script), expectedResult);
    }

    private static void assertTrue(String script) throws ScriptException {
        assertResult(script, Boolean.TRUE);
    }

    private static void assertFalse(String script) throws ScriptException {
        assertResult(script, Boolean.FALSE);
    }
}
