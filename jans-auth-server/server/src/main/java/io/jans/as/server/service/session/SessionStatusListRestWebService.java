package io.jans.as.server.service.session;

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
public class SessionStatusListRestWebService {

    @Inject
    private Logger log;

    @Inject
    private SessionStatusListService sessionStatusListService;

    @GET
    @Path("/session_status_list")
    @Consumes({CONTENT_TYPE_STATUSLIST_JSON, CONTENT_TYPE_STATUSLIST_JWT})
    @Produces({CONTENT_TYPE_STATUSLIST_JSON, CONTENT_TYPE_STATUSLIST_JWT})
    public Response requestStatusList(@HeaderParam("Accept") String acceptHeader) {
        try {
            return sessionStatusListService.requestStatusList(acceptHeader);
        } catch (WebApplicationException e) {
            log.debug(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
