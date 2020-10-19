/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.config.oxtrust;

import io.jans.model.passport.PassportConfiguration;
import io.jans.orm.model.base.Entry;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

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
	@AttributeName(name = "jansPassportConf")
	private PassportConfiguration passportConfiguration;

	public PassportConfiguration getPassportConfiguration() {
		return passportConfiguration;
	}

	public void setPassportConfiguration(PassportConfiguration passportConfiguration) {
		this.passportConfiguration = passportConfiguration;
	}

}
