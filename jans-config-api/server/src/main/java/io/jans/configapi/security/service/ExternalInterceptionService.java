/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.security.service;

import io.jans.configapi.external.service.ExternalConfigService;
import io.jans.configapi.model.configuration.ApiAppConfiguration;

import java.util.Map;
import java.io.Serializable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.inject.Inject;

import org.json.JSONObject;
import org.slf4j.Logger;

@ApplicationScoped
@Named("interceptionService")
public class ExternalInterceptionService implements Serializable {

    private static final long serialVersionUID = 4564959567069741194L;

    @Inject
    transient Logger log;

    @Inject
    ExternalConfigService externalConfigService;

    public boolean authorization(HttpServletRequest request, HttpServletResponse response,
            ApiAppConfiguration apiAppConfiguration, Map<String, Object> requestParameters,
            JSONObject responseAsJsonObject) {
        log.debug(
                "External Interception Service - Authorization script params -  request:{}, response:{}, apiAppConfiguration:{}, requestParameters:{}, responseAsJsonObject:{}, externalConfigService{}, externalConfigService.isEnabled():{} ",
                request, response, apiAppConfiguration, requestParameters, responseAsJsonObject, externalConfigService,
                externalConfigService.isEnabled());
        if (externalConfigService.isEnabled()) {
            return externalConfigService.checkAuthorization(request, response, apiAppConfiguration, requestParameters,
                    responseAsJsonObject);
        }

        return true;
    }

}
