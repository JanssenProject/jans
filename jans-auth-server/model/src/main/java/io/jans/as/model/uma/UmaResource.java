/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.uma;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.jans.as.model.exception.InvalidParameterException;
import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Resource that needs protection by registering a resource description at the AS.
 *
 * @author Yuriy Zabrovarnyy
 * Date: 17/05/2017
 */

@IgnoreMediaTypes("application/*+json")
// try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonPropertyOrder({"name", "uri", "type", "scopes", "scopeExpression", "icon_uri"})
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement
public class UmaResource {

    private List<String> scopes;

    private String scopeExpression;

    private String description;

    private String iconUri;

    private String name;

    private String type;

    private Integer iat;

    private Integer exp;

    @JsonProperty(value = "iat")
    @XmlElement(name = "iat")
    public Integer getIat() {
        return iat;
    }

    public void setIat(Integer iat) {
        this.iat = iat;
    }

    @JsonProperty(value = "exp")
    @XmlElement(name = "exp")
    public Integer getExp() {
        return exp;
    }

    public void setExp(Integer exp) {
        this.exp = exp;
    }

    @JsonProperty(value = "description")
    @XmlElement(name = "description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty(value = "type")
    @XmlElement(name = "type")
    public String getType() {
        return type;
    }

    public UmaResource setType(String type) {
        this.type = type;
        return this;
    }

    @JsonProperty(value = "name")
    @XmlElement(name = "name")
    public String getName() {
        return name;
    }

    public UmaResource setName(String name) {
        this.name = name;
        return this;
    }

    @JsonProperty(value = "icon_uri")
    @XmlElement(name = "icon_uri")
    public String getIconUri() {
        return iconUri;
    }

    public UmaResource setIconUri(String iconUri) {
        this.iconUri = iconUri;
        return this;
    }

    @JsonProperty(value = "resource_scopes")
    @XmlElement(name = "resource_scopes")
    public List<String> getScopes() {
        return scopes;
    }

    public UmaResource setScopes(List<String> scopes) {
        this.scopes = scopes;
        return this;
    }

    @JsonProperty(value = "scope_expression")
    @XmlElement(name = "scope_expression")
    public String getScopeExpression() {
        return scopeExpression;
    }

    public void setScopeExpression(String scopeExpression) {
        assertValidExpression(scopeExpression);
        this.scopeExpression = scopeExpression;
    }

    @SuppressWarnings("java:S112")
    @JsonIgnore
    public static void assertValidExpression(String scopeExpression) {
        if (!isValidExpression(scopeExpression)) {
            throw new RuntimeException("Scope expression is not valid json logic expression. Expression:" + scopeExpression);
        }
    }

    @JsonIgnore
    public static boolean isValidExpression(String scopeExpression) {
        return StringUtils.isBlank(scopeExpression) || JsonLogicNodeParser.isNodeValid(scopeExpression);
    }

    @Override
    public String toString() {
        return "UmaResource{" +
                "name='" + name + '\'' +
                ", scopes=" + scopes +
                ", scopeExpression=" + scopeExpression +
                ", description='" + description + '\'' +
                ", iconUri='" + iconUri + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
