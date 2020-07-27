/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.common;

import org.apache.commons.lang.StringUtils;
import org.gluu.service.CacheService;

import javax.inject.Inject;

/**
 * An extension grant with the grant type value: urn:ietf:params:oauth:grant-type:device_code
 */
public class DeviceCodeGrant extends AuthorizationGrant {

    private String deviceCode;

    @Inject
    private CacheService cacheService;

    public DeviceCodeGrant() {
    }

    public void init(DeviceAuthorizationCacheControl cacheData, User user) {
        super.init(user, AuthorizationGrantType.DEVICE_CODE, cacheData.getClient(), null);
        setDeviceCode(cacheData.getDeviceCode());
        setIsCachedWithNoPersistence(true);
        setScopes(cacheData.getScopes());
    }

    @Override
    public void save() {
        CacheGrant cachedGrant = new CacheGrant(this, appConfiguration);
        String cacheKey = StringUtils.isNotBlank(cachedGrant.getDeviceCode()) ? cachedGrant.getDeviceCode() : cachedGrant.getGrantId();
        cacheService.put(cachedGrant.getExpiresIn(), cacheKey, cachedGrant);
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

}
