/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql;

import java.util.List;
import java.util.TimeZone;

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
public final class SqlComplexFilterSample {

    private static final Logger LOG = LoggerFactory.getLogger(SqlConnectionProvider.class);

    private SqlComplexFilterSample() {
        TimeZone newTimeZone = TimeZone.getTimeZone("America/Los_Angeles");
        TimeZone.setDefault(newTimeZone);
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        SqlEntryManagerSample sqlEntryManagerSample = new SqlEntryManagerSample();

        // Create SQL entry manager
        SqlEntryManager sqlEntryManager = sqlEntryManagerSample.createSqlEntryManager();

        String[] targetArray = new String[] { "001-220-3456" };
        
        Filter displayNameFilter = Filter.createSubstringFilter("displayName", null, targetArray,
                null);
        Filter descriptionFilter = Filter.createSubstringFilter("description", null, targetArray,
                null);
        Filter mailFilter = Filter.createSubstringFilter("mail", null, targetArray, null);
        Filter givenNameFilter = Filter.createSubstringFilter("givenName", null, targetArray, null);
        Filter middleNameFilter = Filter.createSubstringFilter("middleName", null, targetArray, null);
        Filter nicknameFilter = Filter.createSubstringFilter("nickname", null, targetArray, null);
        Filter snFilter = Filter.createSubstringFilter("sn", null, targetArray, null);
        Filter uidFilter = Filter.createSubstringFilter("uid", null, targetArray, null);
        Filter mobileFilter = Filter.createSubstringFilter("mobile", null, targetArray, null).multiValued(3);
        Filter inumFilter = Filter.createSubstringFilter("test", null, targetArray, null);
        Filter.createORFilter(displayNameFilter, descriptionFilter, mailFilter, uidFilter,
                givenNameFilter, middleNameFilter, nicknameFilter, snFilter, inumFilter, mobileFilter);

        List<SimpleUser> users = sqlEntryManager.findEntries("ou=people,o=jans", SimpleUser.class, mobileFilter, SearchScope.SUB, null, null, 0,
                0, 0);
        for (SimpleUser user : users) {
            LOG.info("User: " + user.getCustomAttributes());
        }
    }

}
