package org.gluu.configapi.rest.resource;

import org.gluu.config.oxtrust.DbApplicationConfiguration;
import org.gluu.configapi.filters.ProtectedApi;
import org.gluu.configapi.service.Fido2Service;
import org.gluu.configapi.util.ApiConstants;
import org.gluu.configapi.util.Jackson;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(ApiConstants.FIDO2 + ApiConstants.CONFIG)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class Fido2ConfigResource extends BaseResource {

    private static final String FIDO2_CONFIGURATION = "fido2Configuration";

    @Inject
    Fido2Service fido2Service;

    @GET
    @ProtectedApi(scopes = {READ_ACCESS})
    public Response getFido2Configuration() throws Exception{
        DbApplicationConfiguration dbApplicationConfiguration = this.fido2Service.find();
        return Response.ok(Jackson.asJsonNode(dbApplicationConfiguration.getDynamicConf())).build();
    }

    @PUT
    @ProtectedApi(scopes = {WRITE_ACCESS})
    public Response updateFido2Configuration(@NotNull String fido2ConfigJson) {
        checkResourceNotNull(fido2ConfigJson, FIDO2_CONFIGURATION);
         this.fido2Service.merge(fido2ConfigJson);
        return Response.ok(fido2ConfigJson).build();
    }
    
}