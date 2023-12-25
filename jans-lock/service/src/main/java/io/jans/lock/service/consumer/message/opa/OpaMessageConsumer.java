package io.jans.lock.service.consumer.message.opa;

import org.slf4j.Logger;

import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.message.consumer.MessageConsumer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * OPA message consumer
 *
 * @author Yuriy Movchan Date: 12/25/2023
 */
@ApplicationScoped
public class OpaMessageConsumer extends MessageConsumer {
	
	public static String MESSAGE_CONSUMER_TYPE = "OPA";

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
		return MESSAGE_CONSUMER_TYPE;
	}

}
