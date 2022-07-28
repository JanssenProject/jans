package io.jans.configapi.plugin.scim.rest;

import com.github.fge.jsonpatch.JsonPatchException;

import io.jans.scim.model.conf.Conf;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.scim.service.ScimConfigService;
import io.jans.scim.model.conf.AppConfiguration;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.plugin.scim.util.Constants;
import io.jans.configapi.util.ApiAccessConstants;

import java.io.IOException;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;

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
        AppConfiguration appConfiguration = scimConfigService.find();
        log.debug("SCIM appConfiguration:{}", appConfiguration);
        return Response.ok(appConfiguration).build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_AUTH_CONFIG_WRITE_ACCESS })
    public Response patchAppConfigurationProperty(@NotNull String requestString) throws JsonPatchException , IOException{
        log.debug("AUTH CONF details to patch - requestString:{} ", requestString);
       
        Conf conf = scimConfigService.findConf();
        AppConfiguration appConfiguration = scimConfigService.find();
        log.debug("AUTH CONF details BEFORE patch - appConfiguration :{}", appConfiguration);
        appConfiguration = Jackson.applyPatch(requestString, conf.getDynamicConf());
        log.debug("AUTH CONF details BEFORE patch merge - appConfiguration:{}", appConfiguration);
        conf.setDynamicConf(appConfiguration);
        
        
        scimConfigService.merge(conf);
        appConfiguration = scimConfigService.find();
        log.debug("AUTH CONF details AFTER patch merge - appConfiguration:{}", appConfiguration);
        return Response.ok(appConfiguration).build();
    }
    /*
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { "https://jans.io/scim/config.write" })
    public Response patchAppConfigurationProperty(@NotNull String requestString) throws IOException,JsonPatchException {
        log.debug("AUTH CONF details to patch - requestString:{}", requestString);
        AppConfiguration appConfiguration = scimConfigService.find();
        //AppConfiguration appConfiguration = conf.getDynamicConf();
        log.trace("AUTH CONF details BEFORE patch - appConfiguration:{}", appConfiguration);
        
        appConfiguration = Jackson.applyPatch(requestString, appConfiguration);
        log.trace("AUTH CONF details BEFORE patch merge - appConfiguration:{}" ,appConfiguration);
        //conf.setDynamicConf(appConfiguration);

        scimConfigService.merge(conf);
        appConfiguration = scimConfigService.find();
        log.debug("AUTH CONF details AFTER patch merge - appConfiguration:{}", appConfiguration);
        return Response.ok(appConfiguration).build();
    }
*/
    
}
