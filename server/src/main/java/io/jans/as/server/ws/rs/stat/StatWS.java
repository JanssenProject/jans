package io.jans.as.server.ws.rs.stat;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.stat.StatService;
import io.jans.orm.PersistenceEntryManager;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@Path("/stat")
public class StatWS {

    private static final int DEFAULT_WS_INTERVAL_LIMIT_IN_SECONDS = 60;

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private Identity identity;

    @Inject
    private StatService statService;

    @Inject
    private AppConfiguration appConfiguration;

    private long lastProcessedAt;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response statGet(@HeaderParam("Authorization") String authorization, @QueryParam("month") String month) {
        return stat(month);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response statPost(@HeaderParam("Authorization") String authorization, @FormParam("month") String month) {
        return stat(month);
    }

    public Response stat(String month) {
        return null;
    }
}
