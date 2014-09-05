/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.uma;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

/**
 * Request for getting token status
 * 
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 * Date: 10/23/2012
 */
@IgnoreMediaTypes("application/*+json") // try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonPropertyOrder({ "token", "resource_id" })
@XmlRootElement
public class RptStatusRequest {

	private String rpt;
	private String resourceSetId;

	public RptStatusRequest() {
    }

    public RptStatusRequest(String p_rpt) {
        this.rpt = p_rpt;
    }

    public RptStatusRequest(String rpt, String resourceSetId) {
		this.rpt = rpt;
		this.resourceSetId = resourceSetId;
	}

    @JsonProperty(value = "token")
	@XmlElement(name = "token")
	public String getRpt() {
		return rpt;
	}

	public void setRpt(String rpt) {
		this.rpt = rpt;
	}

    @JsonProperty(value = "resource_id")
	@XmlElement(name = "resource_id")
	public String getResourceSetId() {
		return resourceSetId;
	}

	public void setResourceSetId(String resourceSetId) {
		this.resourceSetId = resourceSetId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RptStatusRequest [rpt=");
		builder.append(rpt);
		builder.append(", resourceSetId=");
		builder.append(resourceSetId);
		builder.append("]");
		return builder.toString();
	}

}
