/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.service.message.pubsub;

import redis.clients.jedis.JedisPubSub;

/**
 * Listener converter for PubSub messages from Redis listener
 *
 * @author Yuriy Movchan Date: 30/11/2023
 */
public class PubSubRedisAdapter extends JedisPubSub implements PubSubInterface {

	private PubSubInterface pubSub;
	private long messagesCount;

	public PubSubRedisAdapter(PubSubInterface pubSub) {
		this.pubSub = pubSub;
		this.messagesCount = 0;
	}

	public PubSubInterface getPubSub() {
		return pubSub;
	}

	@Override
	public void onMessage(String channel, String message) {
		this.pubSub.onMessage(channel, message);
		messagesCount++;
	}

	public long getMessagesCount() {
		return messagesCount;
	}

	@Override
	public void unsubscribe() {
		super.unsubscribe();
	}

	@Override
	public boolean isSubscribed() {
		return super.isSubscribed();
	}

}
