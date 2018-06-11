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
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.gluu.persist.exception.BasePersistenceException;
import org.gluu.persist.ldap.impl.LdapEntryManager;
import org.gluu.persist.ldap.impl.LdapEntryManagerFactory;
import org.jboss.weld.util.reflection.ParameterizedTypeImpl;
import org.slf4j.Logger;
import org.xdi.exception.ConfigurationException;
import org.xdi.model.SimpleProperty;
import org.xdi.model.custom.script.CustomScriptType;
import org.xdi.model.ldap.GluuLdapConfiguration;
import org.xdi.oxauth.model.appliance.GluuAppliance;
import org.xdi.oxauth.model.auth.AuthenticationMode;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.config.oxIDPAuthConf;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.util.SecurityProviderUtility;
import org.xdi.oxauth.service.cdi.event.AuthConfigurationEvent;
import org.xdi.oxauth.service.cdi.event.ReloadAuthScript;
import org.xdi.oxauth.service.external.ExternalAuthenticationService;
import org.xdi.oxauth.service.logger.LoggerService;
import org.xdi.oxauth.service.status.ldap.LdapStatusTimer;
import org.xdi.service.JsonService;
import org.xdi.service.PythonService;
import org.xdi.service.cdi.async.Asynchronous;
import org.xdi.service.cdi.event.ConfigurationUpdate;
import org.xdi.service.cdi.event.LdapConfigurationReload;
import org.xdi.service.cdi.event.Scheduled;
import org.xdi.service.cdi.util.CdiUtil;
import org.xdi.service.custom.lib.CustomLibrariesLoader;
import org.xdi.service.custom.script.CustomScriptManager;
import org.xdi.service.timer.QuartzSchedulerManager;
import org.xdi.service.timer.event.TimerEvent;
import org.xdi.service.timer.schedule.TimerSchedule;
import org.xdi.util.StringHelper;
import org.xdi.util.properties.FileConfiguration;
import org.xdi.util.security.StringEncrypter;
import org.xdi.util.security.StringEncrypter.EncryptionException;

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

    public static final String LDAP_AUTH_CONFIG_NAME = "ldapAuthConfig";

    public static final String LDAP_ENTRY_MANAGER_NAME = "ldapEntryManager";
    public static final String LDAP_AUTH_ENTRY_MANAGER_NAME = "ldapAuthEntryManager";

    @Inject
    private Logger log;

	@Inject
	private BeanManager beanManager;

	@Inject
	private Event<String> event;

	@Inject
	private Event<TimerEvent> timerEvent;

	@Inject @Named(LDAP_ENTRY_MANAGER_NAME)
	private Instance<LdapEntryManager> ldapEntryManagerInstance;
	
	@Inject @Named(LDAP_AUTH_ENTRY_MANAGER_NAME)
	private Instance<List<LdapEntryManager>> ldapAuthEntryManagerInstance;

	@Inject @Named(LDAP_AUTH_CONFIG_NAME)
	private Instance<List<GluuLdapConfiguration>> ldapAuthConfigInstance;

	@Inject
	private Instance<AuthenticationMode> authenticationModeInstance;

	@Inject
	private Instance<EncryptionService> encryptionServiceInstance;

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
	private CustomLibrariesLoader customLibrariesLoader;

	@Inject
	private LdapStatusTimer ldapStatusTimer;
	
	@Inject
	private QuartzSchedulerManager quartzSchedulerManager;

	@Inject
	private LoggerService loggerService;

	@Inject
	private JsonService jsonService;

    private AtomicBoolean isActive;
	private long lastFinishedTime;
	private AuthenticationMode authenticationMode;

	private List<GluuLdapConfiguration> ldapAuthConfigs;
	private LdapEntryManagerFactory entryManagerFactory;

	@PostConstruct
    public void createApplicationComponents() {
    	SecurityProviderUtility.installBCProvider();
    }

    public void applicationInitialized(@Observes @Initialized(ApplicationScoped.class) Object init) {
        log.debug("Initializing application services");
    	customLibrariesLoader.init();

        createEntryManagerFactory();

        configurationFactory.create();
        loggerService.configure();

        LdapEntryManager localLdapEntryManager = ldapEntryManagerInstance.get();
        this.ldapAuthConfigs = loadLdapAuthConfigs(localLdapEntryManager);

        setDefaultAuthenticationMethod(localLdapEntryManager);

		// Initialize python interpreter
        pythonService.initPythonInterpreter(configurationFactory.getLdapConfiguration().getString("pythonModulesDir", null));

		// Initialize script manager
        List<CustomScriptType> supportedCustomScriptTypes = Arrays.asList(CustomScriptType.PERSON_AUTHENTICATION, CustomScriptType.CONSENT_GATHERING,
        		CustomScriptType.CLIENT_REGISTRATION, CustomScriptType.ID_GENERATOR, CustomScriptType.UMA_RPT_POLICY, CustomScriptType.UMA_CLAIMS_GATHERING,
				CustomScriptType.APPLICATION_SESSION, CustomScriptType.DYNAMIC_SCOPE);

        // Start timer
        quartzSchedulerManager.start();

        // Schedule timer tasks
        metricService.initTimer();
        configurationFactory.initTimer();
        ldapStatusTimer.initTimer();
        cleanerTimer.initTimer();
        customScriptManager.initTimer(supportedCustomScriptTypes);
        keyGeneratorTimer.initTimer();
        initTimer();
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

    public void initTimer() {
		this.isActive = new AtomicBoolean(false);
		this.lastFinishedTime = System.currentTimeMillis();

		timerEvent.fire(new TimerEvent(new TimerSchedule(60, DEFAULT_INTERVAL), new AuthConfigurationEvent(),
				Scheduled.Literal.INSTANCE));
    }
    
    @Asynchronous
    public void reloadConfigurationTimerEvent(@Observes @Scheduled AuthConfigurationEvent authConfigurationEvent) {
		if (this.isActive.get()) {
			return;
		}

		if (!this.isActive.compareAndSet(false, true)) {
			return;
		}

		try {
			reloadConfiguration();
		} catch (Throwable ex) {
			log.error("Exception happened while reloading application configuration", ex);
		} finally {
			this.isActive.set(false);
			this.lastFinishedTime = System.currentTimeMillis();
		}
	}

	private void reloadConfiguration() {
        LdapEntryManager localLdapEntryManager = ldapEntryManagerInstance.get();

        log.trace("Attempting to use {}: {}", LDAP_ENTRY_MANAGER_NAME, localLdapEntryManager.getOperationService());
		List<GluuLdapConfiguration> newLdapAuthConfigs = loadLdapAuthConfigs(localLdapEntryManager);
		
		if (!this.ldapAuthConfigs.equals(newLdapAuthConfigs)) {
			recreateLdapAuthEntryManagers(newLdapAuthConfigs);
			this.ldapAuthConfigs = newLdapAuthConfigs;

			event.select(ReloadAuthScript.Literal.INSTANCE).fire(ExternalAuthenticationService.MODIFIED_INTERNAL_TYPES_EVENT_TYPE);
		}

		setDefaultAuthenticationMethod(localLdapEntryManager);
	}

	/*
	 * Utility method which can be used in custom scripts
	 */
	public LdapEntryManager createLdapAuthEntryManager(GluuLdapConfiguration ldapAuthConfig) {
    	Properties ldapConnectionProperties = prepareAuthConnectionProperties(ldapAuthConfig);

    	LdapEntryManager ldapAuthEntryManager = this.entryManagerFactory.createEntryManager(ldapConnectionProperties);
	    log.debug("Created custom authentication LdapEntryManager: {}", ldapAuthEntryManager);
	        
		return ldapAuthEntryManager;
	}

    @Produces @ApplicationScoped @Named(LDAP_ENTRY_MANAGER_NAME)
    public LdapEntryManager createLdapEntryManager() {
    	FileConfiguration ldapConfig = configurationFactory.getLdapConfiguration();
        Properties connectionProperties = (Properties) ldapConfig.getProperties();

        EncryptionService securityService = encryptionServiceInstance.get();
        Properties decryptedConnectionProperties = securityService.decryptProperties(connectionProperties);

        LdapEntryManager ldapEntryManager = this.entryManagerFactory.createEntryManager(decryptedConnectionProperties); 
        log.info("Created {}: {}", new Object[] { LDAP_ENTRY_MANAGER_NAME, ldapEntryManager });

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

		List<Properties> ldapAuthProperties = prepareAuthConnectionProperties(this.ldapAuthConfigs);
		for (int i = 0; i < ldapAuthProperties.size(); i++) {
			LdapEntryManager ldapAuthEntryManager = this.entryManagerFactory.createEntryManager(ldapAuthProperties.get(i));
	        log.debug("Created {}#{}: {}", new Object[] { LDAP_AUTH_ENTRY_MANAGER_NAME, i, ldapAuthEntryManager });
	        
	        ldapAuthEntryManagers.add(ldapAuthEntryManager);
		}

		return ldapAuthEntryManagers;
	}

    public void recreateLdapEntryManager(@Observes @LdapConfigurationReload String event) {
    	// Get existing application scoped instance
    	LdapEntryManager oldLdapEntryManager = CdiUtil.getContextBean(beanManager, LdapEntryManager.class, LDAP_ENTRY_MANAGER_NAME);

        // Close existing connections
    	closeLdapEntryManager(oldLdapEntryManager);

        // Force to create new bean
    	LdapEntryManager ldapEntryManager = ldapEntryManagerInstance.get();
        ldapEntryManagerInstance.destroy(ldapEntryManager);
        log.info("Recreated instance {}: {}", LDAP_ENTRY_MANAGER_NAME, ldapEntryManager);
    }

    private void createEntryManagerFactory() {
		this.entryManagerFactory = new LdapEntryManagerFactory();
	}

	private void closeLdapEntryManager(LdapEntryManager oldLdapEntryManager) {
		// Close existing connections
    	log.debug("Attempting to destroy {}: {}", LDAP_ENTRY_MANAGER_NAME, oldLdapEntryManager);
    	oldLdapEntryManager.destroy();
        log.debug("Destroyed {}: {}", LDAP_ENTRY_MANAGER_NAME, oldLdapEntryManager);
	}

	private void closeLdapEntryManagers(List<LdapEntryManager> oldLdapEntryManagers) {
		// Close existing connections
		for (LdapEntryManager oldLdapEntryManager : oldLdapEntryManagers) {
	    	log.debug("Attempting to destroy {}: {}", LDAP_AUTH_ENTRY_MANAGER_NAME, oldLdapEntryManager);
			oldLdapEntryManager.destroy();
	        log.debug("Destroyed {}: {}", LDAP_AUTH_ENTRY_MANAGER_NAME, oldLdapEntryManager);
		}
	}

    public void recreateLdapAuthEntryManagers(List<GluuLdapConfiguration> newLdapAuthConfigs) {
    	// Get existing application scoped instance
		List<LdapEntryManager> oldLdapAuthEntryManagers = CdiUtil.getContextBean(beanManager,
				new ParameterizedTypeImpl(List.class, LdapEntryManager.class), LDAP_AUTH_ENTRY_MANAGER_NAME);

    	// Recreate components
		this.ldapAuthConfigs = newLdapAuthConfigs;
        
        // Close existing connections
        closeLdapEntryManagers(oldLdapAuthEntryManagers);

		// Destroy old Ldap auth entry managers
		for (LdapEntryManager oldLdapAuthEntryManager : oldLdapAuthEntryManagers) {
	    	log.debug("Attempting to destroy {}: {}", LDAP_AUTH_ENTRY_MANAGER_NAME, oldLdapAuthEntryManager);
			oldLdapAuthEntryManager.destroy();
	        log.debug("Destroyed {}: {}", LDAP_AUTH_ENTRY_MANAGER_NAME, oldLdapAuthEntryManager);
		}
		
        // Force to create new Ldap auth entry managers bean
    	List<LdapEntryManager> ldapAuthEntryManagers = ldapAuthEntryManagerInstance.get();
    	ldapAuthEntryManagerInstance.destroy(ldapAuthEntryManagers);
        log.info("Recreated instance {}: {}", LDAP_AUTH_ENTRY_MANAGER_NAME, ldapAuthEntryManagers);

        // Force to create new auth configuration bean
    	List<GluuLdapConfiguration> oldLdapAuthConfigs = ldapAuthConfigInstance.get();
		ldapAuthConfigInstance.destroy(oldLdapAuthConfigs);
    }

    private List<Properties> prepareAuthConnectionProperties(List<GluuLdapConfiguration> ldapAuthConfigs) {
    	List<Properties> result = new ArrayList<Properties>();

		// Prepare connection providers per LDAP authentication configuration
        for (GluuLdapConfiguration ldapAuthConfig : ldapAuthConfigs) {
            Properties decrypytedConnectionProperties = prepareAuthConnectionProperties(ldapAuthConfig);

    		result.add(decrypytedConnectionProperties);
    	}

		return result;
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

		EncryptionService securityService = encryptionServiceInstance.get();
		Properties decrypytedProperties = securityService.decryptProperties(properties);

		return decrypytedProperties;
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

	private void setDefaultAuthenticationMethod(LdapEntryManager localLdapEntryManager) {
		String currentAuthMethod = null;
		if (this.authenticationMode != null) {
			currentAuthMethod = this.authenticationMode.getName();
		}

		String actualAuthMethod = getActualDefaultAuthenticationMethod(localLdapEntryManager);
		
		if (!StringHelper.equals(currentAuthMethod, actualAuthMethod)) {
			authenticationMode = null;
			if (actualAuthMethod != null) {
				this.authenticationMode = new AuthenticationMode(actualAuthMethod);
			}

			authenticationModeInstance.destroy(authenticationModeInstance.get());
		}
	}

	private String getActualDefaultAuthenticationMethod(LdapEntryManager localLdapEntryManager) {
		GluuAppliance appliance = loadAppliance(localLdapEntryManager, "oxAuthenticationMode");

		if (appliance == null) {
			return null;
		}

		return appliance.getAuthenticationMode();
	}
	
	@Produces @ApplicationScoped
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
		} catch (BasePersistenceException ex) {
			log.error("Failed to load appliance entry from Ldap", ex);
			return null;
		}

		return appliance;
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

	private List<oxIDPAuthConf> loadLdapIdpAuthConfigs(LdapEntryManager localLdapEntryManager) {
		GluuAppliance appliance = loadAppliance(localLdapEntryManager, "oxIDPAuthentication");

		if ((appliance == null) || (appliance.getOxIDPAuthentication() == null)) {
			return null;
		}

		List<oxIDPAuthConf> configurations = new ArrayList<oxIDPAuthConf>();
		for (String configurationJson : appliance.getOxIDPAuthentication()) {

			try {
				oxIDPAuthConf configuration = jsonService.jsonToObject(configurationJson, oxIDPAuthConf.class);
				if (configuration.getType().equalsIgnoreCase("ldap") || configuration.getType().equalsIgnoreCase("auth")) {
					configurations.add(configuration);
				}
			} catch (Exception ex) {
				log.error("Failed to create object by json: '{}'", configurationJson, ex);
			}
		}

		return configurations;
	}

	private GluuLdapConfiguration loadLdapAuthConfig(oxIDPAuthConf configuration) {
		if (configuration == null) {
			return null;
		}

		try {
			if (configuration.getType().equalsIgnoreCase("auth")) {
				return jsonService.jsonToObject(configuration.getConfig(), GluuLdapConfiguration.class);
			}
		} catch (Exception ex) {
			log.error("Failed to create object by oxIDPAuthConf: '{}'", configuration, ex);
		}

		return null;
	}
	
	public void updateLoggingSeverity(@Observes @ConfigurationUpdate AppConfiguration appConfiguration) {
		String loggingLevel = appConfiguration.getLoggingLevel();
		if (StringHelper.isEmpty(loggingLevel)) {
			return;
		}

		log.info("Setting loggers level to: '{}'", loggingLevel);
		
		LoggerContext loggerContext = LoggerContext.getContext(false);

		if (StringHelper.equalsIgnoreCase("DEFAULT", loggingLevel)) {
			log.info("Reloading log4j configuration");
			loggerContext.reconfigure();
			return;
		}

		Level level = Level.toLevel(loggingLevel, Level.INFO);

		for (org.apache.logging.log4j.core.Logger logger : loggerContext.getLoggers()) {
			String loggerName = logger.getName();
			if (loggerName.startsWith("org.xdi.service") || loggerName.startsWith("org.xdi.oxauth") || loggerName.startsWith("org.gluu") || level == Level.OFF) {
				logger.setLevel(level);
			}
		}
	}

    public void destroy(@Observes @BeforeDestroyed(ApplicationScoped.class) ServletContext init) {
    	log.info("Closing LDAP connection at server shutdown...");
        LdapEntryManager ldapEntryManager = ldapEntryManagerInstance.get();
        closeLdapEntryManager(ldapEntryManager);
        
    	List<LdapEntryManager> ldapAuthEntryManagers = ldapAuthEntryManagerInstance.get();
        closeLdapEntryManagers(ldapAuthEntryManagers);
    }

}