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

import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

public class ConfigAuthContext extends ExternalScriptContext {

    private CustomScriptConfiguration script;
    private ApiAppConfiguration apiAppConfiguration;
    private Map<String, Object> requestParameters;
    private final Map<String, SimpleCustomProperty> configurationAttributes;

    public ConfigAuthContext(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
            ApiAppConfiguration apiAppConfiguration, Map<String, Object> requestParameters,
            CustomScriptConfiguration script) {
        this(httpRequest, httpResponse, apiAppConfiguration, requestParameters, script, null);
    }

    public ConfigAuthContext(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
            ApiAppConfiguration apiAppConfiguration, Map<String, Object> requestParameters,
            CustomScriptConfiguration script, Map<String, SimpleCustomProperty> configurationAttributes) {
        super(httpRequest, httpResponse);
        this.apiAppConfiguration = apiAppConfiguration;
        this.requestParameters = requestParameters;
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

    public Map<String, Object> getRequestParameters() {
        return requestParameters;
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
        return "ConfigAuthContext [script=" + script + ", apiAppConfiguration=" + apiAppConfiguration
                + ", requestParameters=" + requestParameters + ", configurationAttributes=" + configurationAttributes
                + "]";
    }

}
