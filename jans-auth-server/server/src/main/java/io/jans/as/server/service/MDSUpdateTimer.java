package io.jans.as.server.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import io.jans.fido2.service.mds.TocService;
import io.jans.service.cdi.async.Asynchronous ;
import io.jans.service.cdi.event;.Scheduled;
import io.jans.service.timer.event;
import io.jans.service.timer.schedule.TimerSchedule;
import org.slf4j.Logger;

/**
 * @author madhumitas
 *
 */
@ApplicationScoped
@Named
public class MDS3UpdateTimer {

	private static final int DEFAULT_INTERVAL = 60 *60*24; // every 24 hours

	@Inject
	private Logger log;

	@Inject
	private Event<TimerEvent> timerEvent;

	@Inject
	private TocService tocService;

	public void initTimer() {
		log.info("Initializing MDS3 Update Timer");

		timerEvent.fire(new TimerEvent(new TimerSchedule(DEFAULT_INTERVAL, DEFAULT_INTERVAL), new MDS3UpdateEvent() {},
				Scheduled.Literal.INSTANCE));

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
			log.info( "{} more days for MDS3 Update",LocalDate.now().until(nextUpdate, ChronoUnit.DAYS) );
		}
	}

}