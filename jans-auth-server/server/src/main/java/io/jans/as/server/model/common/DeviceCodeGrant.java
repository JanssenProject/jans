/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.common;

import io.jans.as.common.model.common.User;
import io.jans.as.model.common.GrantType;
import io.jans.service.CacheService;
import org.apache.commons.lang.StringUtils;

import jakarta.inject.Inject;

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
    public GrantType getGrantType() {
        return GrantType.DEVICE_CODE;
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
