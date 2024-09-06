package io.jans.casa.plugins.strongauthn;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import io.jans.casa.core.ITrackable;
import io.jans.casa.conf.Basic2FASettings;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.strongauthn.conf.Configuration;
import io.jans.casa.plugins.strongauthn.conf.EnforcementPolicy;
import io.jans.casa.plugins.strongauthn.service.StrongAuthSettingsService;
import io.jans.casa.plugins.strongauthn.service.TrustedDevicesSweeper;
import io.jans.casa.service.settings.IPluginSettingsHandler;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

/**
 * A plugin for handling second factor authentication settings for administrators and users.
 * @author jgomer
 */
public class StrongAuthnSettingsPlugin extends Plugin implements ITrackable {

    public static final int TRUSTED_DEVICE_EXPIRATION_DAYS = 30;
    public static final int TRUSTED_LOCATION_EXPIRATION_DAYS = 15;

    private static final int ONE_DAY = (int) TimeUnit.DAYS.toSeconds(1);

    private Logger logger = LoggerFactory.getLogger(getClass());
    private IPluginSettingsHandler<Configuration> settingsHandler;
    private Scheduler scheduler;
    private JobKey jobKey;

    public StrongAuthnSettingsPlugin(PluginWrapper wrapper) throws Exception {
        super(wrapper);
        settingsHandler = StrongAuthSettingsService.instance(wrapper.getPluginId()).getSettingsHandler();
        scheduler = StdSchedulerFactory.getDefaultScheduler();
    }

    @Override
    public void start() {

        if (settingsHandler.getSettings() == null) {

            try {
                logger.info("Initializing missing 2FA settings");
                Configuration conf = new Configuration();
                conf.setBasic2FASettings(new Basic2FASettings());
                conf.setEnforcement2FA(Collections.singletonList(EnforcementPolicy.EVERY_LOGIN));

                settingsHandler.setSettings(conf);
                settingsHandler.save();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

        }
        jobKey = initTimer(10);

    }
    
    @Override
    public void stop() {

        try {
            if (jobKey != null) {
                logger.info("Removing trusted devices sweeper job");
                scheduler.deleteJob(jobKey);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    @Override
    public void delete() {

        try {
            logger.warn("Flushing strong authentication settings...");
            settingsHandler.setSettings(null);
            settingsHandler.save();
            logger.info("Done.");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    private JobKey initTimer(int gap) {

        try {
            if (!scheduler.isStarted()) {
                scheduler.start();
            }

            Class<? extends Job> cls = TrustedDevicesSweeper.class;
            String name = cls.getSimpleName();
            String group = getClass().getSimpleName();
            logger.info("Scheduling timer {}", name);

            JobDetail job = JobBuilder.newJob(cls).withIdentity("job_" + name, group).build();
            SimpleTrigger trigger = TriggerBuilder.newTrigger().withIdentity("trigger_" + name, group)
                    .startAt(new Date(System.currentTimeMillis() + gap * 1000))
                    .withSchedule(simpleSchedule().withIntervalInSeconds(ONE_DAY).repeatForever())
                    .build();

            scheduler.scheduleJob(job, trigger);
            return job.getKey();

        } catch (SchedulerException e) {
            logger.warn("Device sweeping won't be available");
            logger.error(e.getMessage(), e);
            return null;
        }

    }

}
