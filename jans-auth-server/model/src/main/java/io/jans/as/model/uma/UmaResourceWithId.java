/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.uma;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Resource that needs protection by registering a resource description
 * at the AS.
 *
 * @author Yuriy Zabrovarnyy
 * Date: 17/05/2017
 */
@IgnoreMediaTypes("application/*+json")
// try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonPropertyOrder({"_id", "_rev", "name", "iconUri", "scopes"})
@XmlRootElement
public class UmaResourceWithId extends UmaResource {

    private String id;

    @JsonProperty(value = "_id")
    @XmlElement(name = "_id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "UmaResourceWithId [id=" + id
                + ", toString()=" + super.toString() + "]";
    }

}
