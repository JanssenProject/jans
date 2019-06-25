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

        connectionProperties.put("couchbase.servers", "localhost");
        connectionProperties.put("couchbase.auth.userName", "admin");
        connectionProperties.put("couchbase.auth.userPassword", "test");
//        connectionProperties.put("couchbase.buckets", "gluu");
        connectionProperties.put("couchbase.buckets", "gluu, gluu_site, gluu_user, gluu_session, gluu_statistic");

        connectionProperties.put("couchbase.bucket.default", "gluu");
//        connectionProperties.put("couchbase.bucket.gluu.mapping", "people, groups");
        connectionProperties.put("couchbase.bucket.gluu_user.mapping", "people, groups");
        connectionProperties.put("couchbase.bucket.gluu_session.mapping", "sessions");
        connectionProperties.put("couchbase.bucket.gluu_statistic.mapping", "statistic");
        connectionProperties.put("couchbase.bucket.gluu_site.mapping", "site");

        connectionProperties.put("couchbase.password.encryption.method", "CRYPT-SHA-256");

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