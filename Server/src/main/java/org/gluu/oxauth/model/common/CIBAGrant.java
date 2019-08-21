/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.common;

import org.gluu.oxauth.model.registration.Client;

/**
 * An extension grant with the grant type value: urn:openid:params:grant-type:ciba
 *
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
public class CIBAGrant extends AuthorizationGrant {

    private CIBAAuthenticationRequestId cibaAuthenticationRequestId;
    private Boolean userAuthorization;

    public CIBAGrant() {
    }

    /*public CIBAGrant(User user, Client client, Date authenticationTime, String authReqId, int expiresIn) {
        init(user, client, authenticationTime, authReqId, expiresIn);
    }*/

    public void init(User user, Client client, int expiresIn) {
        super.init(user, AuthorizationGrantType.CIBA, client, null);
        setCIBAAuthenticationRequestId(new CIBAAuthenticationRequestId(expiresIn));
        setIsCachedWithNoPersistence(true);
    }

    /**
     * The authorization server MUST NOT issue a refresh token.
     */
    @Override
    public RefreshToken createRefreshToken() {
        throw new UnsupportedOperationException(
                "The authorization server MUST NOT issue a refresh token.");
    }

    public CIBAAuthenticationRequestId getCIBAAuthenticationRequestId() {
        return cibaAuthenticationRequestId;
    }

    public void setCIBAAuthenticationRequestId(CIBAAuthenticationRequestId cibaAuthenticationRequestId) {
        this.cibaAuthenticationRequestId = cibaAuthenticationRequestId;
    }

    public Boolean isUserAuthorization() {
        return userAuthorization;
    }

    public void setUserAuthorization(Boolean userAuthorization) {
        this.userAuthorization = userAuthorization;
    }
}
