/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.user;

import io.jans.scim.model.scim2.AttributeDefinition;
import io.jans.scim.model.scim2.annotations.Attribute;
import io.jans.scim.model.scim2.annotations.StoreReference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a group to which a user belongs. See section 4.1.2 of RFC 7643.
 */
/*
 * Created by jgomer on 2017-09-12.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Group {

    public enum Type {DIRECT, INDIRECT}

    @Attribute(description = "The identifier of the User's group.",
            isRequired = true,  //Specs says the converse, but doesn't make sense
            mutability = AttributeDefinition.Mutability.READ_ONLY)
    @StoreReference(ref = "memberOf")
    private String value;

    @Attribute(description = "The URI of the corresponding Group resource to which the user belongs",
            referenceTypes = { "User", "Group" },
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            type = AttributeDefinition.Type.REFERENCE)
    @JsonProperty("$ref")
    private String ref;

    @Attribute(description = "A human readable name, primarily used for display purposes.",
            mutability = AttributeDefinition.Mutability.READ_ONLY)
    private String display;

    @Attribute(description = "A label indicating the attribute's function; e.g., 'direct' or 'indirect'.",
            canonicalValues = { "direct", "indirect" },
            mutability = AttributeDefinition.Mutability.READ_ONLY)
    private String type;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        result = prime * result + ((ref == null) ? 0 : ref.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((display == null) ? 0 : display.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Group other = (Group) obj;
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        if (ref == null) {
            if (other.ref != null) {
                return false;
            }
        } else if (!ref.equals(other.ref)) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        if (display == null) {
            if (other.display != null) {
                return false;
            }
        } else if (!display.equals(other.display)) {
            return false;
        }
        return true;
    }

}
