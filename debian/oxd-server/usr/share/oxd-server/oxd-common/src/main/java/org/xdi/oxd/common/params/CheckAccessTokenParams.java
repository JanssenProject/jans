package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Check access token parameters.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/10/2013
 */

public class CheckAccessTokenParams implements IParams {

    /**
     * Discovery url
     */
    @JsonProperty(value = "discovery_url")
    private String discoveryUrl;

    /**
     * Id token
     */
    @JsonProperty(value = "id_token")
    private String idToken;

    /**
     * Access token
     */
    @JsonProperty(value = "access_token")
    private String accessToken;

    /**
     * Constructor
     */
    public CheckAccessTokenParams() {
    }

    /**
     * Returns discovery url
     *
     * @return discovery url
     */
    public String getDiscoveryUrl() {
        return discoveryUrl;
    }

    /**
     * Sets discovery url
     *
     * @param p_discoveryUrl discovery url
     */
    public void setDiscoveryUrl(String p_discoveryUrl) {
        discoveryUrl = p_discoveryUrl;
    }

    /**
     * Gets id token.
     *
     * @return id token
     */
    public String getIdToken() {
        return idToken;
    }

    /**
     * Sets id token.
     *
     * @param p_idToken id token
     */
    public void setIdToken(String p_idToken) {
        idToken = p_idToken;
    }

    /**
     * Gets access token.
     *
     * @return access token
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Sets access token.
     *
     * @param p_accessToken access token
     */
    public void setAccessToken(String p_accessToken) {
        accessToken = p_accessToken;
    }

    /**
     * Returns string representation of object
     *
     * @return string representation of object
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("CheckAccessTokenParams");
        sb.append("{discoveryUrl='").append(discoveryUrl).append('\'');
        sb.append(", idToken='").append(idToken).append('\'');
        sb.append(", accessToken='").append(accessToken).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
