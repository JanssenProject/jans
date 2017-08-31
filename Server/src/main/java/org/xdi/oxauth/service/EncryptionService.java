/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import java.util.Properties;

import javax.ejb.Stateless;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xdi.util.StringHelper;
import org.xdi.util.security.PropertiesDecrypter;
import org.xdi.util.security.StringEncrypter;
import org.xdi.util.security.StringEncrypter.EncryptionException;

/**
 * Allows to decrypt passwords
 *
 * @author Yuriy Movchan Date: 09/23/2014
 */
@ApplicationScoped
@Named
public class EncryptionService {

    @Inject
    private Logger log;

    @Inject
    private StringEncrypter stringEncrypter;

    public String decrypt(String encryptedString) throws EncryptionException {
		if (StringHelper.isEmpty(encryptedString)) {
			return null;
		}

		return stringEncrypter.decrypt(encryptedString);
    }

	public String decrypt(String encryptedValue, boolean returnSource) {
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
			resultValue = stringEncrypter.decrypt(encryptedValue);
		} catch (Exception ex) {
			if (!returnSource) {
				log.error(String.format("Failed to decrypt value: '%s'", encryptedValue, ex));
			}
		}

		return resultValue;
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

}