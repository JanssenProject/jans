/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.auth;

import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.service.ConfigurationService;

import javax.inject.Inject;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;

public abstract class AuthorizationService implements Serializable {
    
    @Inject
    Logger logger;

    @Inject
    ConfigurationFactory configurationFactory;

    @Inject
    ConfigurationService configurationService;

    public abstract void validateAuthorization(String token, ResourceInfo resourceInfo) throws Exception;

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

    protected String getClientKeyStorePassword() {
        logger.debug("\n\n\n AuthorizationService::getClientKeyStorePassword() configurationService.find().getKeyStoreSecret() = "+configurationService.find().getKeyStoreSecret()+"\n\n");
        return configurationService.find().getKeyStoreSecret();
    }

    protected String getClientKeyStoreFile() {
        logger.debug("\n\n\n AuthorizationService::getClientKeyStoreFile() configurationService = "+configurationService+"\n\n");
        logger.debug(" AuthorizationService::getClientKeyStoreFile() configurationService.find() = "+configurationService.find());
        logger.debug(" AuthorizationService::getClientKeyStoreFile() configurationService.find().toString() = "+configurationService.find().toString());
        logger.debug("AuthorizationService::getClientKeyStoreFile() configurationService.find().getKeyStoreFile() = "+configurationService.find().getKeyStoreFile()+"\n\n");
        return configurationService.find().getKeyStoreFile();
    }

    private void addMethodScopes(ResourceInfo resourceInfo, List<String> scopes) {
        Method resourceMethod = resourceInfo.getResourceMethod();
        ProtectedApi methodAnnotation = resourceMethod.getAnnotation(ProtectedApi.class);
        if (methodAnnotation != null) {
            scopes.addAll(Stream.of(methodAnnotation.scopes()).collect(Collectors.toList()));
        }
    }

    protected long computeAccessTokenExpirationTime(Integer expiresIn) {
        Calendar calendar = Calendar.getInstance();
        if (expiresIn != null) {
            calendar.add(Calendar.SECOND, expiresIn);
            calendar.add(Calendar.SECOND, -10); // Subtract 10 seconds to avoid expirations during executing request
        }

        return calendar.getTimeInMillis();
    }

}
