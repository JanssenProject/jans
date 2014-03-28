package org.xdi.oxauth.service;

import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.configuration.ConfigurationException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.gluu.site.ldap.OperationsFacade;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.gluu.site.ldap.persistence.exception.LdapMappingException;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Factory;
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
import org.xdi.model.SimpleProperty;
import org.xdi.model.ldap.GluuLdapConfiguration;
import org.xdi.oxauth.model.appliance.GluuAppliance;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.config.oxIDPAuthConf;
import org.xdi.service.PythonService;
import org.xdi.service.ldap.LdapConnectionService;
import org.xdi.oxauth.util.FileConfiguration;
import org.xdi.util.StringHelper;
import org.xdi.util.security.PropertiesDecrypter;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version 0.1, 10.24.2011
 */
@Scope(ScopeType.APPLICATION)
@Name("appInitializer")
@Startup()
public class AppInitializer {

	private final static String EVENT_TYPE = "AppInitializerTimerEvent";
    private final static int DEFAULT_INTERVAL = 30; // 30 seconds

    public  static final String LDAP_AUTH_CONFIG_NAME = "ldapAuthConfig";

    public  static final String LDAP_ENTRY_MANAGER_NAME = "ldapEntryManager";
    public  static final String LDAP_AUTH_ENTRY_MANAGER_NAME = "ldapAuthEntryManager";

    @Logger
    private Log log;

    private AtomicBoolean isActive;
	private long lastFinishedTime;

    @Create
    public void createApplicationComponents() throws ConfigurationException {
        createConnectionProvider("centralLdapConfiguration", "centralConnectionProvider", "bindCentralConnectionProvider");
        ConfigurationFactory.create();

        GluuLdapConfiguration ldapAuthConfig = loadLdapAuthConfig((LdapEntryManager) Component.getInstance(LDAP_ENTRY_MANAGER_NAME, true));
        reloadConfigurationImpl(ldapAuthConfig);

        createAuthConnectionProvider(ldapAuthConfig, "authConnectionProvider", "bindAuthConnectionProvider");
        
        addSecurityProviders();
        PythonService.instance().initPythonInterpreter();
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
		GluuLdapConfiguration newLdapAuthConfig = loadLdapAuthConfig((LdapEntryManager) Component.getInstance(LDAP_ENTRY_MANAGER_NAME, true));

		reloadConfigurationImpl(newLdapAuthConfig);
	}

	private void reloadConfigurationImpl(GluuLdapConfiguration newLdapAuthConfig) {
		Context applicationContext = Contexts.getApplicationContext();
        if (newLdapAuthConfig == null) {
        	if (applicationContext.isSet(LDAP_AUTH_CONFIG_NAME)) {
        		applicationContext.remove(LDAP_AUTH_CONFIG_NAME);
        	}
        } else {
        	applicationContext.set(LDAP_AUTH_CONFIG_NAME, newLdapAuthConfig);
        }
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
        LdapEntryManager ldapEntryManager = new LdapEntryManager(new OperationsFacade(getConnectionProvider(), getBindConnectionProvider()));
        log.debug("Created {0}: {1}", LDAP_ENTRY_MANAGER_NAME, ldapEntryManager);
        return ldapEntryManager;
    }

	@Factory(value = LDAP_AUTH_ENTRY_MANAGER_NAME, scope = ScopeType.APPLICATION, autoCreate = true)
	public LdapEntryManager createLdapAuthEntryManager() {
		LdapEntryManager ldapEntryManager = new LdapEntryManager(new OperationsFacade(getAuthConnectionProvider(), getBindAuthConnectionProvider()));
        log.debug("Created {0}: {1}", LDAP_AUTH_ENTRY_MANAGER_NAME, ldapEntryManager);

		return ldapEntryManager;
	}

    private LdapConnectionService getAuthConnectionProvider() {
        return (LdapConnectionService) Contexts.getApplicationContext().get("authConnectionProvider");
    }

    private LdapConnectionService getBindAuthConnectionProvider() {
        return (LdapConnectionService) Contexts.getApplicationContext().get("bindAuthConnectionProvider");
    }

    private LdapConnectionService getConnectionProvider() {
        return (LdapConnectionService) Contexts.getApplicationContext().get("centralConnectionProvider");
    }

    private LdapConnectionService getBindConnectionProvider() {
        return (LdapConnectionService) Contexts.getApplicationContext().get("bindCentralConnectionProvider");
    }

    private void createConnectionProvider(String configurationComponentName, String connectionProviderComponentName,
                                          String bindConnectionProviderComponentName) throws ConfigurationException {
        FileConfiguration configuration = ConfigurationFactory.getLdapConfiguration();
        Contexts.getApplicationContext().set(configurationComponentName, configuration);

        LdapConnectionService connectionProvider = new LdapConnectionService(PropertiesDecrypter.decryptProperties(configuration.getProperties()));
        Contexts.getApplicationContext().set(connectionProviderComponentName, connectionProvider);

        // TODO: Use own properties with prefix specified in variable 'bindConfigurationComponentName'
        Properties bindProperties = (Properties) configuration.getProperties().clone();
        bindProperties.remove("bindDN");
        bindProperties.remove("bindPassword");

        LdapConnectionService bindCentralConnectionProvider = new LdapConnectionService(PropertiesDecrypter.decryptProperties(bindProperties));
        Contexts.getApplicationContext().set(bindConnectionProviderComponentName, bindCentralConnectionProvider);
    }

    private void createAuthConnectionProvider(GluuLdapConfiguration ldapAuthConfig, String connectionProviderComponentName,
                                          String bindConnectionProviderComponentName) throws ConfigurationException {
        FileConfiguration configuration = ConfigurationFactory.getLdapConfiguration();
        Properties properties = (Properties) configuration.getProperties().clone();
        if (ldapAuthConfig != null) {
            properties.setProperty("servers", buildServersString(ldapAuthConfig.getServers()));
    		properties.setProperty("bindDN", ldapAuthConfig.getBindDN());
    		properties.setProperty("bindPassword", ldapAuthConfig.getBindPassword());
    		properties.setProperty("useSSL", Boolean.toString(ldapAuthConfig.isUseSSL()));
        }

        LdapConnectionService connectionProvider = new LdapConnectionService(PropertiesDecrypter.decryptProperties(properties));
        Contexts.getApplicationContext().set(connectionProviderComponentName, connectionProvider);

        // TODO: Use own properties with prefix specified in variable 'bindConfigurationComponentName'
        Properties bindProperties = (Properties) properties.clone();
        bindProperties.remove("bindDN");
        bindProperties.remove("bindPassword");

        LdapConnectionService bindCentralConnectionProvider = new LdapConnectionService(PropertiesDecrypter.decryptProperties(bindProperties));
        Contexts.getApplicationContext().set(bindConnectionProviderComponentName, bindCentralConnectionProvider);
    }

	private String buildServersString(List<SimpleProperty> servers) {
		StringBuilder sb = new StringBuilder();

		if (servers == null) {
			return sb.toString();
		}
		
		boolean first = true;
		for (SimpleProperty server : servers) {
			if (first) {
				first = false;
			} else {
				sb.append(",");
			}

			sb.append(server.getValue());
		}

		return sb.toString();
	}

	public oxIDPAuthConf loadLdapIdpAuthConfig(LdapEntryManager localLdapEntryManager) {
		String baseDn = ConfigurationFactory.getBaseDn().getAppliance();
		String applianceInum = ConfigurationFactory.getConfiguration().getApplianceInum();
		if (StringHelper.isEmpty(baseDn) || StringHelper.isEmpty(applianceInum)) {
			return null;
		}

		String applianceDn = String.format("inum=%s,%s", applianceInum, baseDn);

		GluuAppliance appliance = null;
		try {
			appliance = localLdapEntryManager.find(GluuAppliance.class, applianceDn);
		} catch (LdapMappingException ex) {
			log.error("Failed to load appliance entry from Ldap", ex);
			return null;
		}

		if ((appliance == null) || (appliance.getOxIDPAuthentication() == null)) {
			return null;
		}

		for (String configurationJson : appliance.getOxIDPAuthentication()) {
			oxIDPAuthConf configuration = null;

			try {
				configuration = (oxIDPAuthConf) jsonToObject(configurationJson, oxIDPAuthConf.class);
				if (configuration.getType().equalsIgnoreCase("ldap") || configuration.getType().equalsIgnoreCase("auth")) {
					return configuration;
				}
			} catch (Exception ex) {
				log.error("Failed to create object by json: '{0}'", ex, configurationJson);
			}
		}

		return null;
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

	public GluuLdapConfiguration loadLdapAuthConfig(LdapEntryManager localLdapEntryManager) {
		return (loadLdapAuthConfig(loadLdapIdpAuthConfig(localLdapEntryManager)));
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

}