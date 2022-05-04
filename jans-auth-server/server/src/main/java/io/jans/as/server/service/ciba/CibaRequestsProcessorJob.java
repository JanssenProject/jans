/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.ciba;

import io.jans.as.model.ciba.PushErrorResponseType;
import io.jans.as.model.common.BackchannelTokenDeliveryMode;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.ciba.CIBAPingCallbackService;
import io.jans.as.server.ciba.CIBAPushErrorService;
import io.jans.as.server.model.common.CibaRequestCacheControl;
import io.jans.as.server.model.common.CibaRequestStatus;
import io.jans.as.server.model.ldap.CIBARequest;
import io.jans.as.server.util.ServerUtil;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.CibaRequestsProcessorEvent;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import org.slf4j.Logger;

import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Job responsible to process all expired CIBA requests and update their status.
 *
 * @author Milton BO
 * @version May 20, 2020
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class CibaRequestsProcessorJob {

    public static final int CHUNK_SIZE = 500; // Default value whether there isn't backchannelRequestsProcessorJobChunkSize json property value

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private Event<TimerEvent> processorEvent;

    @Inject
    private CIBAPushErrorService cibaPushErrorService;

    @Inject
    private CIBAPingCallbackService cibaPingCallbackService;

    @Inject
    private CibaRequestService cibaRequestService;

    private long lastFinishedTime;

    private AtomicBoolean isActive;

    private ExecutorService executorService;

    /**
     * Method invoked from the appInitializer to start processing every some time.
     */
    public void initTimer() {
        log.debug("Initializing CIBA requests processor");
        this.isActive = new AtomicBoolean(false);
        int intervalSec = appConfiguration.getBackchannelRequestsProcessorJobIntervalSec();

        // Schedule to start processor every N seconds
        processorEvent.fire(new TimerEvent(new TimerSchedule(intervalSec, intervalSec),
                new CibaRequestsProcessorEvent(), Scheduled.Literal.INSTANCE));

        this.lastFinishedTime = System.currentTimeMillis();
        this.executorService = Executors.newCachedThreadPool(ServerUtil.daemonThreadFactory());
    }

    @Asynchronous
    public void process(@Observes @Scheduled CibaRequestsProcessorEvent cibaRequestsProcessorEvent) {
        if (this.isActive.get()) {
            return;
        }
        if (!this.isActive.compareAndSet(false, true)) {
            return;
        }

        try {
            if (jobIsFree()) {
                processImpl();
                this.lastFinishedTime = System.currentTimeMillis();
            } else {
                log.trace("Starting conditions aren't reached for CIBA requestes processor");
            }
        } finally {
            this.isActive.set(false);
        }
    }

    /**
     * Defines whether the job is still in process or it is free according to the time interval defined.
     *
     * @return True in case it is free to start a new process.
     */
    private boolean jobIsFree() {
        int interval = appConfiguration.getBackchannelRequestsProcessorJobIntervalSec();
        if (interval < 0) {
            log.info("CIBA Requests processor timer is disabled.");
            log.warn("CIBA Requests processor timer Interval (cleanServiceInterval in oxauth configuration) is negative which turns OFF internal clean up by the server. Please set it to positive value if you wish internal CIBA Requests processor up timer run.");
            return false;
        }

        long timeDiffrence = System.currentTimeMillis() - this.lastFinishedTime;
        return timeDiffrence >= interval * 1000L;
    }

    /**
     * Main process that process CIBA requests in cache.
     */
    public void processImpl() {
        try {
            int chunkSize = appConfiguration.getBackchannelRequestsProcessorJobChunkSize() <= 0 ?
                    CHUNK_SIZE : appConfiguration.getBackchannelRequestsProcessorJobChunkSize();

            List<CIBARequest> expiredRequests = cibaRequestService.loadExpiredByStatus(
                    CibaRequestStatus.PENDING, chunkSize);
            expiredRequests.forEach(cibaRequest -> cibaRequestService.updateStatus(cibaRequest,
                    CibaRequestStatus.IN_PROCESS));

            for (CIBARequest expiredRequest : expiredRequests) {
                CibaRequestCacheControl cibaRequest = cibaRequestService.getCibaRequest(expiredRequest.getAuthReqId());
                if (cibaRequest != null) {
                    executorService.execute(() ->
                            processExpiredRequest(cibaRequest, expiredRequest.getAuthReqId())
                    );
                }
                cibaRequestService.removeCibaRequest(expiredRequest);
            }
        } catch (Exception e) {
            log.error("Failed to process CIBA request from cache.", e);
        }
    }

    /**
     * Method responsible to process expired CIBA requests, set them as expired in cache
     * and send callbacks to the client
     *
     * @param cibaRequest Object containing data related to the CIBA request.
     * @param authReqId   Authentication request id.
     */
    private void processExpiredRequest(CibaRequestCacheControl cibaRequest, String authReqId) {
        if (cibaRequest.getStatus() != CibaRequestStatus.PENDING
                && cibaRequest.getStatus() != CibaRequestStatus.EXPIRED) {
            return;
        }
        log.info("Authentication request id {} has expired", authReqId);

        cibaRequestService.removeCibaCacheRequest(cibaRequest.cacheKey());

        if (cibaRequest.getClient().getBackchannelTokenDeliveryMode() == BackchannelTokenDeliveryMode.PUSH) {
            cibaPushErrorService.pushError(cibaRequest.getAuthReqId(),
                    cibaRequest.getClient().getBackchannelClientNotificationEndpoint(),
                    cibaRequest.getClientNotificationToken(),
                    PushErrorResponseType.EXPIRED_TOKEN,
                    "Request has expired and there was no answer from the end user.");
        } else if (cibaRequest.getClient().getBackchannelTokenDeliveryMode() == BackchannelTokenDeliveryMode.PING) {
            cibaPingCallbackService.pingCallback(
                    cibaRequest.getAuthReqId(),
                    cibaRequest.getClient().getBackchannelClientNotificationEndpoint(),
                    cibaRequest.getClientNotificationToken()
            );
        }
    }

}