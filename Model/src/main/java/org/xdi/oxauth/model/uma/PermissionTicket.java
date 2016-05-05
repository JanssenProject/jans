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
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 * Date: 10/18/2012
 */
@IgnoreMediaTypes("application/*+json") // try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonPropertyOrder({ "ticket" })
//@JsonRootName(value = "resourceSetPermissionTicket")
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ticket == null) ? 0 : ticket.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PermissionTicket other = (PermissionTicket) obj;
		if (ticket == null) {
			if (other.ticket != null)
				return false;
		} else if (!ticket.equals(other.ticket))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ResourceSetPermissionTiket [ticket=").append(ticket).append("]");
		return builder.toString();
	}

}
