package io.jans.configapi.plugin.scim.rest;

import static io.jans.as.model.util.Util.escapeLog;

import io.jans.as.model.config.Conf;
import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.plugin.scim.service.ScimConfigService;
import io.jans.configapi.plugin.scim.model.config.AppConfiguration;
import io.jans.configapi.plugin.scim.model.config.ScimConfigurationEntry;
//import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.plugin.scim.util.Constants;

import io.jans.scim.model.scim2.SearchRequest;
import io.jans.scim.model.scim2.patch.PatchRequest;
import io.jans.scim.model.scim2.user.UserResource;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


@Path(Constants.CONFIG)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ScimConfigResource {

    public static final String SEARCH_SUFFIX = ".search";
    @Inject
    Logger log;

    @Inject
    ScimConfigService scimConfigService;

    @GET
    //@ProtectedApi(scopes = { ApiAccessConstants.JANS_AUTH_CONFIG_READ_ACCESS })
    public Response getAppConfiguration() {
        AppConfiguration appConfiguration = scimConfigService.getConfiguration();
        log.error("SCIM appConfiguration:{}", appConfiguration);
        return Response.ok(appConfiguration).build();
    }
    
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
   // @ProtectedApi(scopes = { ApiAccessConstants.JANS_AUTH_CONFIG_WRITE_ACCESS })
    public Response patchAppConfigurationProperty(@NotNull String requestString) throws Exception {
        log.error("AUTH CONF details to patch - requestString:{}", requestString);
        ScimConfigurationEntry conf = scimConfigService.findConf();
        AppConfiguration appConfiguration = scimConfigService.find();
        log.error("AUTH CONF details BEFORE patch - conf:{}, appConfiguration:{}", conf, appConfiguration);
        appConfiguration = Jackson.applyPatch(requestString, conf.getDynamicConf());
        log.error("AUTH CONF details BEFORE patch merge - appConfiguration:{}" ,appConfiguration);
        conf.setDynamicConf(appConfiguration);

        scimConfigService.merge(conf);
        appConfiguration = scimConfigService.find();
        log.error("AUTH CONF details AFTER patch merge - appConfiguration:{}", appConfiguration);
        return Response.ok(appConfiguration).build();
    }

    
}
