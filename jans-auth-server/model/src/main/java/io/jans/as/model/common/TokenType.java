/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The access token type provides the client with the information required to
 * successfully utilize the access token to make a protected resource request
 * (along with type-specific attributes). The client MUST NOT use an access
 * token if it does not understand or does not trust the token type.
 *
 * @author Javier Rojas Blum
 * @version September 30, 2021
 */
public enum TokenType {
    /**
     * The bearer token type is defined in [ietf-oauth-v2-bearer]
     */
    BEARER("Bearer"),

    DPOP("DPoP");

    private final String name;

    TokenType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the corresponding {@link TokenType} for a parameter token_type.
     *
     * @param param The token_type parameter.
     * @return The corresponding token type if found, otherwise
     * <code>null</code>.
     */
    @JsonCreator
    public static TokenType fromString(String param) {
        if (param != null) {
            for (TokenType rt : TokenType.values()) {
                if (param.equalsIgnoreCase(rt.name)) {
                    return rt;
                }
            }
        }
        return null;
    }

    /**
     * Returns a string representation of the object. In this case the parameter
     * name for the token_type parameter.
     */
    @Override
    @JsonValue
    public String toString() {
        return name;
    }
}