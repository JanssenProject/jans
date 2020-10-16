/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.config.oxtrust;

import io.jans.config.oxauth.WebKeysSettings;
import io.jans.orm.model.base.Entry;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

/**
 * @author Rahat Ali
 * @version 2.1, 19/04/2015
 */
@DataEntry
@ObjectClass(value = "oxAuthConfiguration")
public class LdapOxAuthConfiguration extends Entry {

    private static final long serialVersionUID = 2453308522994526877L;

    @DN
    private String dn;

    @AttributeName(name = "jsConfDyn")
    private String oxAuthConfigDynamic;

    @AttributeName(name = "jsConfStatic")
    private String oxAuthConfstatic;

    @AttributeName(name = "jsConfErrors")
    private String jsConfErrors;

    @AttributeName(name = "jsConfWebKeys")
    private String jsConfWebKeys;

    @JsonObject
    @AttributeName(name = "jsWebKeysSettings")
    private WebKeysSettings jsWebKeysSettings;

    @AttributeName(name = "jsRevision")
    private long revision;

    public LdapOxAuthConfiguration() {
    }

    public WebKeysSettings getOxWebKeysSettings() {
        return jsWebKeysSettings;
    }

    public void setOxWebKeysSettings(WebKeysSettings jsWebKeysSettings) {
        this.jsWebKeysSettings = jsWebKeysSettings;
    }

    public String getOxAuthConfigDynamic() {
        return oxAuthConfigDynamic;
    }

    public void setOxAuthConfigDynamic(String oxAuthConfigDynamic) {
        this.oxAuthConfigDynamic = oxAuthConfigDynamic;
    }

    public String getOxAuthConfstatic() {
        return oxAuthConfstatic;
    }

    public void setOxAuthConfstatic(String oxAuthConfstatic) {
        this.oxAuthConfstatic = oxAuthConfstatic;
    }

    public String getOxAuthConfErrors() {
        return jsConfErrors;
    }

    public void setOxAuthConfErrors(String jsConfErrors) {
        this.jsConfErrors = jsConfErrors;
    }

    public String getOxAuthConfWebKeys() {
        return jsConfWebKeys;
    }

    public void setOxAuthConfWebKeys(String jsConfWebKeys) {
        this.jsConfWebKeys = jsConfWebKeys;
    }

    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LdapAppConfiguration [dn=").append(dn).append(", application=").append(oxAuthConfigDynamic).append("]");
        return builder.toString();
    }

}
