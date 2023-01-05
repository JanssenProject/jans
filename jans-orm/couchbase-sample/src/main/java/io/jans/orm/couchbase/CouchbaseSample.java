/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.couchbase;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.couchbase.impl.CouchbaseEntryManager;
import io.jans.orm.couchbase.model.SimpleAttribute;
import io.jans.orm.couchbase.model.SimpleSession;
import io.jans.orm.couchbase.model.SimpleToken;
import io.jans.orm.couchbase.model.SimpleUser;
import io.jans.orm.couchbase.operation.impl.CouchbaseConnectionProvider;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.SortOrder;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.orm.search.filter.Filter;

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

        // Find all with lower and or filters
        Filter orFilterWithLower = Filter.createORFilter(Filter.createEqualityFilter(Filter.createLowercaseFilter("description"), "test1"),
        		Filter.createEqualityFilter(Filter.createLowercaseFilter("description"), "test2"));
        List<SimpleUser> usersWithOrFilter = couchbaseEntryManager.findEntries("ou=sessions,o=gluu", SimpleUser.class, orFilterWithLower);
        for (SimpleUser user : usersWithOrFilter) {
            LOG.info("User with uid: '{}' with DN: '{}'", user.getUserId(), user.getDn());
        }

        // Find all with lower and or filters
        Filter orFilterWithLower2 = Filter.createORFilter(Filter.createSubstringFilter(Filter.createLowercaseFilter("description"), null, new String[] { "test1" }, null),
        		Filter.createSubstringFilter(Filter.createLowercaseFilter("displayName"), null, new String[] { "test1" }, null));
        List<SimpleUser> usersWithOrFilter2 = couchbaseEntryManager.findEntries("ou=sessions,o=gluu", SimpleUser.class, orFilterWithLower2);
        for (SimpleUser user : usersWithOrFilter2) {
            LOG.info("User with uid: '{}' with DN: '{}'", user.getUserId(), user.getDn());
        }

        // Find all users which have specified object classes defined in SimpleUser
        List<SimpleUser> users = couchbaseEntryManager.findEntries("ou=people,o=jans", SimpleUser.class, null);
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
            System.out.println("authetication result: " + result1 + ", " + result2 + ", user: " + user.getDn());
        }

        Filter filterAttributes = Filter.createEqualityFilter("jansStatus", "active");
        List<SimpleAttribute> attributes = couchbaseEntryManager.findEntries("ou=attributes,o=jans", SimpleAttribute.class, filterAttributes, SearchScope.SUB, null, null, 10,
                0, 0);
        LOG.info("Found attributes: " + attributes.size());
        for (SimpleAttribute attribute : attributes) {
            LOG.info("Attribute with displayName: " + attribute.getCustomAttributes().get(1));
        }

        Filter filterSessions = Filter.createEqualityFilter("jansState", "authenticated");
        List<SimpleSession> sessions = couchbaseEntryManager.findEntries("ou=sessions,o=jans", SimpleSession.class, filterSessions, SearchScope.SUB, null, null, 0, 1,
                0);
        LOG.info("Found sessions: " + sessions.size());

        List<SimpleToken> tokens = couchbaseEntryManager.findEntries("ou=tokens,o=jans", SimpleToken.class, null, SearchScope.SUB,
                new String[] { "code" }, null, 1, 0, 0);
        LOG.info("Found tokens: " + tokens.size());

        try {
            PagedResult<SimpleUser> listViewResponse = couchbaseEntryManager.findPagedEntries("ou=people,o=jans", SimpleUser.class, null,
                    new String[] { "uid", "displayName", "status" }, "uid", SortOrder.ASCENDING, 0, 6, 4);

            LOG.info("Found persons: " + listViewResponse.getEntriesCount() + ", total persons: " + listViewResponse.getTotalEntriesCount());
            for (SimpleUser user : listViewResponse.getEntries()) {
                System.out.println(user.getUserId());
            }
        } catch (Exception ex) {
            LOG.info("Failed to search", ex);
        }

        try {
            PagedResult<SimpleUser> listViewResponse = couchbaseEntryManager.findPagedEntries("ou=people,o=jans", SimpleUser.class, null,
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
