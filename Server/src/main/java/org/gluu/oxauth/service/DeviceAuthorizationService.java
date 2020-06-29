/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.service;

import org.gluu.oxauth.model.common.CibaRequestCacheControl;
import org.gluu.oxauth.model.common.DeviceAuthorizationCacheControl;
import org.gluu.oxauth.model.configuration.AppConfiguration;
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
        log.trace("Ciba request saved in cache, userCode: {}, deviceCode: {}, clientId: {}", data.getUserCode(), data.getDeviceCode(), data.getClient().getClientId());
    }

    public DeviceAuthorizationCacheControl getDeviceAuthorizationCacheData(String deviceCode) {
        Object cachedObject = cacheService.get(deviceCode);
        if (cachedObject == null) {
            // retry one time : sometimes during high load cache client may be not fast enough
            cachedObject = cacheService.get(deviceCode);
            log.trace("Failed to fetch DeviceAuthorizationCacheControl request from cache, deviceCode: {}", deviceCode);
        }
        return cachedObject instanceof DeviceAuthorizationCacheControl ? (DeviceAuthorizationCacheControl) cachedObject : null;
    }
}
