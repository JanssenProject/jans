/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.cloud.spanner;

import java.util.Arrays;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.cloud.spanner.impl.SpannerEntryManager;
import io.jans.orm.cloud.spanner.impl.SpannerEntryManagerFactory;
import io.jans.orm.cloud.spanner.model.SimpleUser;
import io.jans.orm.cloud.spanner.operation.impl.SpannerConnectionProvider;
import io.jans.orm.model.base.CustomObjectAttribute;

/**
 * @author Yuriy Movchan Date: 01/15/2020
 */
public final class SpannerSample {

    private static final Logger LOG = LoggerFactory.getLogger(SpannerConnectionProvider.class);

    private SpannerSample() {
    }

    public static void main(String[] args) {
        // Create Sql entry manager
        SpannerEntryManager couchbaseEntryManager = createSpannerEntryManager();

        // Create and fill user bean
        SimpleUser newUser = new SimpleUser();
        newUser.setDn(String.format("inum=%s,ou=people,o=jans", System.currentTimeMillis()));
        newUser.setUserId("sample_user_" + System.currentTimeMillis());
        newUser.setUserPassword("test");
        newUser.getCustomAttributes().add(new CustomObjectAttribute("jansAddress", Arrays.asList("London", "Texas", "New York")));
        newUser.getCustomAttributes().add(new CustomObjectAttribute("jansGuid", "test_value"));
        
        // Call ORM API to store entry
        couchbaseEntryManager.persist(newUser);
        
        couchbaseEntryManager.destroy();
    }

    public static SpannerEntryManager createSpannerEntryManager() {
    	SpannerEntryManagerFactory couchbaseEntryManagerFactory = new SpannerEntryManagerFactory();
        couchbaseEntryManagerFactory.create();
        Properties connectionProperties = getSampleConnectionProperties();

        SpannerEntryManager couchbaseEntryManager = couchbaseEntryManagerFactory.createEntryManager(connectionProperties);

        return couchbaseEntryManager;
    }

    private static Properties getSampleConnectionProperties() {
        Properties connectionProperties = new Properties();

        connectionProperties.put("spanner#connection.project", "jans-project");
        connectionProperties.put("spanner#connection.instance", "jans-instance");
        connectionProperties.put("spanner#connection.database", "jansdb");

        boolean emulator = true;
		if (emulator) {
			connectionProperties.put("spanner#connection.emulator-host", "localhost:9010");
		} else {
			connectionProperties.put("spanner#connection.credentials-file", "V:\\dev-gluu-cloud-platform-32136abdceb7.json");
		}

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

}
