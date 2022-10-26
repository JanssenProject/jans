/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.ws.rs;

import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.uma.UmaConstants;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.uma.service.UmaResourceService;
import io.jans.as.server.uma.service.UmaScopeService;
import io.jans.as.server.uma.service.UmaValidationService;
import io.jans.as.server.util.ServerUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static io.jans.as.model.util.Util.escapeLog;

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
 * Date: 02/12/2015
 */
@Path("/host/rsrc/resource_set")
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
    @Consumes({io.jans.as.model.uma.UmaConstants.JSON_MEDIA_TYPE})
    @Produces({io.jans.as.model.uma.UmaConstants.JSON_MEDIA_TYPE})
    public Response createResource(
            @HeaderParam("Authorization")
                    String authorization,
            io.jans.as.model.uma.UmaResource resource) {
        try {
            String id = UUID.randomUUID().toString();
            log.trace("Try to create resource, id: {}", id);

            return putResourceImpl(Response.Status.CREATED, authorization, id, resource);
        } catch (Exception ex) {
            if (log.isErrorEnabled()) {
                log.error("Exception during resource creation", ex);
            }

            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR, io.jans.as.model.uma.UmaErrorResponseType.SERVER_ERROR, ex.getMessage());
        }
    }

    @PUT
    @Path("{rsid}")
    @Consumes({io.jans.as.model.uma.UmaConstants.JSON_MEDIA_TYPE})
    @Produces({io.jans.as.model.uma.UmaConstants.JSON_MEDIA_TYPE})
    public Response updateResource(@HeaderParam("Authorization") String authorization,
                                   @PathParam("rsid") String rsid,
                                   io.jans.as.model.uma.UmaResource resource) {
        try {
            return putResourceImpl(Response.Status.OK, authorization, rsid, resource);
        } catch (Exception ex) {
            if (log.isErrorEnabled()) {
                log.error("Exception during resource update, rsId: " + escapeLog(rsid) + ", message: " + ex.getMessage(), ex);
            }

            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR, io.jans.as.model.uma.UmaErrorResponseType.SERVER_ERROR, ex.getMessage());
        }
    }

    @GET
    @Path("{rsid}")
    @Produces({io.jans.as.model.uma.UmaConstants.JSON_MEDIA_TYPE})
    public Response getResource(
            @HeaderParam("Authorization")
                    String authorization,
            @PathParam("rsid")
                    String rsid) {
        try {
            errorResponseFactory.validateFeatureEnabled(FeatureFlagType.UMA);

            final AuthorizationGrant authorizationGrant = umaValidationService.assertHasProtectionScope(authorization);
            umaValidationService.validateRestrictedByClient(authorizationGrant.getClientDn(), rsid);
            if (log.isDebugEnabled()) {
                log.debug("Getting resource description: '{}'", escapeLog(rsid));
            }

            final io.jans.as.model.uma.persistence.UmaResource ldapResource = resourceService.getResourceById(rsid);

            final io.jans.as.model.uma.UmaResourceWithId response = new io.jans.as.model.uma.UmaResourceWithId();

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
            if (log.isErrorEnabled()) {
                log.error("Exception happened", ex);
            }
            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR, io.jans.as.model.uma.UmaErrorResponseType.SERVER_ERROR, ex.getMessage());
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
    public List<String> getResourceList(
            @HeaderParam("Authorization")
                    String authorization,
            @QueryParam("scope")
                    String scope) {
        try {
            log.trace("Getting list of resource descriptions.");

            errorResponseFactory.validateFeatureEnabled(FeatureFlagType.UMA);

            final AuthorizationGrant authorizationGrant = umaValidationService.assertHasProtectionScope(authorization);
            final String clientDn = authorizationGrant.getClientDn();

            final List<io.jans.as.model.uma.persistence.UmaResource> ldapResources = resourceService
                    .getResourcesByAssociatedClient(clientDn);

            final List<String> result = new ArrayList<>(ldapResources.size());
            for (io.jans.as.model.uma.persistence.UmaResource ldapResource : ldapResources) {

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
                throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR, io.jans.as.model.uma.UmaErrorResponseType.SERVER_ERROR, ex.getMessage());
            }
        }
    }

    @DELETE
    @Path("{rsid}")
    public Response deleteResource(
            @HeaderParam("Authorization")
                    String authorization,
            @PathParam("rsid")
                    String rsid) {
        try {
            log.debug("Deleting resource descriptions'");

            errorResponseFactory.validateFeatureEnabled(FeatureFlagType.UMA);

            final AuthorizationGrant authorizationGrant = umaValidationService.assertHasProtectionScope(authorization);
            umaValidationService.validateRestrictedByClient(authorizationGrant.getClientDn(), rsid);
            resourceService.remove(rsid);

            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Exception ex) {
            if (log.isErrorEnabled()) {
                log.error("Error on DELETE Resource - " + ex.getMessage(), ex);
            }

            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR, io.jans.as.model.uma.UmaErrorResponseType.SERVER_ERROR, ex.getMessage());
        }
    }

    private Response putResourceImpl(Response.Status status, String authorization, String rsid, io.jans.as.model.uma.UmaResource resource) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("putResourceImpl, rsid: {}, status: {}", escapeLog(rsid), status.name());
        }

        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.UMA);

        AuthorizationGrant authorizationGrant = umaValidationService.assertHasProtectionScope(authorization);
        umaValidationService.validateResource(resource);

        String userDn = authorizationGrant.getUserDn();
        String clientDn = authorizationGrant.getClientDn();

        io.jans.as.model.uma.persistence.UmaResource ldapUpdatedResource;

        if (status == Response.Status.CREATED) {
            ldapUpdatedResource = addResource(rsid, resource, userDn, clientDn);
        } else {
            umaValidationService.validateRestrictedByClient(clientDn, rsid);
            ldapUpdatedResource = updateResource(rsid, resource);
        }

        io.jans.as.model.uma.UmaResourceResponse response = new io.jans.as.model.uma.UmaResourceResponse();
        response.setId(ldapUpdatedResource.getId());

        return Response.status(status).
                type(MediaType.APPLICATION_JSON_TYPE).
                entity(ServerUtil.asJson(response)).
                build();
    }

    private io.jans.as.model.uma.persistence.UmaResource addResource(String rsid, io.jans.as.model.uma.UmaResource resource, String userDn, String clientDn) {
        if (log.isDebugEnabled()) {
            log.debug("Adding new resource: '{}'", escapeLog(rsid));
        }

        final String resourceDn = resourceService.getDnForResource(rsid);
        final List<String> scopeDNs = umaScopeService.getScopeDNsByIdsAndAddToPersistenceIfNeeded(resource.getScopes());

        final Calendar calendar = Calendar.getInstance();
        Date iat = calendar.getTime();
        Date exp = getExpirationDate(calendar);

        if (resource.getIat() != null && resource.getIat() > 0) {
            iat = new Date(resource.getIat() * 1000L);
        }
        if (resource.getExp() != null && resource.getExp() > 0) {
            exp = new Date(resource.getExp() * 1000L);
        }

        final io.jans.as.model.uma.persistence.UmaResource ldapResource = new io.jans.as.model.uma.persistence.UmaResource();

        ldapResource.setName(resource.getName());
        ldapResource.setDescription(resource.getDescription());
        ldapResource.setIconUri(resource.getIconUri());
        ldapResource.setId(rsid);
        ldapResource.setCreator(userDn);
        ldapResource.setDn(resourceDn);
        ldapResource.setScopes(scopeDNs);
        ldapResource.setScopeExpression(resource.getScopeExpression());
        ldapResource.setClients(new ArrayList<>(Collections.singletonList(clientDn)));
        ldapResource.setType(resource.getType());
        ldapResource.setCreationDate(iat);
        ldapResource.setExpirationDate(exp);
        ldapResource.setTtl(appConfiguration.getUmaResourceLifetime());

        resourceService.addResource(ldapResource);

        return ldapResource;
    }

    private Date getExpirationDate(Calendar creationCalender) {
        int lifetime = appConfiguration.getUmaResourceLifetime();
        if (lifetime <= 0) {
            lifetime = DEFAULT_RESOURCE_LIFETIME;
        }
        creationCalender.add(Calendar.SECOND, lifetime);
        return creationCalender.getTime();
    }

    private io.jans.as.model.uma.persistence.UmaResource updateResource(String rsid, io.jans.as.model.uma.UmaResource resource) {
        if (log.isDebugEnabled()) {
            log.debug("Updating resource description: '{}'.", escapeLog(rsid));
        }

        io.jans.as.model.uma.persistence.UmaResource ldapResource = resourceService.getResourceById(rsid);
        if (ldapResource == null) {
            return throwNotFoundException(rsid);
        }

        ldapResource.setName(resource.getName());
        ldapResource.setDescription(resource.getDescription());
        ldapResource.setIconUri(resource.getIconUri());
        ldapResource.setScopes(umaScopeService.getScopeDNsByIdsAndAddToPersistenceIfNeeded(resource.getScopes()));
        ldapResource.setScopeExpression(resource.getScopeExpression());
        ldapResource.setType(resource.getType());
        if (resource.getExp() != null && resource.getExp() > 0) {
            ldapResource.setExpirationDate(new Date(resource.getExp() * 1000L));
            ldapResource.setTtl(appConfiguration.getUmaResourceLifetime());
        }

        resourceService.updateResource(ldapResource);

        return ldapResource;
    }

    private <T> T throwNotFoundException(String rsid) {
        if (log.isErrorEnabled()) {
            log.error("Specified resource description doesn't exist, id: {}", escapeLog(rsid));
        }
        throw errorResponseFactory.createWebApplicationException(Response.Status.NOT_FOUND, io.jans.as.model.uma.UmaErrorResponseType.NOT_FOUND, "Resource does not exists.");
    }

    @HEAD
    public Response unsupportedHeadMethod() {
        log.error("HEAD method is not allowed");
        throw new WebApplicationException(Response.status(NOT_ALLOWED_STATUS).entity("HEAD Method Not Allowed").build());
    }

    @OPTIONS
    public Response unsupportedOptionsMethod() {
        log.error("OPTIONS method is not allowed");
        throw new WebApplicationException(Response.status(NOT_ALLOWED_STATUS).entity("OPTIONS Method Not Allowed").build());
    }

}
