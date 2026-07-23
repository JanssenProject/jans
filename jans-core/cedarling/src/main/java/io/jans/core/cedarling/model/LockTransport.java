/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.core.cedarling.model;

/**
 * 
 * @author Yuriy Movchan Date: 10/08/2022
 */
public enum LockTransport {
	REST("rest"),
	GRPC("grpc");
	
	private final String type;

	private LockTransport(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}
}