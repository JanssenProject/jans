package org.gluu.couchbase;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.gluu.couchbase.model.SimpleAttribute;
import org.gluu.couchbase.model.SimpleGrant;
import org.gluu.couchbase.model.SimpleSession;
import org.gluu.couchbase.model.SimpleUser;
import org.gluu.persist.couchbase.impl.CouchbaseEntryManager;
import org.gluu.persist.couchbase.impl.CouchbaseEntryManagerFactory;
import org.gluu.persist.couchbase.operation.impl.CouchbaseConnectionProvider;
import org.gluu.persist.model.PagedResult;
import org.gluu.persist.model.SearchScope;
import org.gluu.persist.model.SortOrder;
import org.gluu.persist.model.base.CustomAttribute;
import org.gluu.persist.model.base.DeletableEntity;
import org.gluu.search.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yuriy Movchan Date: 11/03/2016
 */
public final class CouchbaseSampleDelete {

    private static final Logger LOG = LoggerFactory.getLogger(CouchbaseConnectionProvider.class);

    private CouchbaseSampleDelete() {
    }

    private static Properties getSampleConnectionProperties() {
        Properties connectionProperties = new Properties();

        connectionProperties.put("couchbase.servers", "cb-dev-backend.gluu.org");
        connectionProperties.put("couchbase.auth.userName", "admin");
        connectionProperties.put("couchbase.auth.userPassword", "jun8azar");
//        connectionProperties.put("couchbase.buckets", "gluu");
        connectionProperties.put("couchbase.buckets", "gluu, gluu_cache");

        connectionProperties.put("couchbase.bucket.default", "gluu");
//        connectionProperties.put("couchbase.bucket.gluu.mapping", "people, groups");
        connectionProperties.put("couchbase.bucket.gluu_cache.mapping", "cache");

        connectionProperties.put("couchbase.password.encryption.method", "CRYPT-SHA-256");

        return connectionProperties;
    }

    public static CouchbaseEntryManager createCouchbaseEntryManager() {
        CouchbaseEntryManagerFactory couchbaseEntryManagerFactory = new CouchbaseEntryManagerFactory();
        couchbaseEntryManagerFactory.create();
        Properties connectionProperties = getSampleConnectionProperties();

        CouchbaseEntryManager couchbaseEntryManager = couchbaseEntryManagerFactory.createEntryManager(connectionProperties);
        LOG.debug("Created CouchbaseEntryManager: " + couchbaseEntryManager);

        return couchbaseEntryManager;
    }

    public static void main(String[] args) {

        // Create Couchbase entry manager
    	String baseDn = "ou=cache,o=@!5304.5F36.0E64.E1AC!0001!179C.62D7,o=gluu";

    	CouchbaseEntryManager couchbaseEntryManager = CouchbaseSampleDelete.createCouchbaseEntryManager();
		Filter filter = Filter.createANDFilter(
		        Filter.createEqualityFilter("oxDeletable", true),
				Filter.createLessOrEqualFilter("oxAuthExpiration", couchbaseEntryManager.encodeTime(baseDn, new Date()))
        );

        int result = couchbaseEntryManager.remove(baseDn, DeletableEntity.class, filter, 100);
        System.out.println(result);
    }

}
