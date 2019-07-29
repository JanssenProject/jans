package org.gluu.couchbase.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.gluu.couchbase.model.SimpleClient;
import org.gluu.persist.couchbase.impl.CouchbaseEntryManager;
import org.gluu.persist.couchbase.impl.CouchbaseEntryManagerFactory;
import org.testng.annotations.Test;

/**
 * @author Yuriy Zabrovarnyy
 */
public class ManualCouchbaseEntryManagerTest {

    @Test(enabled = false) // manual
    public void sample() throws IOException {
        CouchbaseEntryManager manager = createCouchbaseEntryManager();

        try {
            List<SimpleClient> attributeList = manager.findEntries("o=gluu", SimpleClient.class, null);
            System.out.println(attributeList);
        } finally {
            manager.destroy();
        }
    }

    // MODIFY ACCORDING TO YOUR SERVER
    public static Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        properties.put("couchbase.auth.userPassword", "");

        try (InputStream is = ManualCouchbaseEntryManagerTest.class.getResourceAsStream("c1.gluu.org.properties")) {
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
