/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.external.context;

import io.jans.configapi.model.configuration.ApiAppConfiguration;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.service.external.context.ExternalScriptContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class ConfigAuthContext extends ExternalScriptContext {

    private static final Logger log = LoggerFactory.getLogger(ConfigAuthContext.class);

    private CustomScriptConfiguration script;
    private ApiAppConfiguration apiAppConfiguration;
    private String token;
    private String issuer;
    private String method;
    private String path;
    private final Map<String, SimpleCustomProperty> configurationAttributes;

    public ConfigAuthContext(HttpServletRequest httpRequest, HttpServletResponse httpResponse, ApiAppConfiguration apiAppConfiguration, String token,
            String issuer, String method, String path, CustomScriptConfiguration script) {
        this(httpRequest, httpResponse, apiAppConfiguration, token, issuer, method, path, script, null);
    }

    public ConfigAuthContext(HttpServletRequest httpRequest, HttpServletResponse httpResponse, ApiAppConfiguration apiAppConfiguration, String token,
            String issuer, String method, String path, CustomScriptConfiguration script,
            Map<String, SimpleCustomProperty> configurationAttributes) {
        super(httpRequest,httpResponse);
        this.apiAppConfiguration = apiAppConfiguration;
        this.token = token;
        this.issuer = issuer;
        this.method = method;
        this.path = path;
        this.script = script;
        this.configurationAttributes = configurationAttributes;
    }

    public CustomScriptConfiguration getScript() {
        return script;
    }

    public void setScript(CustomScriptConfiguration script) {
        this.script = script;
    }

    public ApiAppConfiguration getApiAppConfiguration() {
        return apiAppConfiguration;
    }

    public void setApiAppConfiguration(ApiAppConfiguration apiAppConfiguration) {
        this.apiAppConfiguration = apiAppConfiguration;
    }
    
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, SimpleCustomProperty> getConfigurationAttributes() {
        return configurationAttributes;
    }

    public Map<String, SimpleCustomProperty> getConfigurationAttibutes() {
        final Map<String, SimpleCustomProperty> attrs = script.getConfigurationAttributes();

        if (httpRequest != null) {
            final String issuer = httpRequest.getHeader(ApiConstants.ISSUER);
            if (StringUtils.isNotBlank(issuer)) {
                SimpleCustomProperty issuerProperty = new SimpleCustomProperty();
                issuerProperty.setValue1(issuer);
                attrs.put(ApiConstants.ISSUER, issuerProperty);
            }
        }
        return attrs != null ? new HashMap<>(attrs) : new HashMap<>();
    }

    @Override
    public String toString() {
        return "ConfigAuthContext [script=" + script
                +" , apiAppConfiguration=" + apiAppConfiguration
                + ", token="
                + token + ", issuer=" + issuer + ", method=" + method + ", path="
                + path + ", configurationAttributes=" + configurationAttributes + "]";
    }

}
