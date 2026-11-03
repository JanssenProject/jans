/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.trace.crypto;

/**
 * Which {@link TraceCryptoProvider} implementation handles TRACE signature verification,
 * canonicalization and receipt hashing.
 *
 * @author Yuriy Movchan
 */
public enum TraceCryptoProviderType {

	/** In-process Java implementation (JCA/BouncyCastle). */
	JAVA,

	/** Native implementation behind the jans-trace-core bindings. */
	NATIVE;

	/**
	 * @return the type named by {@code value} (trimmed, case-insensitive), or {@code null} when
	 *         {@code value} is not the name of a type
	 */
	public static TraceCryptoProviderType fromString(String value) {
		if (value == null) {
			return null;
		}
		String name = value.trim();
		for (TraceCryptoProviderType type : values()) {
			if (type.name().equalsIgnoreCase(name)) {
				return type;
			}
		}
		return null;
	}

}
