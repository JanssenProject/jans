/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.cloud.spanner.impl.SpannerEntryManagerFactory;
import io.jans.orm.ldap.impl.LdapEntryManagerFactory;

import java.io.Serializable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@Named
public class DataSourceTypeService implements Serializable {

    private static final long serialVersionUID = -1941135478226842653L;

    @Inject
    private PersistenceEntryManager entryManager;

    public boolean isLDAP(String key) {
        return entryManager.getPersistenceType(key).equals(LdapEntryManagerFactory.PERSISTENCE_TYPE);
    }

    public boolean isSpanner(String key) {
        return entryManager.getPersistenceType(key).equals(SpannerEntryManagerFactory.PERSISTENCE_TYPE);
    }

}
