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
 * Scope types
 *
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version January 19, 2017
 */
public enum ScopeType implements AttributeEnum {

    /**
     * Specify what access privileges are being requested for Access Tokens.
     * The scopes associated with Access Tokens determine what resources will
     * be available when they are used to access OAuth 2.0 protected endpoints.
     * For OpenID Connect, scopes can be used to request that specific sets of
     * information be made available as Claim Values.
     * OpenID Connect defines the following scope values that are used to request Claims:
     * <p>
     * <ul>
     * <li>
     * <b>profile</b>. This scope value requests access to the End-User's default profile Claims,
     * which are: name, family_name, given_name, middle_name, nickname, preferred_username, profile,
     * picture, website, gender, birthdate, zoneinfo, locale, and updated_at.
     * </li>
     * <li>
     * <b>email</b>. This scope value requests access to the email and email_verified Claims.
     * </li>
     * <li>
     * <b>address</b>. This scope value requests access to the address Claim.
     * </li>
     * <li>
     * <b>phone</b>. This scope value requests access to the phone_number and phone_number_verified Claims.
     * </li>
     * </ul>
     * <p>
     * The Claims requested by the profile, email, address, and phone scope values are returned from the
     * UserInfo Endpoint.
     */
    OPENID("openid", "OpenID"),

    /**
     * Dynamic scope calls scripts which add claims dynamically.
     */
    DYNAMIC("dynamic", "Dynamic"),

    UMA("uma", "UMA"),

    SPONTANEOUS("spontaneous", "Spontaneous"),

    /**
     * OAuth 2.0 Scopes for any of their API's.
     * This scope type would only have a description, but no claims.
     * Once a client obtains this token, it may be passed to the backend API (let's say the calendar API).
     */
    OAUTH("oauth", "OAuth");

    private final String value;
    private final String displayName;

    private static final Map<String, ScopeType> mapByValues = new HashMap<>();

    static {
        for (ScopeType enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    ScopeType(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    @JsonCreator
    public static ScopeType fromString(String param) {
        if (param != null) {
            for (ScopeType st : ScopeType.values()) {
                if (param.equals(st.value)) {
                    return st;
                }
            }
        }
        return null;
    }

    @Override
    public String getValue() {
        return value;
    }

    /**
     * Gets display name
     *
     * @return display name name
     */
    public String getDisplayName() {
        return displayName;
    }

    public static ScopeType getByValue(String value) {
        return mapByValues.get(value);
    }

    public Enum<? extends AttributeEnum> resolveByValue(String value) {
        return getByValue(value);
    }

    @Override
    @JsonValue
    public String toString() {
        return value;
    }

}