package org.gluu.ldap;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.util.StaticUtils;
import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;
import org.gluu.site.ldap.persistence.BatchOperation;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.xdi.ldap.model.CustomAttribute;
import org.xdi.ldap.model.SearchScope;
import org.xdi.log.LoggingHelper;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by eugeniuparvan on 1/12/17.
 */
public class LdapSampleBatchJob {
    private static final Logger log;

    static {
        StatusLogger.getLogger().setLevel(Level.OFF);
        LoggingHelper.configureConsoleAppender();
        log = Logger.getLogger(LdapSample.class);
    }

    public static void main(String[] args) {
        LdapSample ldapSample = new LdapSample();
        final LdapEntryManager ldapEntryManager = ldapSample.createLdapEntryManager();

        BatchOperation<SimpleTokenLdap> tokenLdapBatchOperation = new BatchOperation<SimpleTokenLdap>(ldapEntryManager) {
            @Override
            protected List<SimpleTokenLdap> getChunkOrNull(int batchSize) {
                final Filter filter = Filter.createPresenceFilter("oxAuthExpiration");
                return ldapEntryManager.findEntries("o=gluu", SimpleTokenLdap.class, filter, SearchScope.SUB, new String[]{"oxAuthExpiration"}, this, 0, batchSize, batchSize);
            }

            @Override
            protected void performAction(List<SimpleTokenLdap> objects) {
                for (SimpleTokenLdap simpleTokenLdap : objects) {
                    CustomAttribute customAttribute = getUpdatedAttribute("oxAuthExpiration", simpleTokenLdap.getAttribute("oxAuthExpiration"));
                    simpleTokenLdap.setCustomAttributes(Arrays.asList(new CustomAttribute[]{customAttribute}));
                    ldapEntryManager.merge(simpleTokenLdap);
                }
            }
        };
        tokenLdapBatchOperation.iterateAllByChunks(100);


        BatchOperation<SimpleSession> sessionBatchOperation = new BatchOperation<SimpleSession>(ldapEntryManager) {
            @Override
            protected List<SimpleSession> getChunkOrNull(int batchSize) {
                final Filter filter = Filter.createPresenceFilter("oxLastAccessTime");
                return ldapEntryManager.findEntries("o=gluu", SimpleSession.class, filter, SearchScope.SUB, new String[]{"oxLastAccessTime"}, this, 0, batchSize, batchSize);
            }

            @Override
            protected void performAction(List<SimpleSession> objects) {
                for (SimpleSession simpleSession : objects) {
                    CustomAttribute customAttribute = getUpdatedAttribute("oxLastAccessTime", simpleSession.getAttribute("oxLastAccessTime"));
                    simpleSession.setCustomAttributes(Arrays.asList(new CustomAttribute[]{customAttribute}));
                    ldapEntryManager.merge(simpleSession);
                }
            }
        };
        sessionBatchOperation.iterateAllByChunks(100);
    }

    private static CustomAttribute getUpdatedAttribute(String attributeName, String attributeValue) {
        try {
            Calendar calendar = Calendar.getInstance();
            Date oxLastAccessTimeDate = StaticUtils.decodeGeneralizedTime(attributeValue);
            calendar.setTime(oxLastAccessTimeDate);
            calendar.add(Calendar.SECOND, -1);

            CustomAttribute customAttribute = new CustomAttribute();
            customAttribute.setName(attributeName);
            customAttribute.setDate(calendar.getTime());
            return customAttribute;
        } catch (ParseException e) {
            log.error("Can't parse attribute", e);
        }
        return null;
    }
}
