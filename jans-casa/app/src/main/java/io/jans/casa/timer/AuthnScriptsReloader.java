package io.jans.casa.timer;

import io.jans.casa.core.*;
import io.jans.casa.extension.AuthnMethod;

import java.util.*;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.json.JSONObject;
import org.quartz.JobExecutionContext;
import org.quartz.listeners.JobListenerSupport;
import org.slf4j.Logger;

@ApplicationScoped
public class AuthnScriptsReloader extends JobListenerSupport {

    private static final int SCAN_INTERVAL = 60;    //check the flows configs every 60sec

    @Inject
    private Logger logger;

    @Inject
    private TimerService timerService;

    @Inject
    private ConfigurationHandler confHandler;

    @Inject
    private PersistenceService persistenceService;

    @Inject
    private ExtensionsManager extManager;

    private String scriptsJobName;
    private Map<String, Integer> flowFingerPrints;

    public void init(int gap) {
        try {
            timerService.addListener(this, scriptsJobName);
            //Start in 2 seconds and repeat indefinitely
            timerService.schedule(scriptsJobName, gap, -1, SCAN_INTERVAL);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public String getName() {
        return scriptsJobName;
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {

        Integer previous, current;

        Map<String, String> mapping = confHandler.getSettings().getAcrPluginMap();
        Set<String> acrs = mapping.keySet();
        logger.debug("AuthnScriptsReloader. Running timer job for acrs: {}", acrs.toString());
        
        for (String acr : acrs) {
            JSONObject job = persistenceService.getAgamaFlowConfigProperties(acr);                       
            previous = flowFingerPrints.get(acr);
            current = Optional.ofNullable(job).map(JSONObject::toString).orElse("").hashCode();
            flowFingerPrints.put(acr, current);
            
            //if previous is null it means the given flow has just appeared - no need to reload its configs 
            if (previous != null && !current.equals(previous)) {
                logger.info("Changes detected in config of {} flow", acr);
                //Force extension reloading (normally to re-read configuration properties)
                extManager.getExtensionForAcr(acr).ifPresent(AuthnMethod::reloadConfiguration);
            }
        }

        flowFingerPrints.keySet().retainAll(acrs);

    }

    @PostConstruct
    private void inited() {
        scriptsJobName = getClass().getSimpleName() + "_scripts";
        flowFingerPrints = new HashMap<>();
    }

}
