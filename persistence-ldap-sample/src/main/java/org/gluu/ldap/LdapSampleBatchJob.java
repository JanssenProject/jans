package org.gluu.ldap;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;
import org.gluu.ldap.model.SimpleClient;
import org.gluu.ldap.model.SimpleSession;
import org.gluu.ldap.model.SimpleTokenLdap;
import org.gluu.log.LoggingHelper;
import org.gluu.persist.exception.EntryPersistenceException;
import org.gluu.persist.ldap.impl.LdapEntryManager;
import org.gluu.persist.model.BatchOperation;
import org.gluu.persist.model.DefaultBatchOperation;
import org.gluu.persist.model.ProcessBatchOperation;
import org.gluu.persist.model.SearchScope;
import org.gluu.persist.model.base.CustomAttribute;
import org.gluu.search.filter.Filter;

import com.unboundid.util.StaticUtils;

/**
 * Created by eugeniuparvan on 1/12/17.
 */
public final class LdapSampleBatchJob {
    private static final Logger LOG;

    static {
        StatusLogger.getLogger().setLevel(Level.OFF);
        LoggingHelper.configureConsoleAppender();
        LOG = Logger.getLogger(LdapSample.class);
    }

    private LdapSampleBatchJob() { }

    public static void main(String[] args) {
        // Prepare sample connection details
        LdapSampleEntryManager ldapSampleEntryManager = new LdapSampleEntryManager();

        // Create LDAP entry manager
        final LdapEntryManager ldapEntryManager = ldapSampleEntryManager.createLdapEntryManager();

        BatchOperation<SimpleTokenLdap> tokenLdapBatchOperation = new ProcessBatchOperation<SimpleTokenLdap>() {
            private int processedCount = 0;

            @Override
            public void performAction(List<SimpleTokenLdap> objects) {
                for (SimpleTokenLdap simpleTokenLdap : objects) {
                    try {
                        CustomAttribute customAttribute = getUpdatedAttribute(ldapEntryManager, "oxAuthExpiration",
                                simpleTokenLdap.getAttribute("oxAuthExpiration"));
                        simpleTokenLdap.setCustomAttributes(Arrays.asList(new CustomAttribute[] {customAttribute}));
                        ldapEntryManager.merge(simpleTokenLdap);
                        processedCount++;
                    } catch (EntryPersistenceException ex) {
                        LOG.error("Failed to update entry", ex);
                    }
                }

                LOG.info("Total processed: " + processedCount);
            }
        };

        final Filter filter1 = Filter.createPresenceFilter("oxAuthExpiration");
        ldapEntryManager.findEntries("o=gluu", SimpleTokenLdap.class, filter1, SearchScope.SUB, new String[] {"oxAuthExpiration"},
                tokenLdapBatchOperation, 0, 0, 100);

        BatchOperation<SimpleSession> sessionBatchOperation = new ProcessBatchOperation<SimpleSession>() {
            private int processedCount = 0;

            @Override
            public void performAction(List<SimpleSession> objects) {
                for (SimpleSession simpleSession : objects) {
                    try {
                        CustomAttribute customAttribute = getUpdatedAttribute(ldapEntryManager, "oxLastAccessTime",
                                simpleSession.getAttribute("oxLastAccessTime"));
                        simpleSession.setCustomAttributes(Arrays.asList(new CustomAttribute[] {customAttribute}));
                        ldapEntryManager.merge(simpleSession);
                        processedCount++;
                    } catch (EntryPersistenceException ex) {
                        LOG.error("Failed to update entry", ex);
                    }
                }

                LOG.info("Total processed: " + processedCount);
            }
        };

        final Filter filter2 = Filter.createPresenceFilter("oxLastAccessTime");
        ldapEntryManager.findEntries("o=gluu", SimpleSession.class, filter2, SearchScope.SUB, new String[] {"oxLastAccessTime"},
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
        List<SimpleClient> result3 = ldapEntryManager.findEntries("o=gluu", SimpleClient.class, filter3, SearchScope.SUB,
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
        List<SimpleClient> result4 = ldapEntryManager.findEntries("o=gluu", SimpleClient.class, filter4, SearchScope.SUB,
                new String[] {"oxAuthClientSecretExpiresAt"}, clientBatchOperation2, 0, 0, 1000);

        LOG.info("Result count (with collecting results): " + result4.size());
    }

    private static CustomAttribute getUpdatedAttribute(LdapEntryManager ldapEntryManager, String attributeName, String attributeValue) {
        try {
            Calendar calendar = Calendar.getInstance();
            Date oxLastAccessTimeDate = StaticUtils.decodeGeneralizedTime(attributeValue);
            calendar.setTime(oxLastAccessTimeDate);
            calendar.add(Calendar.SECOND, -1);

            CustomAttribute customAttribute = new CustomAttribute();
            customAttribute.setName(attributeName);
            customAttribute.setValue(ldapEntryManager.encodeTime(calendar.getTime()));
            return customAttribute;
        } catch (ParseException e) {
            LOG.error("Can't parse attribute", e);
        }
        return null;
    }
}
