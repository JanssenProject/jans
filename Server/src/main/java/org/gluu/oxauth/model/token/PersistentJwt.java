/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.token;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxauth.model.common.AccessToken;
import org.gluu.oxauth.model.common.AuthorizationGrantType;
import org.gluu.oxauth.model.common.IdToken;
import org.gluu.oxauth.model.common.RefreshToken;
import org.gluu.oxauth.model.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Javier Rojas Blum Date: 05.22.2012
 */
public class PersistentJwt {

    private final static Logger log = LoggerFactory.getLogger(PersistentJwt.class);

    private String userId;
    private String clientId;
    private AuthorizationGrantType authorizationGrantType;
    private Date authenticationTime;
    private List<String> scopes;
    private List<AccessToken> accessTokens;
    private List<RefreshToken> refreshTokens;
    private AccessToken longLivedAccessToken;
    private IdToken idToken;

    public PersistentJwt() {
    }

    public PersistentJwt(String jwt) {
        try {
            load(jwt);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public AuthorizationGrantType getAuthorizationGrantType() {
        return authorizationGrantType;
    }

    public void setAuthorizationGrantType(AuthorizationGrantType authorizationGrantType) {
        this.authorizationGrantType = authorizationGrantType;
    }

    public Date getAuthenticationTime() {
        return authenticationTime;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public void setAuthenticationTime(Date authenticationTime) {
        this.authenticationTime = authenticationTime;
    }

    public List<AccessToken> getAccessTokens() {
        return accessTokens;
    }

    public void setAccessTokens(List<AccessToken> accessTokens) {
        this.accessTokens = accessTokens;
    }

    public List<RefreshToken> getRefreshTokens() {
        return refreshTokens;
    }

    public void setRefreshTokens(List<RefreshToken> refreshTokens) {
        this.refreshTokens = refreshTokens;
    }

    public AccessToken getLongLivedAccessToken() {
        return longLivedAccessToken;
    }

    public void setLongLivedAccessToken(AccessToken longLivedAccessToken) {
        this.longLivedAccessToken = longLivedAccessToken;
    }

    public IdToken getIdToken() {
        return idToken;
    }

    public void setIdToken(IdToken idToken) {
        this.idToken = idToken;
    }

    @Override
    public String toString() {
        JSONObject jsonObject = new JSONObject();

        try {
            if (StringUtils.isNotBlank(userId)) {
                jsonObject.put("user_id", userId);
            }
            if (StringUtils.isNotBlank(clientId)) {
                jsonObject.put("client_id", clientId);
            }
            if (authorizationGrantType != null) {
                jsonObject.put("authorization_grant_type", authorizationGrantType);
            }
            if (authenticationTime != null) {
                jsonObject.put("authentication_time", authenticationTime.getTime());
            }
            if (scopes != null) {
                JSONArray scopesJsonArray = new JSONArray();
                for (String scope : scopes) {
                    scopesJsonArray.put(scope);
                }
                jsonObject.put("scopes", scopesJsonArray);
            }
            if (accessTokens != null) {
                JSONArray accessTokensJsonArray = new JSONArray();

                for (AccessToken accessToken : accessTokens) {
                    JSONObject accessTokenJsonObject = new JSONObject();

                    if (accessToken.getCode() != null && !accessToken.getCode().isEmpty()) {
                        accessTokenJsonObject.put("code", accessToken.getCode());
                    }
                    if (accessToken.getCreationDate() != null) {
                        accessTokenJsonObject.put("creation_date", accessToken.getCreationDate().getTime());
                    }
                    if (accessToken.getExpirationDate() != null) {
                        accessTokenJsonObject.put("expiration_date", accessToken.getExpirationDate().getTime());
                    }

                    accessTokensJsonArray.put(accessTokenJsonObject);
                }

                jsonObject.put("access_tokens", accessTokensJsonArray);
            }
            if (refreshTokens != null) {
                JSONArray refreshTokensJsonArray = new JSONArray();

                for (RefreshToken refreshToken : refreshTokens) {
                    JSONObject refreshTokenJsonObject = new JSONObject();

                    if (refreshToken.getCode() != null && !refreshToken.getCode().isEmpty()) {
                        refreshTokenJsonObject.put("code", refreshToken.getCode());
                    }
                    if (refreshToken.getCreationDate() != null) {
                        refreshTokenJsonObject.put("creation_date", refreshToken.getCreationDate().getTime());
                    }
                    if (refreshToken.getExpirationDate() != null) {
                        refreshTokenJsonObject.put("expiration_date", refreshToken.getExpirationDate().getTime());
                    }
                }

                jsonObject.put("refresh_tokens", refreshTokensJsonArray);
            }
            if (longLivedAccessToken != null) {
                JSONObject longLivedAccessTokenJsonObject = new JSONObject();

                if (longLivedAccessToken.getCode() != null && !longLivedAccessToken.getCode().isEmpty()) {
                    longLivedAccessTokenJsonObject.put("code", longLivedAccessToken.getCode());
                }
                if (longLivedAccessToken.getCreationDate() != null) {
                    longLivedAccessTokenJsonObject.put("creation_date", longLivedAccessToken.getCreationDate().getTime());
                }
                if (longLivedAccessToken.getExpirationDate() != null) {
                    longLivedAccessTokenJsonObject.put("expiration_date", longLivedAccessToken.getExpirationDate().getTime());
                }

                jsonObject.put("long_lived_access_token", longLivedAccessTokenJsonObject);
            }
            if (idToken != null) {
                JSONObject idTokenJsonObject = new JSONObject();

                if (idToken.getCode() != null && !idToken.getCode().isEmpty()) {
                    idTokenJsonObject.put("code", idToken.getCode());
                }
                if (idToken.getCreationDate() != null) {
                    idTokenJsonObject.put("creation_date", idToken.getCreationDate().getTime());
                }
                if (idToken.getExpirationDate() != null) {
                    idTokenJsonObject.put("expiration_date", idToken.getExpirationDate().getTime());
                }

                jsonObject.put("id_token", idTokenJsonObject);
            }


        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }

        return jsonObject.toString();
    }

    private boolean load(String jwt) throws JSONException {
        boolean result = false;

        JSONObject jsonObject = new JSONObject(jwt);

        if (jsonObject.has("user_id")) {
            userId = jsonObject.getString("user_id");
        }
        if (jsonObject.has("client_id")) {
            clientId = jsonObject.getString("client_id");
        }
        if (jsonObject.has("authorization_grant_type")) {
            authorizationGrantType = AuthorizationGrantType.fromString(jsonObject.getString("authorization_grant_type"));
        }
        if (jsonObject.has("authentication_time")) {
            authenticationTime = new Date(jsonObject.getLong("authentication_time"));
        }
        if (jsonObject.has("scopes")) {
            JSONArray jsonArray = jsonObject.getJSONArray("scopes");
            scopes = Util.asList(jsonArray);
        }
        if (jsonObject.has("access_tokens")) {
            JSONArray accessTokensJsonArray = jsonObject.getJSONArray("access_tokens");
            accessTokens = new ArrayList<AccessToken>();

            for (int i = 0; i < accessTokensJsonArray.length(); i++) {
                JSONObject accessTokenJsonObject = accessTokensJsonArray.getJSONObject(i);

                if (accessTokenJsonObject.has("code")
                        && accessTokenJsonObject.has("creation_date")
                        && accessTokenJsonObject.has("expiration_date")) {
                    String tokenCode = accessTokenJsonObject.getString("code");
                    Date creationDate = new Date(accessTokenJsonObject.getLong("creation_date"));
                    Date expirationDate = new Date(accessTokenJsonObject.getLong("expiration_date"));

                    AccessToken accessToken = new AccessToken(tokenCode, creationDate, expirationDate);
                    accessTokens.add(accessToken);
                }
            }
        }
        if (jsonObject.has("refresh_tokens")) {
            JSONArray refreshTokensJsonArray = jsonObject.getJSONArray("refresh_tokens");
            refreshTokens = new ArrayList<RefreshToken>();

            for (int i = 0; i < refreshTokensJsonArray.length(); i++) {
                JSONObject refreshTokenJsonObject = refreshTokensJsonArray.getJSONObject(i);

                if (refreshTokenJsonObject.has("code")
                        && refreshTokenJsonObject.has("creation_date")
                        && refreshTokenJsonObject.has("expiration_date")) {
                    String tokenCode = refreshTokenJsonObject.getString("code");
                    Date creationDate = new Date(refreshTokenJsonObject.getLong("creation_date"));
                    Date expirationDate = new Date(refreshTokenJsonObject.getLong("expiration_date"));

                    RefreshToken refreshToken = new RefreshToken(tokenCode, creationDate, expirationDate);
                    refreshTokens.add(refreshToken);
                }
            }
        }
        if (jsonObject.has("long_lived_access_token")) {
            JSONObject longLivedAccessTokenJsonObject = jsonObject.getJSONObject("long_lived_access_token");

            if (longLivedAccessTokenJsonObject.has("code")
                    && longLivedAccessTokenJsonObject.has("creation_date")
                    && longLivedAccessTokenJsonObject.has("expiration_date")) {
                String tokenCode = longLivedAccessTokenJsonObject.getString("code");
                Date creationDate = new Date(longLivedAccessTokenJsonObject.getLong("creation_date"));
                Date expirationDate = new Date(longLivedAccessTokenJsonObject.getLong("expiration_date"));

                longLivedAccessToken = new AccessToken(tokenCode, creationDate, expirationDate);
            }
        }
        if (jsonObject.has("id_token")) {
            JSONObject idTokenJsonObject = jsonObject.getJSONObject("id_token");

            if (idTokenJsonObject.has("code")
                    && idTokenJsonObject.has("creation_date")
                    && idTokenJsonObject.has("expiration_date")) {
                String tokenCode = idTokenJsonObject.getString("code");
                Date creationDate = new Date(idTokenJsonObject.getLong("creation_date"));
                Date expirationDate = new Date(idTokenJsonObject.getLong("expiration_date"));

                idToken = new IdToken(tokenCode, creationDate, expirationDate);
            }
        }

        return result;
    }
}