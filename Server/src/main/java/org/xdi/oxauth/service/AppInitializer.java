/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.codehaus.jackson.map.ObjectMapper;
import org.gluu.site.ldap.OperationsFacade;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.gluu.site.ldap.persistence.exception.LdapMappingException;
import org.slf4j.Logger;
import org.xdi.exception.ConfigurationException;
import org.xdi.model.SimpleProperty;
import org.xdi.model.custom.script.CustomScriptType;
import org.xdi.model.ldap.GluuLdapConfiguration;
import org.xdi.oxauth.model.appliance.GluuAppliance;
import org.xdi.oxauth.model.auth.AuthenticationMode;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.config.StaticConfiguration;
import org.xdi.oxauth.model.config.oxIDPAuthConf;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.util.SecurityProviderUtility;
import org.xdi.oxauth.service.cdi.event.ConfigurationUpdate;
import org.xdi.oxauth.service.cdi.event.LdapConfigurationReload;
import org.xdi.oxauth.service.timer.QuartzSchedulerManager;
import org.xdi.oxauth.service.timer.schedule.JobShedule;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.service.PythonService;
import org.xdi.service.custom.script.CustomScriptManager;
import org.xdi.service.ldap.LdapConnectionService;
import org.xdi.util.StringHelper;
import org.xdi.util.properties.FileConfiguration;
import org.xdi.util.security.StringEncrypter;
import org.xdi.util.security.StringEncrypter.EncryptionException;

import com.unboundid.ldap.sdk.ResultCode;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 * @version 0.1, 24/10/2011
 */
@ApplicationScoped
@Named
public class AppInitializer {

	private final static String EVENT_TYPE = "AppInitializerTimerEvent";
    private final static int DEFAULT_INTERVAL = 30; // 30 seconds

    public static final String DEFAULT_ACR_VALUES = "defaultAuthMode";

    public static final String LDAP_AUTH_CONFIG_NAME = "ldapAuthConfig";

    public static final String LDAP_ENTRY_MANAGER_NAME = "ldapEntryManager";
    public static final String LDAP_AUTH_ENTRY_MANAGER_NAME = "ldapAuthEntryManager";

    @Inject
    private Logger log;
    
    @Inject
    private ApplianceService applianceService;

    @Inject
    private PythonService pythonService;

    @Inject
    private MetricService metricService;

    @Inject
    private CustomScriptManager customScriptManager;

	@Inject
	private ConfigurationFactory configurationFactory;
	
	@Inject
	private CleanerTimer cleanerTimer;
	
	@Inject
	private KeyGeneratorTimer keyGeneratorTimer;
	
	@Inject
	private QuartzSchedulerManager quartzSchedulerManager;
    
	private FileConfiguration ldapConfig;
	private List<GluuLdapConfiguration> ldapAuthConfigs;

	private LdapConnectionService connectionProvider;
	private LdapConnectionService bindConnectionProvider;

	private List<LdapConnectionService> authConnectionProviders;
	private List<LdapConnectionService> authBindConnectionProviders;

    private AtomicBoolean isActive;
	private long lastFinishedTime;
	private AuthenticationMode authenticationMode;

	@PostConstruct
    public void createApplicationComponents() {
    	SecurityProviderUtility.installBCProvider();
    }

    public void applicationInitialized(@Observes @Initialized(ApplicationScoped.class) Object init) {
		List<CustomScriptType> supportedCustomScriptTypes = Arrays.asList(CustomScriptType.PERSON_AUTHENTICATION, CustomScriptType.CLIENT_REGISTRATION,
				CustomScriptType.ID_GENERATOR, CustomScriptType.UMA_AUTHORIZATION_POLICY, CustomScriptType.APPLICATION_SESSION, CustomScriptType.DYNAMIC_SCOPE);
    	createConnectionProvider();
        configurationFactory.create();

        LdapEntryManager localLdapEntryManager = ServerUtil.bean(LdapEntryManager.class, LDAP_ENTRY_MANAGER_NAME);
        List<GluuLdapConfiguration> ldapAuthConfigs = loadLdapAuthConfigs(localLdapEntryManager);
        createAuthConnectionProviders(ldapAuthConfigs);

        setDefaultAuthenticationMethod(localLdapEntryManager);

        pythonService.initPythonInterpreter(configurationFactory.getLdapConfiguration().getString("pythonModulesDir", null));
        customScriptManager.init(supportedCustomScriptTypes);
        metricService.init();

        // Start timer
        quartzSchedulerManager.start();
    	
    	// Schedule timer tasks
        cleanerTimer.initTimer();
        configurationFactory.initTimer();
        keyGeneratorTimer.initTimer();
	}

    @Produces @ApplicationScoped
	public StringEncrypter getStringEncrypter() {
		String encodeSalt = configurationFactory.getCryptoConfigurationSalt();
    	
    	if (StringHelper.isEmpty(encodeSalt)) {
    		throw new ConfigurationException("Encode salt isn't defined");
    	}
    	
    	try {
    		StringEncrypter stringEncrypter = StringEncrypter.instance(encodeSalt);
    		
    		return stringEncrypter;
		} catch (EncryptionException ex) {
    		throw new ConfigurationException("Failed to create StringEncrypter instance");
		}
	}

//    public void init(@Initialized(ApplicationScoped.class) ServletContext init) {
//      Events.instance().raiseTimedEvent(EVENT_TYPE, new TimerSchedule(interval, interval));

//		this.isActive = new AtomicBoolean(false);
//		this.lastFinishedTime = System.currentTimeMillis();
//
//		Events.instance().raiseTimedEvent(EVENT_TYPE, new TimerSchedule(1 * 60 * 1000L, DEFAULT_INTERVAL * 1000L));
//    }

    public void destoy(@Observes @BeforeDestroyed(ApplicationScoped.class) ServletContext init) {
    	// TODO:
    	// Close connection here
    	// Clean up caches, etc...
    }
    
//    public void reloadConfigurationTimerEvent(@Observes @AppReloadTimer @Event<String> reloadEvent) {
//    	documentEvent.fire(event, options)
//		if (this.isActive.get()) {
//			return;
//		}
//
//		if (!this.isActive.compareAndSet(false, true)) {
//			return;
//		}
//
//		try {
//			reloadConfiguration();
//		} catch (Throwable ex) {
//			log.error("Exception happened while reloading application configuration", ex);
//		} finally {
//			this.isActive.set(false);
//			this.lastFinishedTime = System.currentTimeMillis();
//		}
//	}

	private void reloadConfiguration() {
        LdapEntryManager localLdapEntryManager = ServerUtil.bean(LdapEntryManager.class, LDAP_ENTRY_MANAGER_NAME);
		List<GluuLdapConfiguration> newLdapAuthConfigs = loadLdapAuthConfigs(localLdapEntryManager);
		
		if (!this.ldapAuthConfigs.equals(newLdapAuthConfigs)) {
		    // TODO: CDI: Fix
//			recreateLdapAuthEntryManagers(newLdapAuthConfigs);
//			Events.instance().raiseEvent(ExternalAuthenticationService.MODIFIED_INTERNAL_TYPES_EVENT_TYPE);
		}

		setDefaultAuthenticationMethod(localLdapEntryManager);
	}

	/*
	 * Utility method which can be used in custom scripts
	 */
	public LdapEntryManager createLdapAuthEntryManager(GluuLdapConfiguration ldapAuthConfig) {
    	LdapConnectionProviders ldapConnectionProviders = createAuthConnectionProviders(ldapAuthConfig);

    	LdapEntryManager ldapAuthEntryManager = new LdapEntryManager(new OperationsFacade(ldapConnectionProviders.getConnectionProvider(), ldapConnectionProviders.getConnectionBindProvider()));
	    log.debug("Created custom authentication LdapEntryManager: {}", ldapAuthEntryManager);
	        
		return ldapAuthEntryManager;
	}

    @Produces @ApplicationScoped @Named(LDAP_ENTRY_MANAGER_NAME)
    public LdapEntryManager getLdapEntryManager() {
        LdapEntryManager ldapEntryManager = new LdapEntryManager(new OperationsFacade(this.connectionProvider, this.bindConnectionProvider));
        log.debug("Created {}: {}", new Object[] { LDAP_ENTRY_MANAGER_NAME, ldapEntryManager });

        return ldapEntryManager;
    }

    @Produces @ApplicationScoped @Named(LDAP_AUTH_CONFIG_NAME)
    public List<GluuLdapConfiguration> createLdapAuthConfigs() {
    	return ldapAuthConfigs;
    }

    @Produces @ApplicationScoped @Named(LDAP_AUTH_ENTRY_MANAGER_NAME)
	public List<LdapEntryManager> createLdapAuthEntryManager() {
		List<LdapEntryManager> ldapAuthEntryManagers = new ArrayList<LdapEntryManager>();
		if (this.ldapAuthConfigs.size() == 0) {
			return ldapAuthEntryManagers;
		}

		for (int i = 0; i < this.ldapAuthConfigs.size(); i++) {
			LdapEntryManager ldapAuthEntryManager = new LdapEntryManager(new OperationsFacade(this.authConnectionProviders.get(i), this.authBindConnectionProviders.get(i)));
	        log.debug("Created {}#{}: {}", new Object[] { LDAP_AUTH_ENTRY_MANAGER_NAME, i, ldapAuthEntryManager });
	        
	        ldapAuthEntryManagers.add(ldapAuthEntryManager);
		}

		return ldapAuthEntryManagers;
	}

    // TODO: CDI: Fix
    public void recreateLdapEntryManager(@Observes @LdapConfigurationReload String event) {
//    	// Backup current references to objects to allow shutdown properly
//    	LdapEntryManager oldLdapEntryManager = (LdapEntryManager) Component.getInstance(LDAP_ENTRY_MANAGER_NAME);
//
//    	// Recreate components
//    	createConnectionProvider();
//
//        // Destroy old components
//    	Contexts.getApplicationContext().remove(LDAP_ENTRY_MANAGER_NAME);
//    	oldLdapEntryManager.destroy();
//
//    	log.debug("Destroyed {}: {}", LDAP_ENTRY_MANAGER_NAME, oldLdapEntryManager);
    }
//
    public void recreateLdapAuthEntryManagers(List<GluuLdapConfiguration> newLdapAuthConfigs) {
//    	// Backup current references to objects to allow shutdown properly
//    	List<LdapEntryManager> oldLdapAuthEntryManagers = (List<LdapEntryManager>) Component.getInstance(LDAP_AUTH_ENTRY_MANAGER_NAME);
//
//    	// Recreate components
//        createAuthConnectionProviders(newLdapAuthConfigs);
//
//        // Destroy old components
//    	Contexts.getApplicationContext().remove(LDAP_AUTH_ENTRY_MANAGER_NAME);
//
//		for (LdapEntryManager oldLdapAuthEntryManager : oldLdapAuthEntryManagers) {
//			oldLdapAuthEntryManager.destroy();
//	        log.debug("Destroyed {}: {}", LDAP_AUTH_ENTRY_MANAGER_NAME, oldLdapAuthEntryManager);
//		}
    }

	private void destroyLdapConnectionService(LdapConnectionService connectionProvider) {
		if (connectionProvider != null) {
			connectionProvider.closeConnectionPool();
	        log.debug("Destoryed connectionProvider: {}", connectionProvider);
        }
	}

    private void createConnectionProvider() {
    	this.ldapConfig = configurationFactory.getLdapConfiguration();

        Properties connectionProperties = (Properties) this.ldapConfig.getProperties();
        this.connectionProvider = createConnectionProvider(connectionProperties);

        Properties bindConnectionProperties = prepareBindConnectionProperties(connectionProperties);
        this.bindConnectionProvider = createBindConnectionProvider(bindConnectionProperties, connectionProperties);
    }

    private void createAuthConnectionProviders(List<GluuLdapConfiguration> newLdapAuthConfigs) {
    	List<LdapConnectionService> tmpAuthConnectionProviders = new ArrayList<LdapConnectionService>();
    	List<LdapConnectionService> tmpAuthBindConnectionProviders = new ArrayList<LdapConnectionService>();

    	// Prepare connection providers per LDAP authentication configuration
        for (GluuLdapConfiguration ldapAuthConfig : newLdapAuthConfigs) {
        	LdapConnectionProviders ldapConnectionProviders = createAuthConnectionProviders(ldapAuthConfig);

	        tmpAuthConnectionProviders.add(ldapConnectionProviders.getConnectionProvider());
	        tmpAuthBindConnectionProviders.add(ldapConnectionProviders.getConnectionBindProvider());
    	}

		this.ldapAuthConfigs = newLdapAuthConfigs;

		// TODO: CDI: Verify
		ServerUtil.destroy(List.class, LDAP_AUTH_CONFIG_NAME);

		this.authConnectionProviders = tmpAuthConnectionProviders;
    	this.authBindConnectionProviders = tmpAuthBindConnectionProviders;
    }
    

    public LdapConnectionProviders createAuthConnectionProviders(GluuLdapConfiguration ldapAuthConfig) {
        Properties connectionProperties = prepareAuthConnectionProperties(ldapAuthConfig);
        LdapConnectionService connectionProvider = createConnectionProvider(connectionProperties);

        Properties bindConnectionProperties = prepareBindConnectionProperties(connectionProperties);
        LdapConnectionService bindConnectionProvider = createBindConnectionProvider(bindConnectionProperties, connectionProperties);
    	
        return new LdapConnectionProviders(connectionProvider, bindConnectionProvider);
    }

	private Properties prepareAuthConnectionProperties(GluuLdapConfiguration ldapAuthConfig) {
        FileConfiguration configuration = configurationFactory.getLdapConfiguration();

		Properties properties = (Properties) configuration.getProperties().clone();
		if (ldapAuthConfig != null) {
		    properties.setProperty("servers", buildServersString(ldapAuthConfig.getServers()));
		    
		    String bindDn = ldapAuthConfig.getBindDN();
		    if (StringHelper.isNotEmpty(bindDn)) {
		    	properties.setProperty("bindDN", bindDn);
				properties.setProperty("bindPassword", ldapAuthConfig.getBindPassword());
		    }
			properties.setProperty("useSSL", Boolean.toString(ldapAuthConfig.isUseSSL()));
			properties.setProperty("maxconnections", Integer.toString(ldapAuthConfig.getMaxConnections()));
		}

		return properties;
	}

    private Properties prepareBindConnectionProperties(Properties connectionProperties) {
		// TODO: Use own properties with prefix specified in variable 'bindConfigurationComponentName'
		Properties bindProperties = (Properties) connectionProperties.clone();
		bindProperties.remove("bindDN");
		bindProperties.remove("bindPassword");

		return bindProperties;
	}

	private LdapConnectionService createConnectionProvider(Properties connectionProperties) {
		EncryptionService securityService = ServerUtil.bean(EncryptionService.class);
		LdapConnectionService connectionProvider = new LdapConnectionService(securityService.decryptProperties(connectionProperties));

		return connectionProvider;
	}

	private LdapConnectionService createBindConnectionProvider(Properties bindConnectionProperties, Properties connectionProperties) {
		LdapConnectionService bindConnectionProvider = createConnectionProvider(bindConnectionProperties);
		if (ResultCode.INAPPROPRIATE_AUTHENTICATION.equals(bindConnectionProvider.getCreationResultCode())) {
			log.warn("It's not possible to create authentication LDAP connection pool using anonymous bind. Attempting to create it using binDN/bindPassword");
			bindConnectionProvider = createConnectionProvider(connectionProperties);
		}
		
		return bindConnectionProvider;
	}

	private String buildServersString(List<?> servers) {
		StringBuilder sb = new StringBuilder();

		if (servers == null) {
			return sb.toString();
		}
		
		boolean first = true;
		for (Object server : servers) {
			if (first) {
				first = false;
			} else {
				sb.append(",");
			}

			if (server instanceof SimpleProperty) {
				sb.append(((SimpleProperty) server).getValue());
			} else {
				sb.append(server);
			}
		}

		return sb.toString();
	}

	private List<oxIDPAuthConf> loadLdapIdpAuthConfigs(LdapEntryManager localLdapEntryManager) {
		GluuAppliance appliance = loadAppliance(localLdapEntryManager, "oxIDPAuthentication");

		if ((appliance == null) || (appliance.getOxIDPAuthentication() == null)) {
			return null;
		}

		List<oxIDPAuthConf> configurations = new ArrayList<oxIDPAuthConf>();
		for (String configurationJson : appliance.getOxIDPAuthentication()) {

			try {
				oxIDPAuthConf configuration = (oxIDPAuthConf) jsonToObject(configurationJson, oxIDPAuthConf.class);
				if (configuration.getType().equalsIgnoreCase("ldap") || configuration.getType().equalsIgnoreCase("auth")) {
					configurations.add(configuration);
				}
			} catch (Exception ex) {
				log.error("Failed to create object by json: '{}'", ex, configurationJson);
			}
		}

		return configurations;
	}

	private void setDefaultAuthenticationMethod(LdapEntryManager localLdapEntryManager) {
		GluuAppliance appliance = loadAppliance(localLdapEntryManager, "oxAuthenticationMode");

		authenticationMode = null;
		if (appliance != null) {
			this.authenticationMode = new AuthenticationMode(appliance.getAuthenticationMode());
		}

	    // TODO: CDI: Fix
		ServerUtil.destroy(String.class, DEFAULT_ACR_VALUES);
	}
	
	@Produces @ApplicationScoped @Named(DEFAULT_ACR_VALUES)
	public AuthenticationMode getDefaultAuthenticationMode() {
		return authenticationMode;
	}

	private GluuAppliance loadAppliance(LdapEntryManager localLdapEntryManager, String ... ldapReturnAttributes) {
		String baseDn = configurationFactory.getBaseDn().getAppliance();
		String applianceInum = configurationFactory.getAppConfiguration().getApplianceInum();
		if (StringHelper.isEmpty(baseDn) || StringHelper.isEmpty(applianceInum)) {
			return null;
		}

		String applianceDn = String.format("inum=%s,%s", applianceInum, baseDn);

		GluuAppliance appliance = null;
		try {
			appliance = localLdapEntryManager.find(GluuAppliance.class, applianceDn, ldapReturnAttributes);
		} catch (LdapMappingException ex) {
			log.error("Failed to load appliance entry from Ldap", ex);
			return null;
		}

		return appliance;
	}

	public GluuLdapConfiguration loadLdapAuthConfig(oxIDPAuthConf configuration) {
		if (configuration == null) {
			return null;
		}

		try {
			if (configuration.getType().equalsIgnoreCase("auth")) {
				return mapLdapConfig(configuration.getConfig());
			}
		} catch (Exception ex) {
			log.error("Failed to create object by oxIDPAuthConf: '{}'", ex, configuration);
		}

		return null;
	}

	private List<GluuLdapConfiguration> loadLdapAuthConfigs(LdapEntryManager localLdapEntryManager) {
		List<GluuLdapConfiguration> ldapAuthConfigs = new ArrayList<GluuLdapConfiguration>();

		List<oxIDPAuthConf> ldapIdpAuthConfigs = loadLdapIdpAuthConfigs(localLdapEntryManager);
		if (ldapIdpAuthConfigs == null) {
			return ldapAuthConfigs;
		}

		for (oxIDPAuthConf ldapIdpAuthConfig : ldapIdpAuthConfigs) {
			GluuLdapConfiguration ldapAuthConfig = loadLdapAuthConfig(ldapIdpAuthConfig);
			if ((ldapAuthConfig != null) && ldapAuthConfig.isEnabled()) {
				ldapAuthConfigs.add(ldapAuthConfig);
			}
		}
		
		return ldapAuthConfigs; 
	}

	private GluuLdapConfiguration mapLdapConfig(String config) throws Exception {
		return (GluuLdapConfiguration) jsonToObject(config, GluuLdapConfiguration.class);
	}

	private Object jsonToObject(String json, Class<?> clazz) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Object clazzObject = mapper.readValue(json, clazz);

		return clazzObject;
	}
	
	public void updateLoggingSeverity(@Observes @ConfigurationUpdate AppConfiguration appConfiguration) {
		String loggingLevel = appConfiguration.getLoggingLevel();
		if (StringHelper.isEmpty(loggingLevel)) {
			return;
		}

		log.info("Setting loggers level to: '{}'", loggingLevel);
		
		LoggerContext loggerContext = LoggerContext.getContext(false);

		if (StringHelper.equalsIgnoreCase("DEFAULT", loggingLevel)) {
			log.info("Reloadming log4j configuration");
			loggerContext.reconfigure();
			return;
		}

		Level level = Level.toLevel(loggingLevel, Level.INFO);

		for (org.apache.logging.log4j.core.Logger logger : loggerContext.getLoggers()) {
			String loggerName = logger.getName();
			if (loggerName.startsWith("org.xdi.service") || loggerName.startsWith("org.xdi.oxauth") || loggerName.startsWith("org.gluu")) {
				logger.setLevel(level);
			}
		}
	}
	
	private class LdapConnectionProviders {
		private LdapConnectionService connectionProvider;
		private LdapConnectionService connectionBindProvider;

		public LdapConnectionProviders(LdapConnectionService connectionProvider, LdapConnectionService connectionBindProvider) {
			this.connectionProvider = connectionProvider;
			this.connectionBindProvider = connectionBindProvider;
		}

		public LdapConnectionService getConnectionProvider() {
			return connectionProvider;
		}

		public LdapConnectionService getConnectionBindProvider() {
			return connectionBindProvider;
		}

	}

}