package org.gluu.oxauthconfigapi.rest.ressource;

import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxauthconfigapi.util.Jackson;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthConfigurationResource extends BaseResource {

	@Inject
	Logger log;

	@Inject
	JsonConfigurationService jsonConfigurationService;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getAppConfiguration() {
		try {
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			JSONObject json = new JSONObject(appConfiguration);
			return Response.ok(json).build();

		} catch (Exception ex) {
			log.error("Failed to retrieve Auth application configuration.", ex);
			return getInternalServerError(ex);
		}
	}

    @PATCH
    //@Consumes({MediaType.APPLICATION_JSON_PATCH_JSON, MediaType.APPLICATION_JSON})
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = {WRITE_ACCESS})
    public Response patchAppConfigurationProperty(@NotNull String requestString) {
        System.out.println("=======================================================================");
        System.out.println("\n\n requestString = " + requestString + "\n\n");
        try {
            AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();

            final JSONObject jsonBefore = new JSONObject(appConfiguration);
            System.out.println("\n\n appConfiguration_before = " + jsonBefore + "\n\n");

            appConfiguration = Jackson.applyPatch(requestString, appConfiguration);

            JSONObject jsonAfter = new JSONObject(appConfiguration);
            System.out.println("\n\n appConfiguration_after = " + jsonAfter + "\n\n");
            System.out.println("=======================================================================");

            jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);

            return Response.ok(jsonAfter).build();
        } catch (Exception ex) {
            log.error("Failed to PATCH configuration.", ex);
            return getInternalServerError(ex);
        }
    }
}
