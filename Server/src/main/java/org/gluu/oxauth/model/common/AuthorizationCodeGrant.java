/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.common;

import java.util.Date;

import org.gluu.oxauth.model.common.User;
import org.gluu.oxauth.model.registration.Client;

/**
 * <p>
 * The authorization code is obtained by using an authorization server as an
 * intermediary between the client and resource owner. Instead of requesting
 * authorization directly from the resource owner, the client directs the
 * resource owner to an authorization server (via its user- agent as defined in
 * [RFC2616]), which in turn directs the resource owner back to the client with
 * the authorization code.
 * </p>
 * <p>
 * Before directing the resource owner back to the client with the authorization
 * code, the authorization server authenticates the resource owner and obtains
 * authorization. Because the resource owner only authenticates with the
 * authorization server, the resource owner's credentials are never shared with
 * the client.
 * </p>
 * <p>
 * The authorization code provides a few important security benefits such as the
 * ability to authenticate the client, and the transmission of the access token
 * directly to the client without passing it through the resource owner's
 * user-agent, potentially exposing it to others, including the resource owner.
 * </p>
 *
 * @author Javier Rojas Blum Date: 09.29.2011
 * @author Yuriy Movchan
 */
public class AuthorizationCodeGrant extends AuthorizationGrant {

    public AuthorizationCodeGrant() {}

    /**
     * Constructs and authorization code grant.
     *
     * @param user               The resource owner.
     * @param client             An application making protected resource requests on behalf of the resource owner and
     *                           with its authorization.
     * @param authenticationTime The Claim Value is the number of seconds from 1970-01-01T0:0:0Z as measured in UTC
     *                           until the date/time that the End-User authentication occurred.
     */
    public AuthorizationCodeGrant(User user, Client client, Date authenticationTime) {
        init(user, client, authenticationTime);
    }
    
    public void init(User user, Client client, Date authenticationTime) {
        super.init(user, AuthorizationGrantType.AUTHORIZATION_CODE, client, authenticationTime);
        setAuthorizationCode(new AuthorizationCode(appConfiguration.getAuthorizationCodeLifetime()));
        setIsCachedWithNoPersistence(true);
    }

    /**
     * Revokes all the issued tokens.
     */
    @Override
    public void revokeAllTokens() {
        super.revokeAllTokens();
        if (getAuthorizationCode() != null) {
            getAuthorizationCode().setRevoked(true);
        }
    }

    /**
     * Checks all tokens for expiration. Each token will check itself and mark
     * as expired when needed.
     */
    @Override
    public void checkExpiredTokens() {
        super.checkExpiredTokens();
        if (getAuthorizationCode() != null) {
            getAuthorizationCode().checkExpired();
        }
    }
}