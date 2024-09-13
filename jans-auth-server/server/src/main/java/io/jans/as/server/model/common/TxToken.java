package io.jans.as.server.model.common;

import io.jans.as.model.common.TokenType;

import java.util.Date;

/**
 * @author Yuriy Z
 */
public class TxToken extends AbstractToken {

    private TokenType tokenType;

    /**
     * <p>
     * Constructs an transaction token.
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
    public TxToken(int lifeTime) {
        super(lifeTime);
        this.tokenType = TokenType.N_A;
    }

    public TxToken(String tokenCode, Date creationDate, Date expirationDate) {
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