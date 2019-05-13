/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.uma.ws.rs;

import com.wordnik.swagger.annotations.*;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.common.AuthorizationGrant;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.model.uma.*;
import org.gluu.oxauth.uma.service.UmaResourceService;
import org.gluu.oxauth.uma.service.UmaScopeService;
import org.gluu.oxauth.uma.service.UmaValidationService;
import org.gluu.oxauth.util.ServerUtil;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.IOException;
import java.util.*;

/**
 * The API available at the resource registration endpoint enables the resource server to put resources under
 * the protection of an authorization server on behalf of the resource owner and manage them over time.
 * Protection of a resource at the authorization server begins on successful registration and ends on successful deregistration.
 * <p/>
 * The resource server uses a RESTful API at the authorization server's resource registration endpoint
 * to create, read, update, and delete resource descriptions, along with retrieving lists of such descriptions.
 * The descriptions consist of JSON documents that are maintained as web resources at the authorization server.
 * (Note carefully the similar but distinct senses in which the word "resource" is used in this section.)
 *
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 *         Date: 02/12/2015
 */
@Path("/host/rsrc/resource_set")
@Api(value = "/host/rsrc/resource_set", description = "The resource server uses the RESTful API at the authorization server's resource set registration endpoint to create, read, update, and delete resource set descriptions, along with retrieving lists of such descriptions.")
public class UmaResourceRegistrationWS {

    private static final int NOT_ALLOWED_STATUS = 405;

    private static final int DEFAULT_RESOURCE_LIFETIME = 2592000; // 1 month

    @Inject
    private Logger log;

    @Inject
    private UmaValidationService umaValidationService;

    @Inject
    private UmaResourceService resourceService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private UmaScopeService umaScopeService;

    @Inject
    private AppConfiguration appConfiguration;

    @POST
    @Consumes({UmaConstants.JSON_MEDIA_TYPE})
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    @ApiOperation(value = "Adds a new resource description using the POST method",
            notes = "Adds a new resource description using the POST method. If the request is successful, the authorization server MUST respond with a status message that includes an _id property.")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Unauthorized")
    })
    public Response createResource(
            @HeaderParam("Authorization")
            String authorization,
            @ApiParam(value = "Resource description", required = true)
            UmaResource resource) {
        try {
            String id = UUID.randomUUID().toString();
            log.trace("Try to create resource, id: {}", id);

            return putResourceImpl(Response.Status.CREATED, authorization, id, resource);
        } catch (Exception ex) {
            log.error("Exception during resource creation", ex);

            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR, UmaErrorResponseType.SERVER_ERROR, ex.getMessage());
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
            return putResourceImpl(Response.Status.OK, authorization, rsid, resource);
        } catch (Exception ex) {
            log.error("Exception during resource update, rsId: " + rsid + ", message: " + ex.getMessage(), ex);

            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR, UmaErrorResponseType.SERVER_ERROR, ex.getMessage());
        }
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
            final AuthorizationGrant authorizationGrant = umaValidationService.assertHasProtectionScope(authorization);
            umaValidationService.validateRestrictedByClient(authorizationGrant.getClientDn(), rsid);
            log.debug("Getting resource description: '{}'", rsid);

            final org.gluu.oxauth.model.uma.persistence.UmaResource ldapResource = resourceService.getResourceById(rsid);

            final UmaResourceWithId response = new UmaResourceWithId();

            response.setId(ldapResource.getId());
            response.setName(ldapResource.getName());
            response.setDescription(ldapResource.getDescription());
            response.setIconUri(ldapResource.getIconUri());
            response.setScopes(umaScopeService.getScopeIdsByDns(ldapResource.getScopes()));
            response.setScopeExpression(ldapResource.getScopeExpression());
            response.setType(ldapResource.getType());
            response.setIat(ServerUtil.dateToSeconds(ldapResource.getCreationDate()));
            response.setExp(ServerUtil.dateToSeconds(ldapResource.getExpirationDate()));

            final ResponseBuilder builder = Response.ok();
            builder.entity(ServerUtil.asJson(response)); // convert manually to avoid possible conflicts between resteasy providers, e.g. jettison, jackson

            return builder.build();
        } catch (Exception ex) {
            log.error("Exception happened", ex);
            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR, UmaErrorResponseType.SERVER_ERROR, ex.getMessage());
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

            final List<org.gluu.oxauth.model.uma.persistence.UmaResource> ldapResources = resourceService
                    .getResourcesByAssociatedClient(clientDn);

            final List<String> result = new ArrayList<String>(ldapResources.size());
            for (org.gluu.oxauth.model.uma.persistence.UmaResource ldapResource : ldapResources) {

                // if scope parameter is not null then filter by it, otherwise just add to result
                if (StringUtils.isNotBlank(scope)) {
                    final List<String> scopeUrlsByDns = umaScopeService.getScopeIdsByDns(ldapResource.getScopes());
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
            } else {
                throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR, UmaErrorResponseType.SERVER_ERROR, ex.getMessage());
            }
        }
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

            final AuthorizationGrant authorizationGrant = umaValidationService.assertHasProtectionScope(authorization);
            umaValidationService.validateRestrictedByClient(authorizationGrant.getClientDn(), rsid);
            resourceService.remove(rsid);

            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Exception ex) {
            log.error("Error on DELETE Resource - " + ex.getMessage(), ex);

            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR, UmaErrorResponseType.SERVER_ERROR, ex.getMessage());
        }
    }

    private Response putResourceImpl(Response.Status status, String authorization, String rsid, UmaResource resource) throws IOException {
        log.trace("putResourceImpl, rsid: {}, status:", rsid, status.name());

        AuthorizationGrant authorizationGrant = umaValidationService.assertHasProtectionScope(authorization);
        umaValidationService.validateResource(resource);

        String userDn = authorizationGrant.getUserDn();
        String clientDn = authorizationGrant.getClientDn();

        final String resourceDn;

        if (status == Response.Status.CREATED) {
            resourceDn = addResource(rsid, resource, userDn, clientDn);
        } else {
            umaValidationService.validateRestrictedByClient(clientDn, rsid);
            resourceDn = updateResource(rsid, resource);
        }

        // Load resource description
        org.gluu.oxauth.model.uma.persistence.UmaResource ldapUpdatedResource = resourceService.getResourceByDn(resourceDn);

        UmaResourceResponse response = new UmaResourceResponse();
        response.setId(ldapUpdatedResource.getId());

        return Response.status(status).
                type(MediaType.APPLICATION_JSON_TYPE).
                entity(ServerUtil.asJson(response)).
                build();
    }

    private String addResource(String rsid, UmaResource resource, String userDn, String clientDn) {
        log.debug("Adding new resource: '{}'", rsid);

        final String resourceDn = resourceService.getDnForResource(rsid);
        final List<String> scopeDNs = umaScopeService.getScopeDNsByIdsAndAddToLdapIfNeeded(resource.getScopes());

        final Calendar calendar = Calendar.getInstance();
        final org.gluu.oxauth.model.uma.persistence.UmaResource ldapResource = new org.gluu.oxauth.model.uma.persistence.UmaResource();

        ldapResource.setName(resource.getName());
        ldapResource.setDescription(resource.getDescription());
        ldapResource.setIconUri(resource.getIconUri());
        ldapResource.setId(rsid);
        ldapResource.setRev("1");
        ldapResource.setCreator(userDn);
        ldapResource.setDn(resourceDn);
        ldapResource.setScopes(scopeDNs);
        ldapResource.setScopeExpression(resource.getScopeExpression());
        ldapResource.setClients(new ArrayList<String>(Collections.singletonList(clientDn)));
        ldapResource.setType(resource.getType());
        ldapResource.setCreationDate(calendar.getTime());
        ldapResource.setExpirationDate(getExpirationDate(calendar));

        resourceService.addResource(ldapResource);

        return resourceDn;
    }

    private Date getExpirationDate(Calendar creationCalender) {
        int lifetime = appConfiguration.getUmaResourceLifetime();
        if (lifetime <= 0) {
            lifetime = DEFAULT_RESOURCE_LIFETIME;
        }
        creationCalender.add(Calendar.SECOND, lifetime);
        return creationCalender.getTime();
    }

    private String updateResource(String rsid, UmaResource resource) {
        log.debug("Updating resource description: '{}'.", rsid);

        org.gluu.oxauth.model.uma.persistence.UmaResource ldapResource = resourceService.getResourceById(rsid);
        if (ldapResource == null) {
            return throwNotFoundException(rsid);
        }

        ldapResource.setName(resource.getName());
        ldapResource.setDescription(resource.getDescription());
        ldapResource.setIconUri(resource.getIconUri());
        ldapResource.setScopes(umaScopeService.getScopeDNsByIdsAndAddToLdapIfNeeded(resource.getScopes()));
        ldapResource.setScopeExpression(resource.getScopeExpression());
        ldapResource.setRev(String.valueOf(incrementRev(ldapResource.getRev())));
        ldapResource.setType(resource.getType());
        if (resource.getExp() != null && resource.getExp() > 0) {
            ldapResource.setExpirationDate(new Date(resource.getExp() * 1000));
        }

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

    private <T> T throwNotFoundException(String rsid) {
        log.error("Specified resource description doesn't exist, id: " + rsid);
        throw errorResponseFactory.createWebApplicationException(Response.Status.NOT_FOUND, UmaErrorResponseType.NOT_FOUND, "Resource does not exists.");
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
