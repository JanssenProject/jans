/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.config;

import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.error.ErrorMessages;
import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DN;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.JsonObject;
import org.gluu.persist.annotation.ObjectClass;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 03/01/2013
 */
@DataEntry
@ObjectClass(values = {"top", "oxAuthConfiguration"})
public class Conf {
    @DN
    private String dn;

    @JsonObject
    @AttributeName(name = "oxAuthConfDynamic")
    private AppConfiguration dynamic;

    @JsonObject
    @AttributeName(name = "oxAuthConfStatic")
    private StaticConfiguration statics;

    @JsonObject
    @AttributeName(name = "oxAuthConfErrors")
    private ErrorMessages errors;

    @JsonObject
    @AttributeName(name = "oxAuthConfWebKeys")
    private WebKeysConfiguration webKeys;

    @AttributeName(name = "oxRevision")
    private long revision;

    public Conf() {
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String p_dn) {
        dn = p_dn;
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
        sb.append("{m_dn='").append(dn).append('\'');
        sb.append(", m_dynamic='").append(dynamic).append('\'');
        sb.append(", m_static='").append(statics).append('\'');
        sb.append(", m_errors='").append(errors).append('\'');
        sb.append(", m_webKeys='").append(webKeys).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
