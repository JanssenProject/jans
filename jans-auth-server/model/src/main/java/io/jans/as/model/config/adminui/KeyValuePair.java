package io.jans.as.model.config.adminui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;
@JsonPropertyOrder({"key", "value"})
@JsonIgnoreProperties(
        ignoreUnknown = true
)
public class KeyValuePair implements Serializable {
    private String key;
    private String value;

    public KeyValuePair() {
        this("", "");
    }

    public KeyValuePair(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
