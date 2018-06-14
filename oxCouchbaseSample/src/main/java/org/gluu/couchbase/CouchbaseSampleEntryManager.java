package org.gluu.couchbase;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.gluu.persist.couchbase.impl.CouchbaseEntryManager;
import org.gluu.persist.couchbase.impl.CouchbaseEntryManagerFactory;

/**
 * @author Yuriy Movchan
 * Date: 01/13/2017
 */
public class CouchbaseSampleEntryManager {

    private static final Logger LOG = Logger.getLogger(CouchbaseSampleEntryManager.class);

    private Properties getSampleConnectionProperties() {
        Properties connectionProperties = new Properties();

        connectionProperties.put("servers", "localhost");
        connectionProperties.put("userName", "admin");
        connectionProperties.put("userPassword", "secret");
        connectionProperties.put("buckets", "gluu, travel-sample");
        connectionProperties.put("bucket.gluu.mapping", "gluu");
        connectionProperties.put("bucket.travel-sample.mapping", "travel-sample");
        connectionProperties.put("encryption.method", "CRYPT-SHA-256");
        

        return connectionProperties;
    }

    public CouchbaseEntryManager createCouchbaseEntryManager() {
        CouchbaseEntryManagerFactory couchbaseEntryManagerFactory = new CouchbaseEntryManagerFactory();
        Properties connectionProperties = getSampleConnectionProperties();

        CouchbaseEntryManager couchbaseEntryManager = couchbaseEntryManagerFactory.createEntryManager(connectionProperties);
        LOG.debug("Created CouchbaseEntryManager: " + couchbaseEntryManager);

        return couchbaseEntryManager;
    }

}
