/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.common;

import org.gluu.oxauth.model.registration.Client;

import java.util.Date;

/**
 * An extension grant with the grant type value: urn:openid:params:grant-type:ciba
 *
 * @author Javier Rojas Blum
 * @version September 4, 2019
 */
public class CIBAGrant extends AuthorizationGrant {

    private CIBAAuthenticationRequestId cibaAuthenticationRequestId;
    private Boolean userAuthorization;
    private String clientNotificationToken;
    private Long lastAccessPollFlowControl;

    public CIBAGrant() {
    }

    public void init(User user, Client client, int expiresIn) {
        super.init(user, AuthorizationGrantType.CIBA, client, null);
        setCIBAAuthenticationRequestId(new CIBAAuthenticationRequestId(expiresIn));
        setIsCachedWithNoPersistence(true);
        setLastAccessPollFlowControl(new Date().getTime());
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

    public String getClientNotificationToken() {
        return clientNotificationToken;
    }

    public void setClientNotificationToken(String clientNotificationToken) {
        this.clientNotificationToken = clientNotificationToken;
    }

    public Long getLastAccessPollFlowControl() {
        return lastAccessPollFlowControl;
    }

    public void setLastAccessPollFlowControl(Long lastAccessPollFlowControl) {
        this.lastAccessPollFlowControl = lastAccessPollFlowControl;
    }

}
