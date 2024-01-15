/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.service.message.consumer;

/**
 * Base message consumer
 * 
 * @author Yuriy Movchan Date: 12/18/2023
 */
public abstract class MessageConsumer implements MessageConsumerInterface {

	public abstract String getMessageConsumerType();

	public abstract void destroy();

}
