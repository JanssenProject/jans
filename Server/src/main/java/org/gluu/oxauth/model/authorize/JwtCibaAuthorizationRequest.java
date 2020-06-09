/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.authorize;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.crypto.AbstractCryptoProvider;
import org.gluu.oxauth.model.exception.InvalidJwtException;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.model.util.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * JWT request used for CIBA requests.
 *
 * @author Milton BO
 * @version June 09, 2020
 */
public class JwtCibaAuthorizationRequest extends JwtSignedRequest {

    // Payload
    private String iss;
    private Integer iat;
    private Integer nbf;
    private String jti;
    private String clientNotificationToken;
    private String acrValues;
    private String loginHintToken;
    private String idTokenHint;
    private String loginHint;
    private String bindingMessage;
    private String userCode;
    private Integer requestedExpiry;

    public JwtCibaAuthorizationRequest(AppConfiguration appConfiguration,
                                       AbstractCryptoProvider cryptoProvider,
                                       String encodedJwt,
                                       Client client) throws InvalidJwtException {
        super(appConfiguration, cryptoProvider, encodedJwt, client);
    }

    /**
     * Method responsible to load corresponding data according to the type of request.
     * @param payload Payload containing custom data related to every type of request.
     */
    @Override
    void loadPayload(String payload) throws JSONException {
        JSONObject jsonPayload = new JSONObject(payload);

        if (jsonPayload.has("aud")) {
            final String audStr = jsonPayload.optString("aud");
            if (StringUtils.isNotBlank(audStr)) {
                this.aud.add(audStr);
            }
            final JSONArray audArray = jsonPayload.optJSONArray("aud");
            if (audArray != null && audArray.length() > 0) {
                this.aud.addAll(Util.asList(audArray));
            }
        }
        if (jsonPayload.has("iss")) {
            iss = jsonPayload.getString("iss");
        }
        if (jsonPayload.has("exp")) {
            exp = jsonPayload.getInt("exp");
        }
        if (jsonPayload.has("iat")) {
            iat = jsonPayload.getInt("iat");
        }
        if (jsonPayload.has("nbf")) {
            nbf = jsonPayload.getInt("nbf");
        }
        if (jsonPayload.has("jti")) {
            jti = jsonPayload.getString("jti");
        }

        if (jsonPayload.has("scope")) {
            JSONArray scopesJsonArray = jsonPayload.optJSONArray("scope");
            if (scopesJsonArray != null) {
                for (int i = 0; i < scopesJsonArray.length(); i++) {
                    String scope = scopesJsonArray.getString(i);
                    scopes.add(scope);
                }
            } else {
                String scopeStringList = jsonPayload.getString("scope");
                scopes.addAll(Util.splittedStringAsList(scopeStringList, " "));
            }
        }
        if (jsonPayload.has("client_notification_token")) {
            clientNotificationToken = jsonPayload.getString("client_notification_token");
        }
        if (jsonPayload.has("acr_values")) {
            acrValues = jsonPayload.getString("acr_values");
        }
        if (jsonPayload.has("login_hint_token")) {
            loginHintToken = jsonPayload.getString("login_hint_token");
        }
        if (jsonPayload.has("id_token_hint")) {
            idTokenHint = jsonPayload.getString("id_token_hint");
        }
        if (jsonPayload.has("login_hint")) {
            loginHint = jsonPayload.getString("login_hint");
        }
        if (jsonPayload.has("binding_message")) {
            bindingMessage = jsonPayload.getString("binding_message");
        }
        if (jsonPayload.has("user_code")) {
            userCode = jsonPayload.getString("user_code");
        }
        if (jsonPayload.has("requested_expiry")) {
            // requested_expirity is an exception, it could be String or Number.
            if (jsonPayload.get("requested_expiry") instanceof Number) {
                requestedExpiry = jsonPayload.getInt("requested_expiry");
            } else {
                requestedExpiry = Integer.parseInt(jsonPayload.getString("requested_expiry"));
            }
        }
    }

    public String getClientNotificationToken() {
        return clientNotificationToken;
    }

    public void setClientNotificationToken(String clientNotificationToken) {
        this.clientNotificationToken = clientNotificationToken;
    }

    public String getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(String acrValues) {
        this.acrValues = acrValues;
    }

    public String getLoginHintToken() {
        return loginHintToken;
    }

    public void setLoginHintToken(String loginHintToken) {
        this.loginHintToken = loginHintToken;
    }

    public String getIdTokenHint() {
        return idTokenHint;
    }

    public void setIdTokenHint(String idTokenHint) {
        this.idTokenHint = idTokenHint;
    }

    public String getLoginHint() {
        return loginHint;
    }

    public void setLoginHint(String loginHint) {
        this.loginHint = loginHint;
    }

    public String getBindingMessage() {
        return bindingMessage;
    }

    public void setBindingMessage(String bindingMessage) {
        this.bindingMessage = bindingMessage;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public Integer getRequestedExpiry() {
        return requestedExpiry;
    }

    public void setRequestedExpiry(Integer requestedExpiry) {
        this.requestedExpiry = requestedExpiry;
    }

    public String getIss() {
        return iss;
    }

    public void setIss(String iss) {
        this.iss = iss;
    }

    public Integer getIat() {
        return iat;
    }

    public void setIat(Integer iat) {
        this.iat = iat;
    }

    public Integer getNbf() {
        return nbf;
    }

    public void setNbf(Integer nbf) {
        this.nbf = nbf;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }
}