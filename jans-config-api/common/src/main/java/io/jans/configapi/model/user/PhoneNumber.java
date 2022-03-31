/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.model.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.orm.annotation.AttributeName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PhoneNumber {

    public enum Type {WORK, HOME, MOBILE, FAX, PAGER, OTHER}

    @AttributeName(name = "value")
    private String value;

    @AttributeName(name = "display")
    private String display;

    @AttributeName(name = "type")
    private String type;

    @AttributeName(name = "primary")
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
        return "PhoneNumber [value=" + value + ", display=" + display + ", type=" + type + ", primary=" + primary + "]";
    }
}
