/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.cloud.spanner.persistence;

import java.util.Properties;

import org.apache.log4j.Logger;

import io.jans.orm.cloud.spanner.impl.SpannerEntryManager;
import io.jans.orm.cloud.spanner.impl.SpannerEntryManagerFactory;

/**
 * @author Yuriy Movchan Date: 01/15/2020
 */
public class SpannerEntryManagerSample {

    private static final Logger LOG = Logger.getLogger(SpannerEntryManagerSample.class);

    private Properties getSampleConnectionProperties() {
        Properties connectionProperties = new Properties();

        connectionProperties.put("spanner#connection.project", "jans-project");
        connectionProperties.put("spanner#connection.instance", "jans-instance");
        connectionProperties.put("spanner#connection.database", "jansdb");

        connectionProperties.put("spanner#connection.emulator-host", "localhost:9010");
        
        // Password hash method
        connectionProperties.put("spanner#password.encryption.method", "SSHA-256");
        
        // Max time needed to create connection pool in milliseconds
        connectionProperties.put("spanner#connection.pool.create-max-wait-time-millis", "20000");

        // # Maximum allowed statement result set size
        connectionProperties.put("spanner#statement.limit.default-maximum-result-size", "1000");

        // # Maximum allowed delete statement result set size
        connectionProperties.put("spanner#statement.limit.maximum-result-delete-size", "10000");

        connectionProperties.put("spanner#binaryAttributes", "objectGUID");
        connectionProperties.put("spanner#certificateAttributes", "userCertificate");

        return connectionProperties;
    }

    public SpannerEntryManager createSpannerEntryManager() {
        SpannerEntryManagerFactory sqlEntryManagerFactory = new SpannerEntryManagerFactory();
        sqlEntryManagerFactory.create();
        Properties connectionProperties = getSampleConnectionProperties();

        SpannerEntryManager sqlEntryManager = sqlEntryManagerFactory.createEntryManager(connectionProperties);
        LOG.debug("Created SpannerEntryManager: " + sqlEntryManager);

        return sqlEntryManager;
    }

}