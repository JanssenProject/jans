/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.gluu.site.ldap.persistence.AttributeData;
import org.gluu.site.ldap.persistence.AttributeDataModification;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.async.TimerSchedule;
import org.jboss.seam.core.Events;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.client.FederationMetadataClient;
import org.xdi.oxauth.client.FederationMetadataResponse;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.federation.FederationMetadata;
import org.xdi.oxauth.model.federation.FederationTrust;
import org.xdi.oxauth.model.federation.FederationTrustStatus;
import org.xdi.oxauth.model.util.Pair;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 05/11/2012
 */
@Name("federationCheckTimer")
@AutoCreate
@Scope(ScopeType.APPLICATION)
public class FederationCheckTimer {

    private final static String EVENT_TYPE = "FederationCheckTimerEvent";
    private final static long DEFAULT_INTERVAL = TimeUnit.HOURS.toMillis(24); // 24 hours

    @Logger
    private Log log;

    @In
    private LdapEntryManager ldapEntryManager;

    @Observer("org.jboss.seam.postInitialization")
    public void init() {
        if (ConfigurationFactory.instance().getConfiguration().getFederationEnabled()) {
            log.trace("Initializing FederationCheckTimer...");

            long interval = ConfigurationFactory.instance().getConfiguration().getFederationCheckInterval();
            if (interval <= 0) {
                interval = DEFAULT_INTERVAL;
            }
            interval = interval * 1000L;
            Events.instance().raiseTimedEvent(EVENT_TYPE, new TimerSchedule(interval, interval));
            log.trace("FederationCheckTimer initialized");
        }
    }

    @Observer(EVENT_TYPE)
    @Asynchronous
    public void process() {
        log.trace("Federation Trust clean up started...");
        cleanUpTrusts();
        log.trace("Federation Trust clean up finished.");
    }

    public void cleanUpTrusts() {
        try {
            final String baseDn = ConfigurationFactory.instance().getBaseDn().getFederationTrust();
            final List<FederationTrust> result = ldapEntryManager.findEntries(baseDn, FederationTrust.class, Filter.create("inum=*"));
            final Map<Pair<String, String>, List<FederationTrust>> map = groupByMetadata(result);
            if (!map.isEmpty()) {
                for (Map.Entry<Pair<String, String>, List<FederationTrust>> entry : map.entrySet()) {
                    cleanUpByMetadata(entry);
                }
            }
        } catch (LDAPException e) {
            log.trace(e.getMessage(), e);
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
        }
    }

    private static Map<Pair<String, String>, List<FederationTrust>> groupByMetadata(List<FederationTrust> p_list) {
        final Map<Pair<String, String>, List<FederationTrust>> result = new HashMap<Pair<String, String>, List<FederationTrust>>();
        if (p_list != null && !p_list.isEmpty()) {
            for (FederationTrust t : p_list) {
                final Pair<String, String> pair = new Pair<String, String>(t.getFederationMetadataUri(), t.getFederationId());
                final List<FederationTrust> value = result.get(pair);
                if (value == null) {
                    result.put(pair, new ArrayList<FederationTrust>(Arrays.asList(t)));
                } else {
                    value.add(t);
                }
            }
        }
        return result;
    }

    private void cleanUpByMetadata(Map.Entry<Pair<String, String>, List<FederationTrust>> p_entry) {
        try {
            final Pair<String, String> pair = p_entry.getKey();
            if (StringUtils.isNotBlank(pair.getFirst()) && StringUtils.isNotBlank(pair.getSecond()) &&
                    p_entry.getValue() != null && !p_entry.getValue().isEmpty()) {
                final FederationMetadataClient client = new FederationMetadataClient(pair.getFirst());
                final FederationMetadataResponse response = client.execGetMetadataById(pair.getSecond());
                if (response != null && response.getMetadata() != null) {
                    final FederationMetadata metadata = response.getMetadata();
                    log.trace("Check trusts against metadata: {0}", metadata.toString());

                    for (FederationTrust t : p_entry.getValue()) {
                        if (t.getRedirectUris() != null && !t.getRedirectUris().isEmpty()) {
                            final List<String> redirectUris = new ArrayList<String>(t.getRedirectUris());
                            final List<String> collectedMetadataUriList = metadata.collectAllRedirectUris();
                            if (redirectUris.retainAll(collectedMetadataUriList)) {
                                final List<String> outdatedList = new ArrayList<String>(t.getRedirectUris());
                                outdatedList.removeAll(collectedMetadataUriList);

                                log.trace("Removed outdated redirectUris {0}, trust dn: {1}", outdatedList, t.getDn());
                                t.setRedirectUris(redirectUris);
                                ldapEntryManager.merge(t);
                            }
                        }
                    }
                } else {
                    setStatusToInactiveByChecker(p_entry);
                }
            }
        } catch (Exception e) {
            // catch all exceptions, due to unavailability (or other reason) of meta data service
            // exception may occur, so we interrupt handling for this particular metadata endpoint but not for
            // other endpoints
            log.trace(e.getMessage(), e);
            setStatusToInactiveByChecker(p_entry);
        }
    }

    private void setStatusToInactiveByChecker(Map.Entry<Pair<String, String>, List<FederationTrust>> p_entry) {
        final Pair<String, String> pair = p_entry.getKey();
        final List<FederationTrust> list = p_entry.getValue();
        log.trace("Unable to retrieve information for metadata endpoint, url: {0}, id: {1}", pair.getFirst(), pair.getSecond());

        for (FederationTrust t : list) {
            log.trace("Unable to check info of trust, set status to inactive_by_checker, dn: {0}", t.getDn());
            final AttributeDataModification statusAttribute = new AttributeDataModification(
                    AttributeDataModification.AttributeModificationType.REPLACE,
                    new AttributeData("oxAuthFederationTrustStatus", FederationTrustStatus.INACTIVE_BY_CHECKER.getValue()));
            ldapEntryManager.merge(t.getDn(), Arrays.asList(statusAttribute));
        }
    }
}
