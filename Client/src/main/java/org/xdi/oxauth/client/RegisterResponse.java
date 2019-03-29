/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.register.RegisterErrorResponseType;
import org.gluu.oxauth.model.register.RegisterResponseParam;
import org.gluu.oxauth.model.util.Util;
import org.jboss.resteasy.client.ClientResponse;

import static org.gluu.oxauth.model.register.RegisterRequestParam.GRANT_TYPES;
import static org.gluu.oxauth.model.register.RegisterRequestParam.RESPONSE_TYPES;
import static org.gluu.oxauth.model.register.RegisterResponseParam.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Represents a register response received from the authorization server.
 *
 * @author Javier Rojas Blum
 * @version July 18, 2017
 */
public class RegisterResponse extends BaseResponseWithErrors<RegisterErrorResponseType> {

    private static final Logger LOG = Logger.getLogger(RegisterResponse.class);

    private String clientId;
    private String clientSecret;
    private String registrationAccessToken;
    private String registrationClientUri;
    private Date clientIdIssuedAt;
    private Date clientSecretExpiresAt;
    private List<ResponseType> responseTypes;
    private List<GrantType> grantTypes;
    private Map<String, String> claims = new HashMap<String, String>();

    public RegisterResponse() {
    }

    /**
     * Constructs a register response.
     */
    public RegisterResponse(ClientResponse<String> clientResponse) {
        super(clientResponse);

        String entity = clientResponse.getEntity(String.class);
        setEntity(entity);
        setHeaders(clientResponse.getMetadata());
        injectDataFromJson(entity);
    }

    @Override
    public RegisterErrorResponseType fromString(String p_string) {
        return RegisterErrorResponseType.fromString(p_string);
    }

    public void injectDataFromJson() {
        injectDataFromJson(getEntity());
    }

    public static RegisterResponse valueOf(String p_json) {
        final RegisterResponse r = new RegisterResponse();
        r.injectDataFromJson(p_json);
        return r;
    }

    public void injectDataFromJson(String p_json) {
        if (StringUtils.isNotBlank(p_json)) {
            try {
                JSONObject jsonObj = new JSONObject(p_json);
                if (jsonObj.has(RegisterResponseParam.CLIENT_ID.toString())) {
                    setClientId(jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString()));
                    jsonObj.remove(RegisterResponseParam.CLIENT_ID.toString());
                }
                if (jsonObj.has(CLIENT_SECRET.toString())) {
                    setClientSecret(jsonObj.getString(CLIENT_SECRET.toString()));
                    jsonObj.remove(CLIENT_SECRET.toString());
                }
                if (jsonObj.has(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString())) {
                    setRegistrationAccessToken(jsonObj.getString(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString()));
                    jsonObj.remove(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString());
                }
                if (jsonObj.has(REGISTRATION_CLIENT_URI.toString())) {
                    setRegistrationClientUri(jsonObj.getString(REGISTRATION_CLIENT_URI.toString()));
                    jsonObj.remove(REGISTRATION_CLIENT_URI.toString());
                }
                if (jsonObj.has(CLIENT_ID_ISSUED_AT.toString())) {
                    long clientIdIssuedAt = jsonObj.getLong(CLIENT_ID_ISSUED_AT.toString());
                    if (clientIdIssuedAt > 0) {
                        setClientIdIssuedAt(new Date(clientIdIssuedAt * 1000L));
                    }
                    jsonObj.remove(CLIENT_ID_ISSUED_AT.toString());
                }
                if (jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString())) {
                    long clientSecretExpiresAt = jsonObj.getLong(CLIENT_SECRET_EXPIRES_AT.toString());
                    if (clientSecretExpiresAt > 0) {
                        setClientSecretExpiresAt(new Date(clientSecretExpiresAt * 1000L));
                    }
                    jsonObj.remove(CLIENT_SECRET_EXPIRES_AT.toString());
                }
                if (jsonObj.has(RESPONSE_TYPES.toString())) {
                    JSONArray responseTypesJsonArray = jsonObj.getJSONArray(RESPONSE_TYPES.toString());
                    responseTypes = Util.asEnumList(responseTypesJsonArray, ResponseType.class);
                }
                if (jsonObj.has(GRANT_TYPES.toString())) {
                    JSONArray grantTypesJsonArray = jsonObj.getJSONArray(GRANT_TYPES.toString());
                    grantTypes = Util.asEnumList(grantTypesJsonArray, GrantType.class);
                }

                for (Iterator<String> it = jsonObj.keys(); it.hasNext(); ) {
                    String key = it.next();
                    getClaims().put(key, jsonObj.getString(key));
                }
            } catch (JSONException e) {
                LOG.error(e.getMessage(), e);
            } catch (NoSuchMethodException e) {
                LOG.error(e.getMessage(), e);
            } catch (IllegalAccessException e) {
                LOG.error(e.getMessage(), e);
            } catch (InvocationTargetException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Returns the client's identifier.
     *
     * @return The client's identifier.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Sets the client's identifier.
     *
     * @param clientId The client's identifier.
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Returns the client's password.
     *
     * @return The client's password.
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * Sets the client's password.
     *
     * @param clientSecret The client's password.
     */
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRegistrationAccessToken() {
        return registrationAccessToken;
    }

    public void setRegistrationAccessToken(String registrationAccessToken) {
        this.registrationAccessToken = registrationAccessToken;
    }

    public String getRegistrationClientUri() {
        return registrationClientUri;
    }

    public void setRegistrationClientUri(String registrationClientUri) {
        this.registrationClientUri = registrationClientUri;
    }

    public Date getClientIdIssuedAt() {
        // findbugs : return copy instead of original object
        return clientIdIssuedAt != null ? new Date(clientIdIssuedAt.getTime()) : null;
    }

    public void setClientIdIssuedAt(Date clientIdIssuedAt) {
        // findbugs : save copy instead of original object
        this.clientIdIssuedAt = clientIdIssuedAt != null ? new Date(clientIdIssuedAt.getTime()) : null;
    }

    /**
     * Return the expiration date after which the client's account will expire.
     * <code>null</code> if the client's account never expires.
     *
     * @return The expiration date.
     */
    public Date getClientSecretExpiresAt() {
        // findbugs : return copy instead of original object
        return clientSecretExpiresAt != null ? new Date(clientSecretExpiresAt.getTime()) : null;
    }

    /**
     * Sets the expiration date after which the client's account will expire.
     * <code>null</code> if the client's account never expires.
     *
     * @param clientSecretExpiresAt The expiration date.
     */
    public void setClientSecretExpiresAt(Date clientSecretExpiresAt) {
        // findbugs : save copy instead of original object
        this.clientSecretExpiresAt = clientSecretExpiresAt != null ? new Date(clientSecretExpiresAt.getTime()) : null;
    }

    public List<ResponseType> getResponseTypes() {
        return responseTypes;
    }

    public void setResponseTypes(List<ResponseType> responseTypes) {
        this.responseTypes = responseTypes;
    }

    public List<GrantType> getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(List<GrantType> grantTypes) {
        this.grantTypes = grantTypes;
    }

    public Map<String, String> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, String> claims) {
        this.claims = claims;
    }
}