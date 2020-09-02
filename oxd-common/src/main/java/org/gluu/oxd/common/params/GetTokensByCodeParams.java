package org.gluu.oxd.common.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/09/2015
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetTokensByCodeParams implements HasOxdIdParams {

    @JsonProperty(value = "oxd_id")
    private String oxd_id;
    @JsonProperty(value = "code")
    private String code;
    @JsonProperty(value = "state")
    private String state;
    @JsonProperty(value = "authentication_method")
    private String authentication_method;
    @JsonProperty(value = "algorithm")
    private String algorithm;

    public GetTokensByCodeParams() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getOxdId() {
        return oxd_id;
    }

    public void setOxdId(String oxdId) {
        this.oxd_id = oxdId;
    }

    public String getAuthenticationMethod() {
        return authentication_method;
    }

    public void setAuthenticationMethod(String authenticationMethod) {
        this.authentication_method = authenticationMethod;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GetTokensByCodeParams that = (GetTokensByCodeParams) o;

        return !(code != null ? !code.equals(that.code) : that.code != null) && !(oxd_id != null ? !oxd_id.equals(that.oxd_id) : that.oxd_id != null);
    }

    @Override
    public int hashCode() {
        int result = oxd_id != null ? oxd_id.hashCode() : 0;
        result = 31 * result + (code != null ? code.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("GetTokensByCodeParams");
        sb.append("{code='").append(code).append('\'');
        sb.append(", oxd_id='").append(oxd_id).append('\'');
        sb.append(", state='").append(state).append('\'');
        sb.append(", authentication_method='").append(authentication_method).append('\'');
        sb.append(", algorithm='").append(algorithm).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
