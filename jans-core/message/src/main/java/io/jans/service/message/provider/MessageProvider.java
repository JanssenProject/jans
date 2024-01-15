/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.service.message.provider;

import io.jans.service.message.model.config.MessageProviderType;

/**
 * Interface for each message provider
 * 
 * @author Yuriy Movchan Date: 30/11/2023
 */
public abstract class MessageProvider<T> implements MessageInterface {

	/*
	 * Delegate internal connection object
	 */
	public abstract T getDelegate();

	public abstract MessageProviderType getProviderType();

	public abstract void shutdown();

}
