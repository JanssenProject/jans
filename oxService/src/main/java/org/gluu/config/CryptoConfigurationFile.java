/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.config;

import org.gluu.util.properties.FileConfiguration;

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
