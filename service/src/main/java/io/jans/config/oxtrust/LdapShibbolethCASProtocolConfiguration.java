/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.config.oxtrust;

import java.io.Serializable;

import io.jans.orm.model.base.Entry;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

/**
 * Shibboleth IDP CAS-related settings configuration entry.
 *
 * @author Dmitry Ognyannikov
 */
@DataEntry
@ObjectClass(value = "jansAppConf")
public class LdapShibbolethCASProtocolConfiguration extends Entry implements Serializable {

    private static final long serialVersionUID = -11887457695212971L;

    @AttributeName(ignoreDuringUpdate = true)
    private String inum;

    @JsonObject
    @AttributeName(name = "jansConfDyn")
    private ShibbolethCASProtocolConfiguration casProtocolConfiguration;

    @AttributeName(name = "jansRevision")
    private long revision;

    public LdapShibbolethCASProtocolConfiguration() {
    }

    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        if (casProtocolConfiguration != null) {
            casProtocolConfiguration.setInum(inum);
        }

        this.inum = inum;
    }

    /**
     * @return the casProtocolConfiguration
     */
    public ShibbolethCASProtocolConfiguration getCasProtocolConfiguration() {
        return casProtocolConfiguration;
    }

    /**
     * @param casProtocolConfiguration
     *            the casProtocolConfiguration to set
     */
    public void setCasProtocolConfiguration(ShibbolethCASProtocolConfiguration casProtocolConfiguration) {
        this.casProtocolConfiguration = casProtocolConfiguration;
    }

    /**
     * @return the revision
     */
    public long getRevision() {
        return revision;
    }

    /**
     * @param revision
     *            the revision to set
     */
    public void setRevision(long revision) {
        this.revision = revision;
    }

}
