package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/08/2013
 */

public class DiscoveryParams implements IParams {

    @JsonProperty(value = "discovery_url")
    private String discoveryUrl;

    public DiscoveryParams() {
    }

    public DiscoveryParams(String p_discoveryUrl) {
        discoveryUrl = p_discoveryUrl;
    }

    public String getDiscoveryUrl() {
        return discoveryUrl;
    }

    public void setDiscoveryUrl(String p_discoveryUrl) {
        discoveryUrl = p_discoveryUrl;
    }

    /**
     * Returns string representation of object
     *
     * @return string representation of object
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("DiscoveryParams");
        sb.append("{discoveryUrl='").append(discoveryUrl).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
