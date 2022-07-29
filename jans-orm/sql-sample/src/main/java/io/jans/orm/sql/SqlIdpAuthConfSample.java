/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.sql.impl.SqlEntryManager;
import io.jans.orm.sql.model.JansConfiguration;
import io.jans.orm.sql.operation.impl.SqlConnectionProvider;
import io.jans.orm.sql.persistence.SqlEntryManagerSample;

/**
 * @author Yuriy Movchan Date: 01/15/2020
 */
public final class SqlIdpAuthConfSample {

    private static final Logger LOG = LoggerFactory.getLogger(SqlConnectionProvider.class);

    private SqlIdpAuthConfSample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        SqlEntryManagerSample sqlEntryManagerSample = new SqlEntryManagerSample();

        // Create SQL entry manager
        SqlEntryManager sqlEntryManager = sqlEntryManagerSample.createSqlEntryManager();

        JansConfiguration jansConfiguration = sqlEntryManager.find(JansConfiguration.class, "ou=configuration,o=jans");

        LOG.info("Found jansConfiguration: " + jansConfiguration);
    }

}
