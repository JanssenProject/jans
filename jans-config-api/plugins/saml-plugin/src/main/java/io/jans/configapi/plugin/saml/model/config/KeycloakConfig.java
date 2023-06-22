package io.jans.configapi.plugin.saml.model.config;

import io.jans.exception.ConfigurationException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Config;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.admin.client.resource.ServerInfoResource;
import org.keycloak.representations.info.ServerInfoRepresentation;



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
