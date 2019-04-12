package org.gluu.config.oxtrust;

import org.gluu.model.passport.PassportConfiguration;
import org.gluu.persist.model.base.Entry;
import org.gluu.persistence.annotation.LdapAttribute;
import org.gluu.persistence.annotation.LdapEntry;
import org.gluu.persistence.annotation.LdapJsonObject;
import org.gluu.persistence.annotation.LdapObjectClass;

/**
 * @author Shekhar L.
 * @author Yuriy Movchan Date: 12/06/2016
 * @Date 07/17/2016
 */

@Entry
@ObjectClass(values = { "top", "oxPassportConfiguration" })
public class LdapOxPassportConfiguration extends Entry {

	private static final long serialVersionUID = -8451013277721189767L;

	@JsonObject
	@Attribute(name = "gluuPassportConfiguration")
	private PassportConfiguration passportConfiguration;

	public PassportConfiguration getPassportConfiguration() {
		return passportConfiguration;
	}

	public void setPassportConfiguration(PassportConfiguration passportConfiguration) {
		this.passportConfiguration = passportConfiguration;
	}

}
