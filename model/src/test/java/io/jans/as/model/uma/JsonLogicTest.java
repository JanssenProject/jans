/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */
package io.jans.as.model.uma;

import org.testng.Assert;
import org.testng.annotations.Test;

import javax.script.ScriptException;

/**
 * @author yuriyz
 */
public class JsonLogicTest {

    private static void assertResult(String script, Boolean expectedResult) throws ScriptException {
        Assert.assertEquals(JsonLogic.eval(script), expectedResult);
    }

    private static void assertTrue(String script) throws ScriptException {
        assertResult(script, Boolean.TRUE);
    }

    private static void assertFalse(String script) throws ScriptException {
        assertResult(script, Boolean.FALSE);
    }

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

        Assert.assertTrue(JsonLogic.apply("{ \"==\" : [1, 1] }"));
        Assert.assertFalse(JsonLogic.apply("{ \"==\" : [1, 0] }"));

        assertTrue("jsonLogic.apply(\n" +
                "  {\"and\" : [\n" +
                "    { \">\" : [3,1] },\n" +
                "    { \"<\" : [1,3] }\n" +
                "  ] }\n" +
                ");");
    }

    @Test
    public void umaSimulation() throws ScriptException {
        String rule = "{" +
                "    \"and\": [ {" +
                "        \"or\": [" +
                "          {\"var\": 0 }," +
                "          {\"var\": 1 }" +
                "        ]" +
                "      }," +
                "      {\"var\": 2 }" +
                "    ]}";
        Assert.assertTrue(JsonLogic.apply(rule, "[true, true, true]"));
        Assert.assertTrue(JsonLogic.apply(rule, "[true, false, true]"));
        Assert.assertTrue(JsonLogic.apply(rule, "[false, true, true]"));

        Assert.assertFalse(JsonLogic.apply(rule, "[false, false, false]"));
        Assert.assertFalse(JsonLogic.apply(rule, "[false, false, true]"));
        Assert.assertFalse(JsonLogic.apply(rule, "[true, true, false]"));
    }
}
