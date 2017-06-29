/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.token;

import org.xdi.oxauth.model.common.GrantType;

import java.net.URI;

/**
 * Validates the parameters received for the token web service.
 *
 * @author Javier Rojas Blum
 * @version June 28, 2017
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
            case EXTENSION:
                result = assertion != null && !assertion.isEmpty();
                break;
            case REFRESH_TOKEN:
                result = refreshToken != null && !refreshToken.isEmpty();
                break;
        }

        return result;
    }

    public static boolean validateParams(String clientId, String clientSecret) {
        return clientId != null && !clientId.isEmpty()
                && clientSecret != null && !clientSecret.isEmpty();
    }
}