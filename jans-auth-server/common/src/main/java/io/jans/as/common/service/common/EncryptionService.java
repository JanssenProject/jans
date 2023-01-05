/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.service.common;

import io.jans.util.StringHelper;
import io.jans.util.security.PropertiesDecrypter;
import io.jans.util.security.StringEncrypter;
import io.jans.util.security.StringEncrypter.EncryptionException;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Properties;

/**
 * Allows to decrypt passwords
 *
 * @author Yuriy Movchan Date: 09/23/2014
 */
@ApplicationScoped
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

    public Properties decryptAllProperties(Properties connectionProperties) {
        return PropertiesDecrypter.decryptAllProperties(stringEncrypter, connectionProperties);
    }

}