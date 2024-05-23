/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.lock.service.provider.metric;

/**
 * Base message consumer
 * 
 * @author Yuriy Movchan Date: 12/20/2023
 */
public abstract class MetricProvider implements MetricProviderInterface {

	public abstract void destroy();

}
