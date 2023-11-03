/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.service;

import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.plugin.keycloak.idp.broker.configuration.KeycloakConfig;
import io.jans.configapi.plugin.keycloak.idp.broker.util.Constants;
import io.jans.orm.PersistenceEntryManager;
import io.jans.util.exception.ConfigurationException;
import io.jans.util.exception.InvalidAttributeException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.*;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.StatusType;
import jakarta.ws.rs.WebApplicationException;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.IdentityProvidersResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperTypeRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

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

    private RealmResource getRealmResource(String realm) {
        logger.info("Get RealmResource for realm:{})", realm);
        if (StringUtils.isBlank(realm)) {
            realm = Constants.REALM_MASTER;
        }
        RealmResource realmResource = keycloakConfig.getInstance().realm(realm);
        logger.info("realmResource:{})", realmResource);
        return realmResource;
    }

    public List<RealmRepresentation> getAllRealms() {
        logger.info("Get All KC Realms");
        List<RealmRepresentation> realmRepresentation = keycloakConfig.getInstance().realms().findAll();

        logger.info("realmRepresentation:{})", realmRepresentation);
        return realmRepresentation;
    }

    public RealmRepresentation getRealmByName(String realmName) {
        logger.info("Get RealmResource for realmName:{})", realmName);

        List<RealmRepresentation> realms = getAllRealms();
        RealmRepresentation realmRepresentation = null;
        if (realms == null || realms.isEmpty()) {
            return realmRepresentation;
        }
        for (RealmRepresentation realm : realms) {
            if (realmName.equals(realm.getRealm())) {
                realmRepresentation = realm;
                break;
            }
        }
        logger.info("realmRepresentation:{})", realmRepresentation);
        return realmRepresentation;
    }

    public RealmRepresentation createNewRealm(RealmRepresentation realmRepresentation) {
        logger.info("Create realmRepresentation:{})", realmRepresentation);
        if (realmRepresentation == null) {
            new InvalidAttributeException("RealmRepresentation is null");
        }
        keycloakConfig.getInstance().realms().create(realmRepresentation);

        realmRepresentation = getRealmByName(realmRepresentation.getDisplayName());
        logger.info("realmRepresentation:{})", realmRepresentation);
        return realmRepresentation;
    }

    public RealmRepresentation updateRealm(RealmRepresentation realmRepresentation) {
        logger.info("Updade realmRepresentation:{})", realmRepresentation);
        if (realmRepresentation == null) {
            new InvalidAttributeException("RealmRepresentation is null");
        }
        RealmResource realmResource = this.getRealmResource(realmRepresentation.getRealm());
        logger.info("realmResource:{})", realmResource);
        realmResource.update(realmRepresentation);
        realmRepresentation = realmResource.toRepresentation();
        logger.info("realmRepresentation:{})", realmRepresentation);
        return realmRepresentation;
    }

    public void deleteRealm(String realmName) {
        logger.info("Delete Realm by name realmName:{})", realmName);

        if (StringUtils.isBlank(realmName)) {
            new InvalidAttributeException("Realm name is null!!!");
        }
        keycloakConfig.getInstance().realm(realmName).remove();
        return;
    }

    public List<IdentityProviderRepresentation> findAllIdentityProviders(String realmName) {
        logger.info("Fetch all IdentityProvider for realmName:{})", realmName);

        if (StringUtils.isBlank(realmName)) {
            new InvalidAttributeException("Realm name is null!!!");
        }

        IdentityProvidersResource identityProvidersResource = getRealmResource(realmName).identityProviders();
        logger.info("identityProvidersResource:{})", identityProvidersResource);
        List<IdentityProviderRepresentation> identityProviders = identityProvidersResource.findAll();

        logger.info("identityProviders:{}", identityProviders);

        return identityProviders;
    }

    public IdentityProviderRepresentation getIdentityProviderById(String realmName, String internalId) {
        logger.info("Fetch IdentityProvider by id realmName:{}, internalId:{})", realmName, internalId);

        if (StringUtils.isBlank(realmName) || StringUtils.isBlank(internalId)) {
            new InvalidAttributeException("Realm name or IdentityProvider internalId is null!!!");
        }

        List<IdentityProviderRepresentation> identityProviders = findAllIdentityProviders(realmName);
        logger.info("identityProviders:{}", identityProviders);
        IdentityProviderRepresentation identityProvider = null;
        if (identityProviders == null || identityProviders.isEmpty()) {
            return identityProvider;
        }

        for (IdentityProviderRepresentation data : identityProviders) {
            if (internalId.equals(data.getInternalId())) {
                identityProvider = data;
                break;
            }
        }
        logger.info("IdentityProvider fetched by id realmName:{}, internalId:{}, identityProvider:{})", realmName,
                internalId, identityProvider);
        return identityProvider;
    }

    public IdentityProviderResource getIdentityProviderResource(String realmName, String alias) {
        logger.info("Get IdentityProviderResource by name realmName:{}, alias:{})", realmName, alias);

        if (StringUtils.isBlank(realmName) || StringUtils.isBlank(alias)) {
            new InvalidAttributeException("Realm name or IdentityProvider alias is null!!!");
        }
        IdentityProviderResource identityProviderResource = this.getIdentityProviderResource(realmName, alias);
        logger.info("IdentityProviderResource fetched by name realmName:{}, alias:{}, identityProviderResource:{})",
                realmName, alias, identityProviderResource);
        return identityProviderResource;
    }

    public IdentityProviderRepresentation getIdentityProviderByName(String realmName, String alias) {
        logger.info("Get IdentityProvider by name realmName:{}, alias:{})", realmName, alias);

        if (StringUtils.isBlank(realmName) || StringUtils.isBlank(alias)) {
            new InvalidAttributeException("Realm name or IdentityProvider alias is null!!!");
        }

        IdentityProviderResource identityProviderResource = this.getIdentityProviderResource(realmName, alias);

        logger.info("identityProviderResource:{})", identityProviderResource);
        IdentityProviderRepresentation identityProvider = identityProviderResource.toRepresentation();

        logger.info("IdentityProvider fetched by name realmName:{}, alias:{}, identityProvider:{})", realmName, alias,
                identityProvider);
        return identityProvider;
    }

    public IdentityProviderRepresentation createIdentityProvider(String realmName,
            IdentityProviderRepresentation identityProviderRepresentation) {
        logger.info("Create new IdentityProvider under realmName:{}, identityProviderRepresentation:{})", realmName,
                identityProviderRepresentation);

        if (StringUtils.isBlank(realmName)) {
            new InvalidAttributeException("Realm name is null!!!");
        }

        if (identityProviderRepresentation == null) {
            new InvalidAttributeException("IdentityProviderRepresentation is null!!!");
        }

        IdentityProvidersResource identityProvidersResource = getRealmResource(realmName).identityProviders();
        logger.info("identityProvidersResource:{})", identityProvidersResource);
        Response response = identityProvidersResource.create(identityProviderRepresentation);

        logger.info("IdentityProvider creation response:{}", response);
        if (response == null) {
            logger.info("IdentityProvider creation response.getStatusInfo():{}, response.getEntity():{}",
                    response.getStatusInfo(), response.getEntity());
            String id = getCreatedId(response);
            logger.info("IdentityProvider creation id():{}", id);
            identityProviderRepresentation = (IdentityProviderRepresentation) response.getEntity();
            logger.info("IdentityProvider creation identityProviderRepresentation():{}",
                    identityProviderRepresentation);
            List<IdentityProviderRepresentation> identityProvider = findAllIdentityProviders(realmName);
            if (identityProvider != null && !identityProvider.isEmpty()) {
                identityProvider.stream()
                        .forEach(e -> System.out.println(e.getInternalId() + "::" + e.getDisplayName()));
            }

            response = identityProvidersResource.getIdentityProviders(Constants.SAML);
            if (response == null) {

                logger.info("SAML IdentityProvider response.getStatusInfo():{}, response.getEntity():{}",
                        response.getStatusInfo(), response.getEntity());

                if (response.getEntity() != null) {
                    logger.info("response.getEntity().getClass():{}", response.getEntity().getClass());
                }
            }

        }

        return identityProviderRepresentation;
    }

    public IdentityProviderRepresentation createIdentityProvider(String realmName,
            IdentityProviderRepresentation identityProviderRepresentation, InputStream idpMetadataStream) {

        try {
            logger.info(
                    "Create new IdentityProvider under realmName:{}, identityProviderRepresentation:{}, idpMetadataStream:{})",
                    realmName, identityProviderRepresentation, idpMetadataStream);

            if (StringUtils.isBlank(realmName)) {
                new InvalidAttributeException("Realm name is null!!!");
            }

            if (identityProviderRepresentation == null) {
                new InvalidAttributeException("IdentityProviderRepresentation is null!!!");
            }

            if (idpMetadataStream == null) {
                new InvalidAttributeException("Idp Metedata file is null!!!");
            }

            MultipartFormDataOutput form = new MultipartFormDataOutput();
            form.addFormData("providerId", "saml", MediaType.TEXT_PLAIN_TYPE);

            byte[] content = idpMetadataStream.readAllBytes();
            String body = new String(content, Charset.forName("utf-8"));
            form.addFormData("file", body, MediaType.APPLICATION_XML_TYPE, "saml-idp-metadata.xml");

            IdentityProvidersResource identityProvidersResource = getRealmResource(realmName).identityProviders();
            logger.info("identityProvidersResource:{})", identityProvidersResource);

            Map<String, String> result = identityProvidersResource.importFrom(form);
            boolean valid = verifySamlIdpConfig(result);
            logger.info("Is IDP metadata file valid:{})", valid);
            if (!valid) {
                new InvalidAttributeException("Idp Metedata file is not valid !!!");
            }

            // Create new SAML identity provider using configuration retrieved from
            // import-config
            Response response = identityProvidersResource.create(identityProviderRepresentation);

            logger.info("IdentityProvider creation response:{}", response);
            if (response == null) {
                logger.info("IdentityProvider creation response.getStatusInfo():{}, response.getEntity():{}",
                        response.getStatusInfo(), response.getEntity());
                String id = getCreatedId(response);
                logger.info("IdentityProvider creation id():{}", id);
                identityProviderRepresentation = (IdentityProviderRepresentation) response.getEntity();
                logger.info("IdentityProvider creation identityProviderRepresentation():{}",
                        identityProviderRepresentation);
                List<IdentityProviderRepresentation> identityProvider = findAllIdentityProviders(realmName);
                if (identityProvider != null && !identityProvider.isEmpty()) {
                    identityProvider.stream()
                            .forEach(e -> System.out.println(e.getInternalId() + "::" + e.getDisplayName()));
                }

                response = identityProvidersResource.getIdentityProviders(Constants.SAML);
                if (response == null) {

                    logger.info("SAML IdentityProvider response.getStatusInfo():{}, response.getEntity():{}",
                            response.getStatusInfo(), response.getEntity());

                    if (response.getEntity() != null) {
                        logger.info("response.getEntity().getClass():{}", response.getEntity().getClass());
                    }
                }

            }
        } catch (Exception ex) {
            throw new ConfigurationException("Error while configuring SAML IDP ", ex);
        }

        return identityProviderRepresentation;
    }

    public IdentityProviderRepresentation updateNewIdentityProvider(String realmName,
            IdentityProviderRepresentation identityProviderRepresentation) {
        logger.info("Update IdentityProvider under realmName:{}, identityProviderRepresentation:{})", realmName,
                identityProviderRepresentation);

        if (StringUtils.isBlank(realmName)) {
            new InvalidAttributeException("Realm name is null!!!");
        }

        if (identityProviderRepresentation == null) {
            new InvalidAttributeException("IdentityProviderRepresentation for updation is null!!!");
        }

        IdentityProviderResource identityProviderResource = this.getIdentityProviderResource(realmName,
                identityProviderRepresentation.getAlias());

        logger.info("identityProviderResource for update is:{})", identityProviderRepresentation);
        identityProviderResource.update(identityProviderRepresentation);
        identityProviderRepresentation = identityProviderResource.toRepresentation();

        logger.info("Updated IdentityProvider under realmName:{}, identityProviderRepresentation:{})", realmName,
                identityProviderRepresentation);

        return identityProviderRepresentation;
    }

    public void deleteIdentityProvider(String realmName, String alias) {
        logger.info("IdentityProvider to delete realmName:{}, alias:{})", realmName, alias);
        if (StringUtils.isBlank(realmName) || StringUtils.isBlank(alias)) {
            new InvalidAttributeException("Realm name or IdentityProvider alias is null!!!");
        }
        IdentityProviderResource identityProviderResource = this.getIdentityProviderResource(realmName, alias);
        logger.info("IdentityProviderResource fetched for delete realmName:{}, alias:{}, identityProviderResource:{})",
                realmName, alias, identityProviderResource);

        identityProviderResource.remove();
        logger.info("Deleted IdentityProvider under realmName:{}, alias:{})", realmName, alias);

        return;
    }

    private static String getCreatedId(Response response) {
        URI location = response.getLocation();
        if (!response.getStatusInfo().equals(Status.CREATED)) {
            StatusType statusInfo = response.getStatusInfo();
            throw new WebApplicationException("Create method returned status " + statusInfo.getReasonPhrase()
                    + " (Code: " + statusInfo.getStatusCode() + "); expected status: Created (201)", response);
        }
        if (location == null) {
            return null;
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private boolean verifySamlIdpConfig(Map<String, String> config) {
        // import endpoint simply converts IDPSSODescriptor into key value pairs.
        // check that saml-idp-metadata.xml was properly converted into key value pairs
        logger.info("verifySamlConfig - config:{}", config);
        if (config == null || config.isEmpty()) {
            return false;
        }
        logger.info("config.keySet().containsAll(Constants.SAML_IDP_CONFIG):{}",
                config.keySet().containsAll(Constants.SAML_IDP_CONFIG));
        return config.keySet().containsAll(Constants.SAML_IDP_CONFIG);
    }

}
