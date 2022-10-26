/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.uma;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * UMA Permission. Used for both:
 * 1. register permission ticket
 * 2. by introspection RPT endpoint to return RPT status.
 *
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 */

// try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@IgnoreMediaTypes("application/*+json")
@JsonPropertyOrder({"resource_id", "resource_scopes", "exp"})
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement
public class UmaPermission implements Serializable {

    private String resourceId;
    private List<String> scopes;
    private Integer expiresAt;

    private Map<String, String> params;

    public UmaPermission() {
    }

    public UmaPermission(String resourceId, List<String> scopes) {
        this.resourceId = resourceId;
        this.scopes = scopes;
    }

    @JsonProperty(value = "resource_id")
    @XmlElement(name = "resource_id")
    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    @JsonProperty(value = "resource_scopes")
    @XmlElement(name = "resource_scopes")
    public List<String> getScopes() {
        if (scopes == null) {
            scopes = new ArrayList<>();
        }
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    @JsonProperty(value = "exp")
    @XmlElement(name = "exp")
    public Integer getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Integer expiresAt) {
        this.expiresAt = expiresAt;
    }

    @JsonProperty(value = "params")
    @XmlElement(name = "params")
    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return "UmaPermission{" +
                "resourceId='" + resourceId + '\'' +
                ", scopes=" + scopes +
                ", expiresAt=" + expiresAt +
                '}';
    }
}
