package org.gluu.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/03/2016
 */

public class UpdateSiteResponse implements IOpResponse {

    @JsonProperty(value = "oxd_id")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "oxd_id")
    private String oxdId;

    public UpdateSiteResponse() {
    }

    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
    }
}
