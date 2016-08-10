package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/06/2016
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class RpGetGatParams implements HasOxdIdParams {

    @JsonProperty(value = "oxd_id")
    private String oxdId;
    @JsonProperty(value = "scopes")
    private List<String> scopes;

    public RpGetGatParams() {
    }

    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RpGetGatParams");
        sb.append("{oxdId='").append(oxdId).append('\'');
        sb.append(", scopes=").append(scopes);
        sb.append('}');
        return sb.toString();
    }
}
