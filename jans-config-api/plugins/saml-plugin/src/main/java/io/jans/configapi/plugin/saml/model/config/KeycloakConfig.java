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
    private static String clientId = "https://samltest.id/saml/sp";
    private static String grantType = OAuth2Constants.PASSWORD;
    private static String username = "admin1";
    private static String password = "admin123";

        
    @Inject
    SamlConfigSource samlConfigSource;
    
    public Keycloak getInstance() {
        log.error("Keycloak instance entry - samlConfigSource:{}", samlConfigSource);
        log.error("samlConfigSource.getProperties():{}, samlConfigSource.getPropertyNames():{} ", samlConfigSource.getProperties(), samlConfigSource.getPropertyNames());
        return getInstance(serverUrl, realm, username, password, clientId, grantType);
      }
    
    public Keycloak getInstance(String serverUrl, String realm, String username, String password, String clientId,String grantType) {
        log.error("Keycloak instance param serverUrl:{}, realm:{}, username:{}, password:{}, clientId:{}, grantType:{} ",serverUrl, realm, username, password, clientId, grantType);
        log.error("samlConfigSource.getProperties():{}, samlConfigSource.getPropertyNames():{} ", samlConfigSource.getProperties(), samlConfigSource.getPropertyNames());
        
        Keycloak keycloak = Keycloak.getInstance(serverUrl,  realm,  username,  password,  clientId);
        log.error("keycloak:{} ", keycloak);
        
        /*
         * ServerInfoResource serverInfoResource = keycloak.serverInfo();
         * log.error("serverInfoResource:{} ", serverInfoResource);
         */
        /*
         * ServerInfoRepresentation serverInfoRepresentation =
         * serverInfoResource.getInfo(); log.error("serverInfoRepresentation:{} ",
         * serverInfoRepresentation);
         */  
        return keycloak;
      }
}
