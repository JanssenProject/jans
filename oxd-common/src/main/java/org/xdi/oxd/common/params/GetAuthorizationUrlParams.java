package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/09/2015
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetAuthorizationUrlParams implements HasOxdIdParams {

    @JsonProperty(value = "oxd_id")
    private String oxdId;
    @JsonProperty(value = "acr_values")
    private List<String> acrValues;
    @JsonProperty(value = "prompt")
    private String prompt;

    public GetAuthorizationUrlParams() {
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
    }

    public List<String> getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(List<String> acrValues) {
        this.acrValues = acrValues;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GetAuthorizationUrlParams that = (GetAuthorizationUrlParams) o;

        if (acrValues != null ? !acrValues.equals(that.acrValues) : that.acrValues != null) return false;
        if (oxdId != null ? !oxdId.equals(that.oxdId) : that.oxdId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = oxdId != null ? oxdId.hashCode() : 0;
        result = 31 * result + (acrValues != null ? acrValues.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("GetAuthorizationUrlParams");
        sb.append("{acrValues=").append(acrValues);
        sb.append(", oxdId='").append(oxdId).append('\'');
        sb.append(", prompt='").append(prompt).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
