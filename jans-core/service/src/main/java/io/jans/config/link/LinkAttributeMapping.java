/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.config.link;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;


/**
 * Attribute mapping
 *
 * @author Yuriy Movchan Date: 10/23/2015
 */
@JsonPropertyOrder({ "source", "destination" })
public class LinkAttributeMapping implements Serializable {

    private static final long serialVersionUID = 8040484460012448855L;

    private String source;
    private String destination;

    public LinkAttributeMapping() {
    }

    public LinkAttributeMapping(String source, String destination) {
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
