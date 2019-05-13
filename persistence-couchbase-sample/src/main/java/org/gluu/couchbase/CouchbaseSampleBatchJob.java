package org.gluu.couchbase;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;
import org.gluu.couchbase.model.SimpleClient;
import org.gluu.couchbase.model.SimpleSession;
import org.gluu.couchbase.model.SimpleTokenCouchbase;
import org.gluu.log.LoggingHelper;
import org.gluu.persist.couchbase.impl.CouchbaseEntryManager;
import org.gluu.persist.exception.EntryPersistenceException;
import org.gluu.persist.model.BatchOperation;
import org.gluu.persist.model.DefaultBatchOperation;
import org.gluu.persist.model.ProcessBatchOperation;
import org.gluu.persist.model.SearchScope;
import org.gluu.persist.model.base.CustomAttribute;
import org.gluu.search.filter.Filter;

/**
 * Created by eugeniuparvan on 1/12/17.
 */
public final class CouchbaseSampleBatchJob {
    private static final Logger LOG;

    static {
        StatusLogger.getLogger().setLevel(Level.OFF);
        LoggingHelper.configureConsoleAppender();
        LOG = Logger.getLogger(CouchbaseSample.class);
    }

    private CouchbaseSampleBatchJob() { }

    public static void main(String[] args) {
        // Prepare sample connection details
        CouchbaseSampleEntryManager couchbaseSampleEntryManager = new CouchbaseSampleEntryManager();

        // Create Couchbase entry manager
        final CouchbaseEntryManager couchbaseEntryManager = couchbaseSampleEntryManager.createCouchbaseEntryManager();

        BatchOperation<SimpleTokenCouchbase> tokenCouchbaseBatchOperation = new ProcessBatchOperation<SimpleTokenCouchbase>() {
            private int processedCount = 0;

            @Override
            public void performAction(List<SimpleTokenCouchbase> objects) {
                for (SimpleTokenCouchbase simpleTokenCouchbase : objects) {
                    try {
                        CustomAttribute customAttribute = getUpdatedAttribute(couchbaseEntryManager, simpleTokenCouchbase.getDn(), "oxAuthExpiration",
                                simpleTokenCouchbase.getAttribute("oxAuthExpiration"));
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

        final Filter filter1 = Filter.createPresenceFilter("oxAuthExpiration");
        couchbaseEntryManager.findEntries("o=gluu", SimpleTokenCouchbase.class, filter1, SearchScope.SUB, new String[] {"oxAuthExpiration"},
                tokenCouchbaseBatchOperation, 0, 0, 100);

        BatchOperation<SimpleSession> sessionBatchOperation = new ProcessBatchOperation<SimpleSession>() {
            private int processedCount = 0;

            @Override
            public void performAction(List<SimpleSession> objects) {
                for (SimpleSession simpleSession : objects) {
                    try {
                        CustomAttribute customAttribute = getUpdatedAttribute(couchbaseEntryManager, simpleSession.getDn(), "oxLastAccessTime",
                                simpleSession.getAttribute("oxLastAccessTime"));
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

        final Filter filter2 = Filter.createPresenceFilter("oxLastAccessTime");
        couchbaseEntryManager.findEntries("o=gluu", SimpleSession.class, filter2, SearchScope.SUB, new String[] {"oxLastAccessTime"},
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

        final Filter filter3 = Filter.createPresenceFilter("oxAuthClientSecretExpiresAt");
        List<SimpleClient> result3 = couchbaseEntryManager.findEntries("o=gluu", SimpleClient.class, filter3, SearchScope.SUB,
                new String[] {"oxAuthClientSecretExpiresAt"}, clientBatchOperation, 0, 0, 1000);

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

        final Filter filter4 = Filter.createPresenceFilter("oxAuthClientSecretExpiresAt");
        List<SimpleClient> result4 = couchbaseEntryManager.findEntries("o=gluu", SimpleClient.class, filter4, SearchScope.SUB,
                new String[] {"oxAuthClientSecretExpiresAt"}, clientBatchOperation2, 0, 0, 1000);

        LOG.info("Result count (with collecting results): " + result4.size());
    }

    private static CustomAttribute getUpdatedAttribute(CouchbaseEntryManager couchbaseEntryManager, String baseDn, String attributeName, String attributeValue) {
        try {
            Calendar calendar = Calendar.getInstance();
            Date oxLastAccessTimeDate = new Date(); //TODO: Fix it StaticUtils.decodeGeneralizedTime(attributeValue);
            calendar.setTime(oxLastAccessTimeDate);
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
