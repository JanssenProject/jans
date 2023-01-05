/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.couchbase;

import java.util.Properties;

import org.apache.log4j.Logger;

import io.jans.orm.couchbase.impl.CouchbaseEntryManager;
import io.jans.orm.couchbase.impl.CouchbaseEntryManagerFactory;

/**
 * @author Yuriy Movchan
 * Date: 01/13/2017
 */
public class CouchbaseEntryManagerSample {

    private static final Logger LOG = Logger.getLogger(CouchbaseEntryManagerSample.class);

    private Properties getSampleConnectionProperties() {
        Properties connectionProperties = new Properties();

        connectionProperties.put("couchbase#servers", "localhost");
        connectionProperties.put("couchbase#auth.userName", "admin");
        connectionProperties.put("couchbase#auth.userPassword", "secret");

        connectionProperties.put("couchbase#connection.dns.use-lookup", "false");
        
        connectionProperties.put("couchbase#buckets", "jans, jans_user, jans_site, jans_cache, jans_token, jans_session");

        connectionProperties.put("couchbase#bucket.default", "jans");
        connectionProperties.put("couchbase#bucket.jans_user.mapping", "people, groups, authorizations");
        connectionProperties.put("couchbase#bucket.jans_site.mapping", "cache-refresh");
        connectionProperties.put("couchbase#bucket.jans_cache.mapping", "cache");
        connectionProperties.put("couchbase#bucket.jans_token.mapping", "tokens");
        connectionProperties.put("couchbase#bucket.jans_session.mapping", "sessions");

        connectionProperties.put("couchbase#password.encryption.method", "SSHA-256");

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