package io.jans.configapi.plugin.fido2.rest;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.fido2.service.Fido2RegistrationService;
import io.jans.configapi.plugin.fido2.util.Constants;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.orm.model.fido2.Fido2RegistrationEntry;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;

import java.util.List;

@Path(Constants.REGISTRATION)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class Fido2RegistrationResource extends BaseResource {

    @Inject
    Logger logger;

    @Inject
    Fido2RegistrationService fido2RegistrationService;

    @GET
    @Path(Constants.ENTRIES + ApiConstants.USERNAME_PATH)
    @ProtectedApi(scopes = {ApiAccessConstants.FIDO2_CONFIG_READ_ACCESS})
    public Response findAllRegisteredByUsername(@PathParam("username") @NotNull String username) {
        logger.debug("FIDO2 registration entries by username.");
        List<Fido2RegistrationEntry> entries = fido2RegistrationService.findAllRegisteredByUsername(username);
        return Response.ok(entries).build();
    }
}
