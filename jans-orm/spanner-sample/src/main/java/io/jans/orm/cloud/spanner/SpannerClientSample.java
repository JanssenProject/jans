/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.cloud.spanner;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.cloud.spanner.impl.SpannerEntryManager;
import io.jans.orm.cloud.spanner.model.SimpleClient;
import io.jans.orm.cloud.spanner.operation.impl.SpannerConnectionProvider;
import io.jans.orm.cloud.spanner.persistence.SpannerEntryManagerSample;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;

/**
 * @author Yuriy Movchan Date: 01/15/2020
 */
public final class SpannerClientSample {

    private static final Logger LOG = LoggerFactory.getLogger(SpannerConnectionProvider.class);

    private SpannerClientSample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        SpannerEntryManagerSample sqlEntryManagerSample = new SpannerEntryManagerSample();

        // Create SQL entry manager
        SpannerEntryManager sqlEntryManager = sqlEntryManagerSample.createSpannerEntryManager();

        List<SimpleClient> result1 = sqlEntryManager.findEntries("o=jans", SimpleClient.class, null, SearchScope.SUB,
                null, null, 0, 0, 1000);
        
        LOG.info("Found clients: " + result1.size());

        PagedResult<SimpleClient> result2 = sqlEntryManager.findPagedEntries("o=jans", SimpleClient.class, null, null,
                null, null, 0, 0, 1000);

        LOG.info("Found clients: " + result2.getEntriesCount());

        
        LOG.info("Result count (without collecting results): " + result1.size());
    }

}
