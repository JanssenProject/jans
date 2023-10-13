package io.jans.casa.rest.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.casa.misc.Utils;
import io.jans.casa.rest.ProtectedApi;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.net.URL;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.OK;

@ApplicationScoped
@Path("/config/cors")
@ProtectedApi( scopes = "https://jans.io/casa.config" )
@Deprecated
public class CORSDomainsWS extends BaseWS {

    @Inject
    private Logger logger;
    
    private ObjectMapper mapper;

    @PostConstruct
    private void init() {
        mapper = new ObjectMapper();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {

        Response.Status httpStatus;
        String json = null;
        
        logger.trace("CORSDomainsWS list operation called");
        try {
            json = Utils.jsonFromObject(mainSettings.getCorsDomains());
            httpStatus = OK;
        } catch (Exception e) {
            json = e.getMessage();
            logger.error(json, e);
            httpStatus = INTERNAL_SERVER_ERROR;
        }
        return Response.status(httpStatus).entity(json).build();
        
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response replace(String body) {

        Response.Status httpStatus;
        String json = null;
        List<String> values = mainSettings.getCorsDomains();
        
        logger.trace("CORSDomainsWS replace operation called");
        try {
            List<String> domains = mapper.readValue(body, new TypeReference<List<String>>(){});
            Set<String> domainSet = new TreeSet();
            
            for (String dom : domains) {
                try {
                    URL url = new URL(dom);
                    if (url.getProtocol().equals("http") || url.getProtocol().equals("https")) {
                        domainSet.add(dom);
                    }
                } catch (Exception e) {
                    logger.error("Error: " + e.getMessage());
                }
            }
            logger.trace("Resulting domains set: {}", domainSet);
            
            mainSettings.setCorsDomains(new ArrayList(domainSet));
            logger.trace("Persisting CORS domains in configuration");
            confHandler.saveSettings();
            httpStatus = OK;
            
        } catch (Exception e) {
            json = e.getMessage();
            logger.error(json, e);
            
            mainSettings.setCorsDomains(values);
            httpStatus = INTERNAL_SERVER_ERROR;
        }
        
        return Response.status(httpStatus).entity(json).build();
    }
    
}
