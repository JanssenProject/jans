/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.uma;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author yuriyz on 05/30/2017.
 */
public class ClaimDefinition implements Serializable {

    @JsonProperty(value = "claim_token_format")
    @XmlElement(name = "claim_token_format")
    private List<String> claimTokenFormat;

    @JsonProperty(value = "claim_type")
    @XmlElement(name = "claim_type")
    private String claimType;

    @JsonProperty(value = "friendly_name")
    @XmlElement(name = "friendly_name")
    private String friendlyName;

    @JsonProperty(value = "issuer")
    @XmlElement(name = "issuer")
    private List<String> issuer;

    @JsonProperty(value = "name")
    @XmlElement(name = "name")
    private String name;

    public ClaimDefinition() {
    }

    public ClaimDefinition(String claimType, String friendlyName, String issuer, String name) {
        this(claimType, friendlyName, new ArrayList<String>(Collections.singletonList(issuer)), name);
    }

    public ClaimDefinition(String claimType, String friendlyName, List<String> issuer, String name) {
        this.claimType = claimType;
        this.friendlyName = friendlyName;
        this.issuer = issuer;
        this.name = name;

        this.claimTokenFormat = new ArrayList<String>();
        this.claimTokenFormat.add("http://openid.net/specs/openid-connect-core-1_0.html#IDToken");
    }

    public ClaimDefinition(List<String> claimTokenFormat, String claimType, String friendlyName, List<String> issuer, String name) {
        this.claimTokenFormat = claimTokenFormat;
        this.claimType = claimType;
        this.friendlyName = friendlyName;
        this.issuer = issuer;
        this.name = name;
    }

    public List<String> getClaimTokenFormat() {
        return claimTokenFormat;
    }

    public void setClaimTokenFormat(List<String> claimTokenFormat) {
        this.claimTokenFormat = claimTokenFormat;
    }

    public String getClaimType() {
        return claimType;
    }

    public void setClaimType(String claimType) {
        this.claimType = claimType;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public List<String> getIssuer() {
        return issuer;
    }

    public void setIssuer(List<String> issuer) {
        this.issuer = issuer;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "ClaimDefinition{" + "claimTokenFormat=" + claimTokenFormat + ", claimType='" + claimType + '\'' + ", friendlyName='" + friendlyName
                + '\'' + ", issuer=" + issuer + ", name='" + name + '\'' + '}';
    }
}
