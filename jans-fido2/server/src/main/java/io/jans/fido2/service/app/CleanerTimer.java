/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.app;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.service.persist.AuthenticationPersistenceService;
import io.jans.fido2.service.persist.RegistrationPersistenceService;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.base.DeletableEntity;
import io.jans.orm.search.filter.Filter;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.CleanerEvent;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * @author Yuriy Movchan Date: 05/13/2020
 */
@ApplicationScoped
@Named
public class CleanerTimer {

	public final static int BATCH_SIZE = 1000;
	private final static int DEFAULT_INTERVAL = 30; // 30 seconds

	@Inject
	private Logger log;

	@Inject
	private PersistenceEntryManager persistenceEntryManager;

	@Inject
	private StaticConfiguration staticConfiguration;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private AuthenticationPersistenceService authenticationPersistenceService;

	@Inject
	private RegistrationPersistenceService registrationPersistenceService;

	@Inject
	private Event<TimerEvent> cleanerEvent;

	private long lastFinishedTime;

	private AtomicBoolean isActive;

	public void initTimer() {
		log.debug("Initializing Cleaner Timer");
		this.isActive = new AtomicBoolean(false);

		// Schedule to start cleaner every 1 minute
		cleanerEvent.fire(
				new TimerEvent(new TimerSchedule(DEFAULT_INTERVAL, DEFAULT_INTERVAL), new CleanerEvent(), Scheduled.Literal.INSTANCE));

		this.lastFinishedTime = System.currentTimeMillis();
	}

	@Asynchronous
	public void process(@Observes @Scheduled CleanerEvent cleanerEvent) {
		if (this.isActive.get()) {
			return;
		}

		if (!this.isActive.compareAndSet(false, true)) {
			return;
		}

		try {
			processImpl();
		} finally {
			this.isActive.set(false);
		}
	}

	private boolean isStartProcess() {
		int interval = appConfiguration.getCleanServiceInterval();
		if (interval < 0) {
			log.info("Cleaner Timer is disabled.");
			log.warn("Cleaner Timer Interval (cleanServiceInterval in oxauth configuration) is negative which turns OFF internal clean up by the server. Please set it to positive value if you wish internal clean up timer run.");
			return false;
		}

		long cleaningInterval = interval * 1000;

		long timeDiffrence = System.currentTimeMillis() - this.lastFinishedTime;

		return timeDiffrence >= cleaningInterval;
	}

	public void processImpl() {
		try {
			if (!isStartProcess()) {
				log.trace("Starting conditions aren't reached");
				return;
			}

			int chunkSize = appConfiguration.getCleanServiceBatchChunkSize();
			if (chunkSize <= 0)
				chunkSize = BATCH_SIZE;

			Date now = new Date();

			for (String baseDn : createCleanServiceBaseDns()) {
				try {
					if (persistenceEntryManager.hasExpirationSupport(baseDn)) {
						continue;
					}

					log.debug("Start clean up for baseDn: " + baseDn);
					final Stopwatch started = Stopwatch.createStarted();

					int removed = cleanup(baseDn, now, chunkSize);

					log.debug("Finished clean up for baseDn: {}, takes: {}ms, removed items: {}", baseDn,
							started.elapsed(TimeUnit.MILLISECONDS), removed);
				} catch (Exception e) {
					log.error("Failed to process clean up for baseDn: " + baseDn, e);
				}
			}

			registrationPersistenceService.cleanup(now, chunkSize);
			authenticationPersistenceService.cleanup(now, chunkSize);

			this.lastFinishedTime = System.currentTimeMillis();
		} catch (Exception e) {
			log.error("Failed to process clean up.", e);
		}
	}

	public Set<String> createCleanServiceBaseDns() {
		final Set<String> cleanServiceBaseDns = Sets.newHashSet();

		cleanServiceBaseDns.add(staticConfiguration.getBaseDn().getFido2Attestation());
		cleanServiceBaseDns.add(staticConfiguration.getBaseDn().getFido2Assertion());

		log.debug("Built-in base dns: " + cleanServiceBaseDns);

		return cleanServiceBaseDns;
	}

	public int cleanup(final String baseDn, final Date now, final int batchSize) {
		try {
			Filter filter = Filter.createANDFilter(Filter.createEqualityFilter("del", true),
					Filter.createLessOrEqualFilter("exp", persistenceEntryManager.encodeTime(baseDn, now)));

			return persistenceEntryManager.remove(baseDn, DeletableEntity.class, filter, batchSize);
		} catch (Exception e) {
			log.error("Failed to perform clean up.", e);
		}

		return 0;
	}

}
