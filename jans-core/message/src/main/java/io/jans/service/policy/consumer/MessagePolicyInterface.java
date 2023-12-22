/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.service.policy.consumer;

import org.json.JSONArray;

/**
 * Interface for each policy consumer
 * 
 * @author Yuriy Movchan Date: 12/20/2023
 */
public interface MessagePolicyInterface {
	
	public boolean putPolicies(String sourceUri, JSONArray policies);

	public boolean removePolicies(String sourceUri);

}
