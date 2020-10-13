/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.config.oxtrust;

import io.jans.orm.model.base.Entry;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

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
