/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.util.security;

import java.util.Properties;

import io.jans.util.StringHelper;
import io.jans.util.exception.EncryptionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decrypt encripted properties
 *
 * @author Yuriy Movchan Date: 04/24/2013
 */
public final class PropertiesDecrypter {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesDecrypter.class);

    public static final String BIND_PASSWORD = "bindPassword";
    public static final String TRUST_STORE_PIN = "ssl.trustStorePin";

    private PropertiesDecrypter() { }

    public static Properties decryptProperties(Properties properties) {
        try {
            return decryptProperties(StringEncrypter.defaultInstance(), properties);
        } catch (EncryptionException ex) {
            LOG.error(String.format("Failed to decript '%s' property", PropertiesDecrypter.BIND_PASSWORD), ex);
        }

        return properties;
    }

    public static Properties decryptProperties(StringEncrypter stringEncrypter, Properties properties) {
        if (properties == null) {
            return properties;
        }

        Properties clondedProperties = (Properties) properties.clone();

        decryptProperty(stringEncrypter, clondedProperties, PropertiesDecrypter.BIND_PASSWORD, true);
        decryptProperty(stringEncrypter, clondedProperties, PropertiesDecrypter.TRUST_STORE_PIN, true);

        return clondedProperties;
    }

    public static Properties decryptAllProperties(StringEncrypter stringEncrypter, Properties properties) {
        if (properties == null) {
            return properties;
        }

        Properties clondedProperties = (Properties) properties.clone();

        for (Object key : clondedProperties.keySet()) {
            String propertyName = (String) key;
            decryptProperty(stringEncrypter, clondedProperties, propertyName, true);
        }

        return clondedProperties;
    }

    private static void decryptProperty(StringEncrypter stringEncrypter, Properties properties, String propertyName, boolean silent) {
        String propertyValue = properties.getProperty(propertyName);
        if (StringHelper.isEmpty(propertyValue)) {
            return;
        }
        try {
            String decryptedProperty = stringEncrypter.decrypt(propertyValue, silent);
            if (StringHelper.isEmpty(decryptedProperty)) {
                decryptedProperty = propertyValue;
            }
            properties.put(propertyName, decryptedProperty);
        } catch (Exception ex) {
            LOG.error(String.format("Failed to decript '%s' property", propertyName), ex);
        }
    }

    public static String decryptProperty(String encryptedValue, boolean returnSource, String encryptionKey,
            String encryptionSalt, String encryptionAlg) {
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
            StringEncrypter stringEncrypter = StringEncrypter.instance(encryptionKey, encryptionSalt, encryptionAlg);
            resultValue = stringEncrypter.decrypt(encryptedValue);
        } catch (Exception ex) {
            if (!returnSource) {
                LOG.error(String.format("Failed to decrypt value: '%s'", encryptedValue, ex));
            }
        }

        return resultValue;
    }

}
