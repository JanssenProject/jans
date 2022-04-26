/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import io.jans.as.model.config.Conf;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.core.util.Jackson;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.json.JSONObject;
import org.slf4j.Logger;

@Path(ApiConstants.JANS_AUTH + ApiConstants.CONFIG)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConfigResource extends ConfigBaseResource {

    @Inject
    Logger log;

    @Inject
    ConfigurationService configurationService;

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_AUTH_CONFIG_READ_ACCESS })
    public Response getAppConfiguration() {
        AppConfiguration appConfiguration = configurationService.find();
        log.debug("ConfigResource::getAppConfiguration() appConfiguration - " + appConfiguration);
        return Response.ok(appConfiguration).build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_AUTH_CONFIG_WRITE_ACCESS })
    public Response patchAppConfigurationProperty(@NotNull String requestString) throws Exception {
        log.debug("AUTH CONF details to patch - requestString = " + requestString);
        Conf conf = configurationService.findConf();
        AppConfiguration appConfiguration = configurationService.find();
        log.debug("AUTH CONF details BEFORE patch - appConfiguration = " + appConfiguration);
        appConfiguration = Jackson.applyPatch(requestString, conf.getDynamic());
        log.debug("AUTH CONF details BEFORE patch merge - appConfiguration = " + appConfiguration);
        conf.setDynamic(appConfiguration);

        configurationService.merge(conf);
        appConfiguration = configurationService.find();
        log.debug("AUTH CONF details AFTER patch merge - appConfiguration = " + appConfiguration);
        return Response.ok(appConfiguration).build();
    }

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_AUTH_CONFIG_READ_ACCESS })
    @Path(ApiConstants.PERSISTENCE)
    public Response getPersistenceDetails() {
        String persistenceType = configurationService.getPersistenceType();
        log.debug("ConfigResource::getPersistenceDetails() - persistenceType - " + persistenceType);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("persistenceType", persistenceType);
        log.debug("\n\n\n ConfigResource::getPersistenceDetails() - jsonObject = " + jsonObject + "\n\n");
        return Response.ok(jsonObject.toString()).build();
    }

}
