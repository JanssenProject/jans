package io.jans.as.server.revoke;

import io.jans.as.server.service.token.GlobalTokenRevocationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;

/**
 * @author Yuriy Z
 */
@Path("/")
public class GlobalTokenRevocationRestWebService {

    @Inject
    private Logger log;

    @Inject
    private GlobalTokenRevocationService globalTokenRevocationService;

    @POST
    @Path("/global-token-revocation")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response requestGlobalTokenRevocation(String requestAsString) {
        try {
            globalTokenRevocationService.requestGlobalTokenRevocation(requestAsString);
            return Response.noContent().build();
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Response.status(500).build();
        }
    }
}
