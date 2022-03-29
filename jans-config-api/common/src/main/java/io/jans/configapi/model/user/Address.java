/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.model.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.orm.annotation.AttributeName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Address {

    public enum Type {WORK, HOME, OTHER}

    @AttributeName(name = "formatted")
    private String formatted;

    @AttributeName(name = "streetAddress")
    private String streetAddress;

    @AttributeName(name = "locality")
    private String locality;

    @AttributeName(name = "region")
    private String region;

    @AttributeName(name = "postalCode")
    private String postalCode;

    @AttributeName(name = "country")
    private String country;

    @AttributeName(name = "type")
    private String type;

    @AttributeName(name = "primary")
    private Boolean primary;

    public String getFormatted() {
        return formatted;
    }

    public void setFormatted(String formatted) {
        this.formatted = formatted;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setType(Type type){
        setType(type.name().toLowerCase());
    }

    public Boolean getPrimary() {
        return primary;
    }

    public void setPrimary(Boolean primary) {
        this.primary = primary;
    }

    @Override
    public String toString() {
        return "Address [formatted=" + formatted + ", streetAddress=" + streetAddress + ", locality=" + locality
                + ", region=" + region + ", postalCode=" + postalCode + ", country=" + country + ", type=" + type
                + ", primary=" + primary + "]";
    }
    
}
