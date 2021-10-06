/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.config;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorMessages;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 03/01/2013
 */
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

    @JsonObject
    @AttributeName(name = "jansConfWebKeys")
    private WebKeysConfiguration webKeys;

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

	public WebKeysConfiguration getWebKeys() {
		return webKeys;
	}

	public void setWebKeys(WebKeysConfiguration webKeys) {
		this.webKeys = webKeys;
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
        sb.append(", errors='").append(errors).append('\'');
        sb.append(", webKeys='").append(webKeys).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
