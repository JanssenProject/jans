/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.service;

import org.gluu.oxauth.model.common.DeviceAuthorizationCacheControl;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.service.CacheService;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

@Stateless
@Named
public class DeviceAuthorizationService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private CacheService cacheService;

    public void saveInCache(DeviceAuthorizationCacheControl data) {
        cacheService.put(data.getExpiresIn(), data.getDeviceCode(), data);
        cacheService.put(data.getExpiresIn(), data.getUserCode(), data);
        log.trace("Ciba request saved in cache, userCode: {}, deviceCode: {}, clientId: {}", data.getUserCode(), data.getDeviceCode(), data.getClient().getClientId());
    }

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
}
