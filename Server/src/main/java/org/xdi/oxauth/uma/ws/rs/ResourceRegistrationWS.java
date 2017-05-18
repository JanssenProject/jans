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
import org.slf4j.Logger;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.common.AuthorizationGrantList;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.uma.UmaResource;
import org.xdi.oxauth.model.uma.UmaResourceResponse;
import org.xdi.oxauth.model.uma.UmaResourceWithId;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.service.token.TokenService;
import org.xdi.oxauth.service.uma.UmaResourceService;
import org.xdi.oxauth.service.uma.ScopeService;
import org.xdi.oxauth.service.uma.UmaValidationService;
import org.xdi.oxauth.util.ServerUtil;

import javax.inject.Inject;
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
@Path("/host/rsrc/resource_set")
@Api(value = "/host/rsrc/resource_set", description = "The resource server uses the RESTful API at the authorization server's resource set registration endpoint to create, read, update, and delete resource set descriptions, along with retrieving lists of such descriptions.")
public class ResourceRegistrationWS {

    private static final int NOT_ALLOWED_STATUS = 405;

    @Inject
    private Logger log;

    @Inject
    private TokenService tokenService;

    @Inject
    private UmaValidationService umaValidationService;

    @Inject
    private UmaResourceService resourceService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;
   
    @Inject
    private AuthorizationGrantList authorizationGrantList;

    @Inject
    private ScopeService umaScopeService;

    @Inject
    private AppConfiguration appConfiguration;

    @POST
    @Consumes({UmaConstants.JSON_MEDIA_TYPE})
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    @ApiOperation(value = "Adds a new resource set description using the POST method",
            notes = "Adds a new resource set description using the POST method. If the request is successful, the authorization server MUST respond with a status message that includes an _id property.")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Unauthorized")
    })
    public Response createResource(
            @HeaderParam("Authorization")
            String authorization,
            @ApiParam(value = "Resource description", required = true)
            UmaResource resource) {
        try {
            String id = generatedId();
            log.trace("Try to create resource, id: {}", id);

            umaValidationService.assertHasProtectionScope(authorization);
            return putResourceImpl(Response.Status.CREATED, authorization, id, resource);
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
    public Response updateResource(@HeaderParam("Authorization") String authorization,
                                   @PathParam("rsid")
                                   @ApiParam(value = "Resource description ID", required = true)
                                   String rsid,
                                   @ApiParam(value = "Resource description JSON object", required = true)
                                       UmaResource resource) {
        try {
            umaValidationService.assertHasProtectionScope(authorization);

            return putResourceImpl(Response.Status.OK, authorization, rsid, resource);
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
    @ApiOperation(value = "Reads a previously registered resource description using the GET method.",
            notes = "Reads a previously registered resource description using the GET method. If the request is successful, the authorization server MUST respond with a status message that includes a body containing the referenced resource set description, along with an \"_id\" property.",
            response = UmaResource.class)
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Unauthorized")
    })
    public Response getResource(
            @HeaderParam("Authorization")
            String authorization,
            @PathParam("rsid")
            @ApiParam(value = "Resource description object ID", required = true)
            String rsid) {
        try {
            umaValidationService.assertHasProtectionScope(authorization);

            log.debug("Getting resource description: '{}'", rsid);

            final org.xdi.oxauth.model.uma.persistence.UmaResource ldapResource = resourceService.getResourceById(rsid);

            final UmaResourceWithId response = new UmaResourceWithId();

            response.setId(ldapResource.getId());
            response.setName(ldapResource.getName());
            response.setUri(ldapResource.getUrl());
            response.setIconUri(ldapResource.getIconUri());
            response.setScopes(umaScopeService.getScopeUrlsByDns(ldapResource.getScopes()));

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
            response = UmaResource.class)
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Unauthorized")
    })
    public List<String> getResourceList(
            @HeaderParam("Authorization")
            String authorization,
            @QueryParam("scope")
            @ApiParam(value = "Scope uri", required = false)
            String scope) {
        try {
            log.trace("Getting list of resource descriptions.");

            final AuthorizationGrant authorizationGrant = umaValidationService.assertHasProtectionScope(authorization);
            final String clientDn = authorizationGrant.getClientDn();

            final List<org.xdi.oxauth.model.uma.persistence.UmaResource> ldapResources = resourceService
                    .getResourcesByAssociatedClient(clientDn);

            final List<String> result = new ArrayList<String>(ldapResources.size());
            for (org.xdi.oxauth.model.uma.persistence.UmaResource ldapResource : ldapResources) {

                // if scope parameter is not null then filter by it, otherwise just add to result
                if (StringUtils.isNotBlank(scope)) {
                    final List<String> scopeUrlsByDns = umaScopeService.getScopeUrlsByDns(ldapResource.getScopes());
                    if (scopeUrlsByDns != null && scopeUrlsByDns.contains(scope)) {
                        result.add(ldapResource.getId());
                    }
                } else {
                    result.add(ldapResource.getId());
                }
            }

            return result;

        } catch (Exception ex) {
            log.error("Exception happened on getResourceList()", ex);
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
            response = UmaResource.class)
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Unauthorized")
    })
    public Response deleteResource(
            @HeaderParam("Authorization")
            String authorization,
            @PathParam("rsid")
            @ApiParam(value = "Resource description ID", required = true)
            String rsid) {
        try {
            log.debug("Deleting resource descriptions'");

            umaValidationService.assertHasProtectionScope(authorization);
            resourceService.remove(rsid);

            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Exception ex) {
            log.error("Error on DELETE Resource - " + ex.getMessage(), ex);

            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            return throwUmaInternalErrorException();
        }
    }

    private Response putResourceImpl(Response.Status status, String authorization, String rsid,
                                     UmaResource resource) throws IllegalAccessException, InvocationTargetException, IOException {
        log.trace("putResourceImpl, rsid: {}, status:", rsid, status.name());
        String patToken = tokenService.getTokenFromAuthorizationParameter(authorization);
        AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(patToken);

        String userDn = authorizationGrant.getUserDn();
        String clientDn = authorizationGrant.getClientDn();

        final String resourceDn;

        if (status == Response.Status.CREATED) {
            resourceDn = addResource(rsid, resource, userDn, clientDn);
        } else {
            resourceDn = updateResource(rsid, resource);
        }

        // Load resource description
        org.xdi.oxauth.model.uma.persistence.UmaResource ldapUpdatedResource = resourceService.getResourceByDn(resourceDn);

        UmaResourceResponse response = new UmaResourceResponse();
        response.setId(ldapUpdatedResource.getId());

        return Response.status(status).
                entity(ServerUtil.asJson(response)).
                build();
    }

    private String addResource(String rsid, UmaResource resource, String userDn,
                               String clientDn) throws IllegalAccessException, InvocationTargetException {
        log.debug("Adding new resource set description: '{}'", rsid);

        final String resourceDn = resourceService.getDnForResource(rsid);
        final List<String> scopeDNs = umaScopeService.getScopeDNsByUrlsAndAddToLdapIfNeeded(resource.getScopes());

        final org.xdi.oxauth.model.uma.persistence.UmaResource ldapResource = new org.xdi.oxauth.model.uma.persistence.UmaResource();
        BeanUtils.copyProperties(ldapResource, resource);

        ldapResource.setName(resource.getName());
        ldapResource.setUrl(resource.getUri());
        ldapResource.setIconUri(resource.getIconUri());
        ldapResource.setId(rsid);
        ldapResource.setRev("1");
        ldapResource.setCreator(userDn);
        ldapResource.setDn(resourceDn);
        ldapResource.setScopes(scopeDNs);

        final Boolean addClient = appConfiguration.getUmaKeepClientDuringResourceRegistration();
        if (addClient != null ? addClient : true) {
            ldapResource.setClients(new ArrayList<String>(Arrays.asList(clientDn)));
        }

        resourceService.addResource(ldapResource);

        return resourceDn;
    }

    private String updateResource(String rsid, UmaResource resource) throws IllegalAccessException, InvocationTargetException {
        log.debug("Updating resource description: '{}'.", rsid);

        org.xdi.oxauth.model.uma.persistence.UmaResource ldapResource = new org.xdi.oxauth.model.uma.persistence.UmaResource();
        ldapResource.setDn(resourceService.getBaseDnForResource());
        ldapResource.setId(rsid);

        List<org.xdi.oxauth.model.uma.persistence.UmaResource> ldapResources = resourceService
                .findResources(ldapResource);
        if (ldapResources.size() == 0) {
            throwNotFoundException(rsid);
        } else if (ldapResources.size() > 1) {
            log.error("There is more than one resource with given id: " + rsid);
            throwUmaInternalErrorException();
        }

        ldapResource = ldapResources.get(0);

        ldapResource.setName(resource.getName());
        ldapResource.setUrl(resource.getUri());
        ldapResource.setIconUri(resource.getIconUri());
        ldapResource.setScopes(umaScopeService.getScopeDNsByUrlsAndAddToLdapIfNeeded(resource.getScopes()));
        ldapResource.setRev(String.valueOf(incrementRev(ldapResource.getRev())));

        resourceService.updateResource(ldapResource);

        return ldapResource.getDn();
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
