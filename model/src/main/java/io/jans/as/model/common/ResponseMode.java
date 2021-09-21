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
 * @author Javier Rojas Blum
 * @version July 28, 2021
 */
public enum ResponseMode implements HasParamName, AttributeEnum {

    /**
     * In this mode, Authorization Response parameters are encoded in the query string added to the redirect_uri when
     * redirecting back to the Client.
     */
    QUERY("query"),
    /**
     * In this mode, Authorization Response parameters are encoded in the fragment added to the redirect_uri when
     * redirecting back to the Client.
     */
    FRAGMENT("fragment"),
    /**
     * In this mode, Authorization Response parameters are encoded as HTML form values that are auto-submitted in the
     * User Agent, and thus are transmitted via the HTTP POST method to the Client, with the result parameters being
     * encoded in the body using the application/x-www-form-urlencoded format.
     */
    FORM_POST("form_post"),

    /**
     * In this mode, Authorization server sends the authorization response as HTTP redirect to the redirect URI of
     * the client. The authorization server adds the parameter response containing the JWT to the query component of
     * the redirect URI using the "application/x-form-urlencoded" format.
     *
     * @see <a href="https://openid.net/specs/openid-financial-api-jarm-ID1.html#response-mode-query.jwt">
     * Financial-grade API: JWT Secured Authorization Response Mode for OAuth 2.0 (JARM)
     * </a>
     */
    QUERY_JWT("query.jwt"),

    /**
     * In this mode, Authorization server sends tha authorization response as HTTP redirect to the redirect URI of
     * the client. The authorization server adds the parameter response containing the JWT to the fragment component
     * of the redirect URI using the "application/x-form-encoded" format.
     *
     * @see <a href="https://openid.net/specs/openid-financial-api-jarm-ID1.html#response-mode-fragment.jwt">
     * Financial-grade API: JWT Secured Authorization Response Mode for OAuth 2.0 (JARM)
     * </a>
     */
    FRAGMENT_JWT("fragment.jwt"),

    /**
     * In this mode, Authorization server uses the OAuth 2.0 Form Post Response Mode technique to convey the JWT to the
     * client. The response parameter containing the JWT is encoded as HTML form value that is auto-submitted in the
     * User Agent, and thus is transmitted via the HTTP POST method to the Client, with the result parameters being
     * encoded in the body using the "application/x-form.encoded" format.
     *
     * @see <a href="https://openid.net/specs/openid-financial-api-jarm-ID1.html#response-mode-form_post.jwt">
     * Financial-grade API: JWT Secured Authorization Response Mode for OAuth 2.0 (JARM)
     * </a>
     */
    FORM_POST_JWT("form_post.jwt"),

    /**
     * The respose mode "jwt" is a shortcut and indicates the default redirect encoding (query, fragment) for the
     * requested response type. The default for response type "code" is "query.jwt" whereas the default for "token"
     * and the response types defined is "fragment.jwt".
     *
     * @see <a href="https://openid.net/specs/openid-financial-api-jarm-ID1.html#response-mode-jwt">
     * Financial-grade API: JWT Secured Authorization Response Mode for OAuth 2.0 (JARM)
     * </a>
     */
    JWT("jwt");

    private final String value;

    private static Map<String, ResponseMode> mapByValues = new HashMap<>();

    static {
        for (ResponseMode enumType : values()) {
            mapByValues.put(enumType.getParamName(), enumType);
        }
    }

    ResponseMode(String value) {
        this.value = value;
    }

    public static ResponseMode getByValue(String value) {
        return mapByValues.get(value);
    }

    @JsonCreator
    public static ResponseMode fromString(String param) {
        if (param != null) {
            for (ResponseMode rm : ResponseMode.values()) {
                if (param.equals(rm.value)) {
                    return rm;
                }
            }
        }

        return null;
    }

    @Override
    public String getParamName() {
        return value;
    }

    @Override
    @JsonValue
    public String toString() {
        return value;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public Enum<? extends AttributeEnum> resolveByValue(String value) {
        return getByValue(value);
    }
}