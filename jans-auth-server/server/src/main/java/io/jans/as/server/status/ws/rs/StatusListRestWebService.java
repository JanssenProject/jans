package io.jans.as.server.status.ws.rs;

import io.jans.as.server.service.token.StatusListService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;

import static io.jans.as.model.config.Constants.CONTENT_TYPE_STATUSLIST_JSON;
import static io.jans.as.model.config.Constants.CONTENT_TYPE_STATUSLIST_JWT;

/**
 * @author Yuriy Z
 */
@Path("/")
public class StatusListRestWebService {

    @Inject
    private Logger log;

    @Inject
    private StatusListService statusService;

    @GET
    @Path("/status_list")
    @Consumes({CONTENT_TYPE_STATUSLIST_JSON, CONTENT_TYPE_STATUSLIST_JWT})
    @Produces({CONTENT_TYPE_STATUSLIST_JSON, CONTENT_TYPE_STATUSLIST_JWT})
    public Response requestStatusList(@HeaderParam("Accept") String acceptHeader) {
        try {
            return statusService.requestStatusList(acceptHeader);
        } catch (WebApplicationException e) {
            log.debug(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
