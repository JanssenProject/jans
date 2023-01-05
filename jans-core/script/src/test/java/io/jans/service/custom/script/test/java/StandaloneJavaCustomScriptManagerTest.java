package io.jans.service.custom.script.test.java;

import io.jans.log.LoggingHelper;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.ldap.impl.LdapEntryManager;
import io.jans.orm.ldap.impl.LdapEntryManagerFactory;
import io.jans.service.custom.script.StandaloneCustomScriptManager;
import io.jans.service.custom.script.test.SampleContext;
import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;
import org.json.JSONObject;

import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 */
@SuppressWarnings("java:S2187")
public class StandaloneJavaCustomScriptManagerTest {

    private static final Logger LOG = Logger.getLogger(StandaloneJavaCustomScriptManagerTest.class);

    static {
        StatusLogger.getLogger().setLevel(Level.OFF);
        LoggingHelper.configureConsoleAppender();
    }

    private static Properties getSampleConnectionProperties() {
        Properties connectionProperties = new Properties();

        connectionProperties.put("ldap#bindDN", "cn=Directory Manager");
        connectionProperties.put("ldap#bindPassword", "secret");
        connectionProperties.put("ldap#servers", "localhost:1636");
        connectionProperties.put("ldap#useSSL", "true");
        connectionProperties.put("ldap#maxconnections", "3");

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
        PersistenceEntryManager persistenceEntryManager = createLdapEntryManager();
        StandaloneCustomScriptManager customScriptManager = new StandaloneCustomScriptManager(persistenceEntryManager, "ou=scripts,o=jans");

        try {
            // Register required external scripts
            SampleDiscoveryExternalScriptService javaScriptService = new SampleDiscoveryExternalScriptService();
            customScriptManager.registerExternalScriptService(javaScriptService);

            // Init script manager and load scripts
            customScriptManager.init();

            // Call script
            Object context = new SampleContext(System.currentTimeMillis());

            JSONObject jsonObject = new JSONObject("{}");
            assertTrue(javaScriptService.modifyDiscovery(jsonObject, context));

            assertEquals(jsonObject.getString("key_from_java"), "value_from_script_on_java");
        } finally {
            customScriptManager.destory();
        }
    }
}
