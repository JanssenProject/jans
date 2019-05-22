package org.gluu.persist.service;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.gluu.persist.PersistenceEntryManagerFactory;
import org.gluu.persist.couchbase.impl.CouchbaseEntryManagerFactory;
import org.gluu.persist.exception.PropertyNotFoundException;
import org.gluu.persist.exception.operation.ConfigurationException;
import org.gluu.persist.hybrid.impl.HybridEntryManagerFactory;
import org.gluu.persist.ldap.impl.LdapEntryManagerFactory;
import org.gluu.persist.model.PersistenceConfiguration;
import org.gluu.persist.reflect.util.ReflectHelper;
import org.gluu.util.StringHelper;
import org.gluu.util.properties.FileConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory which creates Persistence Entry Manager
 *
 * @author Yuriy Movchan Date: 05/10/2019
 */
@ApplicationScoped
public class PersistanceFactoryService {

	static {
		if (System.getProperty("gluu.base") != null) {
			BASE_DIR = System.getProperty("gluu.base");
		} else if ((System.getProperty("catalina.base") != null) && (System.getProperty("catalina.base.ignore") == null)) {
			BASE_DIR = System.getProperty("catalina.base");
		} else if (System.getProperty("catalina.home") != null) {
			BASE_DIR = System.getProperty("catalina.home");
		} else if (System.getProperty("jboss.home.dir") != null) {
			BASE_DIR = System.getProperty("jboss.home.dir");
		} else {
			BASE_DIR = null;
		}
	}

	public static final String BASE_DIR;
	public static final String DIR = BASE_DIR + File.separator + "conf" + File.separator;

	private static final String GLUU_FILE_PATH = DIR + "gluu.properties";

	@Inject
	private Logger log;

	@Inject
	private Instance<PersistenceEntryManagerFactory> persistenceEntryManagerFactoryInstance;

	public PersistenceConfiguration loadPersistenceConfiguration() {
		return loadPersistenceConfiguration(null);
	}

	public PersistenceConfiguration loadPersistenceConfiguration(String applicationPropertiesFile) {
		PersistenceConfiguration currentPersistenceConfiguration = null;

		String gluuFileName = determineGluuConfigurationFileName();
		if (gluuFileName != null) {
			currentPersistenceConfiguration = createPersistenceConfiguration(gluuFileName);
		}

		// Fall back to old LDAP persistence layer
		if (currentPersistenceConfiguration == null) {
			log.warn("Failed to load persistence configuration. Attempting to use LDAP layer");
			PersistenceEntryManagerFactory defaultEntryManagerFactory = getPersistenceEntryManagerFactoryImpl(LdapEntryManagerFactory.class);
			currentPersistenceConfiguration = createPersistenceConfiguration(defaultEntryManagerFactory.getPersistenceType(), LdapEntryManagerFactory.class,
					defaultEntryManagerFactory.getConfigurationFileNames());
		}

		return currentPersistenceConfiguration;
	}

	private PersistenceConfiguration createPersistenceConfiguration(String gluuFileName) {
		try {
			// Determine persistence type
			FileConfiguration gluuFileConf = new FileConfiguration(gluuFileName);
			if (!gluuFileConf.isLoaded()) {
				log.error("Unable to load configuration file '{}'", gluuFileName);
				return null;
			}

			String persistenceType = gluuFileConf.getString("persistence.type");
			PersistenceEntryManagerFactory persistenceEntryManagerFactory = getPersistenceEntryManagerFactory(persistenceType);
			if (persistenceEntryManagerFactory == null) {
				log.error("Unable to get Persistence Entry Manager Factory by type '{}'", persistenceType);
				return null;
			}

			// Determine configuration file name and factory class type
			Class<? extends PersistenceEntryManagerFactory> persistenceEntryManagerFactoryType = (Class<? extends PersistenceEntryManagerFactory>) persistenceEntryManagerFactory
					.getClass().getSuperclass();
			Map<String, String> persistenceFileNames = persistenceEntryManagerFactory.getConfigurationFileNames();

			PersistenceConfiguration persistenceConfiguration = createPersistenceConfiguration(persistenceType, persistenceEntryManagerFactoryType,
					persistenceFileNames);

			return persistenceConfiguration;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return null;
	}

	private PersistenceConfiguration createPersistenceConfiguration(String persistenceType, Class<? extends PersistenceEntryManagerFactory> persistenceEntryManagerFactoryType,
			Map<String, String> persistenceFileNames) {
		if (persistenceFileNames == null) {
			log.error("Unable to get Persistence Entry Manager Factory by type '{}'", persistenceType);
			return null;
		}

		PropertiesConfiguration mergedPropertiesConfiguration = new PropertiesConfiguration(); 
		long mergedPersistenceFileLastModifiedTime = -1;
		StringBuilder mergedPersistenceFileName = new StringBuilder();

		for (String prefix : persistenceFileNames.keySet()) {
			String persistenceFileName = persistenceFileNames.get(prefix);
			
			// Build merged file name
			if (mergedPersistenceFileName.length() > 0) {
				mergedPersistenceFileName.append("!");
			}
			mergedPersistenceFileName.append(persistenceFileName);

			// Find last changed file modification time
			String persistenceFileNamePath = DIR + persistenceFileName;
			File persistenceFile = new File(persistenceFileNamePath);
			if (!persistenceFile.exists()) {
				log.error("Unable to load configuration file '{}'", persistenceFileNamePath);
				return null;
			}
			mergedPersistenceFileLastModifiedTime = Math.max(mergedPersistenceFileLastModifiedTime, persistenceFile.lastModified());

			// Load persistence configuration
			FileConfiguration persistenceFileConf = new FileConfiguration(persistenceFileNamePath);
			if (!persistenceFileConf.isLoaded()) {
				log.error("Unable to load configuration file '{}'", persistenceFileNamePath);
				return null;
			}
			PropertiesConfiguration propertiesConfiguration = persistenceFileConf.getPropertiesConfiguration();

			// Allow to override value via environment variables
			replaceWithSystemValues(propertiesConfiguration);
			
			// Merge all configuration into one with prefix
			appendPropertiesWithPrefix(mergedPropertiesConfiguration, propertiesConfiguration, prefix);
		}

		FileConfiguration mergedFileConfiguration = new FileConfiguration(mergedPersistenceFileName.toString(), mergedPropertiesConfiguration);

		PersistenceConfiguration persistenceConfiguration = new PersistenceConfiguration(mergedPersistenceFileName.toString(), mergedFileConfiguration,
				persistenceEntryManagerFactoryType, mergedPersistenceFileLastModifiedTime);

		return persistenceConfiguration;
	}

	private void replaceWithSystemValues(PropertiesConfiguration propertiesConfiguration) {
		Iterator<?> keys = propertiesConfiguration.getKeys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
			if (System.getenv(key) != null) {
				propertiesConfiguration.setProperty(key, System.getenv(key));
			}
        }
	}

	private void appendPropertiesWithPrefix(PropertiesConfiguration mergedConfiguration, PropertiesConfiguration appendConfiguration, String prefix) {
		Iterator<?> keys = appendConfiguration.getKeys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            Object value = appendConfiguration.getProperty(key);
            mergedConfiguration.setProperty(prefix + "." + key, value);
        }
	}

	private String determineGluuConfigurationFileName() {
		File ldapFile = new File(GLUU_FILE_PATH);
		if (ldapFile.exists()) {
			return GLUU_FILE_PATH;
		}

		return null;
	}

    public PersistenceEntryManagerFactory getPersistenceEntryManagerFactory(PersistenceConfiguration persistenceConfiguration) {
        return getPersistenceEntryManagerFactoryImpl(persistenceConfiguration.getEntryManagerFactoryType());
    }

	private PersistenceEntryManagerFactory getPersistenceEntryManagerFactoryImpl(Class<? extends PersistenceEntryManagerFactory> persistenceEntryManagerFactoryClass) {
		PersistenceEntryManagerFactory persistenceEntryManagerFactory;
		if (persistenceEntryManagerFactoryInstance == null) {
			try {
				persistenceEntryManagerFactory = ReflectHelper.createObjectByDefaultConstructor(persistenceEntryManagerFactoryClass);
			} catch (PropertyNotFoundException | IllegalArgumentException | InstantiationException | IllegalAccessException
					| InvocationTargetException e) {
	            throw new ConfigurationException(
	                    String.format("Failed to create PersistenceEntryManagerFactory by type '%s'!", persistenceEntryManagerFactoryClass));
			}
		} else {
			persistenceEntryManagerFactory = persistenceEntryManagerFactoryInstance
	                .select(persistenceEntryManagerFactoryClass).get();
		}

		return persistenceEntryManagerFactory;
	}

	public PersistenceEntryManagerFactory getPersistenceEntryManagerFactory(String persistenceType) {
		return getPersistenceEntryManagerFactoryImpl(persistenceType);
	}

	private PersistenceEntryManagerFactory getPersistenceEntryManagerFactoryImpl(String persistenceType) {
		PersistenceEntryManagerFactory persistenceEntryManagerFactory = null;
		if (persistenceEntryManagerFactoryInstance == null) {
			switch (persistenceType) {
			case LdapEntryManagerFactory.PERSISTANCE_TYPE:
				persistenceEntryManagerFactory = new LdapEntryManagerFactory();
				break;
			case CouchbaseEntryManagerFactory.PERSISTANCE_TYPE:
				persistenceEntryManagerFactory = new CouchbaseEntryManagerFactory();
				break;
			case HybridEntryManagerFactory.PERSISTANCE_TYPE:
				persistenceEntryManagerFactory = new HybridEntryManagerFactory();
				break;

			default:
				break;
			}
		} else {
			// Get persistence entry manager factory
			for (PersistenceEntryManagerFactory currentPersistenceEntryManagerFactory : persistenceEntryManagerFactoryInstance) {
				log.debug("Found Persistence Entry Manager Factory with type '{}'", currentPersistenceEntryManagerFactory);
				if (StringHelper.equalsIgnoreCase(currentPersistenceEntryManagerFactory.getPersistenceType(), persistenceType)) {
					return currentPersistenceEntryManagerFactory;
				}
			}
		}

		return persistenceEntryManagerFactory;
	}

	public void initLog() {
		if (this.log == null) {
			this.log = LoggerFactory.getLogger(PersistanceFactoryService.class);
		}
		
	}

}
