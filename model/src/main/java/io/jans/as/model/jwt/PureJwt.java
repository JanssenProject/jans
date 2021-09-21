/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jwt;

import io.jans.as.model.util.Base64Util;
import org.apache.commons.lang.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version July 31, 2016
 */

public class PureJwt {

    private final String encodedHeader;
    private final String encodedPayload;
    private final String encodedSignature;
    private final String signingInput;

    private final String decodedHeader;
    private final String decodedPayload;

    public PureJwt(String encodedHeader, String encodedPayload, String encodedSignature) {

        this.encodedHeader = encodedHeader;
        this.encodedPayload = encodedPayload;
        this.encodedSignature = encodedSignature;
        this.signingInput = this.encodedHeader + "." + this.encodedPayload;

        String decodedPayloadTemp = null;
        String decodedHeaderTemp = null;
        decodedHeaderTemp = new String(Base64Util.base64urldecode(encodedHeader), StandardCharsets.UTF_8);
        decodedPayloadTemp = new String(Base64Util.base64urldecode(encodedPayload), StandardCharsets.UTF_8);
        this.decodedHeader = decodedHeaderTemp;
        this.decodedPayload = decodedPayloadTemp;
    }

    public static PureJwt parse(String encodedString) {
        if (StringUtils.isNotBlank(encodedString)) {
            String[] jwtParts = encodedString.split("\\.");
            if (jwtParts.length == 3) {
                return new PureJwt(jwtParts[0], jwtParts[1], jwtParts[2]);
            } else if (jwtParts.length == 2) {
                return new PureJwt(jwtParts[0], jwtParts[1], "");
            }
        }
        return null;
    }

    public String getDecodedHeader() {
        return decodedHeader;
    }

    public String getDecodedPayload() {
        return decodedPayload;
    }

    public String getSigningInput() {
        return signingInput;
    }

    public String getEncodedHeader() {
        return encodedHeader;
    }

    public String getEncodedPayload() {
        return encodedPayload;
    }

    public String getEncodedSignature() {
        return encodedSignature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PureJwt pureJwt = (PureJwt) o;

        if (encodedHeader != null ? !encodedHeader.equals(pureJwt.encodedHeader) : pureJwt.encodedHeader != null)
            return false;
        if (encodedPayload != null ? !encodedPayload.equals(pureJwt.encodedPayload) : pureJwt.encodedPayload != null)
            return false;
        return encodedSignature != null ? encodedSignature.equals(pureJwt.encodedSignature) : pureJwt.encodedSignature == null;
    }

    @Override
    public int hashCode() {
        int result = encodedHeader != null ? encodedHeader.hashCode() : 0;
        result = 31 * result + (encodedPayload != null ? encodedPayload.hashCode() : 0);
        result = 31 * result + (encodedSignature != null ? encodedSignature.hashCode() : 0);
        return result;
    }
}
