package org.gluu.couchbase;

import java.util.Date;

import org.gluu.persist.couchbase.impl.CouchbaseEntryManager;
import org.gluu.persist.couchbase.operation.impl.CouchbaseConnectionProvider;
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

    public static void main(String[] args) {
        // Prepare sample connection details
        CouchbaseSampleEntryManager couchbaseSampleEntryManager = new CouchbaseSampleEntryManager();

        // Create Couchbase entry manager
        CouchbaseEntryManager couchbaseEntryManager = couchbaseSampleEntryManager.createCouchbaseEntryManager();

        String baseDn = "ou=cache,o=gluu";
		Filter filter = Filter.createANDFilter(
		        Filter.createEqualityFilter("del", true),
				Filter.createLessOrEqualFilter("oxAuthExpiration", couchbaseEntryManager.encodeTime(baseDn, new Date()))
        );

        int result = couchbaseEntryManager.remove(baseDn, DeletableEntity.class, filter, 100);
        System.out.println(result);
    }

}
