package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 13/08/2013
 */

public class RptStatusParams implements IParams {

    @JsonProperty(value = "uma_discovery_url")
    private String umaDiscoveryUrl;
    @JsonProperty(value = "pat")
    private String patToken;
    @JsonProperty(value = "rpt")
    private String rpt;

    public RptStatusParams() {
    }

    public String getUmaDiscoveryUrl() {
        return umaDiscoveryUrl;
    }

    public void setUmaDiscoveryUrl(String p_umaDiscoveryUrl) {
        umaDiscoveryUrl = p_umaDiscoveryUrl;
    }

    public String getPatToken() {
        return patToken;
    }

    public void setPatToken(String p_patToken) {
        patToken = p_patToken;
    }

    public String getRpt() {
        return rpt;
    }

    public void setRpt(String p_rpt) {
        rpt = p_rpt;
    }

    /**
     * Returns string representation of object
     *
     * @return string representation of object
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RptStatusParams");
        sb.append("{umaDiscoveryUrl='").append(umaDiscoveryUrl).append('\'');
        sb.append(", patToken='").append(patToken).append('\'');
        sb.append(", rpt='").append(rpt).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
