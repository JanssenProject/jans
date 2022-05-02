/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.token;

import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.model.token.HttpAuthTokenType;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Token specific service methods
 *
 * @author Yuriy Movchan Date: 10/03/2012
 */
@Named
public class TokenService {

    @Inject
    private AuthorizationGrantList authorizationGrantList;

    public boolean isToken(@Nullable String authorizationParameter, @NotNull HttpAuthTokenType tokenType) {
        return StringUtils.startsWithIgnoreCase(authorizationParameter, tokenType.getPrefix());
    }

    @Nullable
    public String extractToken(@Nullable String authorizationParameter, @NotNull HttpAuthTokenType tokenType) {
        if (isToken(authorizationParameter, tokenType) && authorizationParameter != null) {
            return authorizationParameter.substring(tokenType.getPrefix().length()).trim();
        }
        return null;
    }

    public boolean isBasicAuthToken(@Nullable String authorizationParameter) {
        return isToken(authorizationParameter, HttpAuthTokenType.Basic);
    }

    public boolean isBearerAuthToken(@Nullable String authorizationParameter) {
        return isToken(authorizationParameter, HttpAuthTokenType.Bearer);
    }

    public boolean isNegotiateAuthToken(@Nullable String authorizationParameter) {
        return isToken(authorizationParameter, HttpAuthTokenType.Negotiate);
    }

    @Nullable
    public String getBasicToken(@Nullable String authorizationParameter) {
        return extractToken(authorizationParameter, HttpAuthTokenType.Basic);
    }

    @Nullable
    public String getBearerToken(@Nullable String authorizationParameter) {
        return extractToken(authorizationParameter, HttpAuthTokenType.Bearer);
    }

    @Nullable
    public String getToken(@Nullable String authorization) {
        return getToken(authorization, HttpAuthTokenType.values());
    }

    @Nullable
    public String getToken(@Nullable String authorization, @Nullable HttpAuthTokenType... allowedTokenTypes) {
        if (StringUtils.isBlank(authorization) || allowedTokenTypes == null || allowedTokenTypes.length == 0) {
            return null;
        }

        for (HttpAuthTokenType tokenType : allowedTokenTypes) {
            if (tokenType != null && isToken(authorization, tokenType)) {
                return extractToken(authorization, tokenType);
            }
        }
        return null;
    }

    @Nullable
    public AuthorizationGrant getAuthorizationGrant(@Nullable String authorization) {
        final String token = getToken(authorization);
        if (StringUtils.isNotBlank(token)) {
            return authorizationGrantList.getAuthorizationGrantByAccessToken(token);
        }
        return null;
    }

    @Nullable
    public AuthorizationGrant getBearerAuthorizationGrant(@Nullable String authorization) {
        return getAuthorizationGrant(authorization, HttpAuthTokenType.Bearer);
    }

    @Nullable
    public AuthorizationGrant getBasicAuthorizationGrant(@Nullable String authorization) {
        return getAuthorizationGrant(authorization, HttpAuthTokenType.Basic);
    }

    @Nullable
    public AuthorizationGrant getAuthorizationGrant(@Nullable String authorization, @Nullable HttpAuthTokenType tokenType) {
        final String token = getToken(authorization, tokenType);
        if (StringUtils.isNotBlank(token)) {
            return authorizationGrantList.getAuthorizationGrantByAccessToken(token);
        }
        return null;
    }

    @NotNull
    public String getClientDn(@Nullable String p_authorization) {
        final AuthorizationGrant grant = getAuthorizationGrant(p_authorization);
        if (grant != null) {
            return grant.getClientDn();
        }
        return "";
    }

}
