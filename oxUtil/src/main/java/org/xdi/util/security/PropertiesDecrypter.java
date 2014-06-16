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

	public static Properties decryptProperties(Properties properties) {
		if (properties == null) {
			return properties;
		}

		Properties clondedProperties = (Properties) properties.clone();

		String encryptedPassword = clondedProperties.getProperty(PropertiesDecrypter.bindPassword);
		if (StringHelper.isEmpty(encryptedPassword)) {
			return properties;
		}

		try {
			clondedProperties.put(PropertiesDecrypter.bindPassword, StringEncrypter.defaultInstance().decrypt(properties.getProperty(PropertiesDecrypter.bindPassword)));
		} catch (EncryptionException ex) {
			log.error(String.format("Failed to decript '%s' property", PropertiesDecrypter.bindPassword), ex);
		}

		return clondedProperties;
	}

	public static String decryptProperty(String encryptedValue, boolean returnSource) {
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
			resultValue = StringEncrypter.defaultInstance().decrypt(encryptedValue);
		} catch (Exception ex) {
			if (!returnSource) {
				log.error(String.format("Failed to decrypt value: '%s'", encryptedValue, ex));
			}
		}

		return resultValue;
	}

}
