/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.lock.model.config.cedarling;

/**
 * 
 * @author Yuriy Movchan Date: 10/08/2022
 */
public enum LogLevel {
	FATAL("FATAL"),
	ERROR("ERROR"),
	WARN("WARN"),
	INFO("INFO"),
	DEBUG("DEBUG"),
	TRACE("TRACE");
	
	private final String type;

	private LogLevel(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}
}