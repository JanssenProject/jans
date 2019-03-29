/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2015, Gluu
 */

package org.gluu.oxauth.model.token;

import java.io.Serializable;

import org.gluu.oxauth.model.exception.InvalidJwtException;
import org.gluu.oxauth.model.jwt.JwtClaims;
import org.gluu.oxauth.model.jwt.JwtHeader;

/**
 * JSON Web Token is a compact token format intended for space constrained
 * environments such as HTTP Authorization headers and URI query parameters.
 *
 * @author Yuriy Movchan Date: 06/30/2015
 */
public class JsonWebResponse implements Serializable {

	private static final long serialVersionUID = -4141298937204111173L;

	protected JwtHeader header;
	protected JwtClaims claims;
	
	public JsonWebResponse() {
        this.header = new JwtHeader();
        this.claims = new JwtClaims();
	}

	public JwtHeader getHeader() {
		return header;
	}

	public void setHeader(JwtHeader header) {
		this.header = header;
	}

	public JwtClaims getClaims() {
		return claims;
	}

	public void setClaims(JwtClaims claims) {
		this.claims = claims;
	}

	public String asString() {
		try {
			return claims.toJsonString();
		} catch (InvalidJwtException ex) {
			ex.printStackTrace();
		}

		return "";
	}

    @Override
    public String toString() {
        return asString();
    }
}
