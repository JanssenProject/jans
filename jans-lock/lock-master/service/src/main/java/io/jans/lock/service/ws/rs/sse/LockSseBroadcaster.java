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

import org.slf4j.Logger;

import io.jans.lock.service.policy.event.PolicyDownloadEvent;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;

/**
 * @author Yuriy Movchan Date: 05/24/2024
 */
@ApplicationScoped
public class LockSseBroadcaster {

	@Inject
	private Logger log;

    private SseBroadcaster sseBroadcaster;
	private Sse sse;

	@PostConstruct
	public void init() {
		log.info("Initializing Token SSE broadcaster ...");
	}

	@Asynchronous
	public void reloadPoliciesTimerEvent(@Observes @Scheduled PolicyDownloadEvent policyDownloadEvent) {
		// test messages
		broadcast("token_list", "{\"sample_data\" : \"data\"}");
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
