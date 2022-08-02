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
import io.jans.orm.couchbase.model.SimpleCache;
import io.jans.orm.couchbase.model.SimpleClient;
import io.jans.orm.couchbase.model.SimpleSession;
import io.jans.orm.couchbase.model.SimpleToken;
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

        BatchOperation<SimpleToken> tokenCouchbaseBatchOperation = new ProcessBatchOperation<SimpleToken>() {
            private int processedCount = 0;

            @Override
            public void performAction(List<SimpleToken> objects) {
                for (SimpleToken simpleTokenCouchbase : objects) {
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
        couchbaseEntryManager.findEntries("ou=tokens,o=jans", SimpleToken.class, filter1, SearchScope.SUB, new String[] {"exp"},
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
        couchbaseEntryManager.findEntries("ou=sessions,o=jans", SimpleSession.class, filter2, SearchScope.SUB, new String[] {"jansLastAccessTime"},
                sessionBatchOperation, 0, 0, 100);

        if (false) {
            Calendar calendar = Calendar.getInstance();
            Date jansLastAccessTimeDate = new Date();
            calendar.setTime(jansLastAccessTimeDate);
            calendar.add(Calendar.SECOND, 60);
            Date date = calendar.getTime();

            for (int i = 0; i < 1111; i++) {
	            String id = String.format("cache_%06d", i);
	            String dn = String.format("id=%s,ou=cache,o=jans", id);
	
	            SimpleCache newCache = new SimpleCache();
	            newCache.setDn(dn);
	            newCache.setId(id);
	            newCache.setData("{'sample_data': 'sample_data_value'}");
	            newCache.setExpirationDate(date);
	            newCache.setDeletable(true);
	
	    		try {
	                couchbaseEntryManager.persist(newCache);
	            } catch (Throwable e) {
	                e.printStackTrace();
	            }
	        }
        }

        BatchOperation<SimpleCache> clientBatchOperation = new ProcessBatchOperation<SimpleCache>() {
            private int processedCount = 0;

            @Override
            public void performAction(List<SimpleCache> objects) {
                for (SimpleCache simpleCache : objects) {
                    processedCount++;
                }

                LOG.info("Total processed: " + processedCount);
            }
        };
        final Filter filter3 = Filter.createPresenceFilter("exp");
        List<SimpleCache> result3 = couchbaseEntryManager.findEntries("ou=cache,o=jans", SimpleCache.class, filter3, SearchScope.SUB,
                new String[] {"exp"}, clientBatchOperation, 0, 0, 333);

        LOG.info("Result count (without collecting results): " + result3.size());

        BatchOperation<SimpleCache> clientBatchOperation2 = new DefaultBatchOperation<SimpleCache>() {
            private int processedCount = 0;

            @Override
            public void performAction(List<SimpleCache> objects) {
                for (SimpleCache simpleCache : objects) {
                    processedCount++;
                }

                LOG.info("Total processed: " + processedCount);
            }
        };

        final Filter filter4 = Filter.createPresenceFilter("exp");
        List<SimpleCache> result4 = couchbaseEntryManager.findEntries("ou=cache,o=jans", SimpleCache.class, filter4, SearchScope.SUB,
                new String[] {"exp"}, clientBatchOperation2, 0, 0, 333);

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
