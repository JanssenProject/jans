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

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/01/2013
 */

@XmlRootElement(name = "base-dn")
@XmlAccessorType(XmlAccessType.FIELD)
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
    @XmlElement(name = "federationMetadata")
    private String federationMetadata;
    @XmlElement(name = "federationRP")
    private String federationRP;
    @XmlElement(name = "federationOP")
    private String federationOP;
    @XmlElement(name = "federationRequest")
    private String federationRequest;
    @XmlElement(name = "federationTrust")
    private String federationTrust;
    @XmlElement(name = "umaBase")
    private String umaBase;
    @XmlElement(name = "umaPolicy")
    private String umaPolicy;

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

    public String getFederationMetadata() {
        return federationMetadata;
    }

    public void setFederationMetadata(String p_federationMetadata) {
        federationMetadata = p_federationMetadata;
    }

    public String getFederationOP() {
        return federationOP;
    }

    public void setFederationOP(String p_federationOP) {
        federationOP = p_federationOP;
    }

    public String getFederationRequest() {
        return federationRequest;
    }

    public void setFederationRequest(String p_federationRequest) {
        federationRequest = p_federationRequest;
    }

    public String getFederationRP() {
        return federationRP;
    }

    public void setFederationRP(String p_federationRP) {
        federationRP = p_federationRP;
    }

    public String getFederationTrust() {
        return federationTrust;
    }

    public void setFederationTrust(String p_federationTrust) {
        federationTrust = p_federationTrust;
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
}
