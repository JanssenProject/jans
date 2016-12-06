package org.xdi.config.oxtrust;

import java.util.List;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapJsonObject;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;
import org.xdi.ldap.model.Entry;
import org.xdi.model.passport.PassportConfiguration;

/**
 * @author Shekhar L.
 * @author Yuriy Movchan Date: 12/06/2016
 * @Date 07/17/2016
 */

@LdapEntry
@LdapObjectClass(values = { "top", "oxPassportConfiguration" })
public class LdapOxPassportConfiguration extends Entry {

	private static final long serialVersionUID = -8451013277721189767L;

	@LdapDN
	private String dn;

	@LdapJsonObject
	@LdapAttribute(name = "gluuPassportConfiguration")
	private List<PassportConfiguration> passportConfigurations;

	@LdapAttribute(name = "gluuStatus")
	private String status;

	public List<PassportConfiguration> getPassportConfigurations() {
		return passportConfigurations;
	}

	public void setPassportConfigurations(List<PassportConfiguration> passportConfigurations) {
		this.passportConfigurations = passportConfigurations;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
