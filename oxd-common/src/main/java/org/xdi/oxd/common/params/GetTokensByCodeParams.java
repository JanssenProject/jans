package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/09/2015
 */

public class GetTokensByCodeParams implements HasOxdIdParams {

    @JsonProperty(value = "oxd_id")
    private String oxdId;
    @JsonProperty(value = "code")
    private String code;
    @JsonProperty(value = "state")
    private String state;
    @JsonProperty(value = "scopes")
    private List<String> scopes;

    public GetTokensByCodeParams() {
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GetTokensByCodeParams that = (GetTokensByCodeParams) o;

        if (code != null ? !code.equals(that.code) : that.code != null) return false;
        if (oxdId != null ? !oxdId.equals(that.oxdId) : that.oxdId != null) return false;
        return !(state != null ? !state.equals(that.state) : that.state != null);
    }

    @Override
    public int hashCode() {
        int result = oxdId != null ? oxdId.hashCode() : 0;
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + (state != null ? state.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("GetTokensByCodeParams");
        sb.append("{code='").append(code).append('\'');
        sb.append(", oxdId='").append(oxdId).append('\'');
        sb.append(", state='").append(state).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
