/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.config.oxtrust;

import org.gluu.config.oxauth.WebKeysSettings;
import org.gluu.persist.model.base.Entry;
import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DN;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.JsonObject;
import org.gluu.persist.annotation.ObjectClass;

/**
 * @author Rahat Ali
 * @version 2.1, 19/04/2015
 */
@DataEntry
@ObjectClass(values = { "top", "oxAuthConfiguration" })
public class LdapOxAuthConfiguration extends Entry {

    private static final long serialVersionUID = 2453308522994526877L;

    @DN
    private String dn;

    @AttributeName(name = "oxAuthConfDynamic")
    private String oxAuthConfigDynamic;

    @AttributeName(name = "oxAuthConfStatic")
    private String oxAuthConfstatic;

    @AttributeName(name = "oxAuthConfErrors")
    private String oxAuthConfErrors;

    @AttributeName(name = "oxAuthConfWebKeys")
    private String oxAuthConfWebKeys;

    @JsonObject
    @AttributeName(name = "oxWebKeysSettings")
    private WebKeysSettings oxWebKeysSettings;

    @AttributeName(name = "oxRevision")
    private long revision;

    public LdapOxAuthConfiguration() {
    }

    public WebKeysSettings getOxWebKeysSettings() {
        return oxWebKeysSettings;
    }

    public void setOxWebKeysSettings(WebKeysSettings oxWebKeysSettings) {
        this.oxWebKeysSettings = oxWebKeysSettings;
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
        return oxAuthConfErrors;
    }

    public void setOxAuthConfErrors(String oxAuthConfErrors) {
        this.oxAuthConfErrors = oxAuthConfErrors;
    }

    public String getOxAuthConfWebKeys() {
        return oxAuthConfWebKeys;
    }

    public void setOxAuthConfWebKeys(String oxAuthConfWebKeys) {
        this.oxAuthConfWebKeys = oxAuthConfWebKeys;
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
