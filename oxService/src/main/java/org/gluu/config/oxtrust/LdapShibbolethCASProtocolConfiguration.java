/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */
package org.gluu.config.oxtrust;

import java.io.Serializable;

import org.gluu.persist.model.base.Entry;
import org.gluu.persist.annotation.LdapAttribute;
import org.gluu.persist.annotation.LdapEntry;
import org.gluu.persist.annotation.LdapJsonObject;
import org.gluu.persist.annotation.LdapObjectClass;

/**
 * Shibboleth IDP CAS-related settings configuration entry.
 *
 * @author Dmitry Ognyannikov
 */
@Entry
@ObjectClass(values = { "top", "oxShibbolethCASProtocolConfiguration" })
public class LdapShibbolethCASProtocolConfiguration extends Entry implements Serializable {

    private static final long serialVersionUID = -11887457695212971L;

    @Attribute(ignoreDuringUpdate = true)
    private String inum;

    @JsonObject
    @Attribute(name = "oxConfApplication")
    private ShibbolethCASProtocolConfiguration casProtocolConfiguration;

    @Attribute(name = "oxRevision")
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
