package io.jans.casa.rest;

import io.jans.casa.core.ConfigurationHandler;
import io.jans.casa.core.PersistenceService;
import io.jans.casa.core.UserService;
import io.jans.casa.core.ExtensionsManager;
import io.jans.casa.extension.AuthnMethod;
import io.jans.casa.misc.Utils;
import io.jans.casa.core.model.Person;
import io.jans.casa.core.pojo.User;
import io.jans.casa.rest.ProtectedApi;

import org.pf4j.PluginDescriptor;
import org.slf4j.Logger;
import org.zkoss.util.Pair;

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

import static io.jans.casa.rest.SecondFactorUserData.StatusCode.*;

@ApplicationScoped
@Path("/v2")
//This class is part of a coming soon casa-flex refactoring
public class NewSecondFactorUserWS {
    
    @Inject
    private ExtensionsManager extManager;

    @Inject
    private PersistenceService persistenceService;

    @Inject
    private UserService userService;

    @Inject
    private Logger logger;
    
    @GET
    @ProtectedApi( scopes = "https://jans.io/casa.config" )
    @Path("config/authn-methods/available")
    @Produces(MediaType.APPLICATION_JSON)
    public Response available() {

        Response.Status httpStatus;
        String json = null;
        
        logger.trace("AuthnMethodsWS available operation called");
        try {
            Set<String> acrs = allMethods().stream().map(AuthnMethod::getAcr).collect(Collectors.toSet());
            logger.debug("ACRs correlated with plugins or internal extensions: {}", acrs);

            json = Utils.jsonFromObject(acrs);
            httpStatus = OK;
        } catch (Exception e) {
            json = e.getMessage();
            logger.error(json, e);
            httpStatus = INTERNAL_SERVER_ERROR;
        }
        return Response.status(httpStatus).entity(json).build();
        
    }
    
    @GET
    @ProtectedApi( scopes = "https://jans.io/casa.2fa" )
    @Path("2fa/user-info/{userid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get2FAUserData(@PathParam("userid") String userId,
                @QueryParam("m") List<String> restrictToMethods) {

        SecondFactorUserData result = new SecondFactorUserData();
        logger.trace("get2FAUserData WS operation called");

        Person person = persistenceService.get(Person.class, persistenceService.getPersonDn(userId));
        if (person == null) {
            result.setCode(UNKNOWN_USER_ID);
        } else {
            try {
                Set<String> restrict = new HashSet<>();
                if (restrictToMethods != null) {
                    restrict.addAll(restrictToMethods);
                }
       
                List<Pair<String, Integer>> methodsCount = allMethods().stream()
                        .filter(aMethod -> restrict.isEmpty() || restrict.contains(aMethod.getAcr()))
                        .map(aMethod -> new Pair<>(aMethod.getAcr(), aMethod.getTotalUserCreds(userId)))
                        .filter(pair -> pair.getY() > 0).collect(Collectors.toList());

                result.setEnrolledMethods(methodsCount.stream()
                            .map(Pair::getX).collect(Collectors.toList()));

                result.setTotalCreds(methodsCount.stream().mapToInt(Pair::getY).sum());
                result.setCode(SUCCESS);
                
                String pref = person.getPreferredMethod();
                result.setTurnedOn(pref != null);

                if (result.getEnrolledMethods().contains(pref)) {
                    result.setPreference(pref);
                }
            } catch (Exception e) {
                result.setCode(FAILED);
                logger.error(e.getMessage(), e);
            }
        }
        return result.getResponse();

    }

    @POST
    @ProtectedApi( scopes = "https://jans.io/casa.2fa" )
    @Path("2fa/turn-on/{userid}")
    public Response switch2FA(@PathParam("userid") String userId, @FormParam("preference") String method) {
    	
    	Response.ResponseBuilder rb;
    	try {
    	    boolean on = true;
            logger.trace("Turning 2FA {} for user '{}'", on ? "on" : "off", userId);

            Person person = persistenceService.get(Person.class, persistenceService.getPersonDn(userId));
            if (person == null) {
                rb = Response.status(NOT_FOUND);
            } else {
                User u = new User();
                u.setId(userId);
                
                boolean success;
                if (on) {
                    success = userService.turn2faOn(u, method);
                } else {
                    success = userService.turn2faOff(u);
                }
                rb = success ? Response.ok() : Response.serverError();
            }
    	} catch (Exception e) {
            rb = Response.serverError();
            logger.error(e.getMessage(), e);
        }
        return rb.build();
        
    }
    
    private List<AuthnMethod> allMethods() {

        Set<String> pluginIds = extManager.authnMethodPluginImplementers().stream()
            .map(PluginDescriptor::getPluginId).collect(Collectors.toSet());
        pluginIds.add(null);    //Account for system extensions too
        
        //TODO: filter when there are several plugins aimed at the same acr
        return extManager.getAuthnMethodExts(pluginIds).stream().collect(Collectors.toList());
        
    }

}