/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.common;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.registration.Client;

import javax.enterprise.inject.Instance;
import java.io.Serializable;
import java.util.Set;

/**
 * @author Javier Rojas Blum
 * @version February 25, 2020
 */
public class CIBACacheGrant implements Serializable {

    private String authorizationRequestId;
    private User user;
    private Client client;
    private Set<String> scopes;
    private String grantId;

    private String sessionDn;
    private int expiresIn = 1;
    private String clientNotificationToken;
    private Long lastAccessControl;
    private boolean userAuthorization;
    private boolean tokensDelivered;

    public CIBACacheGrant() {
    }

    public CIBACacheGrant(CIBAGrant grant, AppConfiguration appConfiguration) {
        if (grant.getCIBAAuthenticationRequestId() != null) {
            authorizationRequestId = grant.getCIBAAuthenticationRequestId().getCode();
        }
        initExpiresIn(grant, appConfiguration);

        user = grant.getUser();
        client = grant.getClient();
        scopes = grant.getScopes();
        grantId = grant.getGrantId();
        sessionDn = grant.getSessionDn();
        clientNotificationToken = grant.getClientNotificationToken();
        lastAccessControl = grant.getLastAccessControl();
        userAuthorization = grant.isUserAuthorization();
        tokensDelivered = grant.isTokensDelivered();
    }

    private void initExpiresIn(CIBAGrant grant, AppConfiguration appConfiguration) {
        if (grant.getCIBAAuthenticationRequestId() != null) {
            expiresIn = grant.getCIBAAuthenticationRequestId().getExpiresIn();
        } else {
            expiresIn = appConfiguration.getBackchannelAuthenticationResponseExpiresIn();
        }
    }

    public CIBAGrant asCIBAGrant(Instance<AbstractAuthorizationGrant> grantInstance) {
        CIBAGrant grant = grantInstance.select(CIBAGrant.class).get();
        grant.init(user, client, expiresIn);

        grant.setCIBAAuthenticationRequestId(new CIBAAuthenticationRequestId(expiresIn));
        grant.getCIBAAuthenticationRequestId().setCode(authorizationRequestId);
        grant.setScopes(scopes);
        grant.setGrantId(grantId);
        grant.setSessionDn(sessionDn);
        grant.setClientNotificationToken(clientNotificationToken);
        grant.setLastAccessControl(lastAccessControl);
        grant.setUserAuthorization(userAuthorization);
        grant.setTokensDelivered(tokensDelivered);

        return grant;
    }

    public String cacheKey() {
        return cacheKey(authorizationRequestId, grantId);
    }

    public static String cacheKey(String authorizationRequestId, String grantId) {
        if (StringUtils.isBlank(authorizationRequestId)) {
            return grantId;
        }
        return authorizationRequestId;
    }

    public int getExpiresIn() {
        return expiresIn;
    }
}