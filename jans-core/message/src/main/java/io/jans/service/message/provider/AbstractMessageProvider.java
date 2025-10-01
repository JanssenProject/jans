/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.service.message.provider;

import java.util.concurrent.ExecutorService;

/**
 * Interface for each message provider
 * 
 * @author Yuriy Movchan Date: 30/11/2023
 */
public abstract class AbstractMessageProvider<T> extends MessageProvider<T> {

	public abstract void create(ExecutorService executorService);

	public abstract void destroy();

}
