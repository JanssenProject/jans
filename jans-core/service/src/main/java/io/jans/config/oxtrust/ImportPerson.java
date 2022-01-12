/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.config.oxtrust;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;


/**
 * Janssen Project configuration
 *
 * @author shekhar laad
 * @date 12/10/2015
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImportPerson implements Serializable {

    private static final long serialVersionUID = -1054551412278023779L;

    private String ldapName;
    private String displayName;
    private String dataType;
    private Boolean required;

    public String getLdapName() {
        return ldapName;
    }

    public void setLdapName(String ldapName) {
        this.ldapName = ldapName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }
}
