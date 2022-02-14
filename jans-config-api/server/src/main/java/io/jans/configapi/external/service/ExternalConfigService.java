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

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@ApplicationScoped
public class ExternalConfigService extends ExternalScriptService {

    private static final long serialVersionUID = 1767751544454591666L;

    Logger log = LogManager.getLogger(ExternalConfigService.class);

    public ExternalConfigService() {
        super(CustomScriptType.CONFIG_API);
    }

    private CustomScriptConfiguration findConfigWithGEVersion(int version) {
        return customScriptConfigurations.stream().filter(sc -> executeExternalGetApiVersion(sc) >= version).findFirst()
                .orElse(null);
    }

    private void logAndSave(CustomScriptConfiguration customScriptConfiguration, Exception e) {
        log.error(e.getMessage(), e);
        saveScriptError(customScriptConfiguration.getCustomScript(), e);
    }

    public boolean checkAuthorization(HttpServletRequest request, HttpServletResponse response,
            ApiAppConfiguration apiAppConfiguration, String token, String issuer, String method, String path) {
        log.debug(
                "External Config Authorization script params -  request:{}, response:{}, apiAppConfiguration:{}, token:{}, issuer:{}, method:{}, path:{}, this.customScriptConfigurations:{} ",
                request, response, apiAppConfiguration, token, issuer, method, path, this.customScriptConfigurations);
        boolean result = true;
        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            ConfigApiType externalType = (ConfigApiType) customScriptConfiguration.getExternalType();
            ConfigAuthContext context = new ConfigAuthContext(request, response, apiAppConfiguration, token, issuer,
                    method, path, customScriptConfiguration);
            result &= externalType.authorize(context);
            if (!result) {
                return result;
            }

        }
        return result;
    }

}
