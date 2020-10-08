package io.jans.service;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.ldap.impl.LdapEntryManagerFactory;

import java.io.Serializable;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

@ApplicationScoped
@Named
public class DataSourceTypeService implements Serializable {

    private static final long serialVersionUID = -1941135478226842653L;

    @Inject
    private PersistenceEntryManager entryManager;

    public boolean isLDAP(String key) {
        return entryManager.getPersistenceType(key).equals(LdapEntryManagerFactory.PERSISTENCE_TYPE);
    }

}
