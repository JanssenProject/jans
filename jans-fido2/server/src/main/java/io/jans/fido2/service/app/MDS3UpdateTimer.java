/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.app;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.jans.fido2.service.mds.TocService;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import org.slf4j.Logger;

/**
 * Class that periodically updates the mds3 blob in the FIDO2 server
 * @author madhumitas
 *
 */
@ApplicationScoped
@Named

public class MDS3UpdateTimer {

	private static final int DEFAULT_INTERVAL = 60 * 60 * 24; // every 24 hours

	@Inject
	private Logger log;

	@Inject
	private Event<TimerEvent> timerEvent;

	@Inject
	private TocService tocService;

	public void initTimer() {
		log.info("Initializing MDS3 Update Timer");

		timerEvent.fire(new TimerEvent(new TimerSchedule(DEFAULT_INTERVAL, DEFAULT_INTERVAL), new MDS3UpdateEvent() {
		}, Scheduled.Literal.INSTANCE));

		log.info("Initialized MDS3 Update Timer");
	}

	@Asynchronous
	public void process(@Observes @Scheduled MDS3UpdateEvent mds3UpdateEvent) {
		LocalDate nextUpdate = tocService.getNextUpdateDate();
		if (nextUpdate.equals(LocalDate.now()) || nextUpdate.isBefore(LocalDate.now())) {
			log.info("Downloading the latest TOC from https://mds.fidoalliance.org/");
			try {
				tocService.downloadMdsFromServer(new URL("https://mds.fidoalliance.org/"));

			} catch (MalformedURLException e) {
				log.error("Error while parsing the FIDO alliance URL :", e);
				return;
			}
			tocService.refresh();
		} else {
			log.info("{} more days for MDS3 Update", LocalDate.now().until(nextUpdate, ChronoUnit.DAYS));
		}
	}

}