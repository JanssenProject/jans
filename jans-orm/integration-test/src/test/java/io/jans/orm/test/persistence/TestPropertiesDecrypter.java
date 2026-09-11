/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test.persistence;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decrypts encrypted values in persistence properties files with encodeSalt
 * from conf/salt. It's test copy of jans-core PropertiesDecrypter/StringEncrypter
 * functionality because jans-orm can't depend on jans-core modules.
 *
 * @author Yuriy Movchan Date: 09/08/2026
 */
public final class TestPropertiesDecrypter {

	private static final Logger LOG = LoggerFactory.getLogger(TestPropertiesDecrypter.class);

	private static final String ENCRYPTION_SCHEME = "DESede";

	private TestPropertiesDecrypter() {
	}

	/*
	 * Try to decrypt every property value. Original value is preserved when it's not encrypted.
	 * It's the same behavior as in jans-core PropertiesDecrypter.decryptAllProperties
	 */
	public static Properties decryptAllProperties(Properties connectionProperties, String encodeSalt) {
		Properties clonedProperties = (Properties) connectionProperties.clone();

		for (Object key : connectionProperties.keySet()) {
			String propertyName = (String) key;
			String propertyValue = connectionProperties.getProperty(propertyName);

			String decryptedValue = decrypt(propertyValue, encodeSalt);
			if (decryptedValue != null) {
				clonedProperties.setProperty(propertyName, decryptedValue);
			}
		}

		return clonedProperties;
	}

	private static String decrypt(String encryptedValue, String encodeSalt) {
		if ((encryptedValue == null) || (encodeSalt == null)) {
			return null;
		}

		try {
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ENCRYPTION_SCHEME);
			SecretKey key = keyFactory.generateSecret(new DESedeKeySpec(encodeSalt.getBytes(StandardCharsets.UTF_8)));

			Cipher cipher = Cipher.getInstance(ENCRYPTION_SCHEME);
			cipher.init(Cipher.DECRYPT_MODE, key);

			byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedValue.getBytes(StandardCharsets.UTF_8)));

			return new String(decryptedBytes, StandardCharsets.UTF_8);
		} catch (Exception ex) {
			LOG.trace("Value is not encrypted. Keeping original value");
			return null;
		}
	}

}
