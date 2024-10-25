/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.service;

/**
 * Speciy that service provides health check details
 *
 * @author Yuriy Movchan Date: 12/22/2023
 */
public interface HealthCheckPluginService {
	
	String provideHealthCheckData();

	String provideServiceName();
}
