/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.gluu;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import java.util.Map;
import java.util.Set;

/**
 * Created by eugeniuparvan on 8/5/16.
 */
@IgnoreMediaTypes("application/*+json")
@JsonPropertyOrder({
        "id_generation_endpoint",
        "introspection_endpoint",
        "auth_level_mapping",
        "scope_to_claims_mapping"
})
public class GluuConfiguration {

    @JsonProperty(value = "id_generation_endpoint")
    private String idGenerationEndpoint;

    @JsonProperty(value = "introspection_endpoint")
    private String introspectionEndpoint;

    @JsonProperty(value = "auth_level_mapping")
    private Map<Integer, Set<String>> authLevelMapping;

    @JsonProperty(value = "scope_to_claims_mapping")
    private Map<String, Set<String>> scopeToClaimsMapping;


    public String getIdGenerationEndpoint() {
        return idGenerationEndpoint;
    }

    public void setIdGenerationEndpoint(String idGenerationEndpoint) {
        this.idGenerationEndpoint = idGenerationEndpoint;
    }

    public String getIntrospectionEndpoint() {
        return introspectionEndpoint;
    }

    public void setIntrospectionEndpoint(String introspectionEndpoint) {
        this.introspectionEndpoint = introspectionEndpoint;
    }

    public Map<Integer, Set<String>> getAuthLevelMapping() {
        return authLevelMapping;
    }

    public void setAuthLevelMapping(Map<Integer, Set<String>> authLevelMapping) {
        this.authLevelMapping = authLevelMapping;
    }

    public Map<String, Set<String>> getScopeToClaimsMapping() {
        return scopeToClaimsMapping;
    }

    public void setScopeToClaimsMapping(Map<String, Set<String>> scopeToClaimsMapping) {
        this.scopeToClaimsMapping = scopeToClaimsMapping;
    }

    @Override
    public String toString() {
        return "GluuConfiguration{" +
                "idGenerationEndpoint='" + idGenerationEndpoint + '\'' +
                ", introspectionEndpoint='" + introspectionEndpoint + '\'' +
                ", authLevelMapping=" + authLevelMapping +
                ", scopeToClaimsMapping=" + scopeToClaimsMapping +
                '}';
    }
}
