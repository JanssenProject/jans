package org.gluu.persist.exception.extension;

import java.util.Map;

import org.gluu.model.SimpleCustomProperty;

/**
 * Base interface for persistence script
 *
 * @author Yuriy Movchan Date: 06/04/2020
 */
public interface PersistenceExtension {
	
	void onBeforeCreate(Object context, Map<String, SimpleCustomProperty> configurationAttributes);
	void onAfterCreate(Object context, Map<String, SimpleCustomProperty> configurationAttributes);
	void onBeforeDestroy(Object context, Map<String, SimpleCustomProperty> configurationAttributes);
	void onAfterDestroy(Object context, Map<String, SimpleCustomProperty> configurationAttributes);

	String createHashedPassword(String credential);
	boolean compareHashedPasswords(String credential, String storedCredential);

}
