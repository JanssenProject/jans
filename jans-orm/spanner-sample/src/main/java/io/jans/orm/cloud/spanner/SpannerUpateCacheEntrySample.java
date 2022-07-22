/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.cloud.spanner;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.cloud.spanner.impl.SpannerEntryManager;
import io.jans.orm.cloud.spanner.model.SimpleCacheEntry;
import io.jans.orm.cloud.spanner.operation.impl.SpannerConnectionProvider;
import io.jans.orm.cloud.spanner.persistence.SpannerEntryManagerSample;

/**
 * @author Yuriy Movchan Date: 03/09/2020
 */
public final class SpannerUpateCacheEntrySample {

    private static final Logger LOG = LoggerFactory.getLogger(SpannerConnectionProvider.class);

    private SpannerUpateCacheEntrySample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        SpannerEntryManagerSample sqlEntryManagerSample = new SpannerEntryManagerSample();

        // Create SQL entry manager
        SpannerEntryManager sqlEntryManager = sqlEntryManagerSample.createSpannerEntryManager();

        String key = UUID.randomUUID().toString();
        final String cacheDn = String.format("uuid=%s,%s", key, "ou=cache,o=jans");

        int expirationInSeconds = 60;
        Calendar expirationDate = Calendar.getInstance();
		expirationDate.setTime(new Date());
		expirationDate.add(Calendar.SECOND, expirationInSeconds);

        SimpleCacheEntry entity = new SimpleCacheEntry();
        entity.setTtl(expirationInSeconds);
        entity.setData("sample_data");
        entity.setId(key);
        entity.setDn(cacheDn);
        entity.setCreationDate(new Date());
        entity.setExpirationDate(expirationDate.getTime());
        entity.setDeletable(true);

		sqlEntryManager.persist(entity);

		// Try to update
		sqlEntryManager.merge(entity);

		// Try to update with removed value
        entity.setData(null);
		sqlEntryManager.merge(entity);
    }

}
