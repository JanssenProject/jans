/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.extension;

/**
 * Base interface for persistence script
 *
 * @author Yuriy Movchan Date: 06/04/2020
 */
public interface PersistenceExtension {

	String createHashedPassword(String credential);
	boolean compareHashedPasswords(String credential, String storedCredential);

}
