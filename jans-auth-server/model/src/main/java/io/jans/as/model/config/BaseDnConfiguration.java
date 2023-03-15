/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version January 15, 2016
 */

@XmlRootElement(name = "base-dn")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseDnConfiguration {
    @XmlElement(name = "configuration")
    private String configuration;
    @XmlElement(name = "people")
    private String people;
    @XmlElement(name = "groups")
    private String groups;
    @XmlElement(name = "clients")
    private String clients;
    @XmlElement(name = "sessions")
    private String sessions;
    @XmlElement(name = "tokens")
    private String tokens;
    @XmlElement(name = "authorizations")
    private String authorizations;
    @XmlElement(name = "scopes")
    private String scopes;
    @XmlElement(name = "attributes")
    private String attributes;
    @XmlElement(name = "scripts")
    private String scripts;
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
    @XmlElement(name = "ciba")
    private String ciba;
    @XmlElement(name = "stat")
    private String stat;
    @XmlElement(name = "par")
    private String par;
    @XmlElement(name = "ssa")
    private String ssa;
	@XmlElement(name = "fido2Attestation")
	private String fido2Attestation;
	@XmlElement(name = "fido2Assertion")
	private String fido2Assertion;

    public String getStat() {
        return stat;
    }

    public void setStat(String stat) {
        this.stat = stat;
    }

    public String getPar() {
        return par;
    }

    public void setPar(String par) {
        this.par = par;
    }

    public String getAuthorizations() {
        return authorizations;
    }

    public void setAuthorizations(String authorizations) {
        this.authorizations = authorizations;
    }

    public String getUmaBase() {
        return umaBase;
    }

    public void setUmaBase(String umaBase) {
        this.umaBase = umaBase;
    }

    public String getUmaPolicy() {
        return umaPolicy;
    }

    public void setUmaPolicy(String umaPolicy) {
        this.umaPolicy = umaPolicy;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
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

    public void setClients(String clients) {
        this.clients = clients;
    }

    public String getSessions() {
        return sessions;
    }

    public void setSessions(String sessions) {
        this.sessions = sessions;
    }

    public String getTokens() {
        return tokens;
    }

    public void setTokens(String tokens) {
        this.tokens = tokens;
    }

    public String getPeople() {
        return people;
    }

    public void setPeople(String people) {
        this.people = people;
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

    public void setScopes(String scopes) {
        this.scopes = scopes;
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

    public String getCiba() {
        return ciba;
    }

    public void setCiba(String ciba) {
        this.ciba = ciba;
    }

    public String getSsa() {
        return ssa;
    }

    public void setSsa(String ssa) {
        this.ssa = ssa;
    }

	public String getFido2Attestation() {
		return fido2Attestation;
	}

	public void setFido2Attestation(String fido2Attestation) {
		this.fido2Attestation = fido2Attestation;
	}

	public String getFido2Assertion() {
		return fido2Assertion;
	}

	public void setFido2Assertion(String fido2Assertion) {
		this.fido2Assertion = fido2Assertion;
	}

}
