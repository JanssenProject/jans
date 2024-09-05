package io.jans.as.server.jwk.ws.rs;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;

/**
 * @author Yuriy Z
 */
@Path("/")
public class ArchivedJwksWebServiceImpl {

    @Inject
    private Logger log;

    @Inject
    private ArchivedJwksService archivedJwksService;

    @GET
    @Path("/jwks/archived")
    @Produces({MediaType.APPLICATION_JSON})
    public Response requestArchivedKid(@QueryParam("kid") String kid) {
        return archivedJwksService.requestArchivedKid(kid);
    }
}
