/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external.context;

import io.jans.as.model.jwt.Jwt;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Yuriy Zabrovarnyy
 */
public class DynamicClientRegistrationContext extends ExternalScriptContext {

    private CustomScriptConfiguration script;
    private JSONObject registerRequest;
    private Jwt softwareStatement;

    public DynamicClientRegistrationContext(HttpServletRequest httpRequest, JSONObject registerRequest, CustomScriptConfiguration script) {
        super(httpRequest);
        this.script = script;
        this.registerRequest = registerRequest;
    }

    public Jwt getSoftwareStatement() {
        return softwareStatement;
    }

    public void setSoftwareStatement(Jwt softwareStatement) {
        this.softwareStatement = softwareStatement;
    }

    public CustomScriptConfiguration getScript() {
        return script;
    }

    public void setScript(CustomScriptConfiguration script) {
        this.script = script;
    }

    public JSONObject getRegisterRequest() {
        return registerRequest;
    }

    public void setRegisterRequest(JSONObject registerRequest) {
        this.registerRequest = registerRequest;
    }

    @Override
    public String toString() {
        return "DynamicClientRegistrationContext{" +
                "softwareStatement=" + softwareStatement +
                "registerRequest=" + registerRequest +
                "script=" + script +
                "} " + super.toString();
    }
}
