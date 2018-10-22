/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server;

import com.google.common.base.Strings;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.xml.DOMConfigurator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJacksonProvider;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.server.guice.GuiceModule;
import org.xdi.oxd.server.persistence.PersistenceService;
import org.xdi.oxd.server.service.ConfigurationService;
import org.xdi.oxd.server.service.MigrationService;
import org.xdi.oxd.server.service.RpService;
import org.xdi.oxd.server.service.SocketService;

import java.io.File;
import java.io.InputStream;
import java.security.Provider;
import java.security.Security;
import java.util.Properties;

/**
 * Server launcher.
 *
 * @author Yuriy Zabrovarnyy
 */
public class ServerLauncher {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ServerLauncher.class);

    private static final Injector INJECTOR = Guice.createInjector(new GuiceModule());
    private static boolean setUpSuite = false;

    /**
     * Main method.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        try {
            start();
        } catch (Throwable e) {
            LOG.error("oxd-server start failed.", e);
            System.exit(1);
        }
    }

    public static void start() {
        configureLogger();
        LOG.info("Starting...");
        printBuildNumber();
        addSecurityProviders();
        registerResteasyProviders();
        checkConfiguration();

        startOxd();
    }

    public static Properties buildProperties() {
        InputStream is = null;
        try {
            is = ClassLoader.getSystemClassLoader().getResourceAsStream("git.properties");
            Properties properties = new Properties();
            properties.load(is);
            return properties;
        } catch (Exception e) {
            LOG.warn("Unable to read git.properties and print build number, " + e.getMessage());
            return null;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private static void printBuildNumber() {
        Properties properties = buildProperties();
        if (properties != null) {
            LOG.info("commit: " + properties.getProperty("git.commit.id") + ", branch: " + properties.getProperty("git.branch") +
                    ", build time:" + properties.getProperty("git.build.time"));
        }
    }

    private static void startOxd() {
        try {
            INJECTOR.getInstance(ConfigurationService.class).load();
            INJECTOR.getInstance(PersistenceService.class).create();
            INJECTOR.getInstance(RpService.class).load();
            INJECTOR.getInstance(MigrationService.class).migrate();
            INJECTOR.getInstance(SocketService.class).listenSocket();
            LOG.info("oxD Server started successfully.");
        } catch (ShutdownException e) {
            LOG.error("Shut down oxd server.", e);
        } catch (Throwable e) {
            LOG.error("Failed to start oxd server.", e);
            if (!isSetUpSuite()) {
                System.exit(1);
            }
        }
    }

    private static void checkConfiguration() {
        final String confProperty = System.getProperty(ConfigurationService.CONF_SYS_PROPERTY_NAME);
        if (!Strings.isNullOrEmpty(confProperty)) {
            if (new File(confProperty).exists()) {
                return; // configuration exists and can be read
            } else {
                throw new AssertionError("Failed to start oxd, system property " +
                        ConfigurationService.CONF_SYS_PROPERTY_NAME + " points to absent/empty file: " + confProperty);
            }
        }
        throw new AssertionError("Failed to start oxd, system property " +
                ConfigurationService.CONF_SYS_PROPERTY_NAME + " is not specified. (Please define it as -D" +
                ConfigurationService.CONF_SYS_PROPERTY_NAME + "=<path to oxd-conf.json>)");
    }

    private static void configureLogger() {
        final String propertyFile = System.getProperty("log4j.configuration");
        if (StringUtils.isNotBlank(propertyFile)) {
            DOMConfigurator.configure(propertyFile);
        }
    }

    private static void registerResteasyProviders() {
        final ResteasyProviderFactory instance = ResteasyProviderFactory.getInstance();
        instance.registerProvider(ResteasyJacksonProvider.class);
        RegisterBuiltin.register(instance);
    }

    private static void addSecurityProviders() {
        try {
            final Provider[] providers = Security.getProviders();
            if (providers != null) {
                boolean hasBC = false;
                for (Provider p : providers) {
                    if (p.getName().equalsIgnoreCase("BC")) {
                        hasBC = true;
                    }
                }
                LOG.debug("BC registered: " + hasBC);
                if (!hasBC) {
                    Security.addProvider(new BouncyCastleProvider());
                    LOG.debug("Registered BC successfully.");
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public static void shutdown() {
        INJECTOR.getInstance(PersistenceService.class).destroy();
        INJECTOR.getInstance(SocketService.class).shutdownNow();
    }

    public static Injector getInjector() {
        return INJECTOR;
    }

    public static boolean isSetUpSuite() {
        return setUpSuite;
    }

    public static void setSetUpSuite(boolean setUpSuite) {
        ServerLauncher.setUpSuite = setUpSuite;
    }
}
