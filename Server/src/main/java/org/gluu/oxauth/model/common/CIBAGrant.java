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
 * @version February 25, 2020
 */
public class CIBAGrant extends AuthorizationGrant {

    private CIBAAuthenticationRequestId cibaAuthenticationRequestId;
    private String clientNotificationToken;
    private Long lastAccessControl;
    private boolean userAuthorization;
    private boolean tokensDelivered;

    public CIBAGrant() {
    }

    public void init(User user, Client client, int expiresIn) {
        super.init(user, AuthorizationGrantType.CIBA, client, null);
        setCIBAAuthenticationRequestId(new CIBAAuthenticationRequestId(expiresIn));
        setIsCachedWithNoPersistence(true);
    }

    public CIBAAuthenticationRequestId getCIBAAuthenticationRequestId() {
        return cibaAuthenticationRequestId;
    }

    public void setCIBAAuthenticationRequestId(CIBAAuthenticationRequestId cibaAuthenticationRequestId) {
        this.cibaAuthenticationRequestId = cibaAuthenticationRequestId;
    }

    public String getClientNotificationToken() {
        return clientNotificationToken;
    }

    public void setClientNotificationToken(String clientNotificationToken) {
        this.clientNotificationToken = clientNotificationToken;
    }

    public Long getLastAccessControl() {
        return lastAccessControl;
    }

    public void setLastAccessControl(Long lastAccessControl) {
        this.lastAccessControl = lastAccessControl;
    }

    public boolean isUserAuthorization() {
        return userAuthorization;
    }

    public void setUserAuthorization(boolean userAuthorization) {
        this.userAuthorization = userAuthorization;
    }

    public boolean isTokensDelivered() {
        return tokensDelivered;
    }

    public void setTokensDelivered(boolean tokensDelivered) {
        this.tokensDelivered = tokensDelivered;
    }
}
