/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.util.security;

import java.util.Properties;

import org.gluu.util.StringHelper;
import org.gluu.util.security.StringEncrypter.EncryptionException;
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

    public static Properties decryptProperties(Properties properties, String encryptionKey) {
        try {
            return decryptProperties(StringEncrypter.defaultInstance(), properties, encryptionKey);
        } catch (EncryptionException ex) {
            LOG.error(String.format("Failed to decript '%s' property", PropertiesDecrypter.BIND_PASSWORD), ex);
        }

        return properties;
    }

    public static Properties decryptProperties(StringEncrypter stringEncrypter, Properties properties) {
        return decryptProperties(stringEncrypter, properties, null);
    }

    public static Properties decryptProperties(StringEncrypter stringEncrypter, Properties properties,
            String encryptionKey) {
        if (properties == null) {
            return properties;
        }

        Properties clondedProperties = (Properties) properties.clone();
        decriptProperty(stringEncrypter, clondedProperties, encryptionKey, PropertiesDecrypter.BIND_PASSWORD, true);
        decriptProperty(stringEncrypter, clondedProperties, encryptionKey, PropertiesDecrypter.TRUST_STORE_PIN, true);

        return clondedProperties;
    }

    public static Properties decryptAllProperties(StringEncrypter stringEncrypter, Properties properties) {
        return decryptAllProperties(stringEncrypter, properties, null);
    }

    public static Properties decryptAllProperties(StringEncrypter stringEncrypter, Properties properties,
            String encryptionKey) {
        if (properties == null) {
            return properties;
        }

        Properties clondedProperties = (Properties) properties.clone();
        for (Object key : clondedProperties.keySet()) {
            String propertyName = (String) key;
            decriptProperty(stringEncrypter, clondedProperties, encryptionKey, propertyName, true);
        }

        return clondedProperties;
    }

    private static void decriptProperty(StringEncrypter stringEncrypter, Properties properties, String encryptionKey,
            String propertyName, boolean silent) {
        String propertyValue = properties.getProperty(propertyName);
        if (StringHelper.isEmpty(propertyValue)) {
            return;
        }

        try {
            String decryptedProperty;
            if (StringHelper.isEmpty(encryptionKey)) {
                decryptedProperty = stringEncrypter.decrypt(propertyValue, silent);
            } else {
                decryptedProperty = stringEncrypter.decrypt(propertyValue, encryptionKey, silent);
            }

            if (StringHelper.isEmpty(decryptedProperty)) {
                decryptedProperty = propertyValue;
            }

            properties.put(propertyName, decryptedProperty);
        } catch (EncryptionException ex) {
            LOG.error(String.format("Failed to decript '%s' property", propertyName), ex);
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
                LOG.error(String.format("Failed to decrypt value: '%s'", encryptedValue, ex));
            }
        }

        return resultValue;
    }

}
