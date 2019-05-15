package org.gluu.persist.model;

import javax.enterprise.inject.Vetoed;

import org.gluu.persist.PersistenceEntryManagerFactory;
import org.gluu.util.properties.FileConfiguration;

/**
 * Persistence configuration
 *
 * @author Yuriy Movchan Date: 05/10/2019
 */
@Vetoed
public class PersistenceConfiguration {

	private final String fileName;
	private final FileConfiguration configuration;
	private final Class<? extends PersistenceEntryManagerFactory> entryManagerFactoryType;
	private final long lastModifiedTime;

	public PersistenceConfiguration(String fileName, FileConfiguration configuration,
			Class<? extends PersistenceEntryManagerFactory> entryManagerFactoryType, long lastModifiedTime) {
		this.fileName = fileName;
		this.configuration = configuration;
		this.entryManagerFactoryType = entryManagerFactoryType;
		this.lastModifiedTime = lastModifiedTime;
	}

	public final String getFileName() {
		return fileName;
	}

	public final FileConfiguration getConfiguration() {
		return configuration;
	}

	public final Class<? extends PersistenceEntryManagerFactory> getEntryManagerFactoryType() {
		return entryManagerFactoryType;
	}

	public final long getLastModifiedTime() {
		return lastModifiedTime;
	}

}
