/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

import java.util.Date;
import java.util.List;

import org.xdi.oxauth.model.registration.Client;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 08/01/2013
 */

public interface IAuthorizationGrantList {

    public void removeAuthorizationGrants(List<AuthorizationGrant> authorizationGrants);

    public AuthorizationGrant createAuthorizationGrant(User user, Client client, Date authenticationTime);

    public AuthorizationCodeGrant createAuthorizationCodeGrant(User user, Client client, Date authenticationTime);

    public ImplicitGrant createImplicitGrant(User user, Client client, Date authenticationTime);

    public ClientCredentialsGrant createClientCredentialsGrant(User user, Client client);

    public ResourceOwnerPasswordCredentialsGrant createResourceOwnerPasswordCredentialsGrant(User user, Client client);

    public AuthorizationCodeGrant getAuthorizationCodeGrant(String clientId, String authorizationCode);

    public AuthorizationGrant getAuthorizationGrantByRefreshToken(String clientId, String refreshTokenCode);

    public List<AuthorizationGrant> getAuthorizationGrant(String clientId);

    public AuthorizationGrant getAuthorizationGrantByAccessToken(String tokenCode);

    public AuthorizationGrant getAuthorizationGrantByIdToken(String idToken);
}