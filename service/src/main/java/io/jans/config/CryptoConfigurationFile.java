/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.config;

import io.jans.util.properties.FileConfiguration;

/**
 * Mapping from crypto.properies to properties
 *
 * @author Oleksiy Tataryn
 * @version 09/24/2014
 */
public final class CryptoConfigurationFile {

    private FileConfiguration cryptoConfiguration;

    public CryptoConfigurationFile(FileConfiguration cryptoConfiguration) {
        this.cryptoConfiguration = cryptoConfiguration;
    }

    public String getEncodeSalt() {
        return cryptoConfiguration.getString("encodeSalt");
    }
}
