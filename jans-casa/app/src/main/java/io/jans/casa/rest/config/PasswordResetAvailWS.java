package io.jans.casa.rest.config;

import io.jans.casa.core.PersistenceService;
import io.jans.casa.rest.ProtectedApi;

import org.slf4j.Logger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.OK;

@ApplicationScoped
@Path("/config/pwd-reset")
@ProtectedApi( scopes = "https://jans.io/casa.config" )
@Deprecated
public class PasswordResetAvailWS extends BaseWS {

    @Inject
    private Logger logger;

    @Inject
    private PersistenceService persistenceService;

    @GET
    @Path("enabled")
    @Produces(MediaType.APPLICATION_JSON)
    public Response isEnabled() {

        Response.Status httpStatus;
        String json;
        
        logger.trace("PasswordResetAvailWS isEnabled operation called");
        try {
            json = Boolean.toString(mainSettings.isEnablePassReset());
            httpStatus = OK;
        } catch (Exception e) {
            json = e.getMessage();
            logger.error(json, e);
            httpStatus = INTERNAL_SERVER_ERROR;
        }
        return Response.status(httpStatus).entity(json).build();

    }

    @POST
    @Path("turn-on")
    @Produces(MediaType.TEXT_PLAIN)
    public Response enable() {
        logger.trace("PasswordResetAvailWS enable operation called");
        return turnOnOff(true);
    }
    
    @POST
    @Path("turn-off")
    @Produces(MediaType.TEXT_PLAIN)
    public Response disable() {
        logger.trace("PasswordResetAvailWS disable operation called");
        return turnOnOff(false);
    }

    private Response turnOnOff(boolean flag) {

        Response.Status httpStatus;
        String json = null;
        boolean value = mainSettings.isEnablePassReset();
        
        try {
            if (value != flag) {
                mainSettings.setEnablePassReset(flag);
                logger.trace("Persisting configuration change");
                confHandler.saveSettings();
            }
            httpStatus = OK;
        } catch (Exception e) {
            json = e.getMessage();
            logger.error(json, e);
            
            //Restore previous value
            mainSettings.setEnablePassReset(value);
            httpStatus = INTERNAL_SERVER_ERROR;
        }
        return Response.status(httpStatus).entity(json).build();  

    }
    
}
