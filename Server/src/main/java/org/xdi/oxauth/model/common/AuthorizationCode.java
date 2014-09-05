/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

import java.util.Date;

/**
 * <p>
 * The authorization code is obtained by using an authorization server as an
 * intermediary between the client and resource owner. Instead of requesting
 * authorization directly from the resource owner, the client directs the
 * resource owner to an authorization server (via its user- agent as defined in
 * [RFC2616]), which in turn directs the resource owner back to the client with
 * the authorization code.
 * </p>
 * <p>
 * Before directing the resource owner back to the client with the authorization
 * code, the authorization server authenticates the resource owner and obtains
 * authorization. Because the resource owner only authenticates with the
 * authorization server, the resource owner's credentials are never shared with
 * the client.
 * </p>
 * <p>
 * The authorization code provides a few important security benefits such as the
 * ability to authenticate the client, and the transmission of the access token
 * directly to the client without passing it through the resource owner's
 * user-agent, potentially exposing it to others, including the resource owner.
 * </p>
 *
 * @author Javier Rojas Blum Date: 09.29.2011
 */
public class AuthorizationCode extends AbstractToken {

    private boolean used;

    /**
     * <p>
     * Constructs an authorization code.
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
    public AuthorizationCode(int lifeTime) {
        super(lifeTime);
        used = false;
    }

    public AuthorizationCode(String code, Date creationDate, Date expirationDate) {
        super(code, creationDate, expirationDate);
        used = false;
        checkExpired();
    }

    /**
     * Checks whether a token is valid. An authorization code is valid if
     * it has not been used before, not revoked and not expired.
     */
    @Override
    public boolean isValid() {
        return super.isValid() && !used;
    }

    /**
     * Returns whether an authorization code has been used.
     *
     * @return <code>true</code> if the authorization code has been used.
     */
    public boolean isUsed() {
        return used;
    }

    /**
     * Sets the flag to indicate whether a token has been used.
     * The authorization code must be used only once and after
     * it must be marked as used.
     *
     * @param used Used or not.
     */
    public synchronized void setUsed(boolean used) {
        this.used = used;
    }
}