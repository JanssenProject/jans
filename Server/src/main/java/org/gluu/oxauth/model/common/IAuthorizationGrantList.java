/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.common;

import java.util.Date;
import java.util.List;

import org.gluu.oxauth.model.registration.Client;

/**
 * @author Yuriy Zabrovarnyy
 * @version August 20, 2019
 */

public interface IAuthorizationGrantList {

    void removeAuthorizationGrants(List<AuthorizationGrant> authorizationGrants);

    AuthorizationGrant createAuthorizationGrant(User user, Client client, Date authenticationTime);

    AuthorizationCodeGrant createAuthorizationCodeGrant(User user, Client client, Date authenticationTime);

    ImplicitGrant createImplicitGrant(User user, Client client, Date authenticationTime);

    ClientCredentialsGrant createClientCredentialsGrant(User user, Client client);

    ResourceOwnerPasswordCredentialsGrant createResourceOwnerPasswordCredentialsGrant(User user, Client client);

    CIBAGrant createCIBAGrant(User user, Client client, int expiresIn);

    AuthorizationCodeGrant getAuthorizationCodeGrant(String clientId, String authorizationCode);

    AuthorizationGrant getAuthorizationGrantByRefreshToken(String clientId, String refreshTokenCode);

    List<AuthorizationGrant> getAuthorizationGrant(String clientId);

    AuthorizationGrant getAuthorizationGrantByAccessToken(String tokenCode);

    AuthorizationGrant getAuthorizationGrantByIdToken(String idToken);

    CIBAGrant getCIBAGrant(String authenticationRequestId);
}