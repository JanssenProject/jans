/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.jans.orm.annotation.AttributeEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * This class allows to enumerate and identify the possible values of the
 * parameter grant_type for access token requests.
 *
 * @author Javier Rojas Blum
 * @version February 25, 2020
 */
public enum GrantType implements HasParamName, AttributeEnum {

    NONE("none"),

    /**
     * The authorization code is obtained by using an authorization server as an
     * intermediary between the client and resource owner. Instead of requesting
     * authorization directly from the resource owner, the client directs the
     * resource owner to an authorization server (via its user- agent as defined
     * in [RFC2616]), which in turn directs the resource owner back to the
     * client with the authorization code.
     */
    AUTHORIZATION_CODE("authorization_code"),

    /**
     * The implicit grant type is used to obtain access tokens (it does not
     * support the issuance of refresh tokens) and is optimized for public
     * clients known to operate a particular redirection URI.  These clients
     * are typically implemented in a browser using a scripting language
     * such as JavaScript.
     */
    IMPLICIT("implicit"),

    /**
     * The resource owner password credentials (i.e. username and password) can
     * be used directly as an authorization grant to obtain an access token. The
     * credentials should only be used when there is a high degree of trust
     * between the resource owner and the client (e.g. its device operating
     * system or a highly privileged application), and when other authorization
     * grant types are not available (such as an authorization code).
     */
    RESOURCE_OWNER_PASSWORD_CREDENTIALS("password"),

    /**
     * The client credentials (or other forms of client authentication) can be
     * used as an authorization grant when the authorization scope is limited to
     * the protected resources under the control of the client, or to protected
     * resources previously arranged with the authorization server. Client
     * credentials are used as an authorization grant typically when the client
     * is acting on its own behalf (the client is also the resource owner), or
     * is requesting access to protected resources based on an authorization
     * previously arranged with the authorization server.
     */
    CLIENT_CREDENTIALS("client_credentials"),

    /**
     * If the authorization server issued a refresh token to the client, the
     * client makes a refresh request to the token endpoint.
     */
    REFRESH_TOKEN("refresh_token"),

    /**
     * Representing a requesting party, to use a permission ticket to request
     * an OAuth 2.0 access token to gain access to a protected resource
     * asynchronously from the time a resource owner grants access.
     */
    OXAUTH_UMA_TICKET("urn:ietf:params:oauth:grant-type:uma-ticket"),

    /**
     * Token exchange grant type for OAuth 2.0
     */
    TOKEN_EXCHANGE("urn:ietf:params:oauth:grant-type:token-exchange"),

    /**
     * CIBA (Client Initiated Backchannel Authentication) Grant Type.
     */
    CIBA("urn:openid:params:grant-type:ciba"),

    /**
     * Device Authorization Grant Type for OAuth 2.0
     */
    DEVICE_CODE("urn:ietf:params:oauth:grant-type:device_code"),
    ;

    private final String value;

    private static final Map<String, GrantType> mapByValues = new HashMap<>();

    static {
        for (GrantType enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    GrantType() {
        this.value = null;
    }

    GrantType(String value) {
        this.value = value;
    }

    /**
     * Gets param name.
     *
     * @return param name
     */
    public String getParamName() {
        return value;
    }

    @Override
    public String getValue() {
        return value;
    }

    /**
     * Returns the corresponding {@link GrantType} for a parameter grant_type of
     * the access token requests. For the extension grant type, the parameter
     * should be a valid URI.
     *
     * @param param The grant_type parameter.
     * @return The corresponding grant type if found, otherwise
     * <code>null</code>.
     */
    @JsonCreator
    public static GrantType fromString(String param) {
        if (param != null) {
            for (GrantType gt : GrantType.values()) {
                if (param.equals(gt.value)) {
                    return gt;
                }
            }
        }

        return null;
    }

    public static String[] toStringArray(GrantType[] grantTypes) {
        if (grantTypes == null) {
            return new String[0];
        }

        String[] resultGrantTypes = new String[grantTypes.length];
        for (int i = 0; i < grantTypes.length; i++) {
            resultGrantTypes[i] = grantTypes[i].getValue();
        }

        return resultGrantTypes;
    }

    public static GrantType getByValue(String value) {
        return mapByValues.get(value);
    }

    public Enum<? extends AttributeEnum> resolveByValue(String value) {
        return getByValue(value);
    }

    /**
     * Returns a string representation of the object. In this case the parameter
     * name for the grant_type parameter.
     *
     * @return The string representation of the object.
     */
    @Override
    @JsonValue
    public String toString() {
        return value;
    }
}
