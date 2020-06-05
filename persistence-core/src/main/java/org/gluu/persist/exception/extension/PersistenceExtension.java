package org.gluu.persist.exception.extension;

/**
 * Base interface for persistence script
 *
 * @author Yuriy Movchan Date: 06/04/2020
 */
public interface PersistenceExtension {

	String createHashedPassword(String credential);
	boolean compareHashedPasswords(String credential, String storedCredential);

}
