package org.xdi.oxauth.dev;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * @author yuriyz
 */
public class JsonLogicTest {

    @BeforeClass
    public void beforeClass() {
    }

    @Test
    public void testJsEngine() throws ScriptException, NoSuchMethodException {
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("nashorn");
        engine.eval("var fun1 = function(name) {\n" +
                "    print('Hi there from Javascript, ' + name);\n" +
                "    return \"greetings from javascript\";\n" +
                "};");
        Invocable invocable = (Invocable) engine;
        Object result = invocable.invokeFunction("fun1", "");
        Assert.assertEquals(result, "greetings from javascript");
    }
}
