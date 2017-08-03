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
	public static final String trustStorePin = "ssl.trustStorePin";

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
		decriptProperty(stringEncrypter, clondedProperties, encryptionKey, PropertiesDecrypter.bindPassword);
		decriptProperty(stringEncrypter, clondedProperties, encryptionKey, PropertiesDecrypter.trustStorePin);
		
		return clondedProperties;
	}

	private static void decriptProperty(StringEncrypter stringEncrypter, Properties properties, String encryptionKey, String propertyName) {
		String encryptedPassword = properties.getProperty(propertyName);
		if (StringHelper.isEmpty(encryptedPassword)) {
			return;
		}
		
		try {
			String decryptedProperty;
			if (StringHelper.isEmpty(encryptionKey)) {
				decryptedProperty = stringEncrypter.decrypt(properties.getProperty(propertyName));
			} else {
				decryptedProperty = stringEncrypter.decrypt(properties.getProperty(propertyName), encryptionKey);
			}
			
			properties.put(propertyName, decryptedProperty);
		} catch (EncryptionException ex) {
			log.error(String.format("Failed to decript '%s' property", propertyName), ex);
		}
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
