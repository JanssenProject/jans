package org.gluu.persist.service;

import java.io.File;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.gluu.persist.PersistenceEntryManagerFactory;
import org.gluu.persist.model.PersistenceConfiguration;
import org.gluu.util.StringHelper;
import org.gluu.util.properties.FileConfiguration;
import org.slf4j.Logger;

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
	public static final String LDAP_DEFAULT_PROPERTIES_FILE = DIR + "gluu-ldap.properties";

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
			String ldapFileName = determineLdapConfigurationFileName(applicationPropertiesFile);
			currentPersistenceConfiguration = loadLdapConfiguration(ldapFileName);
		}

		return currentPersistenceConfiguration;
	}

	private PersistenceConfiguration createPersistenceConfiguration(String gluuFileName) {
		try {
			// Determine persistence type
			FileConfiguration gluuFileConf = new FileConfiguration(gluuFileName);
			String persistenceType = gluuFileConf.getString("persistence.type");

			// Determine configuration file name and factory class type
			String persistenceFileName = null;
			Class<? extends PersistenceEntryManagerFactory> persistenceEntryManagerFactoryType = null;

			for (PersistenceEntryManagerFactory persistenceEntryManagerFactory : persistenceEntryManagerFactoryInstance) {
				log.debug("Found Persistence Entry Manager Factory with type '{}'", persistenceEntryManagerFactory);
				if (StringHelper.equalsIgnoreCase(persistenceEntryManagerFactory.getPersistenceType(), persistenceType)) {
					persistenceFileName = persistenceEntryManagerFactory.getDefaultConfigurationFileName();
					persistenceEntryManagerFactoryType = (Class<? extends PersistenceEntryManagerFactory>) persistenceEntryManagerFactory
							.getClass().getSuperclass();
					break;
				}
			}

			if (persistenceFileName == null) {
				log.error("Unable to get Persistence Entry Manager Factory by type '{}'", persistenceType);
				return null;
			}

			String persistenceFileNamePath = DIR + persistenceFileName;

			FileConfiguration persistenceFileConf = new FileConfiguration(persistenceFileNamePath);
			if (!persistenceFileConf.isLoaded()) {
				log.error("Unable to load configuration file '{}'", persistenceFileNamePath);
				return null;
			}

			// Allow to override value via environment variables
			replaceWithSystemValues(persistenceFileConf);

			long persistenceFileLastModifiedTime = -1;
			File persistenceFile = new File(persistenceFileNamePath);
			if (persistenceFile.exists()) {
				persistenceFileLastModifiedTime = persistenceFile.lastModified();
			}

			PersistenceConfiguration persistenceConfiguration = new PersistenceConfiguration(persistenceFileName, persistenceFileConf,
					persistenceEntryManagerFactoryType, persistenceFileLastModifiedTime);

			return persistenceConfiguration;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return null;
	}

	private PersistenceConfiguration loadLdapConfiguration(String ldapFileName) {
		try {
			FileConfiguration ldapConfiguration = new FileConfiguration(ldapFileName);

			// Allow to override value via environment variables
			replaceWithSystemValues(ldapConfiguration);

			long ldapFileLastModifiedTime = -1;
			File ldapFile = new File(ldapFileName);
			if (ldapFile.exists()) {
				ldapFileLastModifiedTime = ldapFile.lastModified();
			}

			PersistenceConfiguration persistenceConfiguration = new PersistenceConfiguration(ldapFileName, ldapConfiguration,
					org.gluu.persist.ldap.impl.LdapEntryManagerFactory.class, ldapFileLastModifiedTime);

			return persistenceConfiguration;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return null;
	}

	private void replaceWithSystemValues(FileConfiguration fileConfiguration) {
		Set<Map.Entry<Object, Object>> ldapProperties = fileConfiguration.getProperties().entrySet();
		for (Map.Entry<Object, Object> ldapPropertyEntry : ldapProperties) {
			String ldapPropertyKey = (String) ldapPropertyEntry.getKey();
			if (System.getenv(ldapPropertyKey) != null) {
				ldapPropertyEntry.setValue(System.getenv(ldapPropertyKey));
			}
		}
	}

	private String determineGluuConfigurationFileName() {
		File ldapFile = new File(GLUU_FILE_PATH);
		if (ldapFile.exists()) {
			return GLUU_FILE_PATH;
		}

		return null;
	}

	private String determineLdapConfigurationFileName(String applictionPropertiesFile) {
		if (applictionPropertiesFile == null) {
			return LDAP_DEFAULT_PROPERTIES_FILE;
		}

		File ldapFile = new File(applictionPropertiesFile);
		if (ldapFile.exists()) {
			return applictionPropertiesFile;
		}

		return LDAP_DEFAULT_PROPERTIES_FILE;
	}

    public PersistenceEntryManagerFactory getPersistenceEntryManagerFactory(PersistenceConfiguration persistenceConfiguration) {
        PersistenceEntryManagerFactory persistenceEntryManagerFactory = persistenceEntryManagerFactoryInstance
                .select(persistenceConfiguration.getEntryManagerFactoryType()).get();

        return persistenceEntryManagerFactory;
    }

}
