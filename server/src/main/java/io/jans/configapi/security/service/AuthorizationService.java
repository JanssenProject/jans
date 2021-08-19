/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.security.service;

import io.jans.configapi.util.AuthUtil;
import io.jans.configapi.configuration.ConfigurationFactory;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.container.ResourceInfo;
import java.io.Serializable;
import java.util.List;

public abstract class AuthorizationService implements Serializable {

    private static final long serialVersionUID = 4012335221233316230L;

    @Inject
    Logger log;

    @Inject
    ConfigurationFactory configurationFactory;

    @Inject
    AuthUtil authUtil;

    public abstract void processAuthorization(String token, String issuer, ResourceInfo resourceInfo, String method,
            String path) throws Exception;

    protected Response getErrorResponse(Response.Status status, String detail) {
        return Response.status(status).entity(detail).build();
    }

    public List<String> getRequestedScopes(String path) {
        return authUtil.getRequestedScopes(path);
    }

    public List<String> getRequestedScopes(ResourceInfo resourceInfo) {
        return authUtil.getRequestedScopes(resourceInfo);
    }

    public boolean validateScope(List<String> authScopes, List<String> resourceScopes) {
        return authUtil.validateScope(authScopes, resourceScopes);
    }

    public List<String> getApiApprovedIssuer() {
        return this.configurationFactory.getApiApprovedIssuer();
    }

    public List<String> getAuthSpecificScopeRequired(ResourceInfo resourceInfo) {
        return authUtil.getAuthSpecificScopeRequired(resourceInfo);
    }

    public List<String> findMissingElements(List<String> list1, List<String> list2) {
        return authUtil.findMissingElements(list1, list2);
    }

    public boolean isEqualCollection(List<String> list1, List<String> list2) {
        return authUtil.isEqualCollection(list1, list2);
    }
}
