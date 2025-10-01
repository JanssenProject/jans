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
 * A class used to represent the URI of a user's photo. See section 4.1.2 of RFC 7643.
 */
/*
 * Created by jgomer on 2017-09-04.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Photo {

    public enum Type {PHOTO, THUMBNAIL}

    @Attribute(description = "URI of a photo of the User.",
            isRequired = true,  //specs says false but it doesn't make sense
            referenceTypes = { "external" },
            type = AttributeDefinition.Type.REFERENCE)
    @Validator(value = Validations.PHOTO)
    private String value;

    @Attribute(description = "A human readable name, primarily used for display purposes.")
    private String display;

    @Attribute(description = "A label indicating the attribute's function; e.g., 'photo' or 'thumbnail'.",
            canonicalValues = { "photo", "thumbnail" })
    private String type;

    @Attribute(description = "A Boolean value indicating the 'primary'  or preferred attribute value for this attribute, " +
            "e.g., the preferred messenger or primary messenger. The primary attribute value 'true' MUST appear no more " +
            "than once.",
            type = AttributeDefinition.Type.BOOLEAN)
    private Boolean primary;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
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
