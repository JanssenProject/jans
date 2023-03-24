/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 *//*


package io.jans.cacherefresh.service.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jans.cacherefresh.config.ConfigurationFactory;
import io.jans.cacherefresh.service.ConfigurationService;
import io.jans.cacherefresh.service.status.cdi.event.StatusCheckerTimerEvent;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import io.jans.util.StringHelper;
import io.jans.util.process.ProcessHelper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.exec.CommandLine;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.charset.StandardCharsets.UTF_8;

@ApplicationScoped
@Named
public class StatusCheckerTimer {

    private static final int DEFAULT_INTERVAL = 5 * 60; // 1 minute
    public static final String PROGRAM_FACTER = "facter";

    @Inject
    private Logger log;

    @Inject
    private Event<TimerEvent> timerEvent;

    @Inject
    private ConfigurationService configurationService;

    @Inject
    private ConfigurationFactory configurationFactory;

    private AtomicBoolean isActive;

    @PostConstruct
    public void create() {
        log.debug("Creating Status Cheker Timer");
    }

    public void initTimer() {
        log.debug("Initializing Status Checker Timer");
        this.isActive = new AtomicBoolean(false);

        final int delay = 1 * 60;
        final int interval = DEFAULT_INTERVAL;

        timerEvent.fire(new TimerEvent(new TimerSchedule(delay, interval), new StatusCheckerTimerEvent(),
                Scheduled.Literal.INSTANCE));
    }

    @Asynchronous
    public void process(@Observes @Scheduled StatusCheckerTimerEvent statusCheckerTimerEvent) {
        log.debug("Status Checker Timer Process");
        if (this.isActive.get()) {
            return;
        }

        if (!this.isActive.compareAndSet(false, true)) {
            return;
        }

        try {
            processInt();
        } finally {
            this.isActive.set(false);
        }
    }

    */
/**
     * Gather periodically site and server status
     *
     *//*

    private void processInt() {
        log.debug("Starting update of sever status");

        StatsData statsData = new StatsData();
        Date currentDateTime = new Date();
        statsData.setLastUpdate(currentDateTime);
        statsData.setFacterData(getFacterData());
        statsData.setDbType(configurationService.getPersistenceType());
        
        configurationService.setStatsData(statsData);
        log.debug("Configuration status update finished");
    }

    private FacterData getFacterData() {
        log.debug("Getting Server status");
        FacterData facterData = new FacterData();
        ObjectMapper mapper = new ObjectMapper();
        if (!isLinux()) {
            return facterData;
        }
        CommandLine commandLine = new CommandLine(PROGRAM_FACTER);
        commandLine.addArgument("-j");
        String resultOutput;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);) {
            boolean result = ProcessHelper.executeProgram(commandLine, false, 0, bos);
            if (!result) {
                return facterData;
            }
            resultOutput = new String(bos.toByteArray(), UTF_8);
            facterData = mapper.readValue(resultOutput, FacterData.class);
        } catch (UnsupportedEncodingException ex) {
            log.error("Failed to parse program {} output", PROGRAM_FACTER, ex);
            return facterData;
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.debug("Server status - facterData:{}", facterData);
        return facterData;
    }

    private boolean isLinux() {
        String osName = System.getProperty("os.name");
        return !StringHelper.isEmpty(osName) && osName.toLowerCase().contains("linux");
    }
}
*/
