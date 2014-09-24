package org.xdi.config;

import org.xdi.util.properties.FileConfiguration;

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
	
	public String getEncodeSalt(){
		return cryptoConfiguration.getString("encodeSalt");
	}
}
