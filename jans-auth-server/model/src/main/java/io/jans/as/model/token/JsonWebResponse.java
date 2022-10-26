/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.token;

import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.model.jwt.JwtHeader;
import io.jans.as.model.jwt.JwtType;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Date;

/**
 * JSON Web Token is a compact token format intended for space constrained
 * environments such as HTTP Authorization headers and URI query parameters.
 *
 * @author Yuriy Movchan Date: 06/30/2015
 */
public class JsonWebResponse implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(JsonWebResponse.class);
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

    /**
     * method to serialize header and claims
     */
    private void writeObject(java.io.ObjectOutputStream oos) throws IOException {
        try {
            oos.writeUTF(header.toBase64JsonObject());
            oos.writeUTF(claims.toBase64JsonObject());
        } catch (InvalidJwtException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * method to deserialize header and claims
     */
    private void readObject(java.io.ObjectInputStream ois) throws IOException, ClassNotFoundException, InvalidJwtException {
        JwtHeader readHeader = new JwtHeader();
        readHeader.load(ois.readUTF());
        setHeader(readHeader);

        JwtClaims readClaims = new JwtClaims();
        readClaims.load(ois.readUTF());
        setClaims(readClaims);
    }
}
