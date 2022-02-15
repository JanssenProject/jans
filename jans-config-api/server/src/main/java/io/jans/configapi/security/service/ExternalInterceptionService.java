/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.security.service;

import io.jans.configapi.external.service.ExternalConfigService;
import io.jans.configapi.model.configuration.ApiAppConfiguration;
import io.jans.configapi.util.AuthUtil;

import java.util.Map;
import java.io.Serializable;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.inject.Inject;

import org.json.JSONObject;
import org.slf4j.Logger;

@ApplicationScoped
@Named("interceptionService")
public class ExternalInterceptionService implements Serializable {

    private static final long serialVersionUID = 4564959567069741194L;

    @Inject
    Logger log;

    @Inject
    ExternalConfigService externalConfigService;

    @Inject
    AuthUtil AuthUtil;

    public boolean authorization(HttpServletRequest request, HttpServletResponse response,
            ApiAppConfiguration apiAppConfiguration, Map<String, Object> requestParameters, JSONObject responseAsJsonObject)
            throws Exception {
        log.error(
                "External Interception Service - Authorization script params -  request:{}, response:{}, apiAppConfiguration:{}, requestParameters:{}, responseAsJsonObject:{}, externalConfigService{} ",
                request, response, apiAppConfiguration, requestParameters, responseAsJsonObject, externalConfigService);
        log.error("External Interception Service - externalConfigService.isEnabled():{}",
                externalConfigService.isEnabled());
        if (externalConfigService.isEnabled()) {
            return externalConfigService.checkAuthorization(request, response, apiAppConfiguration, requestParameters,
                    responseAsJsonObject);
        }

        return true;
    }

}
