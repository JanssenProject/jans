/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.gluu.site.ldap.OperationsFacade;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.gluu.site.ldap.persistence.exception.LdapMappingException;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.async.TimerSchedule;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Events;
import org.jboss.seam.log.Log;
import org.xdi.exception.ConfigurationException;
import org.xdi.model.SimpleProperty;
import org.xdi.model.custom.script.CustomScriptType;
import org.xdi.model.ldap.GluuLdapConfiguration;
import org.xdi.oxauth.model.appliance.GluuAppliance;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.config.oxIDPAuthConf;
import org.xdi.oxauth.model.util.SecurityProviderUtility;
import org.xdi.oxauth.service.custom.CustomScriptManagerMigrator;
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
@Scope(ScopeType.APPLICATION)
@Name("appInitializer")
@Startup
public class AppInitializer {

	private final static String EVENT_TYPE = "AppInitializerTimerEvent";
    private final static int DEFAULT_INTERVAL = 30; // 30 seconds

    public static final String DEFAULT_AUTH_MODE_NAME = "defaultAuthModeName";

    public static final String LDAP_AUTH_CONFIG_NAME = "ldapAuthConfig";

    public static final String LDAP_ENTRY_MANAGER_NAME = "ldapEntryManager";
    public static final String LDAP_AUTH_ENTRY_MANAGER_NAME = "ldapAuthEntryManager";

    @Logger
    private Log log;
    
    @In
    private ApplianceService applianceService;
    
    @In
    private ConfigurationFactory configurationFactory;

	private FileConfiguration ldapConfig;
	private List<GluuLdapConfiguration> ldapAuthConfigs;

	private LdapConnectionService connectionProvider;
	private LdapConnectionService bindConnectionProvider;

	private List<LdapConnectionService> authConnectionProviders;
	private List<LdapConnectionService> authBindConnectionProviders;

    private AtomicBoolean isActive;
	private long lastFinishedTime;

    @Create
    public void createApplicationComponents() {
    	SecurityProviderUtility.installBCProvider();

    	createStringEncrypter();

    	createConnectionProvider();
        configurationFactory.create();

        LdapEntryManager localLdapEntryManager = (LdapEntryManager) Component.getInstance(LDAP_ENTRY_MANAGER_NAME, true);
        List<GluuLdapConfiguration> ldapAuthConfigs = loadLdapAuthConfigs(localLdapEntryManager);
        createAuthConnectionProviders(ldapAuthConfigs);
        
        setDefaultAuthenticationMethod(localLdapEntryManager);

        addSecurityProviders();
        PythonService.instance().initPythonInterpreter();
    }

	@Observer("org.jboss.seam.postInitialization")
	@Asynchronous
    public void postInitialization() {
		List<CustomScriptType> supportedCustomScriptTypes = Arrays.asList(CustomScriptType.PERSON_AUTHENTICATION, CustomScriptType.CLIENT_REGISTRATION,
				CustomScriptType.ID_GENERATOR, CustomScriptType.UMA_AUTHORIZATION_POLICY, CustomScriptType.APPLICATION_SESSION, CustomScriptType.DYNAMIC_SCOPE);
		CustomScriptManager.instance().init(supportedCustomScriptTypes);
        CustomScriptManagerMigrator.instance().migrateOldConfigurations();
	}

	private void createStringEncrypter() {
		String encodeSalt = configurationFactory.getCryptoConfigurationSalt();
    	
    	if (StringHelper.isEmpty(encodeSalt)) {
    		throw new ConfigurationException("Encode salt isn't defined");
    	}
    	
    	try {
    		StringEncrypter stringEncrypter = StringEncrypter.instance(encodeSalt);

    		Context applicationContext = Contexts.getApplicationContext();
			applicationContext.set("stringEncrypter", stringEncrypter);
		} catch (EncryptionException ex) {
    		throw new ConfigurationException("Failed to create StringEncrypter instance");
		}
	}

    @Observer("org.jboss.seam.postInitialization")
    public void initReloadTimer() {
		this.isActive = new AtomicBoolean(false);
		this.lastFinishedTime = System.currentTimeMillis();

		Events.instance().raiseTimedEvent(EVENT_TYPE, new TimerSchedule(1 * 60 * 1000L, DEFAULT_INTERVAL * 1000L));
    }

	@Observer(EVENT_TYPE)
	@Asynchronous
	public void reloadConfigurationTimerEvent() {
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
        LdapEntryManager localLdapEntryManager = (LdapEntryManager) Component.getInstance(LDAP_ENTRY_MANAGER_NAME, true);
		List<GluuLdapConfiguration> newLdapAuthConfigs = loadLdapAuthConfigs(localLdapEntryManager);
		
		if (!this.ldapAuthConfigs.equals(newLdapAuthConfigs)) {
			recreateLdapAuthEntryManagers(newLdapAuthConfigs);
		}

		setDefaultAuthenticationMethod(localLdapEntryManager);
	}

	private void addSecurityProviders() {
        try {
            final Provider[] providers = Security.getProviders();
            if (providers != null) {
                boolean hasBC = false;
                for (Provider p : providers) {
                    if (p.getName().equalsIgnoreCase("BC")) {
                        hasBC = true;
                    }
                }
                if (!hasBC) {
                    Security.addProvider(new BouncyCastleProvider());
                }
            }
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
        }
    }

    @Factory(value = LDAP_ENTRY_MANAGER_NAME, scope = ScopeType.APPLICATION, autoCreate = true)
    public LdapEntryManager createLdapEntryManager() {
        LdapEntryManager ldapEntryManager = new LdapEntryManager(new OperationsFacade(this.connectionProvider, this.bindConnectionProvider));
        log.debug("Created {0}: {1}", LDAP_ENTRY_MANAGER_NAME, ldapEntryManager);
        return ldapEntryManager;
    }

	@Factory(value = LDAP_AUTH_ENTRY_MANAGER_NAME, scope = ScopeType.APPLICATION, autoCreate = true)
	public List<LdapEntryManager> createLdapAuthEntryManager() {
		List<LdapEntryManager> ldapAuthEntryManagers = new ArrayList<LdapEntryManager>();
		if (this.ldapAuthConfigs.size() == 0) {
			return ldapAuthEntryManagers;
		}

		for (int i = 0; i < this.ldapAuthConfigs.size(); i++) {
			LdapEntryManager ldapAuthEntryManager = new LdapEntryManager(new OperationsFacade(this.authConnectionProviders.get(i), this.authBindConnectionProviders.get(i)));
	        log.debug("Created {0}#{1}: {2}", LDAP_AUTH_ENTRY_MANAGER_NAME, i, ldapAuthEntryManager);
	        
	        ldapAuthEntryManagers.add(ldapAuthEntryManager);
		}

		return ldapAuthEntryManagers;
	}

    @Observer(ConfigurationFactory.LDAP_CONFIGUARION_RELOAD_EVENT_TYPE)
    public void recreateLdapEntryManager() {
    	// Backup current references to objects to allow shutdown properly
    	LdapEntryManager oldLdapEntryManager = (LdapEntryManager) Component.getInstance(LDAP_ENTRY_MANAGER_NAME);

    	// Recreate components
    	createConnectionProvider();

        // Destroy old components
    	Contexts.getApplicationContext().remove(LDAP_ENTRY_MANAGER_NAME);
    	oldLdapEntryManager.destroy();

    	log.debug("Destroyed {0}: {1}", LDAP_ENTRY_MANAGER_NAME, oldLdapEntryManager);
    }

    public void recreateLdapAuthEntryManagers(List<GluuLdapConfiguration> newLdapAuthConfigs) {
    	// Backup current references to objects to allow shutdown properly
    	List<LdapEntryManager> oldLdapAuthEntryManagers = (List<LdapEntryManager>) Component.getInstance(LDAP_AUTH_ENTRY_MANAGER_NAME);

    	// Recreate components
        createAuthConnectionProviders(newLdapAuthConfigs);

        // Destroy old components
    	Contexts.getApplicationContext().remove(LDAP_AUTH_ENTRY_MANAGER_NAME);

		for (LdapEntryManager oldLdapAuthEntryManager : oldLdapAuthEntryManagers) {
			oldLdapAuthEntryManager.destroy();
	        log.debug("Destroyed {0}: {1}", LDAP_AUTH_ENTRY_MANAGER_NAME, oldLdapAuthEntryManager);
		}
    }

	private void destroyLdapConnectionService(LdapConnectionService connectionProvider) {
		if (connectionProvider != null) {
			connectionProvider.closeConnectionPool();
	        log.debug("Destoryed connectionProvider: {1}", connectionProvider);
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
		Contexts.getApplicationContext().set(LDAP_AUTH_CONFIG_NAME, newLdapAuthConfigs);

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
		EncryptionService securityService = EncryptionService.instance();
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
				log.error("Failed to create object by json: '{0}'", ex, configurationJson);
			}
		}

		return configurations;
	}

	private void setDefaultAuthenticationMethod(LdapEntryManager localLdapEntryManager) {
		GluuAppliance appliance = loadAppliance(localLdapEntryManager, "oxAuthenticationMode");

		String authenticationMode = null;
		if (appliance != null) {
			authenticationMode = appliance.getAuthenticationMode();
		}

		Contexts.getApplicationContext().set(DEFAULT_AUTH_MODE_NAME, authenticationMode);
	}

	private GluuAppliance loadAppliance(LdapEntryManager localLdapEntryManager, String ... ldapReturnAttributes) {
		String baseDn = ConfigurationFactory.instance().getBaseDn().getAppliance();
		String applianceInum = ConfigurationFactory.instance().getConfiguration().getApplianceInum();
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
			if (configuration.getType().equalsIgnoreCase("ldap")) {
				return mapOldLdapConfig(configuration);
			} else if (configuration.getType().equalsIgnoreCase("auth")) {
				return mapLdapConfig(configuration.getConfig());
			}
		} catch (Exception ex) {
			log.error("Failed to create object by oxIDPAuthConf: '{0}'", ex, configuration);
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
			if (ldapAuthConfig != null) {
				ldapAuthConfigs.add(ldapAuthConfig);
			}
		}
		
		return ldapAuthConfigs; 
	}

	@Deprecated
	// Remove it after 2013/10/01
	private GluuLdapConfiguration mapOldLdapConfig(oxIDPAuthConf oneConf) {
		GluuLdapConfiguration ldapConfig = new GluuLdapConfiguration();
		ldapConfig.setServers(Arrays.asList(
				new SimpleProperty(oneConf.getFields().get(0).getValues().get(0) + ":" + oneConf.getFields().get(1).getValues().get(0))));
		ldapConfig.setBindDN(oneConf.getFields().get(2).getValues().get(0));
		ldapConfig.setBindPassword(oneConf.getFields().get(3).getValues().get(0));
		ldapConfig.setUseSSL(Boolean.valueOf(oneConf.getFields().get(4).getValues().get(0)));
		ldapConfig.setMaxConnections(3);
		ldapConfig.setConfigId("auth_ldap_server");
		ldapConfig.setEnabled(oneConf.getEnabled());
		return ldapConfig;
	}

	private GluuLdapConfiguration mapLdapConfig(String config) throws Exception {
		return (GluuLdapConfiguration) jsonToObject(config, GluuLdapConfiguration.class);
	}

	private Object jsonToObject(String json, Class<?> clazz) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Object clazzObject = mapper.readValue(json, clazz);

		return clazzObject;
	}

	public static AppInitializer instance() {
        return ServerUtil.instance(AppInitializer.class);
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