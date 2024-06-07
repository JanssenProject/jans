/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.cluster;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import io.jans.as.server.model.config.ConfigurationFactory;
import io.jans.as.server.service.cdi.event.TokenPoolUpdateEvent;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * @author Yuriy Movchan
 * @version 1.0, 06/03/2024
 */
@ApplicationScoped
public class TokenPoolManager {

	@Inject
	private Logger log;

	@Inject
	private ConfigurationFactory configurationFactory;

	@Inject
	private Event<TimerEvent> timerEvent;

	private AtomicBoolean isActive;

	@PostConstruct
	public void init() {
		log.info("Initializing Token Pool Manager ...");
		this.isActive = new AtomicBoolean(false);
	}

	public void initTimer() {
		log.debug("Initializing Policy Download Service Timer");

		final int delay = 10;
		final int interval = 10;

		timerEvent.fire(new TimerEvent(new TimerSchedule(delay, interval), new TokenPoolUpdateEvent(),
				Scheduled.Literal.INSTANCE));
	}

	@Asynchronous
	public void reloadPoliciesTimerEvent(@Observes @Scheduled TokenPoolUpdateEvent tokenPoolUpdateEvent) {
		if (this.isActive.get()) {
			return;
		}

		if (!this.isActive.compareAndSet(false, true)) {
			return;
		}

		try {
			updateTokenPools();
		} catch (Throwable ex) {
			log.error("Exception happened while reloading policies", ex);
		} finally {
			this.isActive.set(false);
		}
	}

	private void updateTokenPools() {
		Integer nodeId = configurationFactory.getNodeId();
		
		// TODO: Update TokenPools in DB associated with this node
		
	}


}