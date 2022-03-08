/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.persistence.model.configuration;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.jans.as.model.util.Util;
import io.jans.model.ldap.GluuLdapConfiguration;
import io.jans.orm.couchbase.model.CouchbaseConnectionConfiguration;
import io.jans.orm.sql.model.SqlConnectionConfiguration;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * oxIDPAuthConf
 *
 * @author Reda Zerrad Date: 08.14.2012
 */

@XmlRootElement(name = "oxIDPAuthConf")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonPropertyOrder({"type", "name", "level", "priority", "enabled", "version", "fields", "config"})
@XmlType(propOrder = {"type", "name", "level", "priority", "enabled", "version", "fields", "config"})
public class IDPAuthConf {
    private String type;
    private String name;

    private int level;
    private int priority;
    private boolean enabled;
    private List<CustomProperty> fields;
    private int version;

    private JsonNode config;

    public IDPAuthConf() {
        this.fields = new ArrayList<CustomProperty>();
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean getEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<CustomProperty> getFields() {
        return this.fields;
    }

    public void setFields(List<CustomProperty> fields) {
        this.fields = fields;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public JsonNode getConfig() {
        return config;
    }

    public void setConfig(JsonNode config) {
        this.config = config;
    }

    public GluuLdapConfiguration asLdapConfiguration() {
        return read(GluuLdapConfiguration.class);
    }

    public CouchbaseConnectionConfiguration asCouchbaseConfiguration() {
        return read(CouchbaseConnectionConfiguration.class);
    }

    public SqlConnectionConfiguration asSqlConfiguration() {
        return read(SqlConnectionConfiguration.class);
    }

    private <T> T read(Class<T> clazz) {
        try {
            if (config != null) {
                return Util.createJsonMapper().treeToValue(config, clazz);
            }
        } catch (JsonProcessingException e) {
            // ignore
        }
        return null;
    }
}
