/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.couchbase.test;

import com.couchbase.client.core.message.kv.subdoc.multi.Lookup;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.subdoc.DocumentFragment;
import com.couchbase.client.java.subdoc.SubdocOptionsBuilder;

import io.jans.orm.couchbase.impl.CouchbaseEntryManager;
import io.jans.orm.couchbase.impl.CouchbaseEntryManagerFactory;
import io.jans.orm.couchbase.model.SimpleClient;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.util.Pair;
import org.testng.annotations.Test;

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
            List<SimpleClient> attributeList = manager.findEntries("o=jans", SimpleClient.class, null);
            System.out.println(attributeList);
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
            final JsonDocument lookup = sessionBucket.get(key);
            System.out.println("expiry: " + lookup.expiry());

            DocumentFragment<Lookup> ttl = sessionBucket.lookupIn(key).get("$document.exptime", new SubdocOptionsBuilder().xattr(true)).execute();
            System.out.println("ttl: " + ttl.content("$document.exptime"));

            updateSession(sessionId);
            manager.merge(sessionId);

            final JsonDocument lookup2 = manager.getOperationService().getConnectionProvider().getBucketMapping("sessions").getBucket().get(key);
            System.out.println("expiry after update: " + lookup2.expiry());

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
    public static Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        properties.put("couchbase.auth.userPassword", "secret");

        try (InputStream is = ManualCouchbaseEntryManagerTest.class.getResourceAsStream("cb-bench-backend.jans.io.properties")) {
            properties.load(is);
            return properties;
        }
    }

    public static CouchbaseEntryManager createCouchbaseEntryManager() throws IOException {
        CouchbaseEntryManagerFactory couchbaseEntryManagerFactory = new CouchbaseEntryManagerFactory();
        couchbaseEntryManagerFactory.create();

        CouchbaseEntryManager couchbaseEntryManager = couchbaseEntryManagerFactory.createEntryManager(loadProperties());
        System.out.println("Created CouchbaseEntryManager: " + couchbaseEntryManager);

        return couchbaseEntryManager;
    }
}
