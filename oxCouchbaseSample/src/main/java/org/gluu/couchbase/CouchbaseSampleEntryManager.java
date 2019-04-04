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
        connectionProperties.put("auth.userName", "admin");
        connectionProperties.put("auth.userPassword", "test");
//        connectionProperties.put("buckets", "gluu");
        connectionProperties.put("buckets", "gluu, gluu_site, gluu_user, gluu_session, gluu_statistic");

        connectionProperties.put("bucket.default", "gluu");
//        connectionProperties.put("bucket.gluu.mapping", "people, groups");
        connectionProperties.put("bucket.gluu_user.mapping", "people, groups");
        connectionProperties.put("bucket.gluu_session.mapping", "sessions");
        connectionProperties.put("bucket.gluu_statistic.mapping", "statistic");
        connectionProperties.put("bucket.gluu_site.mapping", "site");

        connectionProperties.put("password.encryption.method", "CRYPT-SHA-256");

        return connectionProperties;
    }

    public CouchbaseEntryManager createCouchbaseEntryManager() {
        CouchbaseEntryManagerFactory couchbaseEntryManagerFactory = new CouchbaseEntryManagerFactory();
        couchbaseEntryManagerFactory.create();
        Properties connectionProperties = getSampleConnectionProperties();

        CouchbaseEntryManager couchbaseEntryManager = couchbaseEntryManagerFactory.createEntryManager(connectionProperties);
        LOG.debug("Created CouchbaseEntryManager: " + couchbaseEntryManager);

        return couchbaseEntryManager;
    }

}
