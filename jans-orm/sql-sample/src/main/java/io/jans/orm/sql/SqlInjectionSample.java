/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.model.SearchScope;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.sql.impl.SqlEntryManager;
import io.jans.orm.sql.model.SimpleUser;
import io.jans.orm.sql.operation.impl.SqlConnectionProvider;
import io.jans.orm.sql.persistence.SqlEntryManagerSample;

/**
 * @author Yuriy Movchan Date: 01/15/2020
 */
public final class SqlInjectionSample {

    private static final Logger LOG = LoggerFactory.getLogger(SqlConnectionProvider.class);

    private SqlInjectionSample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        SqlEntryManagerSample sqlEntryManagerSample = new SqlEntryManagerSample();

        // Create SQL entry manager
        SqlEntryManager sqlEntryManager = sqlEntryManagerSample.createSqlEntryManager();

        // Try to do injection on user insertion
        SimpleUser newUser1 = new SimpleUser();
        newUser1.setDn(String.format("inum=%s,ou=people,o=jans", System.currentTimeMillis()));
        newUser1.setUserId("105 OR 1=1");
        sqlEntryManager.persist(newUser1);

        SimpleUser dummyUser1 = sqlEntryManager.find(SimpleUser.class, newUser1.getDn());
        LOG.info("Dummy User '{}'", dummyUser1.getUserId());

        // Remove user
        sqlEntryManager.remove(newUser1);

        // Try to do injection on user insertion
        SimpleUser newUser2 = new SimpleUser();
        newUser2.setDn(String.format("inum=%s,ou=people,o=jans", System.currentTimeMillis()));
        newUser2.setUserId("DROP TABLE jansClnt");
        sqlEntryManager.persist(newUser2);

        SimpleUser dummyUser2 = sqlEntryManager.find(SimpleUser.class, newUser2.getDn());
        LOG.info("Dummy User '{}'", dummyUser2.getUserId());

        // Remove user
        sqlEntryManager.remove(newUser2);

        // Try to do injection on user search
        Filter filter1 = Filter.createEqualityFilter("jansStatus", "105 OR 1=1");
        List<SimpleUser> attributes1 = sqlEntryManager.findEntries("o=jans", SimpleUser.class, filter1, SearchScope.SUB, null, null, 10, 0, 0);
        LOG.info("Found users '{}'", attributes1.size());
        // ORM send query:
        //  select *, doc.doc_id
        //  from jansdb.jansAttr doc
        //  where doc.jansStatus = ?

        // Try to do injection on user search
        Filter filter2 = Filter.createEqualityFilter("jansStatus", "DROP TABLE jansClnt");
        List<SimpleUser> attributes2 = sqlEntryManager.findEntries("o=jans", SimpleUser.class, filter2, SearchScope.SUB, null, null, 10, 0, 0);
        LOG.info("Found users '{}'", attributes2.size());
        // Result: Found users '0' with filter (jansStatus=105 OR 1=1)
        // ORM send query:
        //  select *, doc.doc_id
        //  from jansdb.jansAttr doc
        //  where doc.jansStatus = ?

        // Try to do injection on user search
        Filter filter3 = Filter.createORFilter(Filter.createEqualityFilter("uid", "\" or \"\"=\""), Filter.createEqualityFilter("userPassword", "105 OR 1=1"));
        List<SimpleUser> attributes3 = sqlEntryManager.findEntries("o=jans", SimpleUser.class, filter3, SearchScope.SUB, null, null, 10, 0, 0);
        LOG.info("Found users '{}'", attributes3.size());
        // ORM send query:
        //  select *, doc.doc_id
        //  from jansdb.jansPerson doc
        //  where doc.uid = ? or doc.userPassword = ?
    }

}
