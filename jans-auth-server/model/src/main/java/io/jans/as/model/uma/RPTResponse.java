/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.uma;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Requester permission token
 *
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 * Date: 10/16/2012
 */
@IgnoreMediaTypes("application/*+json")
// try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonPropertyOrder({"rpt"})
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement
public class RPTResponse {

    private String rpt;

    public RPTResponse() {
    }

    public RPTResponse(String token) {
        this.rpt = token;
    }

    @JsonProperty(value = "rpt")
    @XmlElement(name = "rpt")
    public String getRpt() {
        return rpt;
    }

    public void setRpt(String rpt) {
        this.rpt = rpt;
    }

    @Override
    public String toString() {
        return "RPTResponse [rpt=" + rpt + "]";
    }

}
