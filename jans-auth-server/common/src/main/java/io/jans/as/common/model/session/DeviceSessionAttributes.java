package io.jans.as.common.model.session;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Yuriy Z
 */
@JsonIgnoreProperties(
        ignoreUnknown = true
)
public class DeviceSessionAttributes implements Serializable {

    @JsonProperty("acr_values")
    private String acrValues;

    @JsonProperty("attributes")
    private Map<String, String> attributes;

    public Map<String, String> getAttributes() {
        if (attributes == null) attributes = new HashMap<>();
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(String acrValues) {
        this.acrValues = acrValues;
    }

    @Override
    public String toString() {
        return "DeviceSessionAttributes{" +
                "acrValues='" + acrValues + '\'' +
                "attributes='" + attributes + '\'' +
                '}';
    }
}
