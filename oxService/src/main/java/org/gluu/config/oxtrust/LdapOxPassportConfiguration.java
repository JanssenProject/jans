package org.gluu.config.oxtrust;

import org.gluu.model.passport.PassportConfiguration;
import io.jans.persist.model.base.Entry;
import io.jans.persist.annotation.AttributeName;
import io.jans.persist.annotation.DataEntry;
import io.jans.persist.annotation.JsonObject;
import io.jans.persist.annotation.ObjectClass;

/**
 * @author Shekhar L.
 * @author Yuriy Movchan Date: 12/06/2016
 * @Date 07/17/2016
 */

@DataEntry
@ObjectClass(value = "oxPassportConfiguration")
public class LdapOxPassportConfiguration extends Entry {

	private static final long serialVersionUID = -8451013277721189767L;

	@JsonObject
	@AttributeName(name = "gluuPassportConfiguration")
	private PassportConfiguration passportConfiguration;

	public PassportConfiguration getPassportConfiguration() {
		return passportConfiguration;
	}

	public void setPassportConfiguration(PassportConfiguration passportConfiguration) {
		this.passportConfiguration = passportConfiguration;
	}

}
