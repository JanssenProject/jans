/*
 * Copyright [2024] [Janssen Project]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jans.lock.service.status;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.exec.CommandLine;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.lock.service.config.ConfigurationFactory;
import io.jans.model.status.FacterData;
import io.jans.model.status.StatsData;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.cdi.event.StatusCheckerTimerEvent;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import io.jans.util.StringHelper;
import io.jans.util.process.ProcessHelper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
public class StatusCheckerTimer {

    private static final int DEFAULT_INTERVAL = 5 * 60; // 1 minute
    public static final String PROGRAM_FACTER = "facter";

    @Inject
    private Logger log;

    @Inject
    private Event<TimerEvent> timerEvent;

    @Inject
    private ConfigurationFactory configurationFactory;

    private StatsData statsData = new StatsData();

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

        statsData = new StatsData();
        statsData.setLastUpdate(new Date());
        statsData.setFacterData(getFacterData());
        statsData.setDbType(configurationFactory.getBaseConfiguration().getString("persistence.type"));
        
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

    @Produces
	@RequestScoped
	public StatsData getStatsData() {
		return statsData;
	}

}
