/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.service.token;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.common.AuthorizationGrant;
import org.gluu.oxauth.model.common.AuthorizationGrantList;
import org.gluu.oxauth.model.token.HttpAuthTokenType;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Token specific service methods
 *
 * @author Yuriy Movchan Date: 10/03/2012
 */
@Named
public class TokenService {

    @Inject
    private AuthorizationGrantList authorizationGrantList;

    public boolean isToken(String authorizationParameter, HttpAuthTokenType tokenType) {
        return StringUtils.startsWithIgnoreCase(authorizationParameter, tokenType.getPrefix());
    }

    public String extractToken(String authorizationParameter, HttpAuthTokenType tokenType) {
        if (isToken(authorizationParameter, tokenType)) {
            return authorizationParameter.substring(tokenType.getPrefix().length()).trim();
        }
        return null;
    }

    public boolean isBasicAuthToken(String authorizationParameter) {
        return isToken(authorizationParameter, HttpAuthTokenType.Basic);
    }

    public boolean isBearerAuthToken(String authorizationParameter) {
        return isToken(authorizationParameter, HttpAuthTokenType.Bearer);
    }

    public String getBasicToken(String authorizationParameter) {
        return extractToken(authorizationParameter, HttpAuthTokenType.Basic);
    }

    public String getBearerToken(String authorizationParameter) {
        return extractToken(authorizationParameter, HttpAuthTokenType.Bearer);
    }

    public String getTokenFromAuthorizationParameter(String authorizationParameter) {
        if (isBasicAuthToken(authorizationParameter)) {
            return getBasicToken(authorizationParameter);
        }

        if (isBearerAuthToken(authorizationParameter)) {
            return getBearerToken(authorizationParameter);
        }
        return null;
    }

    public String getTokenFromAuthorizationParameter(String authorization, HttpAuthTokenType... allowedTokenTypes) {
        if (StringUtils.isBlank(authorization) || allowedTokenTypes == null || allowedTokenTypes.length == 0) {
            return null;
        }

        for (HttpAuthTokenType tokenType : allowedTokenTypes) {
            if (isToken(authorization, tokenType)) {
                return extractToken(authorization, tokenType);
            }
        }
        return null;
    }

    public AuthorizationGrant getAuthorizationGrant(String p_authorization) {
        final String token = getTokenFromAuthorizationParameter(p_authorization);
        if (StringUtils.isNotBlank(token)) {
            return authorizationGrantList.getAuthorizationGrantByAccessToken(token);
        }
        return null;
    }

    public AuthorizationGrant getAuthorizationGrantByPrefix(String authorization, String prefix) {
        if (StringUtils.startsWithIgnoreCase(authorization, prefix)) {
            return authorizationGrantList.getAuthorizationGrantByAccessToken(authorization.substring(prefix.length()));
        }
        return null;
    }

    public AuthorizationGrant getBearerAuthorizationGrant(String authorization) {
        return getAuthorizationGrantByTokenType(authorization, HttpAuthTokenType.Bearer);
    }

    public AuthorizationGrant getBasicAuthorizationGrant(String authorization) {
        return getAuthorizationGrantByTokenType(authorization, HttpAuthTokenType.Basic);
    }

    public AuthorizationGrant getAuthorizationGrantByTokenType(String authorization, HttpAuthTokenType tokenType) {
        if (StringUtils.startsWithIgnoreCase(authorization, tokenType.getPrefix())) {
            String token = authorization.substring(tokenType.getPrefix().length());
            return authorizationGrantList.getAuthorizationGrantByAccessToken(token);
        }
        return null;
    }

    public String getClientDn(String p_authorization) {
        final AuthorizationGrant grant = getAuthorizationGrant(p_authorization);
        if (grant != null) {
            return grant.getClientDn();
        }
        return "";
    }

}
