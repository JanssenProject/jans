/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.common;

import io.jans.as.model.common.GrantType;
import io.jans.service.CacheService;

import jakarta.inject.Inject;

/**
 * An extension grant with the grant type value: urn:openid:params:grant-type:ciba
 *
 * @author Javier Rojas Blum
 * @version May 5, 2020
 */
public class CIBAGrant extends AuthorizationGrant {

    private String authReqId;
    private boolean tokensDelivered;

    @Inject
    private CacheService cacheService;

    public CIBAGrant() {
    }

    @Override
    public GrantType getGrantType() {
        return GrantType.CIBA;
    }

    public void init(CibaRequestCacheControl cibaRequest) {
        super.init(cibaRequest.getUser(), AuthorizationGrantType.CIBA, cibaRequest.getClient(), null);
        setAuthReqId(cibaRequest.getAuthReqId());
        setAcrValues(cibaRequest.getAcrValues());
        setScopes(cibaRequest.getScopes());
        setIsCachedWithNoPersistence(true);
    }

    @Override
    public void save() {
        CacheGrant cachedGrant = new CacheGrant(this, appConfiguration);
        cacheService.put(cachedGrant.getExpiresIn(), cachedGrant.getAuthReqId(), cachedGrant);
    }

    public String getAuthReqId() {
        return authReqId;
    }

    public void setAuthReqId(String authReqId) {
        this.authReqId = authReqId;
    }

    public boolean isTokensDelivered() {
        return tokensDelivered;
    }

    public void setTokensDelivered(boolean tokensDelivered) {
        this.tokensDelivered = tokensDelivered;
    }

}
