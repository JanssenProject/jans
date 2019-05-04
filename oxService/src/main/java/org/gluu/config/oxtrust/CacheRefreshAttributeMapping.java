/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.config.oxtrust;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;


/**
 * Attribute mapping
 *
 * @author Yuriy Movchan Date: 10/23/2015
 */
@JsonPropertyOrder({ "source", "destination" })
public class CacheRefreshAttributeMapping implements Serializable {

    private static final long serialVersionUID = 8040484460012448855L;

    private String source;
    private String destination;

    public CacheRefreshAttributeMapping() {
    }

    public CacheRefreshAttributeMapping(String source, String destination) {
        this.source = source;
        this.destination = destination;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

}
