package org.gluu.service.cache;

import org.gluu.persist.couchbase.impl.CouchbaseEntryManager;
import org.gluu.persist.couchbase.impl.CouchbaseEntryManagerFactory;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.testng.Assert.assertEquals;

/**
 * @author Yuriy Zabrovarnyy
 */
public class CouchbaseNativeCacheTest {

    @Test(enabled = true) // manual
    public void couchbaseCacheProvider() throws IOException {
        CouchbaseEntryManager manager = createCouchbaseEntryManager();

        try {
            final String baseDn = "o=gluu";

            final CacheConfiguration cacheConfiguration = new CacheConfiguration();
            cacheConfiguration.setNativePersistenceConfiguration(new NativePersistenceConfiguration());
            cacheConfiguration.getNativePersistenceConfiguration().setBaseDn("");

            NativePersistenceCacheProvider provider = new NativePersistenceCacheProvider();
            provider.setBaseDn(baseDn);
            provider.setCacheConfiguration(cacheConfiguration);
            provider.setEntryManager(manager);

            Map<String, String> sessionAttributes = new HashMap<>();
            sessionAttributes.put("attr1", "value1");
            sessionAttributes.put("attr2", "value2");

            SampleSessionId sessionId = new SampleSessionId();
            sessionId.setId(UUID.randomUUID().toString());
            sessionId.setDn(sessionId.getId());
            sessionId.setAuthenticationTime(new Date());
            sessionId.setState(SessionIdState.AUTHENTICATED);
            sessionId.setSessionAttributes(sessionAttributes);

            provider.put(130, sessionId.getId(), sessionId);

            final SampleSessionId fromCache = (SampleSessionId) provider.get(sessionId.getId());

            assertEquals(fromCache.getId(), sessionId.getId());
        } finally {
            manager.destroy();
        }
    }

    // MODIFY ACCORDING TO YOUR SERVER
    public static Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        properties.put("couchbase.auth.userPassword", "jun8azar");

        try (InputStream is = CouchbaseNativeCacheTest.class.getResourceAsStream("cb-dev-backend.gluu.org.properties")) {
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
