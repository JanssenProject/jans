/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.uma;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yuriyz
 */
@IgnoreMediaTypes("application/*+json")
// try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonPropertyOrder({"ticket"})
@XmlRootElement
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
    @XmlElement(name = "rule")
    public JsonNode getRule() {
        return rule;
    }

    public void setRule(JsonNode rule) {
        this.rule = rule;
    }

    @JsonProperty(value = "data")
    @XmlElement(name = "data")
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
