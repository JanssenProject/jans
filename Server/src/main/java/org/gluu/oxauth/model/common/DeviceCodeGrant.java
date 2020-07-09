/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.common;

import org.gluu.service.CacheService;

import javax.inject.Inject;

/**
 * An extension grant with the grant type value: urn:ietf:params:oauth:grant-type:device_code
 */
public class DeviceCodeGrant extends AuthorizationGrant {

    private String deviceCode;
    private boolean tokensDelivered;

    @Inject
    private CacheService cacheService;

    public DeviceCodeGrant() {
    }

    public void init(DeviceAuthorizationCacheControl cacheData, User user) {
        super.init(user, AuthorizationGrantType.DEVICE_CODE, cacheData.getClient(), null);
        setDeviceCode(cacheData.getDeviceCode());
        setIsCachedWithNoPersistence(true);
    }

    @Override
    public void save() {
        CacheGrant cachedGrant = new CacheGrant(this, appConfiguration);
        cacheService.put(cachedGrant.getExpiresIn(), cachedGrant.getDeviceCode(), cachedGrant);
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public boolean isTokensDelivered() {
        return tokensDelivered;
    }

    public void setTokensDelivered(boolean tokensDelivered) {
        this.tokensDelivered = tokensDelivered;
    }

}
