/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.oxtrust.service;

import java.io.Serializable;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.util.StringHelper;
import org.gluu.util.security.PropertiesDecrypter;
import org.gluu.util.security.StringEncrypter;
import org.gluu.util.security.StringEncrypter.EncryptionException;

/**
 * Allows to decrypted properties with passwords
 *
 * @author Yuriy Movchan Date: 09/23/2014
 */
@ApplicationScoped
public class EncryptionService implements Serializable {

	private static final long serialVersionUID = -5813201875981117513L;

	@Inject
	private StringEncrypter stringEncrypter;

	public String decrypt(String encryptedString) throws EncryptionException {
		if (StringHelper.isEmpty(encryptedString)) {
			return null;
		}

		return stringEncrypter.decrypt(encryptedString);
	}

	public String encrypt(String unencryptedString) throws EncryptionException {
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