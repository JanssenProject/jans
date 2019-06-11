/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.crypto;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.common.WebKeyStorage;
import org.gluu.oxauth.model.configuration.AppConfiguration;

/**
 * @author Javier Rojas Blum
 * @version April 25, 2017
 */
public class CryptoProviderFactory {

    private static OxAuthCryptoProvider keyStoreProvider = null;

    public static AbstractCryptoProvider getCryptoProvider(AppConfiguration configuration) throws Exception {
        AbstractCryptoProvider cryptoProvider = null;
        WebKeyStorage webKeyStorage = configuration.getWebKeysStorage();
        if (webKeyStorage == null) {
            return null;
        }

        switch (webKeyStorage) {
            case KEYSTORE:
                cryptoProvider = getKeyStoreProvider(configuration);

                break;
            case PKCS11:
                cryptoProvider = new OxElevenCryptoProvider(
                        configuration.getOxElevenGenerateKeyEndpoint(),
                        configuration.getOxElevenSignEndpoint(),
                        configuration.getOxElevenVerifySignatureEndpoint(),
                        configuration.getOxElevenDeleteKeyEndpoint(),
                        configuration.getOxElevenTestModeToken());
                break;
        }

        return cryptoProvider;
    }

    private synchronized static AbstractCryptoProvider getKeyStoreProvider(AppConfiguration configuration) throws Exception {
        if (keyStoreProvider != null &&
                StringUtils.isNotBlank(keyStoreProvider.getKeyStoreFile()) &&
                StringUtils.isNotBlank(keyStoreProvider.getKeyStoreSecret()) &&
                StringUtils.isNotBlank(keyStoreProvider.getDnName()) &&
                keyStoreProvider.getKeyStoreFile().equals(configuration.getKeyStoreFile()) &&
                keyStoreProvider.getKeyStoreSecret().equals(configuration.getKeyStoreSecret()) &&
                keyStoreProvider.getDnName().equals(configuration.getDnName())) {
            return keyStoreProvider;
        }

        return keyStoreProvider = new OxAuthCryptoProvider(configuration.getKeyStoreFile(), configuration.getKeyStoreSecret(), configuration.getDnName());
    }
}
