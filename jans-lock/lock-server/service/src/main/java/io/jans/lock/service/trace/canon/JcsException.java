/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.canon;

/**
 * Thrown when a JSON value cannot be canonicalized according to RFC 8785 (JSON Canonicalization
 * Scheme): non-finite numbers, lone UTF-16 surrogates in strings or property names, and Jackson
 * node types that have no JSON representation (POJO, binary, missing).
 * 
 * @author Yuriy Movchan
 */
public class JcsException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public JcsException(String message) {
		super(message);
	}

	public JcsException(String message, Throwable cause) {
		super(message, cause);
	}

}
