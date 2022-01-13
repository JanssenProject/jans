/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.couchbase;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;

import io.jans.orm.couchbase.impl.CouchbaseEntryManager;
import io.jans.orm.couchbase.model.SimpleClient;
import io.jans.orm.couchbase.model.SimpleSession;
import io.jans.orm.couchbase.model.SimpleTokenCouchbase;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.BatchOperation;
import io.jans.orm.model.DefaultBatchOperation;
import io.jans.orm.model.ProcessBatchOperation;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.base.CustomAttribute;
import io.jans.orm.search.filter.Filter;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by eugeniuparvan on 1/12/17.
 */
public final class CouchbaseBatchJobSample {
    private static final Logger LOG;

    static {
        StatusLogger.getLogger().setLevel(Level.OFF);
        LoggingHelper.configureConsoleAppender();
        LOG = Logger.getLogger(CouchbaseSample.class);
    }

    private CouchbaseBatchJobSample() { }

    public static void main(String[] args) {
        // Prepare sample connection details
        CouchbaseEntryManagerSample couchbaseEntryManagerSample = new CouchbaseEntryManagerSample();

        // Create Couchbase entry manager
        final CouchbaseEntryManager couchbaseEntryManager = couchbaseEntryManagerSample.createCouchbaseEntryManager();

        BatchOperation<SimpleTokenCouchbase> tokenCouchbaseBatchOperation = new ProcessBatchOperation<SimpleTokenCouchbase>() {
            private int processedCount = 0;

            @Override
            public void performAction(List<SimpleTokenCouchbase> objects) {
                for (SimpleTokenCouchbase simpleTokenCouchbase : objects) {
                    try {
                        CustomAttribute customAttribute = getUpdatedAttribute(couchbaseEntryManager, simpleTokenCouchbase.getDn(), "exp",
                                simpleTokenCouchbase.getAttribute("exp"));
                        simpleTokenCouchbase.setCustomAttributes(Arrays.asList(new CustomAttribute[] {customAttribute}));
                        couchbaseEntryManager.merge(simpleTokenCouchbase);
                        processedCount++;
                    } catch (EntryPersistenceException ex) {
                        LOG.error("Failed to update entry", ex);
                    }
                }

                LOG.info("Total processed: " + processedCount);
            }
        };

        final Filter filter1 = Filter.createPresenceFilter("exp");
        couchbaseEntryManager.findEntries("o=jans", SimpleTokenCouchbase.class, filter1, SearchScope.SUB, new String[] {"exp"},
                tokenCouchbaseBatchOperation, 0, 0, 100);

        BatchOperation<SimpleSession> sessionBatchOperation = new ProcessBatchOperation<SimpleSession>() {
            private int processedCount = 0;

            @Override
            public void performAction(List<SimpleSession> objects) {
                for (SimpleSession simpleSession : objects) {
                    try {
                        CustomAttribute customAttribute = getUpdatedAttribute(couchbaseEntryManager, simpleSession.getDn(), "jansLastAccessTime",
                                simpleSession.getAttribute("jansLastAccessTime"));
                        simpleSession.setCustomAttributes(Arrays.asList(new CustomAttribute[] {customAttribute}));
                        couchbaseEntryManager.merge(simpleSession);
                        processedCount++;
                    } catch (EntryPersistenceException ex) {
                        LOG.error("Failed to update entry", ex);
                    }
                }

                LOG.info("Total processed: " + processedCount);
            }
        };

        final Filter filter2 = Filter.createPresenceFilter("jansLastAccessTime");
        couchbaseEntryManager.findEntries("o=jans", SimpleSession.class, filter2, SearchScope.SUB, new String[] {"jansLastAccessTime"},
                sessionBatchOperation, 0, 0, 100);

        BatchOperation<SimpleClient> clientBatchOperation = new ProcessBatchOperation<SimpleClient>() {
            private int processedCount = 0;

            @Override
            public void performAction(List<SimpleClient> objects) {
                for (SimpleClient simpleClient : objects) {
                    processedCount++;
                }

                LOG.info("Total processed: " + processedCount);
            }
        };

        final Filter filter3 = Filter.createPresenceFilter("exp");
        List<SimpleClient> result3 = couchbaseEntryManager.findEntries("o=jans", SimpleClient.class, filter3, SearchScope.SUB,
                new String[] {"exp"}, clientBatchOperation, 0, 0, 1000);

        LOG.info("Result count (without collecting results): " + result3.size());

        BatchOperation<SimpleClient> clientBatchOperation2 = new DefaultBatchOperation<SimpleClient>() {
            private int processedCount = 0;

            @Override
            public void performAction(List<SimpleClient> objects) {
                for (SimpleClient simpleClient : objects) {
                    processedCount++;
                }

                LOG.info("Total processed: " + processedCount);
            }
        };

        final Filter filter4 = Filter.createPresenceFilter("exp");
        List<SimpleClient> result4 = couchbaseEntryManager.findEntries("o=jans", SimpleClient.class, filter4, SearchScope.SUB,
                new String[] {"exp"}, clientBatchOperation2, 0, 0, 1000);

        LOG.info("Result count (with collecting results): " + result4.size());
    }

    private static CustomAttribute getUpdatedAttribute(CouchbaseEntryManager couchbaseEntryManager, String baseDn, String attributeName, String attributeValue) {
        try {
            Calendar calendar = Calendar.getInstance();
            Date jansLastAccessTimeDate = new Date(); //TODO: Fix it StaticUtils.decodeGeneralizedTime(attributeValue);
            calendar.setTime(jansLastAccessTimeDate);
            calendar.add(Calendar.SECOND, -1);

            CustomAttribute customAttribute = new CustomAttribute();
            customAttribute.setName(attributeName);
            customAttribute.setValue(couchbaseEntryManager.encodeTime(baseDn, calendar.getTime()));
            return customAttribute;
        } catch (Exception ex) {
            LOG.error("Can't parse attribute", ex);
        }
        return null;
    }
}
