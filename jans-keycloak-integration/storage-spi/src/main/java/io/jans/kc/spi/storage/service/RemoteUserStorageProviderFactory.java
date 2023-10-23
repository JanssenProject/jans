package io.jans.kc.spi.storage.service;

import io.jans.kc.spi.storage.config.PluginConfiguration;
import io.jans.kc.spi.storage.util.Constants;

import org.jboss.logging.Logger;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.storage.UserStorageProviderFactory;




public class RemoteUserStorageProviderFactory implements UserStorageProviderFactory<RemoteUserStorageProvider> {

    private static Logger log = Logger.getLogger(RemoteUserStorageProviderFactory.class);

    public static final String PROVIDER_NAME = "jans-keycloak-storage-api";
    private PluginConfiguration pluginConfiguration;

    @Override
    public RemoteUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        log.debugv("RemoteUserStorageProviderFactory::create() - session:{}, model:{}", session, model);
        return new RemoteUserStorageProvider(session, model,pluginConfiguration);
    }

    @Override
    public String getId() {
        
        return Constants.PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Janssen User Storage Provider";
    }

    @Override
    public void init(Config.Scope config) {
       
        this.pluginConfiguration = PluginConfiguration.fromKeycloakConfiguration(config);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        log.debug("RemoteUserStorageProviderFactory::postInit()");
    }

    @Override
    public void close() {
        log.debug("RemoteUserStorageProviderFactory::close() - Exit");
    }

}
