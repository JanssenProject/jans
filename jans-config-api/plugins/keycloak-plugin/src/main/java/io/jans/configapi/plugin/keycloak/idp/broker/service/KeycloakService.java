/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.service;

import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.plugin.saml.model.config.KeycloakConfig;
import io.jans.orm.PersistenceEntryManager;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.*;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;


import org.slf4j.Logger;

@ApplicationScoped
public class KeycloakService {

    @Inject
    Logger logger;

    @Inject
    PersistenceEntryManager persistenceManager;

    @Inject
    ConfigurationFactory configurationFactory;

    @Inject
    KeycloakConfig keycloakConfig;

    public RealmResource getRealmResource(String realm) {
        logger.info("Get RealmResource for realm:{})", realm);
        if (StringUtils.isBlank(realm)) {
            realm = "master";
        }
        RealmResource realmResource = keycloakConfig.getInstance().realm(realm);
        logger.info("realmResource:{})", realmResource);
        return realmResource;
    }

    public ClientsResource getClientsResource(String realm) {
        logger.info("Get ClientsResource for realm:{})", realm);
        RealmResource realmResource = this.getRealmResource(realm);
        logger.info("realm-resource:{})", realmResource);

        ClientsResource clientsResource = realmResource.clients();
        logger.info(" clientsResource:{})", clientsResource);

        return clientsResource;
    }

    public UsersResource getUsersResource(String realm) {
        logger.info("Get UsersResource for realm:{})", realm);
        RealmResource realmResource = this.getRealmResource(realm);
        logger.info("realmResource:{})", realmResource);

        UsersResource usersResource = realmResource.users();
        logger.info(" usersResource:{})", usersResource);

        return usersResource;
    }   

    public List<UserRepresentation> getAllUsers() {
        List<UserRepresentation> userList = getUsers(null);
        logger.info("All userList:{}", userList);
        return userList;
    }
    
    public List<UserRepresentation> getUsers(String realm) {
        logger.info("Fetching users in realm:{})", realm);
        List<UserRepresentation> userList = getUsersResource(null).list();
        logger.info("All userList:{}", userList);
        return userList;
    }

    public List<ClientRepresentation> getAllClients() {
        List<ClientRepresentation> clientList = getClients(null);
        logger.info("All clients - clientList:{}", clientList);
        return clientList;
    }
    
    public List<ClientRepresentation> getClients(String realm) {
        logger.info("Serach client in realm:{})", realm);

        List<ClientRepresentation> clients = getClientsResource(null).findAll();

        logger.info("clients:{}", clients);
        return clients;
    }

    public List<ClientRepresentation> getClientByClientId(String clientId) {
        logger.info("Searching client by clientId:{}", clientId);

        List<ClientRepresentation> clientList = serachClients(clientId, null);

        logger.info("Clients by clientId:{} are clientList:{}", clientId, clientList);
        return clientList;
    }
    
    public List<ClientRepresentation> serachClients(String clientId, String realm) {
        logger.info("Searching client by clientId:{} in realm:{})", clientId, realm);

        List<ClientRepresentation> clientList = getClientsResource(null).findByClientId(clientId);

        logger.info("All clientList:{}", clientList);
        return clientList;
    }
    
    public ClientRepresentation getClientById(String id) {
        logger.info("Searching client by String id:{}", id);

        ClientResource clientResource = getClientsResource(null).get(id);
        logger.info("clientResource:{}", clientResource);
        
        ClientRepresentation client = clientResource.toRepresentation();
        logger.info("ClientRepresentation:{}", client);
        return client;
    }

    public ClientRepresentation createClient(ClientRepresentation clientRepresentation)  {
        logger.info(" createClient() - clientRepresentation:{}", clientRepresentation);

        ClientsResource clientsResource = getClientsResource(null);

        // Create client (requires manage-users role)
        Response response = clientsResource.create(clientRepresentation);
        logger.info(" createClient() - response:{}", response);

        logger.info(
                " createClient() - response.getStatus():{}, response.getStatusInfo():{}, response.getLocation():{}",
                response.getStatus(), response.getStatusInfo(), response.getLocation());
        logger.info("response.getLocation():{}", response.getLocation());
        String id = CreatedResponseUtil.getCreatedId(response);

        logger.info("New client created with id:{}", id);

        ClientResource clientResource = clientsResource.get(id);
        ClientRepresentation client = clientResource.toRepresentation();
        logger.info("New client created with client:{}", client);

        return client;
    }

    public ClientRepresentation updateClient(ClientRepresentation clientRepresentation) {
        logger.info(" updateClient() - clientRepresentation:{}", clientRepresentation);

        ClientsResource clientsResource = getClientsResource(null);

        ClientResource clientResource = clientsResource.get(clientRepresentation.getId());
        clientResource.update(clientRepresentation);

        ClientRepresentation client = clientResource.toRepresentation();
        logger.info("Updated client:{}", client);
        return client;
    }

    public void deleteClient(String id)  {
        logger.info(" deleteClient() - id:{}", id);

        ClientsResource clientsResource = getClientsResource(null);
        logger.info("clientsResource:{})", clientsResource);

        ClientResource clientResource = clientsResource.get(id);
        logger.info("client resource to delete:{})", clientResource);
        clientResource.remove();
        logger.info("afrer deleting client identified by id:{})", id);

    }
    
    public ProtocolMappersResource getClientProtocolMappersResource(String clientId) {
        logger.info(" Get Client ProtocolMappersResource for client - clientId:{}", clientId);
        ProtocolMappersResource protocolMappersResource = null;
        List<ClientRepresentation> clients = this.getClientByClientId(clientId);
        logger.info("clients:{}", clients);

        if (clients != null && !clients.isEmpty()) {
            ClientResource clientResource = getClientsResource(null).get(clients.get(0).getId());
            logger.info(" clientResource:{}", clientResource);
    
            protocolMappersResource = clientResource.getProtocolMappers();
               
            logger.info(" protocolMappersResource:{} for client:{}", protocolMappersResource, clientId);
        }

        return protocolMappersResource;
    }
    
    public List<ProtocolMapperRepresentation> getClientProtocolMapperRepresentation(String clientId) {
        logger.info(" Get Client ProtocolMapper for client - clientId:{}", clientId);
        List<ProtocolMapperRepresentation> protocolMapperRepresentationList = null;
        ProtocolMappersResource protocolMappersResource = this.getClientProtocolMappersResource(clientId);
        logger.info("clientId:{} -> protocolMappersResource:{}", clientId, protocolMappersResource);

        if (protocolMappersResource != null ) {
            protocolMapperRepresentationList = protocolMappersResource.getMappers();           
            logger.info(" protocolMappers:{} for client:{}", protocolMapperRepresentationList, clientId);
        }

        return protocolMapperRepresentationList;
    }
    
    public List<ProtocolMapperRepresentation> addClientProtocolMappersResource(String clientId, ProtocolMapperRepresentation protocolMapperRepresentation) {
        logger.info(" Add ProtocolMapper for client - clientId:{} with protocolMapperRepresentation:{}", clientId, protocolMapperRepresentation);
        List<ProtocolMapperRepresentation> protocolMappers = null;
        List<ClientRepresentation> clients = this.getClientByClientId(clientId);
        logger.info("clients:{}", clients);

        if (clients != null && !clients.isEmpty()) {
            ClientResource clientResource = getClientsResource(null).get(clients.get(0).getId());
            logger.info(" clientResource:{}", clientResource);
    
            ProtocolMappersResource protocolMappersResource = clientResource.getProtocolMappers();
            protocolMappers = protocolMappersResource.getMappers();
           
            logger.info(" protocolMappers:{} for client:{}", protocolMappers, clientId);
        }

        return protocolMappers;
    }

}

