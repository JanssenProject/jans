package org.gluu.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 31/05/2016
 */

public class RsProtectResponse implements IOpResponse {

    @JsonProperty(value = "oxd_id")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "oxd_id")
    private String oxdId;

    public RsProtectResponse() {
    }

    public RsProtectResponse(String oxdId) {
        this.oxdId = oxdId;
    }

    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
    }
}
