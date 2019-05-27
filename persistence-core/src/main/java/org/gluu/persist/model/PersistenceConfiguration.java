package org.gluu.persist.model;

import org.gluu.persist.PersistenceEntryManagerFactory;
import org.gluu.util.properties.FileConfiguration;

/**
 * Persistence configuration
 *
 * @author Yuriy Movchan Date: 05/10/2019
 */
public class PersistenceConfiguration {

	private String fileName;
	private FileConfiguration configuration;
	private Class<? extends PersistenceEntryManagerFactory> entryManagerFactoryType;
	private long lastModifiedTime;

	public PersistenceConfiguration() {}

	public PersistenceConfiguration(String fileName, FileConfiguration configuration,
			Class<? extends PersistenceEntryManagerFactory> entryManagerFactoryType, long lastModifiedTime) {
		this.fileName = fileName;
		this.configuration = configuration;
		this.entryManagerFactoryType = entryManagerFactoryType;
		this.lastModifiedTime = lastModifiedTime;
	}

	public String getFileName() {
		return fileName;
	}

	public FileConfiguration getConfiguration() {
		return configuration;
	}

	public Class<? extends PersistenceEntryManagerFactory> getEntryManagerFactoryType() {
		return entryManagerFactoryType;
	}

	public long getLastModifiedTime() {
		return lastModifiedTime;
	}

}
