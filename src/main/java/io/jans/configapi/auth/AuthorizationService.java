/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.auth;

import io.jans.configapi.filters.ProtectedApi;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.container.ResourceInfo;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AuthorizationService implements Serializable {

    private static final long serialVersionUID = 4012335221233316230L;

    @Inject
    Logger log;

    public abstract void processAuthorization(String token, ResourceInfo resourceInfo, String method,
            String path) throws Exception;

    protected Response getErrorResponse(Response.Status status, String detail) {
        return Response.status(status).entity(detail).build();
    }

    public List<String> getRequestedScopes(ResourceInfo resourceInfo) {
        Class<?> resourceClass = resourceInfo.getResourceClass();
        ProtectedApi typeAnnotation = resourceClass.getAnnotation(ProtectedApi.class);
        List<String> scopes = new ArrayList<String>();
        if (typeAnnotation == null) {
            addMethodScopes(resourceInfo, scopes);
        } else {
            scopes.addAll(Stream.of(typeAnnotation.scopes()).collect(Collectors.toList()));
            addMethodScopes(resourceInfo, scopes);
        }
        return scopes;
    }

    public boolean validateScope(List<String> authScopes, List<String> resourceScopes) {
        return authScopes.containsAll(resourceScopes);
    }

    private void addMethodScopes(ResourceInfo resourceInfo, List<String> scopes) {
        Method resourceMethod = resourceInfo.getResourceMethod();
        ProtectedApi methodAnnotation = resourceMethod.getAnnotation(ProtectedApi.class);
        if (methodAnnotation != null) {
            scopes.addAll(Stream.of(methodAnnotation.scopes()).collect(Collectors.toList()));
        }
    }

}
