package io.jans.as.server.service;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.cdi.event.ClientPeriodicUpdateEvent;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.base.CustomAttribute;
import io.jans.orm.model.base.CustomEntry;
import io.jans.orm.util.Pair;
import io.jans.service.BaseCacheService;
import io.jans.service.CacheService;
import io.jans.service.LocalCacheService;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * @author Yuriy Z
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class ClientLastUpdateAtTimer {

    private static final String[] CLIENT_OBJECT_CLASSES = new String[]{"jansClnt"};

    private static final int INTERVAL_IN_SECONDS = 3;

    @Inject
    private Logger log;

    @Inject
    private Event<TimerEvent> timerEvent;

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private CacheService cacheService;

    @Inject
    private LocalCacheService localCacheService;

    private AtomicBoolean isActive;

    private long lastFinishedTime;

    private final ConcurrentMap<Client, Pair<Date, Boolean>> lastUpdatedAtDebounceMap = new ConcurrentHashMap<>();

    public void initTimer() {
        log.debug("Initializing Client Periodic Update Timer");
        this.isActive = new AtomicBoolean(false);

        timerEvent.fire(
                new TimerEvent(new TimerSchedule(INTERVAL_IN_SECONDS, INTERVAL_IN_SECONDS), new ClientPeriodicUpdateEvent(), Scheduled.Literal.INSTANCE));

        this.lastFinishedTime = System.currentTimeMillis();
    }

    private boolean isStartProcess() {
        int interval = appConfiguration.getClientPeriodicUpdateTimerInterval();
        if (interval < 0) {
            log.info("Client Periodic Update Timer is disabled.");
            log.warn("Client Periodic Update Timer Interval (clientPeriodicUpdateTimerInterval in AS configuration) is negative which turns it OFF. " +
                    "Please set it to positive value if you wish internal timer to run.");
            return false;
        }

        long intervalInSeconds = interval * 1000L;

        long timeDiffrence = System.currentTimeMillis() - this.lastFinishedTime;

        return timeDiffrence >= intervalInSeconds;
    }


    @Asynchronous
    public void process(@Observes @Scheduled ClientPeriodicUpdateEvent event) {
        if (this.isActive.get()) {
            return;
        }

        if (!this.isActive.compareAndSet(false, true)) {
            return;
        }

        try {
            if (!isStartProcess()) {
                return;
            }

            processImpl();
        } finally {
            this.isActive.set(false);
            this.lastFinishedTime = System.currentTimeMillis();
        }
    }

    private void processImpl() {
         try {
             for (Map.Entry<Client, Pair<Date, Boolean>> entry :  lastUpdatedAtDebounceMap.entrySet()) {
                 updateAccessTime(entry.getKey(), entry.getValue().getSecond(), entry.getValue().getFirst());
             }
         } finally {
             lastUpdatedAtDebounceMap.clear();
         }
    }

    public void addLastUpdateAtTime(Client client, boolean isUpdateLogonTime) {
        lastUpdatedAtDebounceMap.put(client, new Pair<>(new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime(), isUpdateLogonTime));
    }

    private void updateAccessTime(Client client, boolean isUpdateLogonTime, Date lastUpdatedAt) {
        if (isFalse(appConfiguration.getUpdateClientAccessTime())) {
            return;
        }

        String clientDn = client.getDn();

        CustomEntry customEntry = new CustomEntry();
        customEntry.setDn(clientDn);
        customEntry.setCustomObjectClasses(CLIENT_OBJECT_CLASSES);

        String lastUpdatedAtDateString = entryManager.encodeTime(customEntry.getDn(), lastUpdatedAt);

        CustomAttribute customAttributeLastAccessTime = new CustomAttribute("jansLastAccessTime", lastUpdatedAtDateString);
        customEntry.getCustomAttributes().add(customAttributeLastAccessTime);

        if (isUpdateLogonTime) {
            CustomAttribute customAttributeLastLogonTime = new CustomAttribute("jansLastLogonTime", lastUpdatedAtDateString);
            customEntry.getCustomAttributes().add(customAttributeLastLogonTime);
        }

        try {
            entryManager.merge(customEntry);
        } catch (EntryPersistenceException epe) {
            log.error("Failed to update jansLastAccessTime and jansLastLogonTime of client '{}'", clientDn);
            log.trace("Failed to update user:", epe);
        }

        removeFromCache(client);
    }

    public void removeFromCache(Client client) {
        BaseCacheService usedCacheService = getCacheService();
        try {
            usedCacheService.remove(client.getDn());
        } catch (Exception e) {
            log.error("Failed to remove client from cache." + client.getDn(), e);
        }
    }

    private BaseCacheService getCacheService() {
        if (isTrue(appConfiguration.getUseLocalCache())) {
            return localCacheService;
        }

        return cacheService;
    }
}
