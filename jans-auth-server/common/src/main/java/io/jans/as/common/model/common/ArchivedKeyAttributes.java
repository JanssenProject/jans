package io.jans.as.common.model.common;

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
public class ArchivedKeyAttributes implements Serializable {

    @JsonProperty("attributes")
    private Map<String, String> attributes;

    public Map<String, String> getAttributes() {
        if (attributes == null) attributes = new HashMap<>();
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return "ArchivedKeyAttributes{" +
                "attributes=" + attributes +
                '}';
    }
}
