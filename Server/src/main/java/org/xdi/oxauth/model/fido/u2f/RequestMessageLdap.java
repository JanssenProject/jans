package org.xdi.oxauth.model.fido.u2f;

import java.util.Date;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;
import org.xdi.ldap.model.BaseEntry;

/**
 * U2F base request
 *
 * @author Yuriy Movchan Date: 06/02/2015
 */
@LdapEntry(sortBy = "creationDate")
@LdapObjectClass(values = {"top", "oxU2fRequest"})
public class RequestMessageLdap extends BaseEntry {

	@LdapAttribute(ignoreDuringUpdate = true, name = "oxId")
	protected String id;

	@LdapAttribute(name = "oxRequestId")
	protected String requestId;

	@LdapAttribute(name = "creationDate")
	protected Date creationDate;

	public RequestMessageLdap() {}

	public RequestMessageLdap(String dn) {
		super(dn);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

}