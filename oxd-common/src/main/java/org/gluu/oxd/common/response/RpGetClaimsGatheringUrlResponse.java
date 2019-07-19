package org.gluu.oxd.common.response;


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/06/2016
 */

public class RpGetClaimsGatheringUrlResponse implements IOpResponse {

    @JsonProperty(value = "url")
    private String url;
    @JsonProperty(value = "state")
    private String state;

    public RpGetClaimsGatheringUrlResponse() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "RpGetClaimsGatheringUrlResponse{" +
                "url='" + url + '\'' +
                "state='" + state + '\'' +
                '}';
    }
}
