package io.jans.casa.rest.config;

import io.jans.casa.core.LogService;
import io.jans.casa.rest.ProtectedApi;

import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.OK;

@ApplicationScoped
@Path("/config/log-level")
@ProtectedApi( scopes = "https://jans.io/casa.config" )
@Deprecated
public class LogLevelWS extends BaseWS {
    
    @Inject
    private Logger logger;
    
    @Inject
    private LogService logService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response get() {
        return Response.status(OK).entity(mainSettings.getLogLevel()).build();
    }
    
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response set(@FormParam("level") String newLevel) {

        Response.Status httpStatus;
        String json = null;
        String value = mainSettings.getLogLevel();
        logger.trace("LogLevelWS set operation called");
        
        try {
            if (!value.equals(newLevel)) {
                if (LogService.SLF4J_LEVELS.contains(newLevel)) {

                    logService.updateLoggingLevel(newLevel);
                    mainSettings.setLogLevel(newLevel);
                    
                    logger.trace("Persisting update in configuration");
                    confHandler.saveSettings();
                    httpStatus = OK;
                    
                } else {
                    httpStatus = BAD_REQUEST;
                    json = String.format("Log level '%s' not recognized", newLevel);
                    logger.warn(json);
                }
            } else {
                httpStatus = OK;
            }

        } catch (Exception e) {
            json = e.getMessage();
            logger.error(json, e);
            
            mainSettings.setLogLevel(value);
            httpStatus = INTERNAL_SERVER_ERROR;
        }
        
        return Response.status(httpStatus).entity(json).build();
        
    }
    
}
    