/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */
package io.jans.config.oxtrust;

import java.io.Serializable;

import io.jans.persist.model.base.Entry;
import io.jans.persist.annotation.AttributeName;
import io.jans.persist.annotation.DataEntry;
import io.jans.persist.annotation.JsonObject;
import io.jans.persist.annotation.ObjectClass;

/**
 * Shibboleth IDP CAS-related settings configuration entry.
 *
 * @author Dmitry Ognyannikov
 */
@DataEntry
@ObjectClass(value = "oxShibbolethCASProtocolConfiguration")
public class LdapShibbolethCASProtocolConfiguration extends Entry implements Serializable {

    private static final long serialVersionUID = -11887457695212971L;

    @AttributeName(ignoreDuringUpdate = true)
    private String inum;

    @JsonObject
    @AttributeName(name = "oxConfApplication")
    private ShibbolethCASProtocolConfiguration casProtocolConfiguration;

    @AttributeName(name = "oxRevision")
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
