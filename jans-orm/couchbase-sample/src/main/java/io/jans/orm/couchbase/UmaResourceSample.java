/*
 * oxCore is available under the MIT License (2014). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.couchbase;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.couchbase.impl.CouchbaseEntryManager;
import io.jans.orm.couchbase.model.UmaResource;
import io.jans.orm.couchbase.operation.impl.CouchbaseConnectionProvider;
import io.jans.orm.search.filter.Filter;

/**
 * @author Yuriy Movchan Date: 09/16/2021
 */
public final class UmaResourceSample {

    private static final Logger LOG = LoggerFactory.getLogger(CouchbaseConnectionProvider.class);

    private UmaResourceSample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        CouchbaseEntryManagerSample couchbaseEntryManagerSample = new CouchbaseEntryManagerSample();

        // Create Couchbase entry manager
        CouchbaseEntryManager entryManager = couchbaseEntryManagerSample.createCouchbaseEntryManager();
        
        final Filter filter = Filter.createEqualityFilter("jansAssociatedClnt", "inum=AB77-1A2B,ou=clients,o=jans");
        List<UmaResource> umaResource = entryManager.findEntries("ou=resources,ou=uma,o=jans", UmaResource.class, filter);

        LOG.info("Found umaResources: " + umaResource);
    }

}
