package org.gluu.oxd.common.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author yuriyz
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoveSiteParams implements HasAccessTokenParams {

    @JsonProperty(value = "oxd_id")
    private String oxd_id;
    @JsonProperty(value = "token")
    private String token;

    public RemoveSiteParams() {
    }

    public RemoveSiteParams(String oxdId) {
        this(oxdId, null);
    }

    public RemoveSiteParams(String oxdId, String token) {
        this.oxd_id = oxdId;
        this.token = token;
    }

    @Override
    public String getOxdId() {
        return oxd_id;
    }

    public void setOxdId(String oxdId) {
        this.oxd_id = oxdId;
    }

    @Override
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "RemoveSiteParams{" +
                "oxd_id='" + oxd_id + '\'' +
                ", token='" + token + '\'' +
                '}';
    }
}
