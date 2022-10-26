package io.jans.agama.timer;

import io.jans.agama.engine.misc.FlowUtils;
import io.jans.agama.engine.model.FlowRun;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import static io.jans.agama.engine.service.AgamaPersistenceService.AGAMA_FLOWRUNS_BASE;

@ApplicationScoped
public class FlowRunsCleaner {
    
    private static final int DELAY = 120;    //seconds
    private static final int INTERVAL = 90;    //seconds
    private static final int GAP = 5000;    //milliseconds
    private static final int DEL_BATCH_SIZE = 100;

    @Inject
    private Logger logger;

    @Inject
    private PersistenceEntryManager entryManager;
    
    @Inject
    private Event<TimerEvent> timerEvent;
    
    @Inject
    private FlowUtils futils;

    private AtomicBoolean isActive;

    public void initTimer() {

        logger.info("Initializing Agama runs cleaner Timer");
        isActive = new AtomicBoolean(false);
        timerEvent.fire(new TimerEvent(new TimerSchedule(DELAY, INTERVAL),
                new FlowRunsCleanerEvent(), Scheduled.Literal.INSTANCE));
        
    }
    
    @Asynchronous
    public void run(@Observes @Scheduled FlowRunsCleanerEvent event) {

        if (!futils.serviceEnabled()) return;
        
        if (isActive.get()) return;
        
        if (!isActive.compareAndSet(false, true)) return;

        try {
            int count = clean();
            logger.info("Flows cleaner timer has run. {} runs removed", count);
        } catch (Exception e) {
            logger.error("An error occurred while running flows cleaner timer", e);
        } finally {
            isActive.set(false);
        }
        
    }
    
    private int clean() {

        //use a small delay window so flow rus are removed a bit after the expiration has occurred
        Date date = new Date(System.currentTimeMillis() - GAP); 
        int total = 0, removed;
        do {
            removed = entryManager.remove(AGAMA_FLOWRUNS_BASE, FlowRun.class, 
                Filter.createLessOrEqualFilter("exp", entryManager.encodeTime(AGAMA_FLOWRUNS_BASE, date)), 
                DEL_BATCH_SIZE);

            total += removed;
            logger.trace("{} entries removed", removed);
        } while (removed > 0);
        return total;
        
    }
    
}
