package io.jans.configapi.plugin.saml.model.config;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.keycloak.admin.client.Keycloak;

@ApplicationScoped
public class KeycloakConfig {

    private static Logger log = LoggerFactory.getLogger(KeycloakConfig.class);
        
    @Inject
    SamlConfigSource samlConfigSource;


    public Keycloak getInstance() {
        log.info("Keycloak instance entry - samlConfigSource:{}", samlConfigSource);
        log.info("samlConfigSource.getProperties():{}, samlConfigSource.getPropertyNames():{} ", samlConfigSource.getProperties(), samlConfigSource.getPropertyNames());
        log.info("Keycloak instance entry - samlConfigSource:{}", samlConfigSource);
        log.info("Keycloak instance entry - samlConfigSource.getValue(\"saml.server.url\"):{}", samlConfigSource.getValue("saml.server.url"));
        log.info("Keycloak instance entry - samlConfigSource.getValue(\"saml.realm.name\"):{}", samlConfigSource.getValue("saml.realm.name"));
        log.info("Keycloak instance entry - samlConfigSource.getValue(\"saml.client.id\"):{}", samlConfigSource.getValue("saml.client.id"));
        log.info("Keycloak instance entry - samlConfigSource.getValue(\"saml.grant.type\"):{}", samlConfigSource.getValue("saml.grant.type"));
        log.info("Keycloak instance entry - samlConfigSource.getValue(\"saml.admin.username\"):{}", samlConfigSource.getValue("saml.admin.username"));
        log.info("Keycloak instance entry - samlConfigSource.getValue(\"saml.admin.password\"):{}", samlConfigSource.getValue("saml.admin.password"));
        log.info("Keycloak instance entry - samlConfigSource.getValue(\"saml.client.id\"):{}", samlConfigSource.getValue("saml.client.id"));
        log.info("Keycloak instance entry - samlConfigSource.getValue(\"saml.client.secret\"):{}", samlConfigSource.getValue("saml.client.secret"));
        return getInstance(samlConfigSource.getValue("saml.server.url"), samlConfigSource.getValue("saml.realm.name"), samlConfigSource.getValue("saml.admin.username"), samlConfigSource.getValue("saml.admin.password"), samlConfigSource.getValue("saml.client.id"), samlConfigSource.getValue("saml.client.secret"));
      }
    
    public Keycloak getInstance(String serverUrl, String realm, String username, String password, String clientId, String clientSecret) {
        log.info("Keycloak instance param serverUrl:{}, realm:{}, username:{}, password:{}, clientId:{}, clientSecret:{} ",serverUrl, realm, username, password, clientId, clientSecret);
        Keycloak keycloak = Keycloak.getInstance(serverUrl,  realm,  username,  password,  clientId, clientSecret);
        log.info("keycloak:{} ", keycloak);       
        return keycloak;
      }
    
    
}
