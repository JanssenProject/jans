/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.service.message.provider;

import io.jans.service.message.pubsub.PubSubInterface;

/**
 * Interface for each message provider
 * 
 * @author Yuriy Movchan Date: 30/11/2023
 */
public interface MessageInterface {

	void subscribe(PubSubInterface pubSubAdapter, String... channels);

	void unsubscribe(PubSubInterface pubSubAdapter);

	boolean publish(String channel, String message);

}
