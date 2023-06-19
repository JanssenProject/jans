/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.service;

import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.plugin.saml.model.config.KeycloakConfig;
import io.jans.orm.PersistenceEntryManager;

import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import io.jans.configapi.plugin.saml.model.SamlClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.*;
import jakarta.ws.rs.core.Response;



@ApplicationScoped
public class SamlService {

    @Inject
    Logger logger;

    @Inject
    PersistenceEntryManager persistenceManager;

    @Inject
    ConfigurationFactory configurationFactory;
    
    @Inject
    KeycloakConfig keycloakConfig;
    
    public RealmResource getRealmResource(String realm) {
        logger.error("Get RealmResource for realm:{})", realm);
        if(StringUtils.isBlank(realm)) {
            realm = "master";
        }
        RealmResource realmResource = keycloakConfig.getInstance().realm(realm);
        logger.error("realmResource:{})", realmResource);
        return realmResource;
    }
    
    
    public ClientsResource getClientsResource(String realm) {
        logger.error("Get ClientsResource for realm:{})", realm);
        RealmResource realmResource = this.getRealmResource(realm);
        logger.error("realmResource:{})", realmResource);
        
        ClientsResource clientsResource = realmResource.clients();
        logger.error(" clientsResource:{})", clientsResource);
        
        return clientsResource;
    }
    
    
    public List<ClientRepresentation> getClient(String realm)  {
        RealmResource realmResource = this.getRealmResource(realm);
        logger.error("Searching by realmResource:{})", realmResource);
        
        ClientsResource clientsResource = realmResource.clients();
        logger.error("Searching by clientsResource:{})", clientsResource);
        
        List<ClientRepresentation>  clientList = clientsResource.findAll();
     
        logger.error("All clientList:{}", clientList);
        return clientList;
    }

    public ClientRepresentation createClient(ClientRepresentation clientRepresentation) throws Exception {
        logger.error(" SamlService::createClient() - clientRepresentation:{}",clientRepresentation);
             
        //Get Keycloak/SAML implmentation instance
        Keycloak keycloak = keycloakConfig.getInstance();
        
        logger.error(" SamlService::createClient() - keycloak:{}",keycloak);

        RealmResource realmResource = this.getRealmResource(null);
        logger.error("Create client in realmResource:{})", realmResource);
        
        ClientsResource clientsResource = realmResource.clients();
        logger.error("clientsResource:{})", clientsResource);
        
        // Create client (requires manage-users role)
        Response response = clientsResource.create(clientRepresentation);
        logger.error(" SamlService::createClient() - response:{}",response);
        
        logger.error("Repsonse: %s %s%n", response.getStatus(), response.getStatusInfo());
        logger.error("response.getLocation():{}",response.getLocation());
        String clienId = CreatedResponseUtil.getCreatedId(response);

        logger.error(" SamlService::createClient() - response.getStatus():{}, response.getStatusInfo():{}, response.getLocation():{}, clienId:{}",response.getStatus(), response.getStatusInfo(), response.getLocation(),clienId);
        System.out.printf("User created with clienId: %s%n", clienId);

     
        
        return clientRepresentation;
    }
}
