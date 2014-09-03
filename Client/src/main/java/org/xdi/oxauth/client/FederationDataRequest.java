package org.xdi.oxauth.client;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xdi.oxauth.model.federation.FederationRequest;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 08/10/2012
 */

public class FederationDataRequest extends BaseRequest {

    private String federationId;
    private FederationRequest.Type type;
    private String displayName;
    private String opId;
    private String domain;
    private String redirectUri;
    private String x509pem;
    private String x509url;

    public FederationDataRequest() {
    }

    public FederationDataRequest(FederationRequest.Type p_type) {
        type = p_type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String p_displayName) {
        displayName = p_displayName;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String p_domain) {
        domain = p_domain;
    }

    public String getFederationId() {
        return federationId;
    }

    public void setFederationId(String p_federationId) {
        federationId = p_federationId;
    }

    public String getOpId() {
        return opId;
    }

    public void setOpId(String p_opId) {
        opId = p_opId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String p_redirectUri) {
        redirectUri = p_redirectUri;
    }

    public FederationRequest.Type getType() {
        return type;
    }

    public void setType(FederationRequest.Type p_type) {
        type = p_type;
    }

    public String getX509pem() {
        return x509pem;
    }

    public void setX509pem(String p_x509pem) {
        x509pem = p_x509pem;
    }

    public String getX509url() {
        return x509url;
    }

    public void setX509url(String p_x509url) {
        x509url = p_x509url;
    }

    public Map<String, String> getParameters() {
        final Map<String, String> map = new HashMap<String, String>();
        if (StringUtils.isNotBlank(federationId)) {
            map.put("federation_id", federationId);
        }
        if (type != null) {
            map.put("entity_type", type.getValue());
        }
        if (StringUtils.isNotBlank(displayName)) {
            map.put("display_name", displayName);
        }
        if (StringUtils.isNotBlank(opId)) {
            map.put("op_id", opId);
        }
        if (StringUtils.isNotBlank(domain)) {
            map.put("domain", domain);
        }
        if (StringUtils.isNotBlank(redirectUri)) {
            map.put("redirect_uri", redirectUri);
        }
        if (StringUtils.isNotBlank(x509pem)) {
            map.put("x509_pem", x509pem);
        }
        if (StringUtils.isNotBlank(x509url)) {
            map.put("x509_url", x509url);
        }
        return map;
    }

    @Override
    public String getQueryString() {
        final StringBuilder sb = new StringBuilder();

        try {
            if (StringUtils.isNotBlank(federationId)) {
                sb.append("federation_id=").append(URLEncoder.encode(federationId, "UTF-8"));
            }
            if (type != null) {
                sb.append("&")
                        .append("entity_type=")
                        .append(URLEncoder.encode(type.getValue(), "UTF-8"));
            }
            if (StringUtils.isNotBlank(displayName)) {
                sb.append("&")
                        .append("display_name=")
                        .append(URLEncoder.encode(displayName, "UTF-8"));
            }
            if (StringUtils.isNotBlank(opId)) {
                sb.append("&")
                        .append("op_id=")
                        .append(URLEncoder.encode(opId, "UTF-8"));
            }
            if (StringUtils.isNotBlank(domain)) {
                sb.append("&")
                        .append("domain=")
                        .append(URLEncoder.encode(domain, "UTF-8"));
            }
            if (StringUtils.isNotBlank(redirectUri)) {
                sb.append("&")
                        .append("redirect_uri=")
                        .append(URLEncoder.encode(redirectUri, "UTF-8"));
            }
            if (StringUtils.isNotBlank(x509pem)) {
                sb.append("&")
                        .append("x509_pem=")
                        .append(URLEncoder.encode(x509pem, "UTF-8"));
            }
            if (StringUtils.isNotBlank(x509url)) {
                sb.append("&")
                        .append("x509_url=")
                        .append(URLEncoder.encode(x509url, "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
