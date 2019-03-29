/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.config.oxtrust;

import org.gluu.config.oxauth.WebKeysSettings;
import org.gluu.persist.model.base.Entry;
import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapJsonObject;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

/**
 * @author Rahat Ali
 * @version 2.1, 19/04/2015
 */
@LdapEntry
@LdapObjectClass(values = { "top", "oxAuthConfiguration" })
public class LdapOxAuthConfiguration extends Entry {

    private static final long serialVersionUID = 2453308522994526877L;

    @LdapDN
    private String dn;

    @LdapAttribute(name = "oxAuthConfDynamic")
    private String oxAuthConfigDynamic;

    @LdapAttribute(name = "oxAuthConfStatic")
    private String oxAuthConfstatic;

    @LdapAttribute(name = "oxAuthConfErrors")
    private String oxAuthConfErrors;

    @LdapAttribute(name = "oxAuthConfWebKeys")
    private String oxAuthConfWebKeys;

    @LdapJsonObject
    @LdapAttribute(name = "oxWebKeysSettings")
    private WebKeysSettings oxWebKeysSettings;

    @LdapAttribute(name = "oxRevision")
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
