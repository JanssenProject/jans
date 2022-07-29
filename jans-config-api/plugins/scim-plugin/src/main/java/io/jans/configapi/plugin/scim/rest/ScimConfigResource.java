package io.jans.configapi.plugin.scim.rest;

import com.github.fge.jsonpatch.JsonPatchException;

import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.scim.service.ScimConfigService;
import io.jans.configapi.plugin.scim.model.config.ScimAppConfiguration;
import io.jans.configapi.plugin.scim.model.config.ScimConf;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.plugin.scim.util.Constants;

import org.slf4j.Logger;

import java.io.IOException;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path(Constants.SCIM_CONFIG)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ScimConfigResource {

    @Inject
    Logger log;

    @Inject
    ScimConfigService scimConfigService;

    @GET
    @ProtectedApi(scopes = { "https://jans.io/scim/config.readonly" })
    public Response getAppConfiguration() {
        ScimAppConfiguration appConfiguration = scimConfigService.find();
        log.debug("SCIM appConfiguration:{}", appConfiguration);
        return Response.ok(appConfiguration).build();
    }
    
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { "https://jans.io/scim/config.write" })
    public Response patchAppConfigurationProperty(@NotNull String requestString) throws IOException,JsonPatchException {
        log.debug("AUTH CONF details to patch - requestString:{}", requestString);
        ScimConf conf = scimConfigService.findConf();
        ScimAppConfiguration appConfiguration = conf.getDynamicConf();
        log.trace("AUTH CONF details BEFORE patch - conf:{}, appConfiguration:{}", conf, appConfiguration);
        
        appConfiguration = Jackson.applyPatch(requestString, appConfiguration);
        log.trace("AUTH CONF details BEFORE patch merge - appConfiguration:{}" ,appConfiguration);
        conf.setDynamicConf(appConfiguration);

        scimConfigService.merge(conf);
        appConfiguration = scimConfigService.find();
        log.debug("AUTH CONF details AFTER patch merge - appConfiguration:{}", appConfiguration);
        return Response.ok(appConfiguration).build();
    }

    
}
