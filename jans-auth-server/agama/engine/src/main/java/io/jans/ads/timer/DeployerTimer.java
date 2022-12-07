package io.jans.ads.timer;

import io.jans.ads.Deployer;
import io.jans.agama.engine.misc.FlowUtils;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

@ApplicationScoped
public class DeployerTimer {

    @Inject
    private Deployer deployer;

    @Inject
    private Logger logger;
    
    @Inject
    private Event<TimerEvent> timerEvent;
    
    @Inject
    private FlowUtils futils;

    private AtomicBoolean isActive;
    
    private static final int DELAY = 5 + (int) (10 * Math.random());    //seconds
    private static final int INTERVAL = 30;    // seconds

    public void initTimer() {
        
        logger.info("Initializing Agama transpilation Timer");
        isActive = new AtomicBoolean(false);
        timerEvent.fire(new TimerEvent(new TimerSchedule(DELAY, INTERVAL),
                new DeploymentEvent(), Scheduled.Literal.INSTANCE));
        
    }
    
    @Asynchronous
    public void run(@Observes @Scheduled DeploymentEvent event) {

        if (!futils.serviceEnabled()) return;

        if (isActive.get()) return;
        
        if (!isActive.compareAndSet(false, true)) return;

        try {
            deployer.process();
            logger.debug("ADS deployer timer has run.");
        } catch (Exception e) {
            logger.error("An error occurred while running ADS deployer timer", e);
        } finally {
            isActive.set(false);
        }

    }
    
}
