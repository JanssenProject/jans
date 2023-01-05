/*
 * oxCore is available under the MIT License (2014). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Gluu
 */

package io.jans.orm.sql;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.model.base.SimpleUser;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.sql.impl.SqlEntryManager;
import io.jans.orm.sql.persistence.SqlEntryManagerSample;

/**
 * @author Yuriy Movchan Date: 11/03/2016
 */
public final class SqlScimSubstringSearchSample {

    private static final Logger LOG = LoggerFactory.getLogger(SqlScimSubstringSearchSample.class);

    private SqlScimSubstringSearchSample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
    	SqlEntryManagerSample sqlEntryManagerSample = new SqlEntryManagerSample();

        // Create Couchbase entry manager
        SqlEntryManager sqlEntryManager = sqlEntryManagerSample.createSqlEntryManager();
        Filter filter = Filter.createORFilter(Filter.createORFilter(Filter.createSubstringFilter("oxTrustPhoneValue", null, new String[] {"\"type\":null"}, null).multiValued(), Filter.createSubstringFilter("oxTrustPhoneValue", null, new String[] {"\"value\":\"", "+", "\""}, null).multiValued()),
        		Filter.createSubstringFilter("mail", null, null, "gluu.org"));
        System.out.println(filter);

        List<SimpleUser> users = sqlEntryManager.findEntries("ou=people,o=gluu", SimpleUser.class, filter);
        for (SimpleUser user : users) {
            LOG.info("User with uid: '{}' with DN: '{}'", user.getUserId(), user.getDn());
        }
    }

}