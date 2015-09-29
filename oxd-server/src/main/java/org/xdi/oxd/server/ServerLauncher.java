/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server;

import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.xml.DOMConfigurator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJacksonProvider;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.server.jetty.JettyServer;

import java.io.File;
import java.security.Provider;
import java.security.Security;

/**
 * Server launcher.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 27/07/2013
 */
public class ServerLauncher {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ServerLauncher.class);

    private static final SocketService SOCKET_SERVICE = SocketService.getInstance();

    /**
     * Main method.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        start();
    }

    public static void start() {
        configureLogger();
        LOG.info("Starting...");
        addSecurityProviders();
        registerResteasyProviders();
        checkConfiguration();
        Configuration configuration = SOCKET_SERVICE.listenSocket();
        startJetty(configuration);
    }

    private static void checkConfiguration() {
        final String confProperty = System.getProperty(Configuration.CONF_SYS_PROPERTY_NAME);
        if (!Strings.isNullOrEmpty(confProperty)) {
            if (new File(confProperty).exists()) {
                return; // configuration exists and can be read
            } else {
                throw new AssertionError("Failed to start oxd, system property " +
                        Configuration.CONF_SYS_PROPERTY_NAME + " points to absent/empty file: " + confProperty);
            }
        }
        throw new AssertionError("Failed to start oxd, system property " +
                Configuration.CONF_SYS_PROPERTY_NAME + " is not specified. (Please defined it as -D" +
                Configuration.CONF_SYS_PROPERTY_NAME + "=<path to oxd-conf.json>)");
    }

    private static void startJetty(Configuration configuration) {
        if (configuration.isStartJetty()) {
            JettyServer server = new JettyServer(configuration.getJettyPort());
            server.start();
        }
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
                if (!hasBC) {
                    Security.addProvider(new BouncyCastleProvider());
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public static void shutdown() {
        SOCKET_SERVICE.shutdownNow();
    }
}
