/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.common.AuthorizationGrantList;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.uma.ResourceSet;
import org.xdi.oxauth.model.uma.ResourceSetStatus;
import org.xdi.oxauth.model.uma.ResourceSetWithId;
import org.xdi.oxauth.model.uma.UmaErrorResponseType;
import org.xdi.oxauth.service.token.TokenService;
import org.xdi.oxauth.service.uma.ResourceSetService;
import org.xdi.oxauth.service.uma.ScopeService;
import org.xdi.oxauth.service.uma.UmaValidationService;
import org.xdi.oxauth.util.ServerUtil;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 *         Date: 10/03/2012
 */
@Name("resourceSetRegistrationRestWebService")
public class ResourceSetRegistrationRestWebServiceImpl implements ResourceSetRegistrationRestWebService {

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

    @Override
    public Response createResourceSet(@HeaderParam("Authorization") String authorization, ResourceSet resourceSet) {
        try {
            String id = generatedId();
            log.trace("Try to create resource set, id: {0}", id);

            umaValidationService.validateAuthorizationWithProtectScope(authorization);
            return putResourceSetImpl(Response.Status.CREATED, authorization, id, resourceSet);
        } catch (Exception ex) {
            log.error("Exception during resource creation", ex);
            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.SERVER_ERROR)).build());
        }
    }

    public Response updateResourceSet(String authorization, String rsid, ResourceSet resourceSet) {
        try {
            umaValidationService.validateAuthorizationWithProtectScope(authorization);

            return putResourceSetImpl(Response.Status.NO_CONTENT, authorization, rsid, resourceSet);
        } catch (Exception ex) {
            log.error("Exception happened", ex);
            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.SERVER_ERROR)).build());
        }
    }

    // it's almost impossible to get simultaneous calls at the very same millisecond but lets anyway synchronize it.
    private synchronized String generatedId() {
        return String.valueOf(System.currentTimeMillis());
    }

    public Response getResourceSet(String authorization, String rsid) {
        try {
            umaValidationService.validateAuthorizationWithProtectScope(authorization);

            log.debug("Getting resource set description: '{0}'", rsid);

            //        String patToken = tokenService.getTokenFromAuthorizationParameter(authorization);
            //        AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(patToken);

            prepareResourceSetsBranch();

            org.xdi.oxauth.model.uma.persistence.ResourceSet ldapResourceSet = new org.xdi.oxauth.model.uma.persistence.ResourceSet();
            ldapResourceSet.setDn(resourceSetService.getBaseDnForResourceSet());
            ldapResourceSet.setId(rsid);

            final List<org.xdi.oxauth.model.uma.persistence.ResourceSet> ldapResourceSets = resourceSetService
                    .findResourceSets(ldapResourceSet);
            if (ldapResourceSets.size() != 1) {
                log.error("Specified resource set description isn't exist");
                throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.NOT_FOUND)).build());
            }

            ldapResourceSet = ldapResourceSets.get(0);

            final ResourceSetWithId resourceSetWithId = new ResourceSetWithId();

            BeanUtils.copyProperties(resourceSetWithId, ldapResourceSet);
            resourceSetWithId.setId(ldapResourceSet.getId());
            resourceSetWithId.setScopes(umaScopeService.getScopeUrlsByDns(ldapResourceSet.getScopes()));

            final ResponseBuilder builder = Response.ok();
            builder.entity(ServerUtil.asJson(resourceSetWithId)); // convert manually to avoid possible conflicts between resteasy providers, e.g. jettison, jackson

            return builder.build();
        } catch (Exception ex) {
            log.error("Exception happened", ex);
            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.SERVER_ERROR)).build());
        }
    }

    /**
     * Gets resource set lists.
     * ATTENTION: "scope" is parameter added by gluu to have additional filtering.
     * There is no such parameter in UMA specification.
     *
     * @param authorization authorization
     * @param p_scope       scope of resource set for additional filtering, can blank string.
     * @return resource set ids.
     */
    public List<String> getResourceSetList(String authorization, String p_scope) {
        try {
            log.trace("Getting resource set descriptions.");

            umaValidationService.validateAuthorizationWithProtectScope(authorization);

            final String patToken = tokenService.getTokenFromAuthorizationParameter(authorization);
            final AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(patToken);
            if (authorizationGrant != null) {
                final String clientDn = authorizationGrant.getClientDn();

                prepareResourceSetsBranch();

                final List<org.xdi.oxauth.model.uma.persistence.ResourceSet> ldapResourceSets = resourceSetService
                        .getResourceSetsByAssociatedClient(clientDn);

                final List<String> result = new ArrayList<String>(ldapResourceSets.size());
                for (org.xdi.oxauth.model.uma.persistence.ResourceSet ldapResourceSet : ldapResourceSets) {

                    // if scope paremeter is not null then filter by it, otherwise just add to result
                    if (StringUtils.isNotBlank(p_scope)) {
                        final List<String> scopeUrlsByDns = umaScopeService.getScopeUrlsByDns(ldapResourceSet.getScopes());
                        if (scopeUrlsByDns != null && scopeUrlsByDns.contains(p_scope)) {
                            result.add(ldapResourceSet.getId());
                        }
                    } else {
                        result.add(ldapResourceSet.getId());
                    }
                }

                return result;
            }
        } catch (Exception ex) {
            log.error("Exception happened on getResourceSetList()", ex);
            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }
        }
        throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.SERVER_ERROR)).build());
    }

    public Response deleteResourceSet(String authorization, String rsid) {
        try {
            umaValidationService.validateAuthorizationWithProtectScope(authorization);

            log.debug("Getting resource set descriptions'");

//        String patToken = tokenService.getTokenFromAuthorizationParameter(authorization);
//        AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(patToken);
//        String clientDn = authorizationGrant.getClientDn();

            prepareResourceSetsBranch();

            final org.xdi.oxauth.model.uma.persistence.ResourceSet ldapResourceSet = new org.xdi.oxauth.model.uma.persistence.ResourceSet();
            ldapResourceSet.setDn(resourceSetService.getBaseDnForResourceSet());
            ldapResourceSet.setId(rsid);

            List<org.xdi.oxauth.model.uma.persistence.ResourceSet> ldapResourceSets = resourceSetService
                    .findResourceSets(ldapResourceSet);
            if (ldapResourceSets.isEmpty()) {
                log.error("Specified resource set description doesn't exist");
                throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                        .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.NOT_FOUND)).build());
            }

            resourceSetService.remove(ldapResourceSets);

            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Exception ex) {
            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            log.error("Exception happened", ex);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.SERVER_ERROR)).build());
        }
    }

    private Response putResourceSetImpl(Response.Status status, String authorization, String rsid,
                                        ResourceSet resourceSet) throws IllegalAccessException, InvocationTargetException, IOException {
        log.trace("putResourceSetImpl, rsid: {0}, status:", rsid, status.name());
        String patToken = tokenService.getTokenFromAuthorizationParameter(authorization);
        AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(patToken);

        String userDn = authorizationGrant.getUserDn();
        String clientInum = authorizationGrant.getClientId();
        String clientDn = authorizationGrant.getClientDn();

        final String resourceSetDn;

        if (status == Response.Status.CREATED) {
            resourceSetDn = addResourceSet(rsid, resourceSet, userDn, clientInum, clientDn);
        } else {
            resourceSetDn = updateResourceSet(rsid, resourceSet, authorizationGrant, clientDn);
        }

        // Load resource set description
        org.xdi.oxauth.model.uma.persistence.ResourceSet ldapUpdatedResourceSet = resourceSetService
                .getResourceSetByDn(resourceSetDn);
        ResourceSetStatus resourceSetStatus = new ResourceSetStatus();


        BeanUtils.copyProperties(resourceSetStatus, ldapUpdatedResourceSet);

        // convert manually to avoid possible conflict between resteasy providers, e.g. jettison, jackson
        final String entity = ServerUtil.asJson(resourceSetStatus);

        final ResponseBuilder builder = Response.status(status);
        builder.entity(entity);

        return builder.build();
    }

    private String addResourceSet(String rsid, ResourceSet resourceSet, String userDn, String clientInum,
                                  String clientDn) throws IllegalAccessException, InvocationTargetException {
        log.debug("Adding new resource set description: '{0}'", rsid);

        prepareResourceSetsBranch();

        final String resourceSetDn = resourceSetService.getDnForResourceSet(rsid);
        final List<String> scopeDNs = umaScopeService.getScopeDNsByUrlsAndAddToLdapIfNeeded(resourceSet.getScopes());

        final org.xdi.oxauth.model.uma.persistence.ResourceSet ldapResourceSet = new org.xdi.oxauth.model.uma.persistence.ResourceSet();
        BeanUtils.copyProperties(ldapResourceSet, resourceSet);

        ldapResourceSet.setId(rsid);
        ldapResourceSet.setRev("1");
        ldapResourceSet.setCreator(userDn);
        ldapResourceSet.setDn(resourceSetDn);
        ldapResourceSet.setScopes(scopeDNs);

        final Boolean addClient = ConfigurationFactory.getConfiguration().getUmaKeepClientDuringResourceSetRegistration();
        if (addClient != null ? addClient : true) {
            ldapResourceSet.setClients(new ArrayList<String>(Arrays.asList(clientDn)));
        }

        resourceSetService.addResourceSet(ldapResourceSet);

        return resourceSetDn;
    }

    private String updateResourceSet(String rsid, ResourceSet resourceSet,
                                     AuthorizationGrant authorizationGrant, String clientDn) throws IllegalAccessException, InvocationTargetException {
        log.debug("Updating resource set description: '{0}'.", rsid);

        prepareResourceSetsBranch();

        org.xdi.oxauth.model.uma.persistence.ResourceSet ldapResourceSet = new org.xdi.oxauth.model.uma.persistence.ResourceSet();
        ldapResourceSet.setDn(resourceSetService.getBaseDnForResourceSet());
        ldapResourceSet.setId(rsid);

        List<org.xdi.oxauth.model.uma.persistence.ResourceSet> ldapResourceSets = resourceSetService
                .findResourceSets(ldapResourceSet);
        if (ldapResourceSets.size() != 1) {
            log.error("Specified resource set description doesn't exist");
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.NOT_FOUND)).build());
        }

        ldapResourceSet = ldapResourceSets.get(0);

        String resourceSetDn = ldapResourceSet.getDn();

        BeanUtils.copyProperties(ldapResourceSet, resourceSet);

        final List<String> scopeDNs = umaScopeService.getScopeDNsByUrlsAndAddToLdapIfNeeded(resourceSet.getScopes());
        ldapResourceSet.setScopes(scopeDNs);

        ldapResourceSet.setRev(String.valueOf(incrementRev(ldapResourceSet.getRev())));
        resourceSetService.updateResourceSet(ldapResourceSet);

        return resourceSetDn;
    }

    private int incrementRev(String rev) {
        try {
            return Integer.parseInt(rev) + 1;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return 1; // fallback
    }

    private void prepareResourceSetsBranch() {
        // Create resource set description branch if needed
        if (!resourceSetService.containsBranch()) {
            resourceSetService.addBranch();
        }
    }


    @Override
    public Response unsupportedHeadMethod() {
        log.error("HEAD method is not allowed");
        throw new WebApplicationException(Response.status(405).entity("HEAD Method Not Allowed").build());
    }

    @Override
    public Response unsupportedOptionsMethod() {
        log.error("OPTIONS method is not allowed");
        throw new WebApplicationException(Response.status(405).entity("OPTIONS Method Not Allowed").build());

    }

}
