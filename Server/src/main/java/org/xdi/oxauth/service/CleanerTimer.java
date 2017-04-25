/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import org.gluu.site.ldap.persistence.BatchOperation;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.slf4j.Logger;
import org.xdi.model.ApplicationType;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.common.AuthorizationGrantList;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.fido.u2f.DeviceRegistration;
import org.xdi.oxauth.model.fido.u2f.RequestMessageLdap;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.service.cdi.event.CleanerEvent;
import org.xdi.oxauth.service.cdi.event.Scheduled;
import org.xdi.oxauth.service.fido.u2f.DeviceRegistrationService;
import org.xdi.oxauth.service.fido.u2f.RequestService;
import org.xdi.oxauth.service.uma.ResourceSetPermissionManager;
import org.xdi.oxauth.service.uma.RptManager;
import org.xdi.service.timer.event.TimerEvent;
import org.xdi.service.timer.schedule.TimerSchedule;

import javax.ejb.Asynchronous;
import javax.ejb.DependsOn;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version December 15, 2015
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class CleanerTimer {

    public final static int BATCH_SIZE = 100;
    private final static int DEFAULT_INTERVAL = 600; // 10 minutes

    @Inject
    private Logger log;

    @Inject
    private LdapEntryManager ldapEntryManager;

    @Inject
    private AuthorizationGrantList authorizationGrantList;

    @Inject
    private ClientService clientService;

    @Inject
    private GrantService grantService;

    @Inject
    private RptManager rptManager;

    @Inject
    private ResourceSetPermissionManager resourceSetPermissionManager;

    @Inject
    private SessionStateService sessionStateService;

    @Inject @Named("u2fRequestService")
    private RequestService u2fRequestService;

    @Inject
    private MetricService metricService;

    @Inject
    private DeviceRegistrationService deviceRegistrationService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
	private Event<TimerEvent> cleanerEvent;

    private AtomicBoolean isActive;
    
    public void initTimer() {
        log.debug("Initializing Cleaner Timer");
        this.isActive = new AtomicBoolean(false);

        int interval = appConfiguration.getCleanServiceInterval();
        if (interval <= 0) {
            interval = DEFAULT_INTERVAL;
        }

        cleanerEvent.fire(new TimerEvent(new TimerSchedule(30, 30), new CleanerEvent(), Scheduled.Literal.INSTANCE));
    }

    @Asynchronous
    public void process(@Observes @Scheduled CleanerEvent cleanerEvent) {
        if (this.isActive.get()) {
            return;
        }

        if (!this.isActive.compareAndSet(false, true)) {
            return;
        }

        try {
            processAuthorizationGrantList();
            processRegisteredClients();

            Date now = new Date();
            this.rptManager.cleanupRPTs(now);
            this.resourceSetPermissionManager.cleanupResourceSetPermissions(now);

            processU2fRequests();
            processU2fDeviceRegistrations();

            processMetricEntries();
        } finally {
            this.isActive.set(false);
        }
    }

    private void processAuthorizationGrantList() {
        log.debug("Start AuthorizationGrant clean up");
        grantService.cleanUp();
        log.debug("End AuthorizationGrant clean up");
    }

    private void processRegisteredClients() {
        log.debug("Start Client clean up");

        BatchOperation<Client> clientBatchService = new BatchOperation<Client>(ldapEntryManager) {
            @Override
            protected List<Client> getChunkOrNull(int chunkSize) {
                return clientService.getClientsWithExpirationDate(this, chunkSize, chunkSize);
            }

            @Override
            protected void performAction(List<Client> entries) {
                for (Client client : entries) {
                    try {
                        GregorianCalendar now = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                        GregorianCalendar expirationDate = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                        expirationDate.setTime(client.getClientSecretExpiresAt());
                        if (expirationDate.before(now)) {
                            List<AuthorizationGrant> toRemove = authorizationGrantList.getAuthorizationGrant(client.getClientId());
                            authorizationGrantList.removeAuthorizationGrants(toRemove);

                            log.debug("Removing Client: {}, Expiration date: {}",
                                    client.getClientId(),
                                    client.getClientSecretExpiresAt());
                            clientService.remove(client);
                        }
                    } catch (Exception e) {
                        log.error("Failed to remove entry", e);
                    }
                }
            }
        };
        clientBatchService.iterateAllByChunks(BATCH_SIZE);

        log.debug("End Client clean up");
    }

    private void processU2fRequests() {
        log.debug("Start U2F request clean up");

        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.SECOND, -90);
        final Date expirationDate = calendar.getTime();

        BatchOperation<RequestMessageLdap> requestMessageLdapBatchService = new BatchOperation<RequestMessageLdap>(ldapEntryManager) {
            @Override
            protected List<RequestMessageLdap> getChunkOrNull(int chunkSize) {
                return u2fRequestService.getExpiredRequestMessages(this, expirationDate);
            }

            @Override
            protected void performAction(List<RequestMessageLdap> entries) {
                for (RequestMessageLdap requestMessageLdap : entries) {
                    try {
                        log.debug("Removing RequestMessageLdap: {}, Creation date: {}",
                                requestMessageLdap.getRequestId(),
                                requestMessageLdap.getCreationDate());
                        u2fRequestService.removeRequestMessage(requestMessageLdap);
                    } catch (Exception e) {
                        log.error("Failed to remove entry", e);
                    }
                }
            }
        };
        requestMessageLdapBatchService.iterateAllByChunks(BATCH_SIZE);
        log.debug("End U2F request clean up");
    }

    private void processU2fDeviceRegistrations() {
        log.debug("Start U2F request clean up");

        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.SECOND, -90);
        final Date expirationDate = calendar.getTime();

        BatchOperation<DeviceRegistration> deviceRegistrationBatchService = new BatchOperation<DeviceRegistration>(ldapEntryManager) {
            @Override
            protected List<DeviceRegistration> getChunkOrNull(int chunkSize) {
                return deviceRegistrationService.getExpiredDeviceRegistrations(this, expirationDate);
            }

            @Override
            protected void performAction(List<DeviceRegistration> entries) {
                for (DeviceRegistration deviceRegistration : entries) {
                    try {
                        log.debug("Removing DeviceRegistration: {}, Creation date: {}",
                                deviceRegistration.getId(),
                                deviceRegistration.getCreationDate());
                        deviceRegistrationService.removeUserDeviceRegistration(deviceRegistration);
                    }
                    catch (Exception e){
                        log.error("Failed to remove entry", e);
                    }
                }
            }
        };
        deviceRegistrationBatchService.iterateAllByChunks(BATCH_SIZE);

        log.debug("End U2F request clean up");
    }

    private void processMetricEntries() {
        log.debug("Start metric entries clean up");

        int keepDataDays = appConfiguration.getMetricReporterKeepDataDays();

        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.DATE, -keepDataDays);
        Date expirationDate = calendar.getTime();

        metricService.removeExpiredMetricEntries(BATCH_SIZE, expirationDate, ApplicationType.OX_AUTH, metricService.applianceInum());

        log.debug("End metric entries clean up");
    }

}