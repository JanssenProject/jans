/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.user;

import io.jans.scim.model.scim2.AttributeDefinition;
import io.jans.scim.model.scim2.annotations.Attribute;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an Instant messaging address for a user. See section 4.1.2 of RFC 7643.
 */
/*
 * Created by jgomer on 2017-09-04.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstantMessagingAddress {

    public enum Type {AIM, GTALK, ICQ, XMPP, MSN, SKYPE, QQ, YAHOO, OTHER}

    @Attribute(description = "Instant messaging address for the User.",
            isRequired = true)  //specs says false but it doesn't make sense
    private String value;

    @Attribute(description = "A human readable name, primarily used for  display purposes.")
    private String display;

    @Attribute(description = "A label indicating the attribute's function; e.g., 'aim', 'gtalk', 'mobile' etc.",
            canonicalValues = { "aim", "gtalk", "icq", "xmpp", "msn", "skype", "qq", "yahoo", "other" })
    private String type;

    @Attribute(description = "A Boolean value indicating the 'primary' or preferred attribute value for this attribute," +
            "e.g., the  preferred messenger or primary messenger. The primary attribute  value 'true' MUST appear no " +
            "more than once.",
            type= AttributeDefinition.Type.BOOLEAN)
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
