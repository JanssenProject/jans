/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.lang.StringUtils;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 27/09/2012
 */

public class FederationMetadataRequest extends BaseRequest {

    private String m_federationId;
    private boolean m_signed = true;

    public FederationMetadataRequest() {
        this("");
    }

    public FederationMetadataRequest(String p_federationId) {
        m_federationId = p_federationId;
    }

    public boolean isSigned() {
        return m_signed;
    }

    public void setSigned(boolean p_signed) {
        m_signed = p_signed;
    }

    public String getFederationId() {
        return m_federationId;
    }

    public void setFederationId(String p_federationId) {
        m_federationId = p_federationId;
    }

    /**
     * Returns a query string with the parameters of the federation metadata request.
     * Any <code>null</code> or empty parameter will be omitted.
     *
     * @return A query string of parameters.
     */
    @Override
    public String getQueryString() {
        final StringBuilder sb = new StringBuilder();

        try {
            if (StringUtils.isNotBlank(m_federationId)) {
                sb.append("federation_id=").append(URLEncoder.encode(m_federationId, "UTF-8"));
            }
            if (!m_signed) {
                sb.append("&signed=").append(URLEncoder.encode(Boolean.valueOf(m_signed).toString(), "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }
}
