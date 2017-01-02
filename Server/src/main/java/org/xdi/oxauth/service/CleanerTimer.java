/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.async.TimerSchedule;
import org.jboss.seam.core.Events;
import org.jboss.seam.log.Log;
import org.xdi.model.ApplicationType;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.common.AuthorizationGrantList;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.fido.u2f.DeviceRegistration;
import org.xdi.oxauth.model.fido.u2f.RequestMessageLdap;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.service.fido.u2f.DeviceRegistrationService;
import org.xdi.oxauth.service.fido.u2f.RequestService;
import org.xdi.oxauth.service.uma.RPTManager;
import org.xdi.oxauth.service.uma.ResourceSetPermissionManager;
import org.xdi.service.batch.BatchService;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version December 15, 2015
 */
@Name("cleanerTimer")
@AutoCreate
@Scope(ScopeType.APPLICATION)
@Startup
public class CleanerTimer {

    public final static int BATCH_SIZE = 100;
    private final static String EVENT_TYPE = "CleanerTimerEvent";
    private final static int DEFAULT_INTERVAL = 600; // 10 minutes

    @Logger
    private Log log;
    @In
    private AuthorizationGrantList authorizationGrantList;
    @In
    private ClientService clientService;
    @In
    private GrantService grantService;
    @In
    private RPTManager rptManager;
    @In
    private ResourceSetPermissionManager resourceSetPermissionManager;
    @In
    private SessionStateService sessionStateService;

    @In
    private RequestService u2fRequestService;

    @In
    private MetricService metricService;
	
	@In
	private DeviceRegistrationService deviceRegistrationService;

	@In
	private ConfigurationFactory configurationFactory;

    private AtomicBoolean isActive;

    @In
    private AppConfiguration appConfiguration;

    @Observer("org.jboss.seam.postInitialization")
    public void init() {
        log.debug("Initializing CleanerTimer");
        this.isActive = new AtomicBoolean(false);

        long interval = appConfiguration.getCleanServiceInterval();
        if (interval <= 0) {
            interval = DEFAULT_INTERVAL;
        }
        interval = interval * 1000L;
        Events.instance().raiseTimedEvent(EVENT_TYPE, new TimerSchedule(interval, interval));
    }

    @Observer(EVENT_TYPE)
    @Asynchronous
    public void process() {
        if (this.isActive.get()) {
            return;
        }

        if (!this.isActive.compareAndSet(false, true)) {
            return;
        }

        try {
            processAuthorizationGrantList();
            processRegisteredClients();
            sessionStateService.cleanUpSessions(); // remove unused session ids

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

        // Cleaning oxAuthToken
        BatchService<Client> clientBatchService = new BatchService<Client>(CleanerTimer.BATCH_SIZE) {
            @Override
            protected List<Client> getChunkOrNull(int offset, int chunkSize) {
                return clientService.getClientsWithExpirationDate(offset, chunkSize, chunkSize);
            }

            @Override
            protected void performAction(List<Client> entries) {
                for (Client client : entries) {
                    GregorianCalendar now = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                    GregorianCalendar expirationDate = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                    expirationDate.setTime(client.getClientSecretExpiresAt());

                    if (expirationDate.before(now)) {
                        List<AuthorizationGrant> toRemove = authorizationGrantList.getAuthorizationGrant(client.getClientId());
                        authorizationGrantList.removeAuthorizationGrants(toRemove);

                        log.debug("Removing Client: {0}, Expiration date: {1}",
                                client.getClientId(),
                                client.getClientSecretExpiresAt());
                        clientService.remove(client);
                    }
                }
            }

        };
        clientBatchService.execute();

        log.debug("End Client clean up");
    }

    private void processU2fRequests() {
        log.debug("Start U2F request clean up");

        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.SECOND, -90);
        final Date expirationDate = calendar.getTime();

        BatchService<RequestMessageLdap> requestMessageLdapBatchService= new BatchService<RequestMessageLdap>(CleanerTimer.BATCH_SIZE) {
            @Override
            protected List<RequestMessageLdap> getChunkOrNull(int offset, int chunkSize) {
                return u2fRequestService.getExpiredRequestMessages(offset, expirationDate);
            }

            @Override
            protected void performAction(List<RequestMessageLdap> entries) {
                for (RequestMessageLdap requestMessageLdap : entries) {
                    log.debug("Removing RequestMessageLdap: {0}, Creation date: {1}",
                            requestMessageLdap.getRequestId(),
                            requestMessageLdap.getCreationDate());
                    u2fRequestService.removeRequestMessage(requestMessageLdap);
                }
            }
        };
        requestMessageLdapBatchService.execute();
        log.debug("End U2F request clean up");
    }

    private void processU2fDeviceRegistrations() {
        log.debug("Start U2F request clean up");

        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.SECOND, -90);
        final Date expirationDate = calendar.getTime();

        BatchService<DeviceRegistration> deviceRegistrationBatchService= new BatchService<DeviceRegistration>(CleanerTimer.BATCH_SIZE) {
            @Override
            protected List<DeviceRegistration> getChunkOrNull(int offset, int chunkSize) {
                return deviceRegistrationService.getExpiredDeviceRegistrations(offset, expirationDate);
            }

            @Override
            protected void performAction(List<DeviceRegistration> entries) {
                for (DeviceRegistration deviceRegistration : entries) {
                    log.debug("Removing DeviceRegistration: {0}, Creation date: {1}",
                            deviceRegistration.getId(),
                            deviceRegistration.getCreationDate());
                    deviceRegistrationService.removeUserDeviceRegistration(deviceRegistration);
                }
            }
        };
        deviceRegistrationBatchService.execute();

        log.debug("End U2F request clean up");
    }

    private void processMetricEntries() {
        log.debug("Start metric entries clean up");

        int keepDataDays = appConfiguration.getMetricReporterKeepDataDays();

        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.DATE, -keepDataDays);
        Date expirationDate = calendar.getTime();

        metricService.removeExpiredMetricEntries(expirationDate, ApplicationType.OX_AUTH, metricService.applianceInum());

        log.debug("End metric entries clean up");
    }

}