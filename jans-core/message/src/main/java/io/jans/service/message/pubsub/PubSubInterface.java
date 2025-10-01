/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.service.message.pubsub;

/**
 * Listener for PubSub messages
 *
 * @author Yuriy Movchan Date: 30/11/2023
 */
public interface PubSubInterface {

	void onMessage(String channel, String message);

	void onSubscribe(String channel, int subscribedChannels);

	void onUnsubscribe(String channel, int subscribedChannels);

}
