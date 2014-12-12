/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.util.security;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.util.StringHelper;
import org.xdi.util.security.StringEncrypter.EncryptionException;

/**
 * Decrypt encripted properties
 * 
 * @author Yuriy Movchan Date: 04/24/2013
 */
public class PropertiesDecrypter {

	private static final Logger log = LoggerFactory.getLogger(PropertiesDecrypter.class);

	public static final String bindPassword = "bindPassword";

	public static Properties decryptProperties(Properties properties, String encryptionKey) {
		try {
			return decryptProperties(StringEncrypter.defaultInstance(), properties, encryptionKey);
		} catch (EncryptionException ex) {
			log.error(String.format("Failed to decript '%s' property", PropertiesDecrypter.bindPassword), ex);
		}
		
		return properties;
	}

	public static Properties decryptProperties(StringEncrypter stringEncrypter, Properties properties) {
		return decryptProperties(stringEncrypter, properties, null);
	}

	public static Properties decryptProperties(StringEncrypter stringEncrypter, Properties properties, String encryptionKey) {
		if (properties == null) {
			return properties;
		}

		Properties clondedProperties = (Properties) properties.clone();

		String encryptedPassword = clondedProperties.getProperty(PropertiesDecrypter.bindPassword);
		if (StringHelper.isEmpty(encryptedPassword)) {
			return properties;
		}

		try {
			String decryptedProperty;
			if (StringHelper.isEmpty(encryptionKey)) {
				decryptedProperty = stringEncrypter.decrypt(properties.getProperty(PropertiesDecrypter.bindPassword));
			} else {
				decryptedProperty = stringEncrypter.decrypt(properties.getProperty(PropertiesDecrypter.bindPassword), encryptionKey);
			}
			
			clondedProperties.put(PropertiesDecrypter.bindPassword, decryptedProperty);
		} catch (EncryptionException ex) {
			log.error(String.format("Failed to decript '%s' property", PropertiesDecrypter.bindPassword), ex);
		}

		return clondedProperties;
	}

	public static String decryptProperty(String encryptedValue, boolean returnSource, String encryptionKey) {
		if (encryptedValue == null) {
			return encryptedValue;
		}

		String resultValue;
		if (returnSource) {
			resultValue = encryptedValue;
		} else {
			resultValue = null;
		}

		try {
			resultValue = StringEncrypter.defaultInstance().decrypt(encryptedValue, encryptionKey);
		} catch (Exception ex) {
			if (!returnSource) {
				log.error(String.format("Failed to decrypt value: '%s'", encryptedValue, ex));
			}
		}

		return resultValue;
	}

}
