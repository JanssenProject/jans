/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.common;

import java.util.Date;

/**
 * <p>
 * Authorization servers MAY issue refresh tokens to web application clients and
 * native application clients.
 * </p>
 * <p>
 * Refresh tokens MUST be kept confidential in transit and storage, and shared
 * only among the authorization server and the client to whom the refresh tokens
 * were issued.
 * </p>
 * <p>
 * The authorization server MUST maintain the binding between a refresh token
 * and the client to whom it was issued. The authorization server MUST verify
 * the binding between the refresh token and client identity whenever the client
 * identity can be authenticated. When client authentication is not possible,
 * the authorization server SHOULD deploy other means to detect refresh token
 * abuse.
 * </p>
 * <p>
 * For example, the authorization server could employ refresh token rotation in
 * which a new refresh token is issued with every access token refresh response.
 * The previous refresh token is invalidated but retained by the authorization
 * server. If a refresh token is compromised and subsequently used by both the
 * attacker and the legitimate client, one of them will present an invalidated
 * refresh token which will inform the authorization server of the breach.
 * </p>
 * <p>
 * The authorization server MUST ensure that refresh tokens cannot be generated,
 * modified, or guessed to produce valid refresh tokens by unauthorized parties.
 * </p>
 *
 * @author Javier Rojas Date: 09.29.2011
 *
 */
public class RefreshToken extends AbstractToken {

	/**
	 * <p>
	 * Constructs a refresh token.
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
	 * @param lifeTime
	 *            The life time of the token.
	 */
	public RefreshToken(int lifeTime) {
		super(lifeTime);
	}

    public RefreshToken(String code, Date creationDate, Date expirationDate) {
        super(code, creationDate, expirationDate);
    }
}