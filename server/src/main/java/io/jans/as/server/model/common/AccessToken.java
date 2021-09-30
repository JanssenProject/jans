/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.common;

import io.jans.as.model.common.TokenType;

import java.util.Date;

/**
 * <p>
 * Access token (as well as any access token type-specific attributes) MUST be
 * kept confidential in transit and storage, and only shared among the
 * authorization server, the resource servers the access token is valid for, and
 * the client to whom the access token is issued.
 * </p>
 * <p>
 * When using the implicit grant type, the access token is transmitted in the
 * URI fragment, which can expose it to unauthorized parties.
 * </p>
 * <p>
 * The authorization server MUST ensure that access tokens cannot be generated,
 * modified, or guessed to produce valid access tokens by unauthorized parties.
 * </p>
 * <p>
 * The client SHOULD request access tokens with the minimal scope and lifetime
 * necessary. The authorization server SHOULD take the client identity into
 * account when choosing how to honor the requested scope and lifetime, and MAY
 * issue an access token with a less rights than requested.
 * </p>
 *
 * @author Javier Rojas Blum
 * @version September 30, 2021
 */
public class AccessToken extends AbstractToken {

    private TokenType tokenType;

    /**
     * <p>
     * Constructs an access token.
     * </p>
     * <p>
     * When created, a token is valid for a given lifetime, and after this
     * period of time, it will be marked as expired automatically by a
     * background process.
     * </p>
     * <p>
     * When required, the token can be marked as revoked.
     * </p>
     *
     * @param lifeTime The life time of the token.
     */
    public AccessToken(int lifeTime) {
        super(lifeTime);
        this.tokenType = TokenType.BEARER;
    }

    public AccessToken(String tokenCode, Date creationDate, Date expirationDate) {
        super(tokenCode, creationDate, expirationDate);
    }

    /**
     * Returns the {@link TokenType}.
     *
     * @return The token type.
     */
    public TokenType getTokenType() {
        return tokenType;
    }

    /**
     * Sets the {@link TokenType}
     *
     * @param tokenType The token type.
     */
    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }
}