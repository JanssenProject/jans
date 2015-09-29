package org.xdi.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 29/09/2015
 */

public class RegisterSiteResponse implements IOpResponse {

    @JsonProperty(value = "oxd_id")
    private String siteId;

    public RegisterSiteResponse() {
    }

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RegisterSiteResponse");
        sb.append("{siteId='").append(siteId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
