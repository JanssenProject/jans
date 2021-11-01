/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jwt;

import org.apache.commons.lang.StringUtils;

import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.token.JsonWebResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(Jwt.class);

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

    public static Jwt parseOrThrow(String encodedJwt) throws InvalidJwtException {
        final Jwt jwt = parse(encodedJwt);
        if (jwt == null)
            throw new InvalidJwtException("Jwt is null");
        return jwt;
    }

    public static Jwt parseSilently(String encodedJwt) {
        try {
            return parse(encodedJwt);
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
            return null;
        }
    }

    @NotNull
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