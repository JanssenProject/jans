/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.config.oxtrust;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.enterprise.inject.Vetoed;
import java.io.Serializable;


/**
 * Saml NameId coxTrust configuration
 *
 * @author Yuriy Movchan Date: 03/12/2018
 */

@JsonIgnoreProperties(ignoreUnknown = true)
@Vetoed
public class NameIdConfig implements Configuration, Serializable {

    private static final long serialVersionUID = 2386538577505167695L;

    private String name;
    private String sourceAttribute;
    private String nameIdType;
    private boolean enabled;

    public final String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = name;
    }

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
