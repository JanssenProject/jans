package io.jans.ca.server.persistence.providers;

import io.jans.exception.ConfigurationException;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.PersistenceEntryManagerFactory;
import io.jans.orm.couchbase.impl.CouchbaseEntryManagerFactory;
import io.jans.orm.service.StandalonePersistanceFactoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ClientApiPersistenceEntryManagerFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ClientApiPersistenceEntryManagerFactory.class);

    private StandalonePersistanceFactoryService standalonePersistanceFactoryService;

    public ClientApiPersistenceEntryManagerFactory(){
        this.standalonePersistanceFactoryService = new StandalonePersistanceFactoryService();
    }
    public final PersistenceEntryManager createPersistenceEntryManager(Properties properties, String persistenceType) {

        try {
            PersistenceEntryManagerFactory persistenceEntryManagerFactory = this.standalonePersistanceFactoryService.getPersistenceEntryManagerFactory(persistenceType);
            if (persistenceEntryManagerFactory.getPersistenceType().equalsIgnoreCase("couchbase")) {
                ((CouchbaseEntryManagerFactory) persistenceEntryManagerFactory).create();
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
}
