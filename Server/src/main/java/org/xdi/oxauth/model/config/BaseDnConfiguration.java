/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version January 15, 2016
 */

@XmlRootElement(name = "base-dn")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseDnConfiguration {
    @XmlElement(name = "appliance")
    private String appliance;
    @XmlElement(name = "people")
    private String people;
    @XmlElement(name = "groups")
    private String groups;
    @XmlElement(name = "clients")
    private String clients;
    @XmlElement(name = "scopes")
    private String scopes;
    @XmlElement(name = "attributes")
    private String attributes;
    @XmlElement(name = "scripts")
    private String scripts;
    @XmlElement(name = "sessionId")
    private String sessionId;
    @XmlElement(name = "umaBase")
    private String umaBase;
    @XmlElement(name = "umaPolicy")
    private String umaPolicy;
    @XmlElement(name = "u2fBase")
    private String u2fBase;
    @XmlElement(name = "metric")
    private String metric;
    @XmlElement(name = "sectorIdentifiers")
    private String sectorIdentifiers;

    public String getUmaBase() {
        return umaBase;
    }

    public void setUmaBase(String p_umaBase) {
        umaBase = p_umaBase;
    }

    public String getUmaPolicy() {
        return umaPolicy;
    }

    public void setUmaPolicy(String p_umaPolicy) {
        umaPolicy = p_umaPolicy;
    }

    public String getAppliance() {
        return appliance;
    }

    public void setAppliance(String p_appliance) {
        appliance = p_appliance;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String p_attributes) {
        attributes = p_attributes;
    }

    public String getScripts() {
        return scripts;
    }

    public void setScripts(String scripts) {
        this.scripts = scripts;
    }

    public String getClients() {
        return clients;
    }

    public void setClients(String p_clients) {
        clients = p_clients;
    }

    public String getPeople() {
        return people;
    }

    public void setPeople(String p_people) {
        people = p_people;
    }

    public String getGroups() {
        return groups;
    }

    public void setGroups(String groups) {
        this.groups = groups;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String p_scopes) {
        scopes = p_scopes;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String p_sessionId) {
        sessionId = p_sessionId;
    }

    public String getU2fBase() {
        return u2fBase;
    }

    public void setU2fBase(String u2fBase) {
        this.u2fBase = u2fBase;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public String getSectorIdentifiers() {
        return sectorIdentifiers;
    }

    public void setSectorIdentifiers(String sectorIdentifiers) {
        this.sectorIdentifiers = sectorIdentifiers;
    }
}
