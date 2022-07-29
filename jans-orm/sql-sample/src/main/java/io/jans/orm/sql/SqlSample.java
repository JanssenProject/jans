/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.SortOrder;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.sql.impl.SqlEntryManager;
import io.jans.orm.sql.model.SimpleAttribute;
import io.jans.orm.sql.model.SimpleGrant;
import io.jans.orm.sql.model.SimpleSession;
import io.jans.orm.sql.model.SimpleUser;
import io.jans.orm.sql.operation.impl.SqlConnectionProvider;
import io.jans.orm.sql.persistence.SqlEntryManagerSample;

/**
 * @author Yuriy Movchan Date: 01/15/2020
 */
public final class SqlSample {

    private static final Logger LOG = LoggerFactory.getLogger(SqlConnectionProvider.class);

    private SqlSample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        SqlEntryManagerSample sqlEntryManagerSample = new SqlEntryManagerSample();

        // Create SQL entry manager
        SqlEntryManager sqlEntryManager = sqlEntryManagerSample.createSqlEntryManager();

        SimpleUser newUser = new SimpleUser();
        newUser.setDn(String.format("inum=%s,ou=people,o=jans", System.currentTimeMillis()));
        newUser.setUserId("sample_user_" + System.currentTimeMillis());
        newUser.setUserPassword("pwd");
        newUser.getCustomAttributes().add(new CustomObjectAttribute("address", Arrays.asList("London", "Texas", "Kiev")));
        newUser.getCustomAttributes().add(new CustomObjectAttribute("transientId", "transientId"));
        sqlEntryManager.persist(newUser);

        SimpleUser dummyUser = sqlEntryManager.find(SimpleUser.class, newUser.getDn());
        LOG.info("Dummy User '{}'", dummyUser);

        // Find all users which have specified object classes defined in SimpleUser
        List<SimpleUser> users = sqlEntryManager.findEntries("ou=people,o=jans", SimpleUser.class, null);
        for (SimpleUser user : users) {
            LOG.info("User with uid: '{}' with DN: '{}'", user.getUserId(), user.getDn());
        }

        if (users.size() > 0) {
            // Add attribute "address" to first user
            SimpleUser user = users.get(0);
            LOG.info("Updating: " + user.getUserId());
 
            String[] values = new String[] { "Somewhere: " + System.currentTimeMillis(), "Somewhere2: " + System.currentTimeMillis() };
            user.getCustomAttributes().add(new CustomObjectAttribute("address", Arrays.asList(values)));
            user.getCustomAttributes().add(new CustomObjectAttribute("transientId", "new_transientId"));
            user.getCustomAttributes().add(new CustomObjectAttribute("jansGuid", "test_guid"));
            user.setUserId("user1");
            user.setUserPassword("test_pwd");

            sqlEntryManager.merge(user);
        }

        for (SimpleUser user : users) {
            boolean result1 = sqlEntryManager.authenticate(user.getDn(), SimpleUser.class, "test_pwd");
            boolean result2 = sqlEntryManager.authenticate("ou=people,o=jans", SimpleUser.class, user.getUserId(), "test");
            System.out.println("authetication result: " + result1 + ", " + result2);
        }

        Filter filter = Filter.createEqualityFilter("jansStatus", "active");
        List<SimpleAttribute> attributes = sqlEntryManager.findEntries("o=jans", SimpleAttribute.class, filter, SearchScope.SUB, null, null, 10,
                0, 0);
        for (SimpleAttribute attribute : attributes) {
            LOG.info("Attribute with displayName: " + attribute.getCustomAttributes().get(1));
        }

        Filter filter2 = Filter.createEqualityFilter("jansState", "authenticated");
        List<SimpleSession> sessions = sqlEntryManager.findEntries("o=jans", SimpleSession.class, filter2, SearchScope.SUB, null, null, 10, 0,
                0);
        LOG.info("Found sessions: " + sessions.size());

        List<SimpleGrant> grants = sqlEntryManager.findEntries("o=jans", SimpleGrant.class, null, SearchScope.SUB,
                new String[] { "grtId" }, null, 1, 0, 0);
        LOG.info("Found grants: " + grants.size());

        try {
            PagedResult<SimpleUser> listViewResponse = sqlEntryManager.findPagedEntries("o=jans", SimpleUser.class, null,
                    new String[] { "uid", "displayName", "jansStatus" }, "uid", SortOrder.ASCENDING, 0, 6, 4);

            LOG.info("Found persons: " + listViewResponse.getEntriesCount() + ", total persons: " + listViewResponse.getTotalEntriesCount());
            for (SimpleUser user : listViewResponse.getEntries()) {
                System.out.println(user.getUserId());
            }
        } catch (Exception ex) {
            LOG.info("Failed to search", ex);
        }

        try {
            PagedResult<SimpleUser> listViewResponse = sqlEntryManager.findPagedEntries("o=jans", SimpleUser.class, null,
                    new String[] { "uid", "displayName", "jansStatus" }, "uid", SortOrder.DESCENDING, 0, 6, 4);

            LOG.info("Found persons: " + listViewResponse.getEntriesCount() + ", total persons: " + listViewResponse.getTotalEntriesCount());
            for (SimpleUser user : listViewResponse.getEntries()) {
                System.out.println(user.getUserId());
            }
        } catch (Exception ex) {
            LOG.info("Failed to search", ex);
        }

    }

}
