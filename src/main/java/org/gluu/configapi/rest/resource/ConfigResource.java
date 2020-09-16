package org.gluu.configapi.rest.resource;

import org.gluu.configapi.filters.ProtectedApi;
import org.gluu.configapi.service.ConfigurationService;
import org.gluu.configapi.util.ApiConstants;
import org.gluu.configapi.util.Jackson;
import org.gluu.oxauth.model.config.Conf;
import org.gluu.oxauth.model.configuration.AppConfiguration;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.OXAUTH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConfigResource extends BaseResource {

    @Inject
    ConfigurationService configurationService;

    @GET
    @ProtectedApi(scopes = { READ_ACCESS })
    public Response getAppConfiguration() {
        AppConfiguration appConfiguration = configurationService.find();
        return Response.ok(appConfiguration).build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { WRITE_ACCESS })
    public Response patchAppConfigurationProperty(@NotNull String requestString) throws Exception {
        Conf conf = configurationService.findForAppConfigurationOnly();
        AppConfiguration appConfiguration = Jackson.applyPatch(requestString, conf.getDynamic());
        conf.setDynamic(appConfiguration);

        configurationService.merge(conf);
        return Response.ok(appConfiguration).build();
    }
}
