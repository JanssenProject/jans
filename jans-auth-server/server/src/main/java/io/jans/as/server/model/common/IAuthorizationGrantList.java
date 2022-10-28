/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.common;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;

import java.util.Date;
import java.util.List;

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

    TokenExchangeGrant createTokenExchangeGrant(User user, Client client);

    CIBAGrant createCIBAGrant(CibaRequestCacheControl request);

    AuthorizationCodeGrant getAuthorizationCodeGrant(String authorizationCode);

    AuthorizationGrant getAuthorizationGrantByRefreshToken(String clientId, String refreshTokenCode);

    List<AuthorizationGrant> getAuthorizationGrant(String clientId);

    AuthorizationGrant getAuthorizationGrantByAccessToken(String tokenCode);

    AuthorizationGrant getAuthorizationGrantByIdToken(String idToken);

    CIBAGrant getCIBAGrant(String authReqId);

    DeviceCodeGrant createDeviceGrant(DeviceAuthorizationCacheControl data, User user);

    DeviceCodeGrant getDeviceCodeGrant(String deviceCode);

}