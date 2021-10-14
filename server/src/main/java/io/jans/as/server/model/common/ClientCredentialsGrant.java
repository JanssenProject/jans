/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.common;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.GrantType;

/**
 * The client credentials (or other forms of client authentication) can be used
 * as an authorization grant when the authorization scope is limited to the
 * protected resources under the control of the client, or to protected
 * resources previously arranged with the authorization server. Client
 * credentials are used as an authorization grant typically when the client is
 * acting on its own behalf (the client is also the resource owner), or is
 * requesting access to protected resources based on an authorization previously
 * arranged with the authorization server.
 *
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version September 30, 2021
 */
public class ClientCredentialsGrant extends AuthorizationGrant {

    public ClientCredentialsGrant() {
    }

    /**
     * Construct a client credentials grant.
     *
     * @param user   The resource owner.
     * @param client An application making protected resource requests on behalf of
     *               the resource owner and with its authorization.
     */
    public ClientCredentialsGrant(User user, Client client) {
        init(user, client);
    }

    @Override
    public GrantType getGrantType() {
        return GrantType.CLIENT_CREDENTIALS;
    }

    public void init(User user, Client client) {
        super.init(user, AuthorizationGrantType.CLIENT_CREDENTIALS, client, null);
    }

    /**
     * The authorization server MUST NOT issue a refresh token.
     */
    @Override
    public RefreshToken createRefreshToken(String dpop) {
        throw new UnsupportedOperationException(
                "The authorization server MUST NOT issue a refresh token.");
    }
}