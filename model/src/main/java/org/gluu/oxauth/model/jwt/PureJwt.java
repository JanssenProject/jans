/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.jwt;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.util.Base64Util;
import org.gluu.oxauth.model.util.Util;

import java.io.UnsupportedEncodingException;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version July 31, 2016
 */

public class PureJwt {

    private final String m_encodedHeader;
    private final String m_encodedPayload;
    private final String m_encodedSignature;
    private final String m_signingInput;

    private final String m_decodedHeader;
    private final String m_decodedPayload;

    public PureJwt(String p_encodedHeader, String p_encodedPayload, String p_encodedSignature) {

        m_encodedHeader = p_encodedHeader;
        m_encodedPayload = p_encodedPayload;
        m_encodedSignature = p_encodedSignature;
        m_signingInput = m_encodedHeader + "." + m_encodedPayload;

        String decodedPayloadTemp = null;
        String decodedHeaderTemp = null;
        try {
            decodedHeaderTemp = new String(Base64Util.base64urldecode(p_encodedHeader), Util.UTF8_STRING_ENCODING);
            decodedPayloadTemp = new String(Base64Util.base64urldecode(p_encodedPayload), Util.UTF8_STRING_ENCODING);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        m_decodedHeader = decodedHeaderTemp;
        m_decodedPayload = decodedPayloadTemp;
    }

    public static PureJwt parse(String p_encodedString) {
        if (StringUtils.isNotBlank(p_encodedString)) {
            String[] jwtParts = p_encodedString.split("\\.");
            if (jwtParts.length == 3) {
                return new PureJwt(jwtParts[0], jwtParts[1], jwtParts[2]);
            } else if (jwtParts.length == 2) {
                return new PureJwt(jwtParts[0], jwtParts[1], "");
            }
        }
        return null;
    }

    public String getDecodedHeader() {
        return m_decodedHeader;
    }

    public String getDecodedPayload() {
        return m_decodedPayload;
    }

    public String getSigningInput() {
        return m_signingInput;
    }

    public String getEncodedHeader() {
        return m_encodedHeader;
    }

    public String getEncodedPayload() {
        return m_encodedPayload;
    }

    public String getEncodedSignature() {
        return m_encodedSignature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PureJwt pureJwt = (PureJwt) o;

        if (m_encodedHeader != null ? !m_encodedHeader.equals(pureJwt.m_encodedHeader) : pureJwt.m_encodedHeader != null)
            return false;
        if (m_encodedPayload != null ? !m_encodedPayload.equals(pureJwt.m_encodedPayload) : pureJwt.m_encodedPayload != null)
            return false;
        if (m_encodedSignature != null ? !m_encodedSignature.equals(pureJwt.m_encodedSignature) : pureJwt.m_encodedSignature != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = m_encodedHeader != null ? m_encodedHeader.hashCode() : 0;
        result = 31 * result + (m_encodedPayload != null ? m_encodedPayload.hashCode() : 0);
        result = 31 * result + (m_encodedSignature != null ? m_encodedSignature.hashCode() : 0);
        return result;
    }
}
