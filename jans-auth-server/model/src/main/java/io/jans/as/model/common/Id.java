/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 26/06/2013
 */
@IgnoreMediaTypes("application/*+json")
@XmlRootElement
public class Id implements Serializable {

    private String idValue;

    public Id() {
    }

    public Id(String id) {
        this.idValue = id;
    }

    @JsonProperty(value = "id")
    @XmlElement(name = "id")
    public String getId() {
        return idValue;
    }

    public void setId(String id) {
        this.idValue = id;
    }

    @Override
    public String toString() {
        return "Id" +
                "{id='" + idValue + '\'' +
                '}';
    }
}
