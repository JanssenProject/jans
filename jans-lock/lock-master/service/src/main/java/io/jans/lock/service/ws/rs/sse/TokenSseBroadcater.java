/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2023, Janssen Project
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
import jakarta.ws.rs.sse.SseEventSink;

/**
 * @author Yuriy Movchan Date: 05/24/2024
 */
@ApplicationScoped
public class TokenSseBroadcater {

	@Inject
	private Logger log;

	@Inject
	private Sse sse;

	@Inject
	private SseBroadcaster sseBroadcaster;


	@PostConstruct
	public void init() {
		log.info("Initializing Token SSE broadcaster ...");
		this.sseBroadcaster = sse.newBroadcaster();
	}

	@Asynchronous
	public void reloadPoliciesTimerEvent(@Observes @Scheduled PolicyDownloadEvent policyDownloadEvent) {
		// test messages
		broadcast("token_list", "{\"sample_data\" : \"data\"}");
	}

	public void broadcast(String eventName, String data) {
		OutboundSseEvent event = sse.newEventBuilder().name(eventName).data(String.class, data).build();
		sseBroadcaster.broadcast(event);
	}

	public void register(SseEventSink sseEventSink) {
		sseBroadcaster.register(sseEventSink);
	}

}
