/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package io.jans.ca.server;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.jans.ca.server.guice.GuiceModule;
import io.jans.ca.server.persistence.service.PersistenceService;
import io.jans.ca.server.service.ConfigurationService;
import io.jans.ca.server.service.MigrationService;
import io.jans.ca.server.service.RpService;
import io.jans.ca.server.service.SchedulerService;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static Injector INJECTOR = Guice.createInjector(new GuiceModule());
    private static boolean setUpSuite = false;

    public static void configureServices(RpServerConfiguration configuration) {
        LOG.info("Starting service configuration...");
        printBuildNumber();
        addSecurityProviders();
        registerResteasyProviders();

        try {
            LOG.info("Configuration: " + configuration);
            INJECTOR.getInstance(ConfigurationService.class).setConfiguration(configuration);
            INJECTOR.getInstance(PersistenceService.class).create();
            INJECTOR.getInstance(RpService.class).load();
            INJECTOR.getInstance(MigrationService.class).migrate();
            INJECTOR.getInstance(SchedulerService.class).scheduleTasks();
            LOG.info("jans_client_api Services are configured successfully.");
        } catch (Throwable e) {
            LOG.error("Failed to start jans_client_api server.", e);
            if (!isSetUpSuite()) {
                System.exit(1);
            }
        }
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
                    ", build time:" + properties.getProperty("git.build.time") + ", oxd_version:" + Utils.getOxdVersion());
        }
    }

    private static void registerResteasyProviders() {
//        final ResteasyProviderFactory instance = ResteasyProviderFactory.getInstance();
//        instance.registerProvider(ResteasyJacksonProvider.class);
//        RegisterBuiltin.register(instance);
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
        shutdown(true);
    }

    public static void shutdown(boolean systemExit) {
        LOG.info("Stopping the server...");
        try {
            INJECTOR.getInstance(PersistenceService.class).destroy();
        } catch (Throwable e) {
            // ignore, we may shut down server before it persistence service is initialize (e.g. due to invalid license)
        }
        LOG.info("Stopped the server successfully.");
        if (systemExit) {
            System.exit(0);
        }
    }

    public static Injector getInjector() {
        return INJECTOR;
    }

    public static void setInjector(AbstractModule module) {
        INJECTOR = Guice.createInjector(module);
    }

    public static boolean isSetUpSuite() {
        return setUpSuite;
    }

    public static void setSetUpSuite(boolean setUpSuite) {
        ServerLauncher.setUpSuite = setUpSuite;
    }
}
