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
import org.keycloak.admin.client.resource.ClientResource;
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
        if (StringUtils.isBlank(realm)) {
            realm = "master";
        }
        RealmResource realmResource = keycloakConfig.getInstance().realm(realm);
        logger.error("realmResource:{})", realmResource);
        return realmResource;
    }

    public ClientsResource getClientsResource(String realm) {
        logger.error("Get ClientsResource for realm:{})", realm);
        RealmResource realmResource = this.getRealmResource(realm);
        logger.error("realm-resource:{})", realmResource);

        ClientsResource clientsResource = realmResource.clients();
        logger.error(" clientsResource:{})", clientsResource);

        return clientsResource;
    }

    public UsersResource getUsersResource(String realm) {
        logger.error("Get UsersResource for realm:{})", realm);
        RealmResource realmResource = this.getRealmResource(realm);
        logger.error("realmResource:{})", realmResource);

        UsersResource usersResource = realmResource.users();
        logger.error(" usersResource:{})", usersResource);

        return usersResource;
    }

    public List<ClientRepresentation> getAllClients() {
        List<ClientRepresentation> clientList = getClients(null);
        logger.error("All clients - clientList:{}", clientList);
        return clientList;
    }

    public List<UserRepresentation> getAllUsers() {
        List<UserRepresentation> userList = getUsers(null);
        logger.error("All userList:{}", userList);
        return userList;
    }

    public List<ClientRepresentation> getClients(String realm) {
        logger.error("Serach client in realm:{})", realm);

        List<ClientRepresentation> clients = getClientsResource(null).findAll();

        logger.error("clients:{}", clients);
        return clients;
    }

    public List<ClientRepresentation> serachClients(String name) {
        logger.error("Searching client by name:{}", name);

        List<ClientRepresentation> clientList = serachClients(name, null);

        logger.error("Clients by name:{} are clientList:{}", name, clientList);
        return clientList;
    }

    public List<ClientRepresentation> serachClients(String name, String realm) {
        logger.error("Searching client by name:{} in realm:{})", name, realm);

        List<ClientRepresentation> clientList = getClientsResource(null).query(name);

        logger.error("All clientList:{}", clientList);
        return clientList;
    }

    public List<UserRepresentation> getUsers(String realm) {
        logger.error("Fetching users in realm:{})", realm);
        List<UserRepresentation> userList = getUsersResource(null).list();
        logger.error("All userList:{}", userList);
        return userList;
    }

    public ClientRepresentation createClient(ClientRepresentation clientRepresentation)  {
        logger.error(" createClient() - clientRepresentation:{}", clientRepresentation);

        ClientsResource clientsResource = getClientsResource(null);

        // Create client (requires manage-users role)
        Response response = clientsResource.create(clientRepresentation);
        logger.error(" createClient() - response:{}", response);

        logger.error(
                " createClient() - response.getStatus():{}, response.getStatusInfo():{}, response.getLocation():{}",
                response.getStatus(), response.getStatusInfo(), response.getLocation());
        logger.error("response.getLocation():{}", response.getLocation());
        String id = CreatedResponseUtil.getCreatedId(response);

        logger.error("New client created with id:{}", id);

        ClientResource clientResource = clientsResource.get(id);
        ClientRepresentation client = clientResource.toRepresentation();
        logger.error("New client created with client:{}", client);

        return client;
    }

    public ClientRepresentation updateClient(ClientRepresentation clientRepresentation) {
        logger.error(" updateClient() - clientRepresentation:{}", clientRepresentation);

        ClientsResource clientsResource = getClientsResource(null);

        ClientResource clientResource = clientsResource.get(clientRepresentation.getId());
        clientResource.update(clientRepresentation);

        ClientRepresentation client = clientResource.toRepresentation();
        logger.error("Updated client:{}", client);
        return client;
    }

    public void deleteClient(String id)  {
        logger.error(" deleteClient() - id:{}", id);

        ClientsResource clientsResource = getClientsResource(null);
        logger.error("clientsResource:{})", clientsResource);

        ClientResource clientResource = clientsResource.get(id);

        clientResource.remove();

        ClientRepresentation client = clientResource.toRepresentation();
        logger.error("Updated client:{}", client);

    }

}
