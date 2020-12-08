/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.token;

import io.jans.as.model.common.GrantType;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Validates the parameters received for the token web service.
 *
 * @author Javier Rojas Blum
 * @version February 25, 2020
 */
public class TokenParamsValidator {

    /**
     * Validates the parameters for a token request.
     *
     * @param grantType    The grant type. This parameter is mandatory. Value must be set
     *                     to: <code>authorization_code</code>, <code>password</code>,
     *                     <code>client_credentials</code>, <code>refresh_token</code>,
     *                     or a valid {@link URI}.
     * @param code         The authorization code.
     * @param redirectUri
     * @param username
     * @param password
     * @param scope
     * @param assertion
     * @param refreshToken
     * @return Returns <code>true</code> when all the parameters are valid.
     */
    public static boolean validateParams(String grantType, String code,
                                         String redirectUri, String username, String password, String scope,
                                         String assertion, String refreshToken) {
        boolean result = false;
        if (grantType == null || grantType.isEmpty()) {
            return false;
        }

        GrantType gt = GrantType.fromString(grantType);

        switch (gt) {
            case AUTHORIZATION_CODE:
                result = code != null && !code.isEmpty() && redirectUri != null && !redirectUri.isEmpty();
                break;
            case RESOURCE_OWNER_PASSWORD_CREDENTIALS:
                result = true;
                break;
            case CLIENT_CREDENTIALS:
                result = true;
                break;
            case REFRESH_TOKEN:
                result = refreshToken != null && !refreshToken.isEmpty();
                break;
            case CIBA:
                result = true;
                break;
            case DEVICE_CODE:
                result = true;
                break;
        }

        return result;
    }

    public static boolean validateParams(String clientId, String clientSecret) {
        return clientId != null && !clientId.isEmpty()
                && clientSecret != null && !clientSecret.isEmpty();
    }

    public static boolean validateGrantType(GrantType requestedGrantType, GrantType[] clientGrantTypesArray, Set<GrantType> grantTypesSupported) {
        List<GrantType> clientGrantTypes = Arrays.asList(clientGrantTypesArray);

        return clientGrantTypes.contains(requestedGrantType) && grantTypesSupported.contains(requestedGrantType);
    }
}