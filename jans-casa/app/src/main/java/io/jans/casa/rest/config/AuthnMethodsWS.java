package io.jans.casa.rest.config;

import io.jans.casa.core.ConfigurationHandler;
import io.jans.casa.core.model.CustomScript;
import io.jans.casa.core.ExtensionsManager;
import io.jans.casa.extension.AuthnMethod;
import io.jans.casa.misc.Utils;
import io.jans.casa.rest.ProtectedApi;

import org.pf4j.PluginDescriptor;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

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
            Set<String> pluginIds = extManager.authnMethodPluginImplementers().stream()
                .map(PluginDescriptor::getPluginId).collect(Collectors.toSet());
            pluginIds.add(null);    //Account for system extensions too
            
            Set<String> uniqueAcrs = confHandler.retrieveAcrs();
            logger.debug("Server-enabled ACRs: {}", uniqueAcrs);
            
            uniqueAcrs.retainAll(extManager.getAuthnMethodExts(pluginIds)
                    .stream().map(AuthnMethod::getAcr).collect(Collectors.toSet()));
            
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
            json = Utils.jsonFromObject(mainSettings.getAcrPluginMap().keySet());
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
            map = mainSettings.getAcrPluginMap();
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
    
    @POST
    @Path("assign-plugin")
    @Produces(MediaType.TEXT_PLAIN)
    public Response assign(@FormParam("acr") String acr, @FormParam("plugin") String pluginId) {

        Response.Status httpStatus;
        Map<String, String> map = null;
        String json = null;
        Boolean exists = null;
        String value = null;

        logger.trace("AuthnMethodsWS assign operation called");
        try {
            pluginId = Utils.isEmpty(pluginId) ? null : pluginId; 
            if (!Utils.isEmpty(acr) && extManager.pluginImplementsAuthnMethod(acr, pluginId) && scriptEnabled(acr)) {                
                //This map can hold null values
                map = mainSettings.getAcrPluginMap();
                exists = map.containsKey(acr);
                value = map.get(acr);
                
                logger.trace("ACR '{}' {}found in current acr-plugin mapping of Casa configuration", acr, exists ? "": "not ");
                logger.trace("Associating ACR '{}' with {}", acr, pluginId);
                map.put(acr, pluginId);
                
                logger.trace("Persisting update in acr/plugin configuration mapping");
                confHandler.saveSettings();
                httpStatus = OK;
            } else {
                httpStatus = BAD_REQUEST;
                json = String.format("Inconsistency. Check the script for ACR '%s' is enabled and that plugin '%s' implements such Authentication Mechanism"
                    , acr, pluginId);
                logger.warn(json);
            }

        } catch (Exception e) {
            json = e.getMessage();
            logger.error(json, e);
            
            if (map != null && exists != null) {
                //restore map in case of error
                if (exists) {
                    map.put(acr, value);
                } else {
                    map.remove(acr);
                }
            }
            httpStatus = INTERNAL_SERVER_ERROR;
        }
        
        return Response.status(httpStatus).entity(json).build();
        
    }
    
    private boolean scriptEnabled(String acr) {
        return Optional.ofNullable(persistenceService.getScript(acr))
            .flatMap(sc -> Optional.ofNullable(sc.getEnabled())).orElse(false);
    }
    
}
