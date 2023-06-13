/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.service;

import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.orm.PersistenceEntryManager;
import org.slf4j.Logger;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

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

    public UserRepresentation createClient(String clientId, String clientSecret) throws Exception {
        logger.error(" SamlService::createClient() - clientId:{}, clientSecret:{}",clientId, clientSecret);
        String serverUrl = "http://localhost:8080";
        String realm = "master";
        // idm-client needs to allow "Direct Access Grants: Resource Owner Password Credentials Grant"
        String adminId = "admin1";
        String adminPassword = "admin123";

//      // Client "idm-client" needs service-account with at least "manage-users, view-clients, view-realm, view-users" roles for "realm-management"
//      Keycloak keycloak = KeycloakBuilder.builder() //
//              .serverUrl(serverUrl) //
//              .realm(realm) //
//              .grantType(OAuth2Constants.CLIENT_CREDENTIALS) //
//              .clientId(clientId) //
//              .clientSecret(clientSecret).build();

        // User "idm-admin" needs at least "manage-users, view-clients, view-realm, view-users" roles for "realm-management"
        Keycloak keycloak = KeycloakBuilder.builder() //
                .serverUrl(serverUrl) //
                .realm(realm) //
                .grantType(OAuth2Constants.PASSWORD) //
                .clientId(clientId) //
                .clientSecret(clientSecret) //
                .username(adminId) //
                .password(adminPassword) //
                .build();
        
        logger.error(" SamlService::createClient() - keycloak:{}",keycloak);

        // Define user
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername("tester1");
        user.setFirstName("First");
        user.setLastName("Last");
        user.setEmail("tom+tester1@tdlabs.local");
        user.setAttributes(Collections.singletonMap("origin", Arrays.asList("demo")));
        logger.error(" SamlService::createClient() - user:{}",user);
        
        // Get realm
        RealmResource realmResource = keycloak.realm(realm);
        UsersResource usersRessource = realmResource.users();
        logger.error(" SamlService::createClient() - realmResource:{}, usersRessource:{}",realmResource, usersRessource);

        // Create user (requires manage-users role)
        Response response = usersRessource.create(user);
        logger.error(" SamlService::createClient() - response:{}",response);
        
        System.out.printf("Repsonse: %s %s%n", response.getStatus(), response.getStatusInfo());
        System.out.println(response.getLocation());
        String userId = CreatedResponseUtil.getCreatedId(response);

        logger.error(" SamlService::createClient() - response.getStatus():{}, response.getStatusInfo():{}, response.getLocation():{}, userId:{}",response.getStatus(), response.getStatusInfo(), response.getLocation(),userId);
        System.out.printf("User created with userId: %s%n", userId);

        // Define password credential
        CredentialRepresentation passwordCred = new CredentialRepresentation();
        passwordCred.setTemporary(false);
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue("test");
        logger.error(" SamlService::createClient() - passwordCred:{}",passwordCred);
        UserResource userResource = usersRessource.get(userId);
        logger.error(" SamlService::createClient() - userResource:{}",userResource);
        // Set password credential
        userResource.resetPassword(passwordCred);

//        // Get realm role "tester" (requires view-realm role)
        RoleRepresentation testerRealmRole = realmResource.roles()//
                .get("tester").toRepresentation();
//
//        // Assign realm role tester to user
        userResource.roles().realmLevel() //
                .add(Arrays.asList(testerRealmRole));

        // Send password reset E-Mail
        // VERIFY_EMAIL, UPDATE_PROFILE, CONFIGURE_TOTP, UPDATE_PASSWORD, TERMS_AND_CONDITIONS
//        usersRessource.get(userId).executeActionsEmail(Arrays.asList("UPDATE_PASSWORD"));

        // Delete User
//        userResource.remove();
        
        return user;
    }
}
