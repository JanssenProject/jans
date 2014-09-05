/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

import org.xdi.oxauth.model.registration.Client;

/**
 * @author Javier Rojas Blum Date: 10.31.2012
 */
public interface TokenIssuerObserver {

    void indexByAuthorizationCode(AuthorizationCode authorizationCode, AuthorizationGrant authorizationGrant);

    void indexByAccessToken(AccessToken accessToken, AuthorizationGrant authorizationGrant);

    void indexByRefreshToken(RefreshToken refreshToken, AuthorizationGrant authorizationGrant);

    void indexByIdToken(IdToken idToken, AuthorizationGrant authorizationGrant);

    void indexByClient(Client client, AuthorizationGrant authorizationGrant);
}