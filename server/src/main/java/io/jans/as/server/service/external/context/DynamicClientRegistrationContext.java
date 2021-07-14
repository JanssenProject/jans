/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external.context;

import io.jans.as.client.RegisterRequest;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.jwt.Jwt;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 */
public class DynamicClientRegistrationContext extends ExternalScriptContext {

    private CustomScriptConfiguration script;
    private JSONObject registerRequestJson;
    private RegisterRequest registerRequest;
    private Jwt softwareStatement;
    private Jwt dcr;
    private Client client;

    public DynamicClientRegistrationContext(HttpServletRequest httpRequest, JSONObject registerRequest, CustomScriptConfiguration script) {
        this(httpRequest, registerRequest, script, null);
    }

    public DynamicClientRegistrationContext(HttpServletRequest httpRequest, JSONObject registerRequest, CustomScriptConfiguration script, Client client) {
        super(httpRequest);
        this.script = script;
        this.registerRequestJson = registerRequest;
        this.client = client;
    }

    public Jwt getDcr() {
        return dcr;
    }

    public void setDcr(Jwt dcr) {
        this.dcr = dcr;
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

    public JSONObject getRegisterRequestJson() {
        return registerRequestJson;
    }

    public void setRegisterRequestJson(JSONObject registerRequestJson) {
        this.registerRequestJson = registerRequestJson;
    }

    public RegisterRequest getRegisterRequest() {
        return registerRequest;
    }

    public void setRegisterRequest(RegisterRequest registerRequest) {
        this.registerRequest = registerRequest;
    }

    public Map<String, SimpleCustomProperty> getConfigurationAttibutes() {
        final Map<String, SimpleCustomProperty> attrs = script.getConfigurationAttributes();

        if (httpRequest != null) {
            final String cert = httpRequest.getHeader("X-ClientCert");
            if (StringUtils.isNotBlank(cert)) {
                SimpleCustomProperty certProperty = new SimpleCustomProperty();
                certProperty.setValue1(cert);
                attrs.put("certProperty", certProperty);
            }
        }
        return attrs != null ? new HashMap<>(attrs) : new HashMap<>();
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    @Override
    public String toString() {
        return "DynamicClientRegistrationContext{" +
                "softwareStatement=" + softwareStatement +
                "registerRequest=" + registerRequestJson +
                "script=" + script +
                "} " + super.toString();
    }
}
