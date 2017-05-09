/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jwt;

import org.apache.commons.lang.StringUtils;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.token.JsonWebResponse;

/**
 * JSON Web Token (JWT) is a compact token format intended for space constrained
 * environments such as HTTP Authorization headers and URI query parameters.
 * JWTs encode claims to be transmitted as a JSON object (as defined in RFC 4627)
 * that is base64url encoded and digitally signed. Signing is accomplished using
 * a JSON Web Signature (JWS). JWTs may also be optionally encrypted using JSON
 * Web Encryption (JWE).
 *
 * @author Javier Rojas Blum
 * @version May 3, 2017
 */
public class Jwt extends JsonWebResponse {

    private String encodedHeader;
    private String encodedClaims;
    private String encodedSignature;

    private boolean loaded = false;

    public Jwt() {
        encodedHeader = null;
        encodedClaims = null;
        encodedSignature = null;
    }

    public String getEncodedSignature() {
        return encodedSignature;
    }

    public void setEncodedSignature(String encodedSignature) {
        this.encodedSignature = encodedSignature;
    }

    public String getSigningInput() throws InvalidJwtException {
        if (loaded) {
            return encodedHeader + "." + encodedClaims;
        } else {
            return header.toBase64JsonObject() + "." + claims.toBase64JsonObject();
        }
    }

    public static Jwt parse(String encodedJwt) throws InvalidJwtException {
        if (StringUtils.isBlank(encodedJwt)) {
            return null;
        }

        String encodedHeader = null;
        String encodedClaims = null;
        String encodedSignature = null;

        String[] jwtParts = encodedJwt.split("\\.");
        if (jwtParts.length == 2) { // Signature Algorithm NONE
            encodedHeader = jwtParts[0];
            encodedClaims = jwtParts[1];
            encodedSignature = "";
        } else if (jwtParts.length == 3) {
            encodedHeader = jwtParts[0];
            encodedClaims = jwtParts[1];
            encodedSignature = jwtParts[2];
        } else {
            throw new InvalidJwtException("Invalid JWT format.");
        }

        Jwt jwt = new Jwt();
        jwt.setHeader(new JwtHeader(encodedHeader));
        jwt.setClaims(new JwtClaims(encodedClaims));
        jwt.setEncodedSignature(encodedSignature);
        jwt.encodedHeader = encodedHeader;
        jwt.encodedClaims = encodedClaims;
        jwt.loaded = true;

        return jwt;
    }

    @Override
    public String toString() {
        try {
            if (encodedSignature == null) {
                return getSigningInput() + ".";
            } else {
                return getSigningInput() + "." + encodedSignature;
            }
        } catch (InvalidJwtException e) {
            e.printStackTrace();
        }

        return "";
    }
}