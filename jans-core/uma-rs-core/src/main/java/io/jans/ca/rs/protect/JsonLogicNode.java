package io.jans.ca.rs.protect;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yuriyz
 */
@JsonPropertyOrder({"ticket"})
public class JsonLogicNode {

    private JsonNode rule;
    private List<String> data;

    public JsonLogicNode() {
    }

    public JsonLogicNode(JsonNode rule, List<String> data) {
        this.rule = rule;
        this.data = data;
    }

    @JsonIgnore
    public boolean isValid() {
        return data != null && !data.isEmpty() && rule != null;
    }

    @JsonProperty(value = "rule")
    public JsonNode getRule() {
        return rule;
    }

    public void setRule(JsonNode rule) {
        this.rule = rule;
    }

    @JsonProperty(value = "data")
    public List<String> getData() {
        return data;
    }

    public void setData(List<String> data) {
        this.data = data;
    }

    @JsonIgnore
    public List<String> getDataCopy() {
        if (data == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(data);
    }

    @Override
    public String toString() {
        return "JsonLogicNode{" +
                "rule=" + rule +
                ", data=" + data +
                '}';
    }
}

