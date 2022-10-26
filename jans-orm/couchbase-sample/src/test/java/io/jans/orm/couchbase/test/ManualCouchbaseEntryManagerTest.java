/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.couchbase.test;

import org.testng.annotations.Test;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.GetOptions;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.LookupInMacro;
import com.couchbase.client.java.kv.LookupInResult;
import com.couchbase.client.java.kv.LookupInSpec;

import io.jans.orm.couchbase.impl.CouchbaseEntryManager;
import io.jans.orm.couchbase.impl.CouchbaseEntryManagerFactory;
import io.jans.orm.couchbase.model.SimpleClient;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author Yuriy Zabrovarnyy
 */
public class ManualCouchbaseEntryManagerTest {

    @Test(enabled = false) // manual
    public void sample() throws IOException {
        CouchbaseEntryManager manager = createCouchbaseEntryManager();

        try {
            List<SimpleClient> resultList = manager.findEntries("ou=clietns,o=jans", SimpleClient.class, null);
            System.out.println(resultList);
        } finally {
            manager.destroy();
        }
    }

    @Test(enabled = false) // manual
    public void sampleSessionId() throws IOException, SearchException {
        CouchbaseEntryManager manager = createCouchbaseEntryManager();

        try {
            SessionId sessionId = createSessionId();
            manager.persist(sessionId);

            final String key = "sessions_" + sessionId.getId();
            System.out.println("Key: " + key + ", ttl:" + sessionId.getTtl());

            Bucket sessionBucket = manager.getOperationService().getConnectionProvider().getBucketMapping("sessions").getBucket();
            GetOptions getOptions1 = GetOptions.getOptions().withExpiry(true);
            final GetResult lookup = sessionBucket.defaultCollection().get(key, getOptions1);
            System.out.println("expiry: " + lookup.expiryTime());

            final LookupInResult ttl = sessionBucket.defaultCollection().lookupIn(key, Collections.singletonList(
                    LookupInSpec.get(LookupInMacro.EXPIRY_TIME).xattr()));
            System.out.println("ttl: " + ttl.contentAs(0, Long.class));

            updateSession(sessionId);
            manager.merge(sessionId);

            GetOptions getOptions3 = GetOptions.getOptions().withExpiry(true);
            final GetResult lookup2 = manager.getOperationService().getConnectionProvider().getBucketMapping("sessions").getBucket().defaultCollection().get(key, getOptions3);
            System.out.println("expiry after update: " + lookup2.expiryTime());

        } finally {
            manager.destroy();
        }
    }

    private SessionId createSessionId() {
        SessionId sessionId = new SessionId();
        sessionId.setId(UUID.randomUUID().toString());
        sessionId.setDn(String.format("jansId=%s,%s", sessionId.getId(), "ou=sessions,o=jans"));
        sessionId.setCreationDate(new Date());

        updateSession(sessionId);
        return sessionId;
    }

    private void updateSession(SessionId sessionId) {
        final Pair<Date, Integer> expiration = expirationDate(sessionId.getCreationDate());
        sessionId.setLastUsedAt(new Date());
        sessionId.setExpirationDate(expiration.getFirst());
        sessionId.setTtl(expiration.getSecond());
    }

    private Pair<Date, Integer> expirationDate(Date creationDate) {
        int expirationInSeconds = 120;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(creationDate);
        calendar.add(Calendar.SECOND, expirationInSeconds);
        return new Pair<>(calendar.getTime(), expirationInSeconds);
    }

    // MODIFY ACCORDING TO YOUR SERVER
    private static Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        properties.put("couchbase.auth.userPassword", "secret");

        try (InputStream is = ManualCouchbaseEntryManagerTest.class.getResourceAsStream("cb-bench-backend.jans.io.properties")) {
            properties.load(is);
            return properties;
        }
    }

    private static Properties getSampleConnectionProperties() {
        Properties connectionProperties = new Properties();

        connectionProperties.put("couchbase#servers", "localhost");
        connectionProperties.put("couchbase#auth.userName", "admin");
        connectionProperties.put("couchbase#auth.userPassword", "secret");
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
    public static CouchbaseEntryManager createCouchbaseEntryManager() throws IOException {
        CouchbaseEntryManagerFactory couchbaseEntryManagerFactory = new CouchbaseEntryManagerFactory();
        couchbaseEntryManagerFactory.create();

        CouchbaseEntryManager couchbaseEntryManager = couchbaseEntryManagerFactory.createEntryManager(getSampleConnectionProperties() /* loadProperties() */);
        System.out.println("Created CouchbaseEntryManager: " + couchbaseEntryManager);

        return couchbaseEntryManager;
    }
}
