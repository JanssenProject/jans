/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.*;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.common.AuthorizationGrantList;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.uma.ResourceSet;
import org.xdi.oxauth.model.uma.ResourceSetResponse;
import org.xdi.oxauth.model.uma.ResourceSetWithId;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.service.token.TokenService;
import org.xdi.oxauth.service.uma.ResourceSetService;
import org.xdi.oxauth.service.uma.ScopeService;
import org.xdi.oxauth.service.uma.UmaValidationService;
import org.xdi.oxauth.util.ServerUtil;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This AM's endpoint is part of resource set registration API.
 * <p/>
 * The host uses the RESTful API at the AM's resource set registration endpoint
 * to create, read, update, and delete resource set descriptions, along with
 * listing groups of such descriptions. The host MUST use its valid PAT obtained
 * previously to gain access to this endpoint. The resource set registration API
 * is a subset of the protection API.
 *
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 *         Date: 02/12/2015
 */
@Name("resourceSetRegistrationRestWebService")
@Path("/host/rsrc/resource_set")
@Api(value = "/host/rsrc/resource_set", description = "The resource server uses the RESTful API at the authorization server's resource set registration endpoint to create, read, update, and delete resource set descriptions, along with retrieving lists of such descriptions.")
public class ResourceSetRegistrationWS {

    private static final int NOT_ALLOWED_STATUS = 405;

    @Logger
    private Log log;

    @In
    private TokenService tokenService;
    @In
    private UmaValidationService umaValidationService;
    @In
    private ResourceSetService resourceSetService;
    @In
    private ErrorResponseFactory errorResponseFactory;
    @In
    private AuthorizationGrantList authorizationGrantList;
    @In
    private ScopeService umaScopeService;
    @In
    private AppConfiguration appConfiguration;

    @POST
    @Consumes({UmaConstants.JSON_MEDIA_TYPE})
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    @ApiOperation(value = "Adds a new resource set description using the POST method",
            notes = "Adds a new resource set description using the POST method. If the request is successful, the authorization server MUST respond with a status message that includes an _id property.")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Unauthorized")
    })
    public Response createResourceSet(
            @HeaderParam("Authorization")
            String authorization,
            @ApiParam(value = "Resource set description", required = true)
            ResourceSet resourceSet) {
        try {
            String id = generatedId();
            log.trace("Try to create resource set, id: {0}", id);

            umaValidationService.assertHasProtectionScope(authorization);
            return putResourceSetImpl(Response.Status.CREATED, authorization, id, resourceSet);
        } catch (Exception ex) {
            log.error("Exception during resource creation", ex);

            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            return throwUmaInternalErrorException();
        }
    }

    @PUT
    @Path("{rsid}")
    @Consumes({UmaConstants.JSON_MEDIA_TYPE})
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    @ApiOperation(value = "Updates a previously registered resource set description using the PUT method",
            notes = "Updates a previously registered resource set description using the PUT method. If the request is successful, the authorization server MUST respond with a status message that includes an \"_id\" property.")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Unauthorized")
    })
    public Response updateResourceSet(@HeaderParam("Authorization") String authorization,
                                      @PathParam("rsid")
                                      @ApiParam(value = "Resource set description ID", required = true)
                                      String rsid,
                                      @ApiParam(value = "Resource set description JSON object", required = true)
                                      ResourceSet resourceSet) {
        try {
            umaValidationService.assertHasProtectionScope(authorization);

            return putResourceSetImpl(Response.Status.OK, authorization, rsid, resourceSet);
        } catch (Exception ex) {
            log.error("Exception during resource update, rsId: " + rsid + ", message: " + ex.getMessage(), ex);

            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            return throwUmaInternalErrorException();
        }
    }

    // it's almost impossible to get simultaneous calls at the very same millisecond but lets anyway synchronize it.
    private synchronized String generatedId() {
        return String.valueOf(System.currentTimeMillis());
    }

    @GET
    @Path("{rsid}")
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    @ApiOperation(value = "Reads a previously registered resource set description using the GET method.",
            notes = "Reads a previously registered resource set description using the GET method. If the request is successful, the authorization server MUST respond with a status message that includes a body containing the referenced resource set description, along with an \"_id\" property.",
            response = ResourceSet.class)
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Unauthorized")
    })
    public Response getResourceSet(
            @HeaderParam("Authorization")
            String authorization,
            @PathParam("rsid")
            @ApiParam(value = "Resource set description object ID", required = true)
            String rsid) {
        try {
            umaValidationService.assertHasProtectionScope(authorization);

            log.debug("Getting resource set description: '{0}'", rsid);

            final org.xdi.oxauth.model.uma.persistence.ResourceSet ldapResourceSet = resourceSetService.getResourceSetById(rsid);

            final ResourceSetWithId response = new ResourceSetWithId();

            BeanUtils.copyProperties(response, ldapResourceSet);
            response.setId(ldapResourceSet.getId());
            response.setScopes(umaScopeService.getScopeUrlsByDns(ldapResourceSet.getScopes()));

            final ResponseBuilder builder = Response.ok();
            builder.entity(ServerUtil.asJson(response)); // convert manually to avoid possible conflicts between resteasy providers, e.g. jettison, jackson

            return builder.build();
        } catch (Exception ex) {
            log.error("Exception happened", ex);
            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            errorResponseFactory.throwUmaInternalErrorException();
            return null;// redundant but required statement by java

        }
    }

    /**
     * Gets resource set lists.
     * ATTENTION: "scope" is parameter added by gluu to have additional filtering.
     * There is no such parameter in UMA specification.
     *
     * @param authorization authorization
     * @param scope         scope of resource set for additional filtering, can blank string.
     * @return resource set ids.
     */
    @GET
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    @ApiOperation(value = "Lists all previously registered resource set identifiers for this user using the GET method.",
            notes = "Lists all previously registered resource set identifiers for this user using the GET method. The authorization server MUST return the list in the form of a JSON array of {rsid} string values.\n" +
                    "\n" +
                    "The resource server uses this method as a first step in checking whether its understanding of protected resources is in full synchronization with the authorization server's understanding.",
            response = ResourceSet.class)
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Unauthorized")
    })
    public List<String> getResourceSetList(
            @HeaderParam("Authorization")
            String authorization,
            @QueryParam("scope")
            @ApiParam(value = "Scope uri", required = false)
            String scope) {
        try {
            log.trace("Getting list of resource set descriptions.");

            final AuthorizationGrant authorizationGrant = umaValidationService.assertHasProtectionScope(authorization);
            final String clientDn = authorizationGrant.getClientDn();

            final List<org.xdi.oxauth.model.uma.persistence.ResourceSet> ldapResourceSets = resourceSetService
                    .getResourceSetsByAssociatedClient(clientDn);

            final List<String> result = new ArrayList<String>(ldapResourceSets.size());
            for (org.xdi.oxauth.model.uma.persistence.ResourceSet ldapResourceSet : ldapResourceSets) {

                // if scope parameter is not null then filter by it, otherwise just add to result
                if (StringUtils.isNotBlank(scope)) {
                    final List<String> scopeUrlsByDns = umaScopeService.getScopeUrlsByDns(ldapResourceSet.getScopes());
                    if (scopeUrlsByDns != null && scopeUrlsByDns.contains(scope)) {
                        result.add(ldapResourceSet.getId());
                    }
                } else {
                    result.add(ldapResourceSet.getId());
                }
            }

            return result;

        } catch (Exception ex) {
            log.error("Exception happened on getResourceSetList()", ex);
            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }
        }

        errorResponseFactory.throwUmaInternalErrorException();
        return Lists.newArrayList(); // redundant but required by java
    }

    @DELETE
    @Path("{rsid}")
    @ApiOperation(value = "Deletes a previously registered resource set description using the DELETE method.",
            notes = "Deletes a previously registered resource set description using the DELETE method, thereby removing it from the authorization server's protection regime.",
            response = ResourceSet.class)
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Unauthorized")
    })
    public Response deleteResourceSet(
            @HeaderParam("Authorization")
            String authorization,
            @PathParam("rsid")
            @ApiParam(value = "Resource set description ID", required = true)
            String rsid) {
        try {
            log.debug("Deleting resource set descriptions'");

            umaValidationService.assertHasProtectionScope(authorization);
            resourceSetService.remove(rsid);

            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Exception ex) {
            log.error("Error on DELETE Resource set - " + ex.getMessage(), ex);

            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            return throwUmaInternalErrorException();
        }
    }

    private Response putResourceSetImpl(Response.Status status, String authorization, String rsid,
                                        ResourceSet resourceSet) throws IllegalAccessException, InvocationTargetException, IOException {
        log.trace("putResourceSetImpl, rsid: {0}, status:", rsid, status.name());
        String patToken = tokenService.getTokenFromAuthorizationParameter(authorization);
        AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(patToken);

        String userDn = authorizationGrant.getUserDn();
        String clientDn = authorizationGrant.getClientDn();

        final String resourceSetDn;

        if (status == Response.Status.CREATED) {
            resourceSetDn = addResourceSet(rsid, resourceSet, userDn, clientDn);
        } else {
            resourceSetDn = updateResourceSet(rsid, resourceSet);
        }

        // Load resource set description
        org.xdi.oxauth.model.uma.persistence.ResourceSet ldapUpdatedResourceSet = resourceSetService
                .getResourceSetByDn(resourceSetDn);

        ResourceSetResponse response = new ResourceSetResponse();
        response.setId(ldapUpdatedResourceSet.getId());

        return Response.status(status).
                entity(ServerUtil.asJson(response)).
                build();
    }

    private String addResourceSet(String rsid, ResourceSet resourceSet, String userDn,
                                  String clientDn) throws IllegalAccessException, InvocationTargetException {
        log.debug("Adding new resource set description: '{0}'", rsid);

        final String resourceSetDn = resourceSetService.getDnForResourceSet(rsid);
        final List<String> scopeDNs = umaScopeService.getScopeDNsByUrlsAndAddToLdapIfNeeded(resourceSet.getScopes());

        final org.xdi.oxauth.model.uma.persistence.ResourceSet ldapResourceSet = new org.xdi.oxauth.model.uma.persistence.ResourceSet();
        BeanUtils.copyProperties(ldapResourceSet, resourceSet);

        ldapResourceSet.setId(rsid);
        ldapResourceSet.setRev("1");
        ldapResourceSet.setCreator(userDn);
        ldapResourceSet.setDn(resourceSetDn);
        ldapResourceSet.setScopes(scopeDNs);

        final Boolean addClient = appConfiguration.getUmaKeepClientDuringResourceSetRegistration();
        if (addClient != null ? addClient : true) {
            ldapResourceSet.setClients(new ArrayList<String>(Arrays.asList(clientDn)));
        }

        resourceSetService.addResourceSet(ldapResourceSet);

        return resourceSetDn;
    }

    private String updateResourceSet(String rsid, ResourceSet resourceSet) throws IllegalAccessException, InvocationTargetException {
        log.debug("Updating resource set description: '{0}'.", rsid);

        org.xdi.oxauth.model.uma.persistence.ResourceSet ldapResourceSet = new org.xdi.oxauth.model.uma.persistence.ResourceSet();
        ldapResourceSet.setDn(resourceSetService.getBaseDnForResourceSet());
        ldapResourceSet.setId(rsid);

        List<org.xdi.oxauth.model.uma.persistence.ResourceSet> ldapResourceSets = resourceSetService
                .findResourceSets(ldapResourceSet);
        if (ldapResourceSets.size() == 0) {
            throwNotFoundException(rsid);
        } else if (ldapResourceSets.size() > 1) {
            log.error("There is more than one resource set with given id: " + rsid);
            throwUmaInternalErrorException();
        }

        ldapResourceSet = ldapResourceSets.get(0);

        BeanUtils.copyProperties(ldapResourceSet, resourceSet);

        ldapResourceSet.setScopes(umaScopeService.getScopeDNsByUrlsAndAddToLdapIfNeeded(resourceSet.getScopes()));
        ldapResourceSet.setRev(String.valueOf(incrementRev(ldapResourceSet.getRev())));

        resourceSetService.updateResourceSet(ldapResourceSet);

        return ldapResourceSet.getDn();
    }

    private int incrementRev(String rev) {
        try {
            return Integer.parseInt(rev) + 1;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return 1; // fallback
    }

    private void throwNotFoundException(String rsid) {
        log.error("Specified resource set description doesn't exist, id: " + rsid);
        errorResponseFactory.throwUmaNotFoundException();
    }

    private Response throwUmaInternalErrorException() {
        errorResponseFactory.throwUmaInternalErrorException();
        return null;
    }

    @HEAD
    @ApiOperation(value = "Not allowed")
    public Response unsupportedHeadMethod() {
        log.error("HEAD method is not allowed");
        throw new WebApplicationException(Response.status(NOT_ALLOWED_STATUS).entity("HEAD Method Not Allowed").build());
    }

    @OPTIONS
    @ApiOperation(value = "Not allowed")
    public Response unsupportedOptionsMethod() {
        log.error("OPTIONS method is not allowed");
        throw new WebApplicationException(Response.status(NOT_ALLOWED_STATUS).entity("OPTIONS Method Not Allowed").build());
    }

}
