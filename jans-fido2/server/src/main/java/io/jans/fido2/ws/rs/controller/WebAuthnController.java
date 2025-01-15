package io.jans.fido2.ws.rs.controller;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.DataMapperService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

/**
 * The endpoint at which the requester can obtain FIDO2 WebAuthn Origins metadata
 * configuration
 *
 * @author Imran Ishaq Date: 11/28/2024
 */
@ApplicationScoped
@Path("/webauthn/configuration")
public class WebAuthnController {
    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private DataMapperService dataMapperService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;
    @GET
    @Produces({ "application/json" })
    public Response getConfiguration() {
        if (appConfiguration.getFido2Configuration() == null) {
            throw errorResponseFactory.forbiddenException();
        }

        ObjectNode response = dataMapperService.createObjectNode();

        ArrayNode originsArray = dataMapperService.createArrayNode();
        appConfiguration.getFido2Configuration().getRequestedParties().forEach(rp -> {
            rp.getOrigins().forEach(originsArray::add);
        });
        response.set("origins", originsArray);

        Response.ResponseBuilder builder = Response.ok().entity(response.toString());
        return builder.build();
    }
}
