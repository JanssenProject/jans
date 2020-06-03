/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.common;

/**
 * An extension grant with the grant type value: urn:openid:params:grant-type:ciba
 *
 * @author Javier Rojas Blum
 * @version May 5, 2020
 */
public class CIBAGrant extends AuthorizationGrant {

    private CIBAAuthenticationRequestId cibaAuthenticationRequestId;
    private boolean tokensDelivered;

    public CIBAGrant() {
    }

    public void init(CibaCacheRequest cibaRequest) {
        super.init(cibaRequest.getUser(), AuthorizationGrantType.CIBA, cibaRequest.getClient(), null);
        setCIBAAuthenticationRequestId(cibaRequest.getCibaAuthenticationRequestId());
        setIsCachedWithNoPersistence(true);
    }

    public CIBAAuthenticationRequestId getCIBAAuthenticationRequestId() {
        return cibaAuthenticationRequestId;
    }

    public void setCIBAAuthenticationRequestId(CIBAAuthenticationRequestId cibaAuthenticationRequestId) {
        this.cibaAuthenticationRequestId = cibaAuthenticationRequestId;
    }

    public boolean isTokensDelivered() {
        return tokensDelivered;
    }

    public void setTokensDelivered(boolean tokensDelivered) {
        this.tokensDelivered = tokensDelivered;
    }

}
