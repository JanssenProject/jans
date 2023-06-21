/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.service;

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
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import org.slf4j.Logger;

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

    public List<ClientRepresentation> getAllClients() {
        List<ClientRepresentation> clientList = getClients(null);
        logger.info("All clients - clientList:{}", clientList);
        return clientList;
    }

    public List<UserRepresentation> getAllUsers() {
        List<UserRepresentation> userList = getUsers(null);
        logger.info("All userList:{}", userList);
        return userList;
    }

    public List<ClientRepresentation> getClients(String realm) {
        logger.info("Serach client in realm:{})", realm);

        List<ClientRepresentation> clients = getClientsResource(null).findAll();

        logger.info("clients:{}", clients);
        return clients;
    }

    public List<ClientRepresentation> serachClients(String name) {
        logger.info("Searching client by name:{}", name);

        List<ClientRepresentation> clientList = serachClients(name, null);

        logger.info("Clients by name:{} are clientList:{}", name, clientList);
        return clientList;
    }

    public List<ClientRepresentation> serachClients(String name, String realm) {
        logger.info("Searching client by name:{} in realm:{})", name, realm);

        List<ClientRepresentation> clientList = getClientsResource(null).query(name);

        logger.info("All clientList:{}", clientList);
        return clientList;
    }

    public List<UserRepresentation> getUsers(String realm) {
        logger.info("Fetching users in realm:{})", realm);
        List<UserRepresentation> userList = getUsersResource(null).list();
        logger.info("All userList:{}", userList);
        return userList;
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

}
