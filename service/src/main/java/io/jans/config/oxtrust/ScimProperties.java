/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.config.oxtrust;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScimProperties implements Serializable {

    /**
    *
    */
    private static final long serialVersionUID = -5154249316054593386L;

    private int maxCount;

    private String userExtensionSchemaURI;

    public int getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public String getUserExtensionSchemaURI() {
        return userExtensionSchemaURI;
    }

    public void setUserExtensionSchemaURI(String userExtensionSchemaURI) {
        this.userExtensionSchemaURI = userExtensionSchemaURI;
    }

}
