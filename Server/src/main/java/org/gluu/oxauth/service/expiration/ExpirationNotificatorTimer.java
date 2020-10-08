package org.gluu.oxauth.service.expiration;

import net.jodah.expiringmap.ExpirationListener;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import org.gluu.oxauth.model.common.SessionId;
import org.gluu.oxauth.model.config.StaticConfiguration;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.service.cdi.event.ExpirationEvent;
import org.gluu.oxauth.service.external.ExternalApplicationSessionService;
import org.gluu.oxauth.service.external.session.SessionEvent;
import org.gluu.oxauth.service.external.session.SessionEventType;
import io.jans.orm.PersistenceEntryManager;
import org.gluu.search.filter.Filter;
import org.gluu.service.cdi.async.Asynchronous;
import org.gluu.service.cdi.event.Scheduled;
import org.gluu.service.timer.event.TimerEvent;
import org.gluu.service.timer.schedule.TimerSchedule;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
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

        long timerInterval = interval * 1000;

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
