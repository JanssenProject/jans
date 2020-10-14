/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.user;

import io.jans.scim.model.scim2.AttributeDefinition;
import io.jans.scim.model.scim2.annotations.Attribute;
import io.jans.scim.model.scim2.Validations;
import io.jans.scim.model.scim2.annotations.Validator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a physical mailing address for a user. See section 4.1.2 of RFC 7643.
 */
/*
 * Created by jgomer on 2017-09-04.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Address {

    public enum Type {WORK, HOME, OTHER}

    //No field is flagged as required in this class since a valid address can be any arbitrary combination of subattributes, see #1037

    @Attribute(description = "The full mailing address, formatted for display or use with a mailing label. This attribute" +
            " MAY contain newlines.")
    private String formatted;

    @Attribute(description = "The full street address component, which may include house number, street name, PO BOX, " +
            "and multi-line extended street address information. This attribute MAY contain newlines.")
    private String streetAddress;

    @Attribute(description = "The city or locality component.")
    private String locality;

    @Attribute(description = "The state or region component.")
    private String region;

    @Attribute(description = "The zipcode or postal code component.")
    private String postalCode;

    @Attribute(description = "The country name component.")
    @Validator(value = Validations.COUNTRY)
    private String country;

    @Attribute(description = "A label indicating the attribute's  function; e.g., 'work' or 'home'.",
            canonicalValues = { "work", "home", "other" })
    private String type;

    @Attribute(description = "A Boolean value indicating the 'primary'  or preferred attribute value for this attribute," +
            " e.g., the  preferred address. The primary attribute value 'true' MUST appear no more than once.",
            type = AttributeDefinition.Type.BOOLEAN)
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

    @JsonProperty
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

}
