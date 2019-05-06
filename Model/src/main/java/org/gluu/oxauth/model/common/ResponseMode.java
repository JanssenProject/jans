/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.common;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author Javier Rojas Blum
 * @version October 1, 2015
 */
public enum ResponseMode implements HasParamName {

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
    FORM_POST("form_post");

    private final String value;

    private static Map<String, ResponseMode> mapByValues = new HashMap<String, ResponseMode>();

    static {
        for (ResponseMode enumType : values()) {
            mapByValues.put(enumType.getParamName(), enumType);
        }
    }

    private ResponseMode(String value) {
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
}