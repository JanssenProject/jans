package io.jans.casa.rest.config;

import io.jans.casa.core.ConfigurationHandler;
import io.jans.casa.core.ExtensionsManager;
import io.jans.casa.extension.AuthnMethod;
import io.jans.casa.misc.Utils;
import io.jans.casa.rest.ProtectedApi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

import org.pf4j.PluginDescriptor;
import org.slf4j.Logger;
import org.zkoss.util.Pair;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.OK;

@ApplicationScoped
@Path("/config/authn-methods")
@ProtectedApi( scopes = "https://jans.io/casa.config" )
public class AuthnMethodsWS extends BaseWS {
    
    @Inject
    private ConfigurationHandler confHandler;
    
    @Inject
    private ExtensionsManager extManager;
    
    @Inject
    private Logger logger;

    @GET
    @Path("available")
    @Produces(MediaType.APPLICATION_JSON)
    public Response available() {

        Response.Status httpStatus;
        String json = null;
        
        logger.trace("AuthnMethodsWS available operation called");
        try {
            Set<String> uniqueAcrs = extManager.getAuthnMethodExts().stream()
                    .map(Pair::getX).map(AuthnMethod::getAcr).collect(Collectors.toSet());
            
            logger.debug("ACRs correlated with plugins or internal extensions: {}", uniqueAcrs);
            json = Utils.jsonFromObject(uniqueAcrs);
            httpStatus = OK;
        } catch (Exception e) {
            json = e.getMessage();
            logger.error(json, e);
            httpStatus = INTERNAL_SERVER_ERROR;
        }
        return Response.status(httpStatus).entity(json).build();
        
    }
 
    @GET
    @Path("enabled")
    @Produces(MediaType.APPLICATION_JSON)
    public Response enabled() {

        Response.Status httpStatus;
        String json = null;
        
        logger.trace("AuthnMethodsWS enabled methods operation called");
        try {
            json = Utils.jsonFromObject(confHandler.getSettings().getAcrPluginMap().keySet());
            httpStatus = OK;
        } catch (Exception e) {
            json = e.getMessage();
            logger.error(json, e);
            httpStatus = INTERNAL_SERVER_ERROR;
        }
        return Response.status(httpStatus).entity(json).build();
        
    }
    
    @POST
    @Path("disable")
    @Produces(MediaType.TEXT_PLAIN)
    public Response disable(@FormParam("acr") String acr) {

        Response.Status httpStatus;
        Map<String, String> map = null;
        String json = null;
        Boolean exists = null;
        String value = null;

        logger.trace("AuthnMethodsWS disable operation called");
        try {
            //This map can hold null values
            map = confHandler.getSettings().getAcrPluginMap();
            exists = map.containsKey(acr);
            value = map.get(acr);

            logger.trace("ACR '{}' {}found in current acr-plugin mapping of Casa configuration", acr, exists ? "": "not ");
            if (exists) {
                map.remove(acr);
                
                logger.trace("Persisting removal of ACR in acr/plugin configuration mapping");
                confHandler.saveSettings();
                httpStatus = OK;
            } else {
                httpStatus = NOT_FOUND;
            }
            
        } catch (Exception e) {
            json = e.getMessage();
            logger.error(json, e);
                
            if (map != null && Boolean.TRUE.equals(exists)) {
                //restore map in case of error
                map.put(acr, value);
            }
            httpStatus = INTERNAL_SERVER_ERROR;
        }
        
        return Response.status(httpStatus).entity(json).build();

    }

}
