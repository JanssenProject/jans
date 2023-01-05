/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.couchbase;

import java.util.List;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.couchbase.impl.CouchbaseEntryManager;
import io.jans.orm.couchbase.operation.impl.CouchbaseConnectionProvider;
import io.jans.orm.model.base.SimpleUser;
import io.jans.orm.search.filter.Filter;

/**
 * @author Yuriy Movchan Date: 11/03/2022
 */
public final class CouchbaseScimSubstringSearchSample {

    private static final Logger LOG = LoggerFactory.getLogger(CouchbaseConnectionProvider.class);

    private CouchbaseScimSubstringSearchSample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
    	CouchbaseEntryManagerSample couchbaseEntryManagerSample = new CouchbaseEntryManagerSample();

        // Create Couchbase entry manager
        CouchbaseEntryManager couchbaseEntryManager = couchbaseEntryManagerSample.createCouchbaseEntryManager();
        Filter filter = Filter.createORFilter(Filter.createORFilter(Filter.createSubstringFilter("oxTrustPhoneValue", null, new String[] {"\"type\":null"}, null).multiValued(), Filter.createSubstringFilter("oxTrustPhoneValue", null, new String[] {"\"value\":\"", "+", "\""}, null).multiValued()),
        		Filter.createSubstringFilter("mail", null, null, "jans.io"));
        System.out.println(filter);

        List<SimpleUser> users = couchbaseEntryManager.findEntries("ou=people,o=jans", SimpleUser.class, filter);
        for (SimpleUser user : users) {
            LOG.info("User with uid: '{}' with DN: '{}'", user.getUserId(), user.getDn());
        }
    }

}