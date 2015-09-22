package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/09/2015
 */

public class GetAuthorizationUrlParams implements IParams {

    @JsonProperty(value = "oxd_id")
    private String oxdId;

    public GetAuthorizationUrlParams() {
    }

    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GetAuthorizationUrlParams that = (GetAuthorizationUrlParams) o;

        return !(oxdId != null ? !oxdId.equals(that.oxdId) : that.oxdId != null);

    }

    @Override
    public int hashCode() {
        return oxdId != null ? oxdId.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("GetAuthorizationUrl");
        sb.append("{oxdId='").append(oxdId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
