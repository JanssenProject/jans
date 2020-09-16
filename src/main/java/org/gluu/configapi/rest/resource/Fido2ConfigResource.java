package org.gluu.configapi.rest.resource;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.gluu.config.oxtrust.DbApplicationConfiguration;
import org.gluu.configapi.filters.ProtectedApi;
import org.gluu.configapi.rest.model.Fido2Configuration;
import org.gluu.configapi.service.JsonConfigurationService;
import org.gluu.configapi.util.ApiConstants;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(ApiConstants.FIDO2 + ApiConstants.CONFIG)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class Fido2ConfigResource extends BaseResource {

    /**
     * 
     */
    private static final String FIDO2_CONFIGURATION = "fido2Configuration";

    @Inject
    JsonConfigurationService jsonConfigurationService;

    @GET
    @ProtectedApi(scopes = { READ_ACCESS })
    public Response getFido2Configuration() {
        Fido2Configuration fido2Configuration = new Fido2Configuration();
        String fido2ConfigJson = null;
        DbApplicationConfiguration dbApplicationConfiguration = this.jsonConfigurationService.loadFido2Configuration();
        if (dbApplicationConfiguration != null) {
            fido2ConfigJson = dbApplicationConfiguration.getDynamicConf();
            Gson gson = new Gson();
            JsonElement jsonElement = gson.fromJson(fido2ConfigJson, JsonElement.class);
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonElement fido2ConfigurationElement = jsonObject.get(FIDO2_CONFIGURATION);
            fido2Configuration = gson.fromJson(fido2ConfigurationElement, Fido2Configuration.class);
        }
        return Response.ok(fido2Configuration).build();
    }

    @PUT
    @ProtectedApi(scopes = { WRITE_ACCESS })
    public Response updateFido2Configuration(@Valid Fido2Configuration fido2Configuration) {
        DbApplicationConfiguration dbApplicationConfiguration = this.jsonConfigurationService.loadFido2Configuration();
        if (dbApplicationConfiguration != null) {
            String fido2ConfigJson = dbApplicationConfiguration.getDynamicConf();
            Gson gson = new Gson();
            JsonElement jsonElement = gson.fromJson(fido2ConfigJson, JsonElement.class);
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonElement updatedElement = gson.toJsonTree(fido2Configuration);
            jsonObject.add(FIDO2_CONFIGURATION, updatedElement);
            this.jsonConfigurationService.saveFido2Configuration(jsonObject.toString());
        }
        return Response.ok(fido2Configuration).build();
    }

}