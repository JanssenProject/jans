/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.cloud.spanner;

import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.cloud.spanner.impl.SpannerEntryManager;
import io.jans.orm.cloud.spanner.model.SimpleSessionState;
import io.jans.orm.cloud.spanner.operation.impl.SpannerConnectionProvider;
import io.jans.orm.cloud.spanner.persistence.SpannerEntryManagerSample;
import io.jans.orm.exception.EntryPersistenceException;

/**
 * @author Yuriy Movchan Date: 03/09/2020
 */
public final class SpannerUpateMissingEntrySample {

    private static final Logger LOG = LoggerFactory.getLogger(SpannerConnectionProvider.class);

    private SpannerUpateMissingEntrySample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        SpannerEntryManagerSample sqlEntryManagerSample = new SpannerEntryManagerSample();

        // Create SQL entry manager
        SpannerEntryManager sqlEntryManager = sqlEntryManagerSample.createSpannerEntryManager();

        String sessionId = UUID.randomUUID().toString();
        final String sessionDn = "uniqueIdentifier=" + sessionId + ",ou=session,o=jans";

        final SimpleSessionState simpleSessionState = new SimpleSessionState();
        simpleSessionState.setDn(sessionDn);
        simpleSessionState.setId(sessionId);
        simpleSessionState.setLastUsedAt(new Date());

        try {
			sqlEntryManager.merge(simpleSessionState);
			System.out.println("Updated");
		} catch (EntryPersistenceException ex) {
            LOG.info("Failed to update, root case exception: {}", ex.getCause().getClass(), ex);
		}
    }

}
