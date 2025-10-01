/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.config.link;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

/**
 * @author Yuriy Movchan
 * @version May 12, 2020
 */
@DataEntry
@ObjectClass(value = "jansAppConf")
public class DbApplicationConfiguration {
    @DN
    private String dn;

    @JsonObject
    @AttributeName(name = "jansConfDyn")
    private String dynamicConf;

    @AttributeName(name = "jansRevision")
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
