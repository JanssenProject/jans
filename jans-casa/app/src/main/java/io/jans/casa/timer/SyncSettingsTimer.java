package io.jans.casa.timer;

import org.apache.commons.beanutils.BeanUtils;
import io.jans.casa.conf.MainSettings;
import io.jans.casa.core.AssetsService;
import io.jans.casa.core.LogService;
import io.jans.casa.core.PersistenceService;
import io.jans.casa.core.TimerService;
import org.quartz.JobExecutionContext;
import org.quartz.listeners.JobListenerSupport;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * A timer useful in environments of several nodes (eg. containers). It allows managed beans that have injected a
 * MainSettings object to have it in (nearly) up-to-date state. As an example, in a 2 node setting, node X may have saved
 * changes in the settings, but there is no means node Y can refresh the contents of the injected instance automatically.
 * This timer helps to solve this problem.
 * @author jgomer
 */
@ApplicationScoped
public class SyncSettingsTimer extends JobListenerSupport {

    public static final int SCAN_INTERVAL_SECONDS = 90;    //sync config settings every 90sec

    @Inject
    private Logger logger;

    @Inject
    private LogService logService;

    @Inject
    private TimerService timerService;

    @Inject
    private PersistenceService persistenceService;

    @Inject
    private AssetsService assetsService;

    @Inject
    private MainSettings mainSettings;

    private String jobName;

    public void activate(int gap) {

        jobName = getClass().getSimpleName() + "_syncsettings";
        try {
            timerService.addListener(this, jobName);
            //Start in 90 seconds and repeat indefinitely
            timerService.schedule(jobName,  gap, -1, SCAN_INTERVAL_SECONDS);
        } catch (Exception e) {
            logger.warn("Automatic synchronization of config settings won't be available");
            logger.error(e.getMessage(), e);
        }

    }

    @Override
    public String getName() {
        return jobName;
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {

        try {
            logger.debug("SyncSettingsTimer timer running...");
            BeanUtils.copyProperties(mainSettings, persistenceService.getAppConfiguration().getSettings());
            assetsService.reloadUrls();
            logService.updateLoggingLevel(mainSettings.getLogLevel());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}
