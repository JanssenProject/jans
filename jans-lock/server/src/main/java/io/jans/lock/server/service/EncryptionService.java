/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.lock.server.service;

import java.io.Serializable;
import java.util.Properties;

import io.jans.orm.util.StringHelper;
import io.jans.util.security.PropertiesDecrypter;
import io.jans.util.security.StringEncrypter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Allows to decrypted properties with passwords
 *
 * @author Yuriy Movchan Date: 12/12/2023
 */
@ApplicationScoped
public class EncryptionService implements Serializable {

	private static final long serialVersionUID = -5813201875981117513L;

	@Inject
	private StringEncrypter stringEncrypter;

	public String decrypt(String encryptedString) throws StringEncrypter.EncryptionException {
		if (StringHelper.isEmpty(encryptedString)) {
			return null;
		}

		return stringEncrypter.decrypt(encryptedString);
	}

	public String encrypt(String unencryptedString) throws StringEncrypter.EncryptionException {
		if (StringHelper.isEmpty(unencryptedString)) {
			return null;
		}

		return stringEncrypter.encrypt(unencryptedString);
	}

	public Properties decryptProperties(Properties connectionProperties) {
		return PropertiesDecrypter.decryptProperties(stringEncrypter, connectionProperties);
	}

	public Properties decryptAllProperties(Properties connectionProperties) {
		return PropertiesDecrypter.decryptAllProperties(stringEncrypter, connectionProperties);
	}

}