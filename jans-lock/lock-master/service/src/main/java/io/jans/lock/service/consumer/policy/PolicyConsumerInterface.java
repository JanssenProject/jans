/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.lock.service.consumer.policy;

import java.util.List;

/**
 * Interface for each policy consumer
 * 
 * @author Yuriy Movchan Date: 12/20/2023
 */
public interface PolicyConsumerInterface {
	
	public boolean putPolicies(String sourceUri, List<String> policies);

	public boolean removePolicies(String sourceUri);

}
