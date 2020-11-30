/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.token;

import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.model.jwt.JwtHeader;

import java.io.Serializable;

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

    public void setClaim(String key, String value) {
	    if (claims == null) {
	        return;
        }
	    claims.setClaim(key, value);
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
