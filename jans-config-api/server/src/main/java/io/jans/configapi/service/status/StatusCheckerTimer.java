/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.status;



import com.fasterxml.jackson.databind.JsonNode;

import static io.jans.as.model.util.Util.escapeLog;

import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.model.status.StatsData;
import io.jans.configapi.model.status.FacterData;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.service.cdi.event.StatusCheckerTimerEvent;
import io.jans.configapi.util.ApiConstants;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import io.jans.util.StringHelper;
import io.jans.util.process.ProcessHelper;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
@Named
public class StatusCheckerTimer {

    private static final int DEFAULT_INTERVAL = 5 * 60; // 1 minute
    public static final String PROGRAM_FACTER = "facter";
    public static final String PROGRAM_SHOW_VERSION = "/opt/jans/printVersion.py";
    public static final String SERVICE_STATUS = "/opt/jans/bin/jans_services_status.py";

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

    /**
     * Gather periodically site and server status
     * 
     * @param when     Date
     * @param interval Interval
     */
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

    public StatsData getServerStatsData() {
        log.debug("Starting update of sever status");

        StatsData statsData = new StatsData();
        Date currentDateTime = new Date();
        statsData.setLastUpdate(currentDateTime);
        statsData.setFacterData(getFacterData());
        statsData.setDbType(configurationService.getPersistenceType());
        
        configurationService.setStatsData(statsData);        

        log.debug("statsData:{}",statsData);
        return statsData;
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
        log.debug("Getting server status for commandLine:{}", commandLine);
        
        String resultOutput;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);) {
            boolean result = ProcessHelper.executeProgram(commandLine, false, 0, bos);
            if (!result) {
                return facterData;
            }
            resultOutput = new String(bos.toByteArray(), UTF_8);
            facterData = mapper.readValue(resultOutput, FacterData.class);
        } catch (UnsupportedEncodingException uex) {
            log.error("Failed to parse program {} output", PROGRAM_FACTER, uex);
            return facterData;
        } catch (Exception ex) {
            log.error("Failed to execute program {} output:{}", PROGRAM_FACTER, ex);
        }
        log.debug("Server status - facterData:{}", facterData);
        return facterData;
    }
    
    public JsonNode getAppVersionData(String artifact) {
        if (log.isInfoEnabled()) {
            log.debug("Getting application version for artifact:{}", escapeLog(artifact));
        }
       
        JsonNode appVersion = null;
        if (!isLinux()) {
            return appVersion;
        }

        CommandLine commandLine = new CommandLine(PROGRAM_SHOW_VERSION);
        if(StringUtils.isNotBlank(artifact) && !artifact.equalsIgnoreCase(ApiConstants.ALL)) {
            commandLine.addArgument("-artifact="+artifact);
        }
        commandLine.addArgument("--json");
        log.debug("Getting application version for commandLine:{}", commandLine);
        
        String resultOutput;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);) {
            
            boolean result = ProcessHelper.executeProgram(commandLine, false, 0, bos);
            if (!result) {
                return appVersion;
            }
            
            resultOutput = new String(bos.toByteArray(), UTF_8);
            log.debug("resultOutput:{}", resultOutput);
            
            if(StringUtils.isNotBlank(resultOutput)) {
                appVersion = Jackson.asJsonNode(resultOutput);
            }
            
        } catch (UnsupportedEncodingException uex) {
            log.debug("Failed to parse program {} output", PROGRAM_SHOW_VERSION, uex);
            return appVersion;
        } catch (Exception ex) {
            log.error("Failed to execute program {} output", PROGRAM_SHOW_VERSION, ex);
            return appVersion;
        }
        log.debug("Server application version - appVersion:{}", appVersion);
        return appVersion;
    }

    public JsonNode getServiceStatus(String serviceName) {
        if (log.isInfoEnabled()) {
            log.info("Getting status for serviceName:{}", escapeLog(serviceName));
        }

        JsonNode serviceStatus = null;
        ObjectMapper mapper = new ObjectMapper();
        if (!isLinux()) {
            return serviceStatus;
        }

        CommandLine commandLine = new CommandLine(SERVICE_STATUS);
        if (StringUtils.isNotBlank(serviceName) && !serviceName.equalsIgnoreCase(ApiConstants.ALL)) {
            commandLine.addArgument(" " + serviceName);
        }
        commandLine.addArgument("--json");
        log.debug("Getting service status for commandLine:{}", commandLine);

        String resultOutput;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);) {

            boolean result = ProcessHelper.executeProgram(commandLine, false, 0, bos);
            if (!result) {
                return serviceStatus;
            }

            resultOutput = new String(bos.toByteArray(), UTF_8);
            log.info("resultOutput:{}", resultOutput);

            if (StringUtils.isNotBlank(resultOutput)) {
                serviceStatus = mapper.readValue(resultOutput, JsonNode.class);
            }

        } catch (UnsupportedEncodingException uex) {
            log.error("Failed to parse serviceStatus data program {} output", SERVICE_STATUS, uex);
            return serviceStatus;
        } catch (Exception ex) {
            log.error("Failed to execute serviceStatus program {} output", SERVICE_STATUS, ex);
            return serviceStatus;
        }
        log.debug("Service Status data - serviceStatus:{}", serviceStatus);
        return serviceStatus;
    }

    private void printDirectory() {
        log.debug("printDirectory");

        if (!isLinux()) {
            return;
        }
        CommandLine commandLine = new CommandLine("pwd");

        String resultOutput = null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);) {
            boolean result = ProcessHelper.executeProgram(commandLine, false, 0, bos);
            if (!result) {
                return;
            }
            resultOutput = new String(bos.toByteArray(), UTF_8);
            log.debug("Directory:{}", resultOutput);

        } catch (UnsupportedEncodingException uex) {
            log.debug("Failed to parse Directory program {} output", "Directory", uex);
            return;
        } catch (Exception ex) {
            log.error("Failed to execute program {} output", PROGRAM_SHOW_VERSION, ex);
        }
        log.debug(" Server Directory:{}", resultOutput);
    }

    private boolean isLinux() {
        String osName = System.getProperty("os.name");
        return !StringHelper.isEmpty(osName) && osName.toLowerCase().contains("linux");
    }
}
