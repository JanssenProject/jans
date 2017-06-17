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
 * Resource set permission ticket
 *
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 */
@IgnoreMediaTypes("application/*+json") // try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonPropertyOrder({ "ticket" })
@XmlRootElement
public class PermissionTicket {

	private String ticket;

	public PermissionTicket() {
    }

	public PermissionTicket(String ticket) {
		this.ticket = ticket;
	}

    @JsonProperty(value = "ticket")
    @XmlElement(name = "ticket")
	public String getTicket() {
		return ticket;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		PermissionTicket that = (PermissionTicket) o;

		return !(ticket != null ? !ticket.equals(that.ticket) : that.ticket != null);

	}

	@Override
	public int hashCode() {
		return ticket != null ? ticket.hashCode() : 0;
	}

	@Override
	public String toString() {
		return "PermissionTiket [ticket=" + ticket + "]";
	}

}
