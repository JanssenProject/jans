/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.uma;

import com.google.common.base.Preconditions;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.InputStream;

/**
 * @author yuriyz
 */
public class JsonLogic {

    private static final Logger LOG = Logger.getLogger(JsonLogic.class);

    private static final JsonLogic INSTANCE = new JsonLogic();

    private final ScriptEngine engine;

    private JsonLogic() {
        ScriptEngineManager factory = new ScriptEngineManager();
        engine = factory.getEngineByName("nashorn");

        Preconditions.checkNotNull(engine);

        loadScript("json_logic.js");
    }

    private void loadScript(String scriptName) {
        Preconditions.checkState(StringUtils.isNotBlank(scriptName));

        InputStream stream = getClass().getClassLoader().getResourceAsStream(scriptName);
        Preconditions.checkNotNull(stream);

        try {
            String script = IOUtils.toString(stream);
            engine.eval(script);
            LOG.trace("Loaded script, name: " + scriptName);
        } catch (Exception e) {
            LOG.error("Failed to load JavaScript script, name: " + scriptName, e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    public static JsonLogic getInstance() {
        return INSTANCE;
    }

    public ScriptEngine getEngine() {
        return engine;
    }

    public Invocable getInvocable() {
        return (Invocable) engine;
    }

    public static Object eval(String script) throws ScriptException {
        return getInstance().getEngine().eval(script);
    }

    public static Object invokeFunction(String name, Object... args) throws ScriptException, NoSuchMethodException {
        return getInstance().getInvocable().invokeFunction(name, args);
    }

    public static boolean apply(String rule) throws ScriptException {
        return applyObject(rule).equals(Boolean.TRUE);
    }

    public static boolean apply(String rule, String data) throws ScriptException {
        return applyObject(rule, data).equals(Boolean.TRUE);
    }

    public static Object applyObject(String rule) throws ScriptException {
        return eval("jsonLogic.apply( " + rule + " );");
    }

    public static Object applyObject(String rule, String data) throws ScriptException {
        return eval("jsonLogic.apply( " + rule + ", " + data + " );");
    }
}
