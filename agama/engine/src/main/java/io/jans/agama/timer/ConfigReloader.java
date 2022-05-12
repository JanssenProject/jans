package io.jans.agama.timer;

import io.jans.agama.engine.service.AgamaPersistenceService;
import io.jans.agama.model.Config;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

@ApplicationScoped
public class ConfigReloader {
    
    private static final int DELAY = 60;    //seconds
    private static final int INTERVAL = 90;    //seconds

    @Inject
    private Logger logger;
    
    @Inject
    private Event<TimerEvent> timerEvent;

    @Inject
    private PersistenceEntryManager entryManager;

    private AtomicBoolean isActive;
    
    private Config config;
    
    @Produces
    @ApplicationScoped
    public Config configInstance() {
        return config;
    }   
    
    private Config getConfiguration() {        
        logger.info("Retrieving Agama configuration");
        return entryManager.find(Config.class, AgamaPersistenceService.AGAMA_BASE);
    }
    
    public void initTimer() {
        
        logger.info("Initializing Agama config reloader Timer");
        config = getConfiguration();
        
        isActive = new AtomicBoolean(false);
        timerEvent.fire(new TimerEvent(new TimerSchedule(DELAY, INTERVAL),
                new ConfigReloaderEvent(), Scheduled.Literal.INSTANCE));
        
    }
    
    @Asynchronous
    public void run(@Observes @Scheduled ConfigReloaderEvent event) {

        if (isActive.get()) return;
        
        if (!isActive.compareAndSet(false, true)) return;
        
        try {
            config = getConfiguration();
            logger.info("Agama config reloader timer has run.");
        } catch (Exception e) {
            logger.error("An error occurred while running agama config reloader timer", e);
        } finally {
            isActive.set(false);
        }
        
    }
    
}
