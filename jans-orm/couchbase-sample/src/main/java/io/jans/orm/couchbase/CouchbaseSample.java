/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.couchbase;

import java.util.Arrays;
import java.util.List;

import io.jans.orm.couchbase.impl.CouchbaseEntryManager;
import io.jans.orm.couchbase.model.SimpleAttribute;
import io.jans.orm.couchbase.model.SimpleGrant;
import io.jans.orm.couchbase.model.SimpleSession;
import io.jans.orm.couchbase.model.SimpleUser;
import io.jans.orm.couchbase.operation.impl.CouchbaseConnectionProvider;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.SortOrder;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.orm.search.filter.Filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yuriy Movchan Date: 11/03/2016
 */
public final class CouchbaseSample {

    private static final Logger LOG = LoggerFactory.getLogger(CouchbaseConnectionProvider.class);

    private CouchbaseSample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        CouchbaseEntryManagerSample couchbaseEntryManagerSample = new CouchbaseEntryManagerSample();

        // Create Couchbase entry manager
        CouchbaseEntryManager couchbaseEntryManager = couchbaseEntryManagerSample.createCouchbaseEntryManager();

        SimpleUser newUser = new SimpleUser();
        newUser.setDn(String.format("inum=%s,ou=people,o=jans", System.currentTimeMillis()));
        newUser.setUserId("sample_user_" + System.currentTimeMillis());
        newUser.setUserPassword("test");
        newUser.getCustomAttributes().add(new CustomObjectAttribute("streetAddress", Arrays.asList("London", "Texas", "Kiev")));
        newUser.getCustomAttributes().add(new CustomObjectAttribute("test", "test_value"));
        couchbaseEntryManager.persist(newUser);

//        SimpleUser dummyUser = couchbaseEntryManager.find(SimpleUser.class, "inum=test,o=test,o=jans");
//        LOG.info("Dummy User '{}'", dummyUser);

        // Find all users which have specified object classes defined in SimpleUser
        List<SimpleUser> users = couchbaseEntryManager.findEntries("o=@!5304.5F36.0E64.E1AC!0001!179C.62D7,o=jans", SimpleUser.class, null);
        for (SimpleUser user : users) {
            LOG.info("User with uid: '{}' with DN: '{}'", user.getUserId(), user.getDn());
        }

        if (users.size() > 0) {
            // Add attribute "streetAddress" to first user
            SimpleUser user = users.get(3);
            LOG.info("Updating: " + user.getUserId());

            String[] values = new String[] { "Somewhere: " + System.currentTimeMillis(), "Somewhere2: " + System.currentTimeMillis() };
            user.getCustomAttributes().add(new CustomObjectAttribute("streetAddress", Arrays.asList(values)));
            user.getCustomAttributes().add(new CustomObjectAttribute("test", "test_value"));
            user.getCustomAttributes().add(new CustomObjectAttribute("test2", "test_value2"));
            user.getCustomAttributes().add(new CustomObjectAttribute("test3", "test_value3"));
            user.setUserId("user1");
            user.setUserPassword("test");

            couchbaseEntryManager.merge(user);
        }

        for (SimpleUser user : users) {
            boolean result1 = couchbaseEntryManager.authenticate(user.getDn(), "test");
            boolean result2 = couchbaseEntryManager.authenticate("ou=people,o=jans", SimpleUser.class, user.getUserId(), "test");
            System.out.println("authetication result: " + result1 + ", " + result2);
        }

        Filter filter = Filter.createEqualityFilter("status", "active");
        List<SimpleAttribute> attributes = couchbaseEntryManager.findEntries("o=jans", SimpleAttribute.class, filter, SearchScope.SUB, null, null, 10,
                0, 0);
        for (SimpleAttribute attribute : attributes) {
            LOG.info("Attribute with displayName: " + attribute.getCustomAttributes().get(1));
        }

        List<SimpleSession> sessions = couchbaseEntryManager.findEntries("o=jans", SimpleSession.class, filter, SearchScope.SUB, null, null, 10, 0,
                0);
        LOG.info("Found sessions: " + sessions.size());

        List<SimpleGrant> grants = couchbaseEntryManager.findEntries("o=jans", SimpleGrant.class, null, SearchScope.SUB,
                new String[] { "grtId" }, null, 1, 0, 0);
        LOG.info("Found grants: " + grants.size());

        try {
            PagedResult<SimpleUser> listViewResponse = couchbaseEntryManager.findPagedEntries("o=jans", SimpleUser.class, null,
                    new String[] { "uid", "displayName", "status" }, "uid", SortOrder.ASCENDING, 0, 6, 4);

            LOG.info("Found persons: " + listViewResponse.getEntriesCount() + ", total persons: " + listViewResponse.getTotalEntriesCount());
            for (SimpleUser user : listViewResponse.getEntries()) {
                System.out.println(user.getUserId());
            }
        } catch (Exception ex) {
            LOG.info("Failed to search", ex);
        }

        try {
            PagedResult<SimpleUser> listViewResponse = couchbaseEntryManager.findPagedEntries("o=jans", SimpleUser.class, null,
                    new String[] { "uid", "displayName", "status" }, "uid", SortOrder.DESCENDING, 0, 6, 4);

            LOG.info("Found persons: " + listViewResponse.getEntriesCount() + ", total persons: " + listViewResponse.getTotalEntriesCount());
            for (SimpleUser user : listViewResponse.getEntries()) {
                System.out.println(user.getUserId());
            }
        } catch (Exception ex) {
            LOG.info("Failed to search", ex);
        }

    }

}
