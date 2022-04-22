/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.uma;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * @author yuriyz on 06/04/2017.
 */
@IgnoreMediaTypes("application/*+json")
// try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class UmaTokenResponse implements Serializable {

    @JsonProperty(value = "access_token")
    @XmlElement(name = "access_token")
    private String accessToken;

    @JsonProperty(value = "token_type")
    @XmlElement(name = "token_type")
    private String tokenType = "Bearer";

    @JsonProperty(value = "pct")
    @XmlElement(name = "pct")
    private String pct;

    @JsonProperty(value = "upgraded")
    @XmlElement(name = "upgraded")
    private Boolean upgraded = false;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getPct() {
        return pct;
    }

    public void setPct(String pct) {
        this.pct = pct;
    }

    public Boolean getUpgraded() {
        return upgraded;
    }

    public void setUpgraded(Boolean upgraded) {
        this.upgraded = upgraded;
    }

    @Override
    public String toString() {
        return "UmaTokenResponse{" +
                "accessToken='" + accessToken + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", pct='" + pct + '\'' +
                ", upgraded=" + upgraded +
                '}';
    }
}
