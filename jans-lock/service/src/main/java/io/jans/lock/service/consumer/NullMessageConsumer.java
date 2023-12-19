package io.jans.lock.service.consumer;

import org.slf4j.Logger;

import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.message.consumer.MessageConsumer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class NullMessageConsumer extends MessageConsumer {
	
	public static String CONSUMER_TYPE = "NULL";

	@Inject
	private Logger log;

	@Override
	@Asynchronous
	public void onMessage(String channel, String message) {
		log.info("onMessage {} : {} : {}", channel, message);
	}

	@Override
	public void onSubscribe(String channel, int subscribedChannels) {
		log.debug("onSubscribe {} : {}", channel, subscribedChannels);
	}

	@Override
	public void onUnsubscribe(String channel, int subscribedChannels) {
		log.debug("onUnsubscribe {} : {}", channel, subscribedChannels);
	}

	@Override
	public String getMessageConsumerType() {
		return CONSUMER_TYPE;
	}

}
