package io.jans.configapi.plugin.scim.rest;

import static io.jans.as.model.util.Util.escapeLog;

import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.plugin.scim.service.ScimConfigService;
import io.jans.configapi.plugin.scim.model.config.AppConfiguration;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.plugin.scim.util.Constants;

import io.jans.scim.model.scim2.SearchRequest;
import io.jans.scim.model.scim2.patch.PatchRequest;
import io.jans.scim.model.scim2.user.UserResource;
import org.slf4j.Logger;

import javax.inject.Inject;
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
        AppConfiguration configuration = scimConfigService.getConfiguration();
        log.error("SCIM configuration - " + configuration);
        return Response.ok(configuration).build();
        //log.error("SCIM configuration - ");
        //return Response.ok("OK_NEW").build();
    }

    
}
