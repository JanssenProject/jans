/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.config.adminui;

import io.jans.orm.annotation.*;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 03/01/2013
 */
@DataEntry
@ObjectClass(value = "jansAdminConfDyn")
public class AdminConf {
    @DN
    private String dn;

    @JsonObject
    @AttributeName(name = "jansConfDyn")
    private DynamicConfig dynamic;

    @AttributeName(name = "jansRevision")
    private long revision;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

	public long getRevision() {
		return revision;
	}

	public void setRevision(long revision) {
		this.revision = revision;
	}

    public DynamicConfig getDynamic() {
        return dynamic;
    }

    public void setDynamic(DynamicConfig dynamic) {
        this.dynamic = dynamic;
    }

	@Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Conf");
        sb.append("{dn='").append(dn).append('\'');
        sb.append(", dynamic='").append(dynamic).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
