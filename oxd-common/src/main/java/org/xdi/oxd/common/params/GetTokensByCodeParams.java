package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/09/2015
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetTokensByCodeParams implements HasOxdIdParams {

    @JsonProperty(value = "oxd_id")
    private String oxdId;
    @JsonProperty(value = "code")
    private String code;

    public GetTokensByCodeParams() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GetTokensByCodeParams that = (GetTokensByCodeParams) o;

        return !(code != null ? !code.equals(that.code) : that.code != null) && !(oxdId != null ? !oxdId.equals(that.oxdId) : that.oxdId != null);
    }

    @Override
    public int hashCode() {
        int result = oxdId != null ? oxdId.hashCode() : 0;
        result = 31 * result + (code != null ? code.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("GetTokensByCodeParams");
        sb.append("{code='").append(code).append('\'');
        sb.append(", oxdId='").append(oxdId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
