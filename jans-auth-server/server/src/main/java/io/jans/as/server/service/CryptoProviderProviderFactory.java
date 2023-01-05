/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import io.jans.as.model.common.WebKeyStorage;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.CryptoProviderFactory;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Crypto Provider
 *
 * @author Yuriy Movchan
 * @version 11/02/2018
 */
@ApplicationScoped
@Named
public class CryptoProviderProviderFactory {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Produces
    @ApplicationScoped
    public AbstractCryptoProvider getCryptoProvider() throws Exception {
        log.debug("Started to create crypto provider");

        WebKeyStorage webKeyStorage = appConfiguration.getWebKeysStorage();
        if (webKeyStorage == null) {
            throw new RuntimeException("Failed to initialize cryptoProvider, cryptoProviderType is not specified!");
        }

        AbstractCryptoProvider cryptoProvider = CryptoProviderFactory.getCryptoProvider(appConfiguration);

        if (cryptoProvider == null) {
            throw new RuntimeException("Failed to initialize cryptoProvider, cryptoProviderType is unsupported: " + webKeyStorage);
        }

        return cryptoProvider;
    }

}
