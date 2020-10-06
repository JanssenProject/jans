/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Gluu
 */

package io.jans.config.oxtrust;

import io.jans.persist.annotation.AttributeName;
import io.jans.persist.annotation.DN;
import io.jans.persist.annotation.DataEntry;
import io.jans.persist.annotation.JsonObject;
import io.jans.persist.annotation.ObjectClass;

/**
 * @author Yuriy MOvchan
 * @version May 12, 2020
 */
@DataEntry
@ObjectClass(value = "gluuApplicationConfiguration")
public class DbApplicationConfiguration {
    @DN
    private String dn;

    @JsonObject
    @AttributeName(name = "gluuConfDynamic")
    private String dynamicConf;

    @AttributeName(name = "oxRevision")
    private long revision;

    public DbApplicationConfiguration() {
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String p_dn) {
        dn = p_dn;
    }

	public String getDynamicConf() {
		return dynamicConf;
	}

	public void setDynamicConf(String dynamicConf) {
		this.dynamicConf = dynamicConf;
	}

	public long getRevision() {
		return revision;
	}

	public void setRevision(long revision) {
		this.revision = revision;
	}

	@Override
	public String toString() {
		return "DbApplicationConfiguration [dn=" + dn + ", dynamicConf=" + dynamicConf + ", revision=" + revision + "]";
	}
}
