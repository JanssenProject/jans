/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.expiration;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.server.service.cdi.event.ExpirationEvent;
import io.jans.as.server.service.external.ExternalApplicationSessionService;
import io.jans.as.server.service.external.session.SessionEvent;
import io.jans.as.server.service.external.session.SessionEventType;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import net.jodah.expiringmap.ExpirationListener;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@Named
public class ExpirationNotificatorTimer implements ExpirationListener<ExpId, Object> {

    private static final int DEFAULT_INTERVAL = 600; // 10 min

    @Inject
    private Logger log;

    @Inject
    private Event<TimerEvent> timerEvent;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ExternalApplicationSessionService externalApplicationSessionService;

    private ExpiringMap<ExpId, Object> expiringMap = ExpiringMap.builder()
            .expirationPolicy(ExpirationPolicy.CREATED)
            .variableExpiration()
            .build();

    private AtomicBoolean isActive;

    private long lastFinishedTime;

    public void initTimer() {
        log.debug("Initializing ExpirationNotificatorTimer");
        this.isActive = new AtomicBoolean(false);

        expiringMap = ExpiringMap.builder()
                .expirationPolicy(ExpirationPolicy.CREATED)
                .maxSize(appConfiguration.getExpirationNotificatorMapSizeLimit())
                .variableExpiration()
                .build();
        expiringMap.addExpirationListener(this);

        timerEvent.fire(new TimerEvent(new TimerSchedule(DEFAULT_INTERVAL, DEFAULT_INTERVAL), new ExpirationEvent(), Scheduled.Literal.INSTANCE));

        this.lastFinishedTime = System.currentTimeMillis();
    }

    @Asynchronous
    public void process(@Observes @Scheduled ExpirationEvent expirationEvent) {
        if (!appConfiguration.getExpirationNotificatorEnabled()) {
            return;
        }

        if (this.isActive.get()) {
            return;
        }

        if (!this.isActive.compareAndSet(false, true)) {
            return;
        }

        try {
            if (!allowToRun()) {
                log.trace("Not allowed to run.");
                return;
            }
            fillMap();
            this.lastFinishedTime = System.currentTimeMillis();
        } catch (Exception ex) {
            log.error("Exception happened while trying to fill expiringMap update", ex);
        } finally {
            this.isActive.set(false);
        }
    }

    private void fillMap() {
        Calendar future = Calendar.getInstance();
        future.add(Calendar.SECOND, appConfiguration.getExpirationNotificatorIntervalInSeconds());

        fillSessions(future.getTime());
    }

    private void fillSessions(Date future) {
        final String baseDn = staticConfiguration.getBaseDn().getSessions();
        final Filter filter = Filter.createANDFilter(
                Filter.createEqualityFilter("del", true),
                Filter.createLessOrEqualFilter("exp", persistenceEntryManager.encodeTime(baseDn, future)));
        final List<SessionId> sessions = persistenceEntryManager.findEntries(baseDn, SessionId.class, filter);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        long now = new Date().getTime();
        for (SessionId session : sessions) {
            final long duration = session.getExpirationDate().getTime() - now;

            if (duration <= 0) {
                remove(session);
                continue;
            }
            expiringMap.put(new ExpId(session.getId(), ExpType.SESSION), session, duration, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void expired(ExpId key, Object value) {
        if (key.getType() == ExpType.SESSION && value instanceof SessionId) {
            externalApplicationSessionService.externalEvent(new SessionEvent(SessionEventType.GONE, (SessionId) value));
        }
    }

    private boolean allowToRun() {
        int interval = appConfiguration.getExpirationNotificatorIntervalInSeconds();
        if (interval < 0) {
            log.info("ExpirationNotificator Timer is disabled.");
            log.warn("ExpirationNotificator Timer Interval (expirationNotificatorIntervalInSeconds in oxauth configuration) is negative which turns OFF internal clean up by the server. Please set it to positive value if you wish internal clean up timer run.");
            return false;
        }

        long timerInterval = interval * 1000L;

        long timeDiffrence = System.currentTimeMillis() - this.lastFinishedTime;

        return timeDiffrence >= timerInterval;
    }

    public boolean remove(SessionId sessionId) {
        try {
            persistenceEntryManager.remove(sessionId.getDn());
            externalApplicationSessionService.externalEvent(new SessionEvent(SessionEventType.GONE, sessionId));
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }
}
