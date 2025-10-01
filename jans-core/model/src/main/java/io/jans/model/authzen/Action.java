package io.jans.model.authzen;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.Serializable;

/**
 * @author Yuriy Z
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Action implements Serializable {

    @JsonProperty("name")
    private String name;

    @JsonProperty("properties")
    private JsonNode properties;

    public String getName() {
        return name;
    }

    public Action setName(String name) {
        this.name = name;
        return this;
    }

    public JsonNode getProperties() {
        return properties;
    }

    public Action setProperties(JsonNode properties) {
        this.properties = properties;
        return this;
    }

    @Override
    public String toString() {
        return "Action{" +
                "name='" + name + '\'' +
                ", properties=" + properties +
                '}';
    }
}
