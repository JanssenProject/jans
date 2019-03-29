/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.common;

import java.util.Date;

import org.gluu.oxauth.model.registration.Client;
import org.xdi.oxauth.model.common.User;

/**
 * <p>
 * The implicit grant is a simplified authorization code flow optimized for
 * clients implemented in a browser using a scripting language such as
 * JavaScript. In the implicit flow, instead of issuing the client an
 * authorization code, the client is issued an access token directly (as the
 * result of the resource owner authorization). The grant type is implicit as no
 * intermediate credentials (such as an authorization code) are issued (and
 * later used to obtain an access token).
 * </p>
 * <p>
 * When issuing an implicit grant, the authorization server does not
 * authenticate the client. In some cases, the client identity can be verified
 * via the redirection URI used to deliver the access token to the client. The
 * access token may be exposed to the resource owner or other applications with
 * access to the resource owner's user-agent.
 * </p>
 * <p>
 * Implicit grants improve the responsiveness and efficiency of some clients
 * (such as a client implemented as an in-browser application) since it reduces
 * the number of round trips required to obtain an access token. However, this
 * convenience should be weighed against the security implications of using
 * implicit grants, especially when the authorization code grant type is
 * available.
 * </p>
 *
 * @author Javier Rojas Blum Date: 09.29.2011
 * @author Yuriy Movchan
 */
public class ImplicitGrant extends AuthorizationGrant {
	
	public ImplicitGrant() {}

    /**
     * Constructs an implicit grant.
     *
     * @param user               The resource owner.
     * @param client             An application making protected resource requests on behalf of the resource owner and
     *                           with its authorization.
     * @param authenticationTime The Claim Value is the number of seconds from 1970-01-01T0:0:0Z as measured in UTC
     *                           until the date/time that the End-User authentication occurred.
     */
    public ImplicitGrant(User user, Client client, Date authenticationTime) {
        init(user, client, authenticationTime);
    }

    public void init(User user, Client client, Date authenticationTime) {
        super.init(user, AuthorizationGrantType.IMPLICIT, client, authenticationTime);
    }

    /**
     * The authorization server MUST NOT issue a refresh token.
     */
    @Override
    public RefreshToken createRefreshToken() {
        throw new UnsupportedOperationException(
                "The authorization server MUST NOT issue a refresh token.");
    }
}