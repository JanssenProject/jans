/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import io.jans.as.common.model.common.ArchivedJwk;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.as.persistence.model.ClientAuthorization;
import io.jans.as.persistence.model.Par;
import io.jans.as.persistence.model.Scope;
import io.jans.as.server.uma.authorization.UmaPCT;
import io.jans.as.server.uma.service.UmaPctService;
import io.jans.as.server.uma.service.UmaResourceService;
import io.jans.model.ApplicationType;
import io.jans.model.metric.ldap.MetricEntry;
import io.jans.model.token.TokenEntity;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.service.cache.CacheProvider;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.CleanerEvent;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.tika.utils.StringUtils;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version August 9, 2017
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class CleanerTimer {

    public static final int BATCH_SIZE = 1000;
    private static final int DEFAULT_INTERVAL = 30; // 30 seconds

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    private UmaPctService umaPctService;

    @Inject
    private UmaResourceService umaResourceService;

    @Inject
    private CacheProvider cacheProvider;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private Event<TimerEvent> cleanerEvent;

    @Inject
    private MetricService metricService;

    private long lastFinishedTime;

    private AtomicBoolean isActive;

    public void initTimer() {
        log.debug("Initializing Cleaner Timer");
        this.isActive = new AtomicBoolean(false);

        // Schedule to start cleaner every 30 seconds
        cleanerEvent.fire(
                new TimerEvent(new TimerSchedule(DEFAULT_INTERVAL, DEFAULT_INTERVAL), new CleanerEvent(), Scheduled.Literal.INSTANCE));

        this.lastFinishedTime = System.currentTimeMillis();
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
            processImpl();
        } finally {
            this.isActive.set(false);
        }
    }

    private boolean isStartProcess() {
        int interval = appConfiguration.getCleanServiceInterval();
        if (interval < 0) {
            log.info("Cleaner Timer is disabled.");
            log.warn("Cleaner Timer Interval (cleanServiceInterval in AS configuration) is negative which turns OFF internal clean up by the server. Please set it to positive value if you wish internal clean up timer run.");
            return false;
        }

        long cleaningInterval = interval * 1000L;

        long timeDifference = System.currentTimeMillis() - this.lastFinishedTime;

        return timeDifference >= cleaningInterval;
    }

    public void processImpl() {
        try {
            if (!isStartProcess()) {
                log.trace("Starting conditions aren't reached");
                return;
            }

            int chunkSize = appConfiguration.getCleanServiceBatchChunkSize();
            if (chunkSize <= 0)
                chunkSize = BATCH_SIZE;

            Date now = new Date();

            final Set<String> processedBaseDns = new HashSet<>();
            for (Map.Entry<String, Class<?>> baseDn : createCleanServiceBaseDns().entrySet()) {

                if (StringUtils.isBlank(baseDn.getKey())) {
                    log.trace("BaseDN key is blank for class: {}", baseDn.getValue());
                    continue;
                }

                final String processedKey = createProcessedKey(baseDn);
                if (entryManager.hasExpirationSupport(baseDn.getKey()) || processedBaseDns.contains(processedKey)) {
                    continue;
                }

                processedBaseDns.add(processedKey);

                if (log.isDebugEnabled())
                    log.debug("Start clean up for baseDn: {}, class: {}", baseDn.getValue(), baseDn.getValue());

                final Stopwatch started = Stopwatch.createStarted();

                int removed = cleanup(baseDn, now, chunkSize);

                if (log.isDebugEnabled())
                    log.debug("Finished clean up for baseDn: {}, takes: {}ms, removed items: {}", baseDn, started.elapsed(TimeUnit.MILLISECONDS), removed);
            }

            processCache(now);
            processInactiveClients(chunkSize);

            this.lastFinishedTime = System.currentTimeMillis();
        } catch (Exception e) {
            log.error("Failed to process clean up.", e);
        }
    }

    private void processInactiveClients(int chunkSize) {
        try {
            final int inactiveIntervalInHours = appConfiguration.getCleanUpInactiveClientAfterHoursOfInactivity();
            if (inactiveIntervalInHours <= 0) {
                return;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR, -inactiveIntervalInHours);
            Date dateMinusInactiveHours = calendar.getTime();

            String clientsBaseDn = staticConfiguration.getBaseDn().getClients();
            Filter filter = Filter.createANDFilter(
                    Filter.createEqualityFilter("del", true),
                    Filter.createLessOrEqualFilter("jansLastAccessTime", entryManager.encodeTime(clientsBaseDn, dateMinusInactiveHours)));

            int removedCount = entryManager.remove(clientsBaseDn, Client.class, filter, chunkSize);
            log.trace("Removed {} inactive clients from {}", removedCount, clientsBaseDn);
        } catch (Exception e) {
            log.error("Failed to perform clean up of inactive clients.", e);
        }
    }

    private static String createProcessedKey(Map.Entry<String, Class<?>> baseDn) {
        return baseDn.getKey() + "_" + (baseDn.getValue() == null ? "" : baseDn.getValue().getSimpleName());
    }

    private Map<String, Class<?>> createCleanServiceBaseDns() {

        final Map<String, Class<?>> cleanServiceBaseDns = Maps.newHashMap();

        cleanServiceBaseDns.put(staticConfiguration.getBaseDn().getClients(), Client.class);
        cleanServiceBaseDns.put(umaPctService.branchBaseDn(), UmaPCT.class);
        cleanServiceBaseDns.put(umaResourceService.getBaseDnForResource(), UmaResource.class);
        cleanServiceBaseDns.put(metricService.buildDn(null, null, ApplicationType.OX_AUTH), MetricEntry.class);
        cleanServiceBaseDns.put(staticConfiguration.getBaseDn().getTokens(), TokenEntity.class);
        cleanServiceBaseDns.put(staticConfiguration.getBaseDn().getAuthorizations(), ClientAuthorization.class);
        cleanServiceBaseDns.put(staticConfiguration.getBaseDn().getScopes(), Scope.class);
        cleanServiceBaseDns.put(staticConfiguration.getBaseDn().getSessions(), SessionId.class);
        cleanServiceBaseDns.put(staticConfiguration.getBaseDn().getPar(), Par.class);
        cleanServiceBaseDns.put(staticConfiguration.getBaseDn().getArchivedJwks(), ArchivedJwk.class);

        return cleanServiceBaseDns;
    }

    public int cleanup(final Map.Entry<String, Class<?>> baseDn, final Date now, final int batchSize) {
        try {
            Filter filter = Filter.createANDFilter(
                    Filter.createEqualityFilter("del", true),
                    Filter.createLessOrEqualFilter("exp", entryManager.encodeTime(baseDn.getKey(), now)));

            int removedCount = entryManager.remove(baseDn.getKey(), baseDn.getValue(), filter, batchSize);
            log.trace("Removed {} entries from {}", removedCount, baseDn.getKey());
            return removedCount;
        } catch (Exception e) {
            log.error("Failed to perform clean up.", e);
        }

        return 0;
    }

    private void processCache(Date now) {
        try {
            cacheProvider.cleanup(now);
        } catch (Exception e) {
            log.error("Failed to clean up cache.", e);
        }
    }
}