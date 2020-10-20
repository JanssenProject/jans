package io.jans.ca.server.persistence.providers;

import io.jans.exception.ConfigurationException;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.couchbase.impl.CouchbaseEntryManagerFactory;
import io.jans.orm.ldap.impl.LdapEntryManagerFactory;

import java.util.Properties;

public class PersistenceEntryManagerFactory {

    public static final PersistenceEntryManager createLdapPersistenceEntryManager(Properties properties) {

        try {
            LdapEntryManagerFactory ldapEntryManagerFactory = new LdapEntryManagerFactory();
            Properties connProps = createConnectionProperties(properties, ldapEntryManagerFactory.getPersistenceType());
            PersistenceEntryManager ret = ldapEntryManagerFactory.createEntryManager(connProps);
            if (ret == null)
                throw new RuntimeException("Could not create persistence entry manager");
            return ret;
        } catch (ConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static final PersistenceEntryManager createCouchbasePersistenceEntryManager(Properties properties) {

        try {
            CouchbaseEntryManagerFactory couchbaseEntryManagerFactory = new CouchbaseEntryManagerFactory();
            couchbaseEntryManagerFactory.create();
            Properties connProps = createConnectionProperties(properties, couchbaseEntryManagerFactory.getPersistenceType());
            PersistenceEntryManager ret = couchbaseEntryManagerFactory.createEntryManager(connProps);
            if (ret == null)
                throw new RuntimeException("Could not create persistence entry manager");

            return ret;
        } catch (ConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static final Properties createConnectionProperties(Properties properties, String connPrefix) {

        Properties connProps = new Properties();
        for (String propname : properties.stringPropertyNames()) {
            connProps.setProperty(connPrefix + "." + propname, properties.getProperty(propname));
        }
        return connProps;
    }
}
