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

package io.jans.lock.service.ws.rs.sse;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import io.jans.as.client.StatusListResponse;
import io.jans.lock.service.TokenStsatusListService;
import io.jans.lock.service.event.TokenStatusListReloadEvent;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;

/**
 * @author Yuriy Movchan Date: 05/24/2024
 */
@ApplicationScoped
public class LockSseBroadcaster {

	private static final int DEFAULT_INTERVAL = 15;
	
	private static final String STATUS_LIST_MESSAGE = "STATUS_LIST";

	@Inject
	private Logger log;

	@Inject
	private Event<TimerEvent> timerEvent;
	
	@Inject
	private TokenStsatusListService tokenStsatusListService;

    private SseBroadcaster sseBroadcaster;
	private Sse sse;

	private AtomicBoolean isActive;

	@PostConstruct
	public void init() {
		log.info("Initializing Token SSE broadcaster ...");
		this.isActive = new AtomicBoolean(false);
	}
	
	@Context
	public void initSse(Sse sse) {
        log.debug("initSse broadcaster");
	    this.sse = sse;
//	    this.eventBuilder = sse.newEventBuilder();
	    this.sseBroadcaster = sse.newBroadcaster();
	}

	public void initTimer() {
		log.debug("Initializing Token Status List Timer");

		final int delay = 30;
		final int interval = DEFAULT_INTERVAL;

		timerEvent.fire(new TimerEvent(new TimerSchedule(delay, interval), new TokenStatusListReloadEvent(),
				Scheduled.Literal.INSTANCE));
	}

	@Asynchronous
	public void reloadTokenStatusListTimerEvent(@Observes @Scheduled TokenStatusListReloadEvent tokenStatusListReloadEvent) {
		if (this.isActive.get()) {
			return;
		}

		if (!this.isActive.compareAndSet(false, true)) {
			return;
		}

		try {
			reloadTokenStatusList();
		} catch (Throwable ex) {
			log.error("Exception happened while reloading token status list", ex);
		} finally {
			this.isActive.set(false);
		}
	}

	private void reloadTokenStatusList() {
		StatusListResponse statusListResponse = tokenStsatusListService.loadTokenStatusList();
		if (statusListResponse == null) {
			return;
		}

		broadcast(STATUS_LIST_MESSAGE, statusListResponse.getEntity());
	}

	public SseBroadcaster getSseBroadcaster() {
		return sseBroadcaster;
	}

	public void setSseBroadcaster(SseBroadcaster sseBroadcaster) {
		this.sseBroadcaster = sseBroadcaster;
	}

	public Sse getSse() {
		return sse;
	}

	public void setSse(Sse sse) {
		this.sse = sse;
	}

	public void broadcast(String eventName, String data) {
		log.info("Broadcast message");

		if (sseBroadcaster == null) {
			return;
		}

		OutboundSseEvent event = sse.newEventBuilder().name(eventName).data(String.class, data).build();
		sseBroadcaster.broadcast(event);
	}
}
