package io.jans.model.authzen;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.Serializable;

/**
 * @author Yuriy Z
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Context implements Serializable {

    @JsonProperty("properties")
    private JsonNode properties;

    public JsonNode getProperties() {
        return properties;
    }

    public Context setProperties(JsonNode properties) {
        this.properties = properties;
        return this;
    }

    @Override
    public String toString() {
        return "Context{" +
                "properties=" + properties +
                '}';
    }
}
