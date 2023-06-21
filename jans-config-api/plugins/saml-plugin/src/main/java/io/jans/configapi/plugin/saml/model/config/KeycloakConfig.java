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

    private static Logger log = LoggerFactory.getLogger(SamlConfigSource.class);
    
    private static String serverUrl= "http://localhost:8180";
    private static String realm = "master";
    private static String clientId = "jans-client-1";
    private static String clientSecret = "yxsKi8ah9pU7ANyjH7HBwJh4XTLYN4x3";
    private static String grantType = OAuth2Constants.PASSWORD;
    private static String username = "admin1";
    private static String password = "admin123";

        
    @Inject
    SamlConfigSource samlConfigSource;


    public Keycloak getInstance() {
        log.error("Keycloak instance entry - samlConfigSource:{}", samlConfigSource);
        log.error("samlConfigSource.getProperties():{}, samlConfigSource.getPropertyNames():{} ", samlConfigSource.getProperties(), samlConfigSource.getPropertyNames());
        log.error("Keycloak instance entry - samlConfigSource:{}", samlConfigSource);
        log.error("Keycloak instance entry - samlConfigSource.getValue(\"saml.server.url\"):{}", samlConfigSource.getValue("saml.server.url"));
        log.error("Keycloak instance entry - samlConfigSource.getValue(\"saml.realm.name\"):{}", samlConfigSource.getValue("saml.realm.name"));
        log.error("Keycloak instance entry - samlConfigSource.getValue(\"saml.client.id\"):{}", samlConfigSource.getValue("saml.client.id"));
        log.error("Keycloak instance entry - samlConfigSource.getValue(\"saml.grant.type\"):{}", samlConfigSource.getValue("saml.grant.type"));
        log.error("Keycloak instance entry - samlConfigSource.getValue(\"saml.admin.username\"):{}", samlConfigSource.getValue("saml.admin.username"));
        log.error("Keycloak instance entry - samlConfigSource.getValue(\"saml.admin.password\"):{}", samlConfigSource.getValue("saml.admin.password"));
        log.error("Keycloak instance entry - samlConfigSource.getValue(\"saml.client.id\"):{}", samlConfigSource.getValue("saml.client.id"));
        log.error("Keycloak instance entry - samlConfigSource.getValue(\"saml.client.secret\"):{}", samlConfigSource.getValue("saml.client.secret"));
        return getInstance(samlConfigSource.getValue("saml.server.url"), realm, username, password, clientId, clientSecret);
      }
    
    public Keycloak getInstance(String serverUrl, String realm, String username, String password, String clientId, String clientSecret) {
        log.error("Keycloak instance param serverUrl:{}, realm:{}, username:{}, password:{}, clientId:{}, clientSecret:{} ",serverUrl, realm, username, password, clientId, clientSecret);
        log.error("samlConfigSource.getProperties():{}, samlConfigSource.getPropertyNames():{} ", samlConfigSource.getProperties(), samlConfigSource.getPropertyNames());
        
        Keycloak keycloak = Keycloak.getInstance(serverUrl,  realm,  username,  password,  clientId, clientSecret);
        log.error("keycloak:{} ", keycloak);
       
        return keycloak;
      }
    
    
}
