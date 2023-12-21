/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.lock.model.config;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import jakarta.enterprise.inject.Vetoed;

/**
 * 
 * @author Yuriy Movchan Date: 12/12/2023
 */
@Vetoed
@DataEntry
@ObjectClass(value = "jansAppConf")
public class Conf {
	@DN
	private String dn;

	@JsonObject
	@AttributeName(name = "jansConfDyn")
	private AppConfiguration dynamic;

	@JsonObject
	@AttributeName(name = "jansConfStatic")
	private StaticConfiguration statics;

	@JsonObject
	@AttributeName(name = "jansConfErrors")
	private ErrorMessages errors;

	@AttributeName(name = "jansRevision")
	private long revision;

	public String getDn() {
		return dn;
	}

	public void setDn(String dn) {
		this.dn = dn;
	}

	public AppConfiguration getDynamic() {
		return dynamic;
	}

	public void setDynamic(AppConfiguration dynamic) {
		this.dynamic = dynamic;
	}

	public StaticConfiguration getStatics() {
		return statics;
	}

	public void setStatics(StaticConfiguration statics) {
		this.statics = statics;
	}

	public ErrorMessages getErrors() {
		return errors;
	}

	public void setErrors(ErrorMessages errors) {
		this.errors = errors;
	}

	public long getRevision() {
		return revision;
	}

	public void setRevision(long revision) {
		this.revision = revision;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("Conf");
		sb.append("{dn='").append(dn).append('\'');
		sb.append(", dynamic='").append(dynamic).append('\'');
		sb.append(", static='").append(statics).append('\'');
		sb.append('}');
		return sb.toString();
	}
}
