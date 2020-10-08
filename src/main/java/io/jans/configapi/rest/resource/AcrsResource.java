package io.jans.configapi.rest.resource;

import com.couchbase.client.core.message.ResponseStatus;
import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.rest.model.AuthenticationMethod;
import io.jans.configapi.service.ConfigurationService;
import io.jans.configapi.util.ApiConstants;
import org.oxauth.persistence.model.configuration.GluuConfiguration;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Puja Sharma
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.ACRS)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AcrsResource extends BaseResource {

    @Inject
    ConfigurationService configurationService;

    @GET
    @ProtectedApi(scopes = {READ_ACCESS})
    public Response getDefaultAuthenticationMethod() {
        final GluuConfiguration gluuConfiguration = configurationService.findGluuConfiguration();

        AuthenticationMethod authenticationMethod = new AuthenticationMethod();
        authenticationMethod.setDefaultAcr(gluuConfiguration.getAuthenticationMode());
        return Response.ok(authenticationMethod).build();
    }

    @PUT
    @ProtectedApi(scopes = {WRITE_ACCESS})
    public Response updateDefaultAuthenticationMethod(@Valid AuthenticationMethod authenticationMethod) {
        final GluuConfiguration gluuConfiguration = configurationService.findGluuConfiguration();
        gluuConfiguration.setAuthenticationMode(authenticationMethod.getDefaultAcr());
        configurationService.merge(gluuConfiguration);
        return Response.ok(ResponseStatus.SUCCESS).build();
    }

}