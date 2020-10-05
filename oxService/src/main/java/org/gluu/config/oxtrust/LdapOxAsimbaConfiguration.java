/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */
package org.gluu.config.oxtrust;

import io.jans.persist.model.base.Entry;
import io.jans.persist.annotation.AttributeName;
import io.jans.persist.annotation.DataEntry;
import io.jans.persist.annotation.JsonObject;
import io.jans.persist.annotation.ObjectClass;

/**
 * Asimba LDAP settings configuration entry.
 *
 * @author Dmitry Ognyannikov
 */
@DataEntry
@ObjectClass(value = "oxAsimbaConfiguration")
public class LdapOxAsimbaConfiguration extends Entry {

    private static final long serialVersionUID = -12489397651302948L;

    @JsonObject
    @AttributeName(name = "oxConfApplication")
    private AsimbaConfiguration applicationConfiguration;

    @AttributeName(name = "oxRevision")
    private long revision;

    public LdapOxAsimbaConfiguration() {
    }

    /**
     * @return the applicationConfiguration
     */
    public AsimbaConfiguration getApplicationConfiguration() {
        return applicationConfiguration;
    }

    /**
     * @param applicationConfiguration
     *            the applicationConfiguration to set
     */
    public void setApplicationConfiguration(AsimbaConfiguration applicationConfiguration) {
        this.applicationConfiguration = applicationConfiguration;
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
