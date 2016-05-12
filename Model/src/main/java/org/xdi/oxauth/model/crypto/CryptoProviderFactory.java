/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.crypto;

import org.xdi.oxauth.model.common.WebKeyStorage;
import org.xdi.oxauth.model.configuration.Configuration;
import org.xdi.oxauth.model.jwk.JSONWebKeySet;

/**
 * @author Javier Rojas Blum
 * @version April 13, 2016
 */
public class CryptoProviderFactory {

    public static AbstractCryptoProvider getCryptoProvider(Configuration configuration, JSONWebKeySet webKeySet) {
        AbstractCryptoProvider cryptoProvider = null;
        WebKeyStorage webKeyStorage = WebKeyStorage.fromString(configuration.getWebKeysStorage());

        switch (webKeyStorage) {
            case LDAP:
                cryptoProvider = new OxAuthCryptoProvider(webKeySet);
                break;
            case PKCS11:
                cryptoProvider = new OxElevenCryptoProvider(
                        configuration.getOxElevenGenerateKeyEndpoint(),
                        configuration.getOxElevenSignEndpoint(),
                        configuration.getOxElevenVerifySignatureEndpoint(),
                        configuration.getOxElevenDeleteKeyEndpoint(),
                        configuration.getOxElevenJwksEndpoint());
                break;
        }

        return cryptoProvider;
    }
}
