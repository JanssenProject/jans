/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.service;

import org.slf4j.Logger;

import io.jans.service.message.provider.MessageInterface;
import io.jans.service.message.provider.MessageProvider;
import io.jans.service.message.pubsub.PubSubInterface;
import jakarta.inject.Inject;

/**
 * Provides operations with messages
 *
 * @author Yuriy Movchan Date: 30/11/2023
 */
public abstract class BaseMessageService implements MessageInterface {

	public static int DEFAULT_EXPIRATION = 60;

	@Inject
	private Logger log;

	public void subscribe(PubSubInterface pubSubAdapter, String... channels) {
		MessageProvider<?> messageProvider = getMessageProvider();
		if (messageProvider == null) {
			log.error("Message provider is invalid!");
			return;
		}

		log.trace("Subscribe '{}' for channels '{}'", pubSubAdapter, channels);
		messageProvider.subscribe(pubSubAdapter, channels);
	}

	public void unsubscribe(PubSubInterface pubSubAdapter) {
		MessageProvider<?> messageProvider = getMessageProvider();
		if (messageProvider == null) {
			log.error("Message provider is invalid!");
			return;
		}

		log.trace("Unsubscribe '{}'", pubSubAdapter);
		messageProvider.unsubscribe(pubSubAdapter);
	}

	public boolean publish(String channel, String message) {
		MessageProvider<?> messageProvider = getMessageProvider();
		if (messageProvider == null) {
			log.error("Message provider is invalid!");
			return false;
		}

		if (log.isTraceEnabled()) {
			log.trace("Publish '{}' to channel '{}'", message, channel);
		}

		boolean result = messageProvider.publish(channel, message);

		return result;
	}

	protected abstract MessageProvider getMessageProvider();

}
