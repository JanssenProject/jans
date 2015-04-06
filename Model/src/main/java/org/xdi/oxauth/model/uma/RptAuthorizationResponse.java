/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.uma;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 03/04/2013
 */
@IgnoreMediaTypes("application/*+json") // try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonPropertyOrder({"rpt"})
@XmlRootElement
public class RptAuthorizationResponse {

    private String rpt;

    public RptAuthorizationResponse() {
    }

    public RptAuthorizationResponse(String rpt) {
        this.rpt = rpt;
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
        final StringBuilder sb = new StringBuilder();
        sb.append("AuthorizationResponse");
        sb.append("{rpt='").append(rpt).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

