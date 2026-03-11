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
public enum LogType {
	OFF("off"),
	MEMORY("memory"),
	STD_OUT("std_out");

	private final String type;

	private LogType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}
}