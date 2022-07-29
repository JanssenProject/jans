/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql.persistence;

import java.util.Properties;

import org.apache.log4j.Logger;

import io.jans.orm.sql.impl.SqlEntryManager;
import io.jans.orm.sql.impl.SqlEntryManagerFactory;

/**
 * @author Yuriy Movchan Date: 01/15/2020
 */
public class SqlEntryManagerSample {

    private static final Logger LOG = Logger.getLogger(SqlEntryManagerSample.class);

    private Properties getSampleConnectionProperties() {
        Properties connectionProperties = new Properties();

        connectionProperties.put("sql#db.schema.name", "jans");
        connectionProperties.put("sql#connection.uri", "jdbc:mysql://localhost:3306/jans?profileSQL=true");

        connectionProperties.put("sql#connection.driver-property.serverTimezone", "GMT+2");
        connectionProperties.put("sql#connection.pool.max-total", "300");
        connectionProperties.put("sql#connection.pool.max-idle", "300");

        connectionProperties.put("sql#auth.userName", "jans");
        connectionProperties.put("sql#auth.userPassword", "secret");
        
        // Password hash method
        connectionProperties.put("sql#password.encryption.method", "SSHA-256");
        
        // Max time needed to create connection pool in milliseconds
        connectionProperties.put("sql#connection.pool.create-max-wait-time-millis", "20000");
        
        // Max wait 20 seconds
        connectionProperties.put("sql#connection.pool.max-wait-time-millis", "20000");
        
        // Allow to evict connection in pool after 30 minutes
        connectionProperties.put("sql#connection.pool.min-evictable-idle-time-millis", "1800000");

        connectionProperties.put("sql#binaryAttributes", "objectGUID");
        connectionProperties.put("sql#certificateAttributes", "userCertificate");

        return connectionProperties;
    }

    public SqlEntryManager createSqlEntryManager() {
        SqlEntryManagerFactory sqlEntryManagerFactory = new SqlEntryManagerFactory();
        sqlEntryManagerFactory.create();
        Properties connectionProperties = getSampleConnectionProperties();

        SqlEntryManager sqlEntryManager = sqlEntryManagerFactory.createEntryManager(connectionProperties);
        LOG.debug("Created SqlEntryManager: " + sqlEntryManager);

        return sqlEntryManager;
    }

}