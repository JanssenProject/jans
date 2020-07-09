/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.service;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.authorize.AuthorizeErrorResponseType;
import org.gluu.oxauth.model.common.DeviceAuthorizationCacheControl;
import org.gluu.oxauth.model.common.DeviceAuthorizationStatus;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.service.CacheService;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.Serializable;
import java.net.URI;

/**
 * Service used to process data related to device code grant type.
 */
@Stateless
@Named
public class DeviceAuthorizationService implements Serializable {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private CacheService cacheService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    /**
     * Saves data in cache, it could be saved with two identifiers used by Token endpoint or device_authorization page.
     * @param data Data to be saved.
     * @param saveDeviceCode Defines whether data should be saved using device code.
     * @param saveUserCode Defines whether data should be saved using user code.
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
     * Returns cache data related to the device code request using one of these codes: device_code or user_code.
     */
    public DeviceAuthorizationCacheControl getDeviceAuthorizationCacheData(String deviceCode, String userCode) {
        String cacheKey = deviceCode != null ? deviceCode : userCode;
        Object cachedObject = cacheService.get(cacheKey);
        if (cachedObject == null) {
            // retry one time : sometimes during high load cache client may be not fast enough
            cachedObject = cacheService.get(cacheKey);
            log.trace("Failed to fetch DeviceAuthorizationCacheControl request from cache, cacheKey: {}", cacheKey);
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
     * @param client Client in process.
     * @param state State of the authorization request.
     * @param servletRequest HttpServletRequest
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
     * @param userCode User code used as key in cache.
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
}
