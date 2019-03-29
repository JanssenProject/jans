/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.common;

/**
 * The access token type provides the client with the information required to
 * successfully utilize the access token to make a protected resource request
 * (along with type-specific attributes). The client MUST NOT use an access
 * token if it does not understand or does not trust the token type.
 *
 * @author Javier Rojas Blum Date: 09.20.2011
 */
public enum TokenType {
    /**
     * The bearer token type is defined in [ietf-oauth-v2-bearer]
     */
    BEARER("bearer");

    private final String name;

    private TokenType(String name) {
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
     *         <code>null</code>.
     */
    public static TokenType fromString(String param) {
        if (param != null) {
            for (TokenType rt : TokenType.values()) {
                if (param.equals(rt.name)) {
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
    public String toString() {
        return name;
    }
}