/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.model.common.DeviceAuthorizationCacheControl;
import io.jans.as.server.model.common.DeviceAuthorizationStatus;
import io.jans.as.common.model.session.SessionId;
import io.jans.service.CacheService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.io.Serializable;
import java.net.URI;
import java.util.Map;

/**
 * Service used to process data related to device code grant type.
 */
@Stateless
@Named
public class DeviceAuthorizationService implements Serializable {

    public static final String SESSION_ATTEMPTS = "attemps";
    public static final String SESSION_LAST_ATTEMPT = "last_attempt";
    public static final String SESSION_USER_CODE = "user_code";

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private CacheService cacheService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private SessionIdService sessionIdService;

    /**
     * Saves data in cache, it could be saved with two identifiers used by Token endpoint or device_authorization page.
     *
     * @param data           Data to be saved.
     * @param saveDeviceCode Defines whether data should be saved using device code.
     * @param saveUserCode   Defines whether data should be saved using user code.
     */
    public void saveInCache(DeviceAuthorizationCacheControl data, boolean saveDeviceCode, boolean saveUserCode) {
        if (saveDeviceCode) {
            cacheService.put(data.getExpiresIn(), data.getDeviceCode(), data);
        }
        if (saveUserCode) {
            cacheService.put(data.getExpiresIn(), data.getUserCode(), data);
        }
        log.trace("Device request saved in cache, userCode: {}, deviceCode: {}, clientId: {}", data.getUserCode(), data.getDeviceCode(), data.getClient().getClientId());
    }

    /**
     * Returns cache data related to the device authz request using device_code as cache key.
     */
    public DeviceAuthorizationCacheControl getDeviceAuthzByUserCode(String userCode) {
        Object cachedObject = cacheService.get(userCode);
        if (cachedObject == null) {
            // retry one time : sometimes during high load cache client may be not fast enough
            cachedObject = cacheService.get(userCode);
            log.trace("Failed to fetch DeviceAuthorizationCacheControl request from cache, cacheKey: {}", userCode);
        }
        return cachedObject instanceof DeviceAuthorizationCacheControl ? (DeviceAuthorizationCacheControl) cachedObject : null;
    }

    /**
     * Returns cache data related to the device authz request using user_code as cache key.
     */
    public DeviceAuthorizationCacheControl getDeviceAuthzByDeviceCode(String deviceCode) {
        Object cachedObject = cacheService.get(deviceCode);
        if (cachedObject == null) {
            // retry one time : sometimes during high load cache client may be not fast enough
            cachedObject = cacheService.get(deviceCode);
            log.trace("Failed to fetch DeviceAuthorizationCacheControl request from cache, cacheKey: {}", deviceCode);
        }
        return cachedObject instanceof DeviceAuthorizationCacheControl ? (DeviceAuthorizationCacheControl) cachedObject : null;
    }

    /**
     * Verifies whether a specific client has Device Code grant type compatibility.
     *
     * @param client Client to check.
     */
    public boolean hasDeviceCodeCompatibility(Client client) {
        for (GrantType gt : client.getGrantTypes()) {
            if (gt.getValue().equals(GrantType.DEVICE_CODE.getValue())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates data related to the cache, status and client in order to return correct redirection
     * used to process device authorizations.
     *
     * @param deviceAuthorizationCacheControl Cache data related to the device code request.
     * @param client                          Client in process.
     * @param state                           State of the authorization request.
     * @param servletRequest                  HttpServletRequest
     */
    public String getDeviceAuthorizationPage(DeviceAuthorizationCacheControl deviceAuthorizationCacheControl, Client client,
                                             String state, HttpServletRequest servletRequest) {
        if (deviceAuthorizationCacheControl == null) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST, state, "Request not processed."))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build());
        }
        if (deviceAuthorizationCacheControl.getStatus() != DeviceAuthorizationStatus.PENDING) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.INVALID_REQUEST, state, "Request already processed."))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build());
        }
        if (!deviceAuthorizationCacheControl.getClient().getClientId().equals(client.getClientId())) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(errorResponseFactory.getErrorAsJson(AuthorizeErrorResponseType.UNAUTHORIZED_CLIENT, state, "Client doesn't match."))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build());
        }
        final URI uri = UriBuilder.fromPath(appConfiguration.getIssuer()).path(servletRequest.getContextPath())
                .path("/device_authorization.htm").build();
        return uri.toString();
    }

    /**
     * Removes device request data from cache using user_code and device_code.
     *
     * @param userCode   User code used as key in cache.
     * @param deviceCode Device code used as key in cache.
     */
    public void removeDeviceAuthRequestInCache(String userCode, String deviceCode) {
        try {
            if (StringUtils.isNotBlank(userCode)) {
                cacheService.remove(userCode);
            }
            if (StringUtils.isNotBlank(deviceCode)) {
                cacheService.remove(deviceCode);
            }
            log.debug("Removed from cache device authorization using user_code: {}, device_code: {}", userCode, deviceCode);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Uses an HttpServletRequest, process it and return userCode in the session whether it exists.
     *
     * @param httpRequest Request received from an user agent.
     */
    public String getUserCodeFromSession(HttpServletRequest httpRequest) {
        SessionId sessionId = sessionIdService.getSessionId(httpRequest);
        if (sessionId != null) {
            final Map<String, String> sessionAttributes = sessionId.getSessionAttributes();
            if (sessionAttributes.containsKey(SESSION_USER_CODE)) {
                return sessionAttributes.get(SESSION_USER_CODE);
            }
        }
        return null;
    }
}
