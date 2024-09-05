/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.service.message.consumer;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.service.message.pubsub.PubSubInterface;

/**
 * Interface for each message consumer
 * 
 * @author Yuriy Movchan Date: 12/18/2023
 */
public interface MessageConsumerInterface extends PubSubInterface {

	public boolean putData(String message, JsonNode pdpMessageNode);

}
