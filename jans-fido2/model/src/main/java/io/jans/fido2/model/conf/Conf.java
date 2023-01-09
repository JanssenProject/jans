/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.conf;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.error.ErrorMessages;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

/**
 * @author Yuriy MOvchan
 * @version May 12, 2020
 */
@DataEntry
@ObjectClass(value = "jansAppConf")
public class Conf {
    @DN
    private String dn;

    @JsonObject
    @AttributeName(name = "jansConfDyn")
    private AppConfiguration dynamicConf;

    @JsonObject
    @AttributeName(name = "jansConfStatic")
    private StaticConfiguration staticConf;

	@JsonObject
	@AttributeName(name = "jansConfErrors")
	private ErrorMessages errors;

    @AttributeName(name = "jansRevision")
    private long revision;

    public Conf() {
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String p_dn) {
        dn = p_dn;
    }

	public AppConfiguration getDynamicConf() {
		return dynamicConf;
	}

	public void setDynamicConf(AppConfiguration dynamicConf) {
		this.dynamicConf = dynamicConf;
	}

	public StaticConfiguration getStaticConf() {
		return staticConf;
	}

	public void setStaticConf(StaticConfiguration staticConf) {
		this.staticConf = staticConf;
	}

	public long getRevision() {
		return revision;
	}

	public void setRevision(long revision) {
		this.revision = revision;
	}

	public ErrorMessages getErrors() {
		return errors;
	}

	public void setErrors(ErrorMessages errors) {
		this.errors = errors;
	}

	@Override
	public String toString() {
		return "Conf [dn=" + dn + ", dynamicConf=" + dynamicConf + ", staticConf=" + staticConf + ", errors=" + errors + ", revision=" + revision + "]";
	}
}
