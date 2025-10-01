/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.service.common;

import java.util.Properties;

import io.jans.util.security.StringEncrypter.EncryptionException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Proxy for compatibility with old versions
 *
 * @author Yuriy Movchan Date: 01/12/2024
 */
@ApplicationScoped
@Deprecated
public class EncryptionService {

	@Inject
	private io.jans.service.EncryptionService encryptionService;

	public String decrypt(String encryptedString) throws EncryptionException {
		return encryptionService.decrypt(encryptedString);
	}

	public String decrypt(String encryptedValue, boolean returnSource) {
		return encryptionService.decrypt(encryptedValue, returnSource);
    }

	public String encrypt(String unencryptedString) throws EncryptionException {
		return encryptionService.decrypt(unencryptedString);
	}

	public Properties decryptProperties(Properties connectionProperties) {
		return encryptionService.decryptProperties(connectionProperties);
	}

	public Properties decryptAllProperties(Properties connectionProperties) {
		return encryptionService.decryptAllProperties(connectionProperties);
	}

}