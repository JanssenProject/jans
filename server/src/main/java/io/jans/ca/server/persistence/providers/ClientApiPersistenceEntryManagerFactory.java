package io.jans.ca.server.persistence.providers;

import io.jans.exception.ConfigurationException;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.PersistenceEntryManagerFactory;
import io.jans.orm.cloud.spanner.impl.SpannerEntryManagerFactory;
import io.jans.orm.couchbase.impl.CouchbaseEntryManagerFactory;
import io.jans.orm.ldap.impl.LdapEntryManagerFactory;
import io.jans.orm.reflect.util.ReflectHelper;
import io.jans.orm.sql.impl.SqlEntryManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.el.PropertyNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class ClientApiPersistenceEntryManagerFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ClientApiPersistenceEntryManagerFactory.class);

    private HashMap<String, PersistenceEntryManagerFactory> persistenceEntryManagerFactoryNames;

    public final PersistenceEntryManager createPersistenceEntryManager(Properties properties, String persistenceType) {

        try {
            if (this.persistenceEntryManagerFactoryNames == null) {
                initPersistenceManagerMaps();
            }

            PersistenceEntryManagerFactory persistenceEntryManagerFactory = this.persistenceEntryManagerFactoryNames.get(persistenceType);
            if (persistenceType.equalsIgnoreCase("couchbase")) {
                ((CouchbaseEntryManagerFactory)persistenceEntryManagerFactory).create();
            }
            Properties connProps = createConnectionProperties(properties, persistenceEntryManagerFactory.getPersistenceType());
            PersistenceEntryManager ret = persistenceEntryManagerFactory.createEntryManager(connProps);
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
            connProps.setProperty(connPrefix + "#" + propname, properties.getProperty(propname));
        }
        return connProps;
    }

    @SuppressWarnings("unchecked")
    private void initPersistenceManagerMaps() {
        this.persistenceEntryManagerFactoryNames = new HashMap<String, PersistenceEntryManagerFactory>();

        org.reflections.Reflections reflections = new org.reflections.Reflections(new org.reflections.util.ConfigurationBuilder()
                .setUrls(org.reflections.util.ClasspathHelper.forPackage("io.jans.orm"))
                .setScanners(new org.reflections.scanners.SubTypesScanner()));
        Set<Class<? extends PersistenceEntryManagerFactory>> classes = reflections.getSubTypesOf(PersistenceEntryManagerFactory.class);

        LOG.info("Found '{}' PersistenceEntryManagerFactory", classes.size());

        List<Class<? extends PersistenceEntryManagerFactory>> classesList = new ArrayList<Class<? extends PersistenceEntryManagerFactory>>(classes);
        for (Class<? extends PersistenceEntryManagerFactory> clazz : classesList) {
            LOG.info("Found PersistenceEntryManagerFactory '{}'", clazz);
            PersistenceEntryManagerFactory persistenceEntryManagerFactory = createPersistenceEntryManagerFactoryImpl(clazz);
            persistenceEntryManagerFactoryNames.put(persistenceEntryManagerFactory.getPersistenceType(), persistenceEntryManagerFactory);

        }
    }

    private PersistenceEntryManagerFactory createPersistenceEntryManagerFactoryImpl(Class<? extends PersistenceEntryManagerFactory> persistenceEntryManagerFactoryClass) {
        PersistenceEntryManagerFactory persistenceEntryManagerFactory;
        try {
            persistenceEntryManagerFactory = ReflectHelper.createObjectByDefaultConstructor(persistenceEntryManagerFactoryClass);
            //persistenceEntryManagerFactory.initStandalone(this);
        } catch (PropertyNotFoundException | IllegalArgumentException | InstantiationException | IllegalAccessException
                | InvocationTargetException e) {
            throw new ConfigurationException(
                    String.format("Failed to create PersistenceEntryManagerFactory by type '%s'!", persistenceEntryManagerFactoryClass));
        }

        return persistenceEntryManagerFactory;
    }
}
