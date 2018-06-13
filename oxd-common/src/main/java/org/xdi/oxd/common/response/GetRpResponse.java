package org.xdi.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author yuriyz
 */
public class GetRpResponse implements IOpResponse {

    @JsonProperty(value = "rp")
    private String rp;

    public GetRpResponse() {
    }

    public GetRpResponse(String rp) {
        this.rp = rp;
    }

    public String getRp() {
        return rp;
    }

    public void setRp(String rp) {
        this.rp = rp;
    }

    @Override
    public String toString() {
        return "GetRpResponse{" +
                "rp='" + rp + '\'' +
                '}';
    }
}
