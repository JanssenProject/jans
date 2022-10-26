/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.external.service;

import io.jans.configapi.external.context.ConfigAuthContext;
import io.jans.configapi.model.configuration.ApiAppConfiguration;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.type.configapi.ConfigApiType;
import io.jans.service.custom.script.ExternalScriptService;

import java.util.Map;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.slf4j.Logger;

@ApplicationScoped
public class ExternalConfigService extends ExternalScriptService {

    private static final long serialVersionUID = 1767751544454591666L;

    @Inject
    transient Logger logger;

    public ExternalConfigService() {
        super(CustomScriptType.CONFIG_API);
    }

    private void logAndSave(CustomScriptConfiguration customScriptConfiguration, Exception ex) {
        logger.error(ex.getMessage(), ex);
        saveScriptError(customScriptConfiguration.getCustomScript(), ex);
    }

    public boolean checkAuthorization(HttpServletRequest request, HttpServletResponse response,
            ApiAppConfiguration apiAppConfiguration, Map<String, Object> requestParameters,
            JSONObject responseAsJsonObject) {
        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeAuthorizeMethod(request, response, apiAppConfiguration, requestParameters, responseAsJsonObject,
                    customScriptConfiguration)) {
                return false;
            }
        }
        return true;
    }

    private boolean executeAuthorizeMethod(HttpServletRequest request, HttpServletResponse response,
            ApiAppConfiguration apiAppConfiguration, Map<String, Object> requestParameters,
            JSONObject responseAsJsonObject, CustomScriptConfiguration customScriptConfiguration) {
        boolean isAuthorized = false;
        logger.debug(
                "External Config Authorization script params -  request:{}, response:{}, apiAppConfiguration:{}, requestParameters:{}, responseAsJsonObject:{}, this.customScriptConfigurations:{} ",
                request, response, apiAppConfiguration, requestParameters, responseAsJsonObject,
                this.customScriptConfigurations);

        try {
            ConfigApiType externalType = (ConfigApiType) customScriptConfiguration.getExternalType();
            ConfigAuthContext context = new ConfigAuthContext(request, response, apiAppConfiguration, requestParameters,
                    customScriptConfiguration);
            return externalType.authorize(responseAsJsonObject, context);

        } catch (Exception ex) {
            logAndSave(customScriptConfiguration, ex);
            return isAuthorized;
        }
    }

}
