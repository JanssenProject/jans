/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.config.oxtrust;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.enterprise.inject.Vetoed;
import java.io.Serializable;


/**
 * Saml NameId cJanssen Project configuration
 *
 * @author Yuriy Movchan Date: 03/12/2018
 */

@JsonIgnoreProperties(ignoreUnknown = true)
@Vetoed
public class NameIdConfig implements Configuration, Serializable {

    private static final long serialVersionUID = 2386538577505167695L;

    private String sourceAttribute;
    private String nameIdType;
    private boolean enabled;

    public final String getSourceAttribute() {
        return sourceAttribute;
    }

    public final void setSourceAttribute(String sourceAttribute) {
        this.sourceAttribute = sourceAttribute;
    }

    public String getNameIdType() {
        return nameIdType;
    }

    public void setNameIdType(String nameIdType) {
        this.nameIdType = nameIdType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
