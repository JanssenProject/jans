package org.gluu.persist.hybrid.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.PersistenceEntryManagerFactory;
import org.gluu.persist.exception.operation.ConfigurationException;
import org.gluu.persist.operation.PersistenceOperationService;
import org.gluu.persist.service.PersistanceFactoryService;
import org.gluu.util.PropertiesHelper;
import org.gluu.util.StringHelper;
import org.gluu.util.properties.FileConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hybrid Entry Manager Factory
 *
 * @author Yuriy Movchan Date: 05/13/2018
 */
@ApplicationScoped
public class HybridEntryManagerFactory implements PersistenceEntryManagerFactory {

    public static final String PROPERTIES_FILE = "gluu-hybrid.properties";

	private static final Logger LOG = LoggerFactory.getLogger(HybridEntryManagerFactory.class);
	
	@Inject
	private PersistanceFactoryService persistanceFactoryService;

	private String[] persistenceTypes;

	private HashMap<String, Properties> сonnectionProperties;
	private Properties hybridMappingProperties;

    @Override
    public String getPersistenceType() {
        return "hybrid";
    }

    @Override
    public HashMap<String, String> getConfigurationFileNames() {
    	HashMap<String, String> confs = new HashMap<String, String>();
    	confs.put("hybrid", PROPERTIES_FILE);

    	HashMap<String, String> allConfs = getAllConfigurationFileNames(PROPERTIES_FILE);
    	confs.putAll(allConfs);

    	return confs;
    }

    private HashMap<String, String> getAllConfigurationFileNames(String confFileName) {
    	HashMap<String, String> allConfs = new HashMap<String, String>();

		FileConfiguration fileConf = new FileConfiguration(PersistanceFactoryService.DIR + confFileName);
		if (!fileConf.isLoaded()) {
			LOG.error("Unable to load configuration file '{}'", PersistanceFactoryService.DIR + confFileName);
            throw new ConfigurationException(String.format("Unable to load configuration file: '%s'", fileConf));
		}

		String storagesList = fileConf.getString("storages", null);
		if (StringHelper.isEmpty(storagesList)) {
            throw new ConfigurationException("'storages' key not exists or value is empty!");
		}
		
		this.persistenceTypes = StringHelper.split(storagesList, ",");
		for (String persistenceType : persistenceTypes) {
			PersistenceEntryManagerFactory persistenceEntryManagerFactory = persistanceFactoryService.getPersistenceEntryManagerFactory(persistenceType);
			if (persistenceEntryManagerFactory == null) {
				throw new ConfigurationException(String.format("Unable to get Persistence Entry Manager Factory by type '%s'", persistenceType));
			}
			
	    	Map<String, String> confs = persistenceEntryManagerFactory.getConfigurationFileNames();
	    	allConfs.putAll(confs);
		}

		return allConfs;
	}


    @Override
    public HybridEntryManager createEntryManager(Properties conf) {
    	this.сonnectionProperties = new HashMap<String, Properties>();

    	HashMap<String, PersistenceEntryManager> persistenceEntryManagers = new HashMap<String, PersistenceEntryManager>();
    	List<PersistenceOperationService> operationServices = new ArrayList<PersistenceOperationService>();

		for (String persistenceType : persistenceTypes) {
			PersistenceEntryManagerFactory persistenceEntryManagerFactory = persistanceFactoryService.getPersistenceEntryManagerFactory(persistenceType);
			if (persistenceEntryManagerFactory == null) {
				throw new ConfigurationException(String.format("Unable to get Persistence Entry Manager Factory by type '%s'", persistenceType));
			}

    		Properties persistenceConnectionProperties = PropertiesHelper.findProperties(conf, persistenceType);
    		PersistenceEntryManager persistenceEntryManager = persistenceEntryManagerFactory.createEntryManager(persistenceConnectionProperties);
    		
    		persistenceEntryManagers.put(persistenceType, persistenceEntryManager);
    		operationServices.add(persistenceEntryManager.getOperationService());

    		this.сonnectionProperties.put(persistenceType, persistenceConnectionProperties);
    	}

		this.hybridMappingProperties = PropertiesHelper.filterProperties(conf, "storage");
		
		HybridPersistenceOperationService hybridOperationService = new HybridPersistenceOperationService(operationServices);
    	
        HybridEntryManager hybridEntryManager = new HybridEntryManager(hybridMappingProperties, persistenceEntryManagers, hybridOperationService);
        LOG.info("Created HybridEntryManager: {}", hybridOperationService);

        return hybridEntryManager;
    }

}
