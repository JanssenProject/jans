package io.jans.lock.service.ws.rs.sse;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

@Path("/lock_sse")
public class TokenListRestWebService {

	@Inject
	private TokenSseBroadcater tokenSseBroadcater;

	@GET
	@Produces(MediaType.SERVER_SENT_EVENTS)
	public void subscribe(@Context SseEventSink sseEventSink) {
		tokenSseBroadcater.register(sseEventSink);
	}

}
