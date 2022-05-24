/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.custom.script.test;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;
import io.jans.log.LoggingHelper;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.ldap.impl.LdapEntryManager;
import io.jans.orm.ldap.impl.LdapEntryManagerFactory;
import io.jans.service.custom.script.StandaloneCustomScriptManager;

public class StandaloneCustomScriptManagerTest {

    private static final Logger LOG = Logger.getLogger(StandaloneCustomScriptManagerTest.class);

    static {
        StatusLogger.getLogger().setLevel(Level.OFF);
        LoggingHelper.configureConsoleAppender();
    }

    private static Properties getSampleConnectionProperties() {
        Properties connectionProperties = new Properties();

        connectionProperties.put("ldap.bindDN", "cn=Directory Manager");
        connectionProperties.put("ldap.bindPassword", "secret");
//        connectionProperties.put("ldap.bindPassword", "test");
        connectionProperties.put("ldap.servers", "localhost:1636");
        connectionProperties.put("ldap.useSSL", "true");
        connectionProperties.put("ldap.maxconnections", "3");

        return connectionProperties;
    }

    public static LdapEntryManager createLdapEntryManager() {
        LdapEntryManagerFactory ldapEntryManagerFactory = new LdapEntryManagerFactory();
        Properties connectionProperties = getSampleConnectionProperties();

        LdapEntryManager ldapEntryManager = ldapEntryManagerFactory.createEntryManager(connectionProperties);
        LOG.debug("Created LdapEntryManager: " + ldapEntryManager);

        return ldapEntryManager;
    }

	public static void main(String[] args) {
		if (System.getenv("PYTHON_HOME") == null) {
			System.err.println("PYTHON_HOME environment variable is not defined");
			System.exit(-1);
		}

		PersistenceEntryManager persistenceEntryManager = createLdapEntryManager();
		StandaloneCustomScriptManager customScriptManager = new StandaloneCustomScriptManager(persistenceEntryManager, "ou=scripts,o=jans", "/opt/gluu/python/libs");

		// Register required external scripts
		SampleIdpExternalScriptService sampleIdpExternalScriptService = new SampleIdpExternalScriptService();
		customScriptManager.registerExternalScriptService(sampleIdpExternalScriptService);

		// Init script manager and load scripts
		customScriptManager.init();

		// Call script
		Object context = new SampleContext(System.currentTimeMillis());
		sampleIdpExternalScriptService.executeExternalUpdateAttributesMethods(context);

		// Reload script if needed
		customScriptManager.reload();

		// Call script
		Object context2 = new SampleContext(System.currentTimeMillis());
		sampleIdpExternalScriptService.executeExternalUpdateAttributesMethods(context2);

		// Destroy custom script manager and scripts
		customScriptManager.destory();
	}

}
