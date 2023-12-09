/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.service;

import io.jans.configapi.plugin.saml.configuration.kc.KeycloakConfig;
import io.jans.configapi.plugin.saml.util.Constants;
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
import java.io.FileInputStream;
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
    KeycloakConfig keycloakConfig;

    private RealmResource getRealmResource(String realm) {
        logger.info("Get RealmResource for realm:{}", realm);
        if (StringUtils.isBlank(realm)) {
            realm = Constants.REALM_MASTER;
        }
        RealmResource realmResource = keycloakConfig.getInstance().realm(realm);
        logger.debug("realmResource:{}", realmResource);
        return realmResource;
    }

      

    public IdentityProvidersResource getIdentityProvidersResource(String realmName) {
        if (StringUtils.isBlank(realmName)) {
            throw new InvalidAttributeException("Realm name is null!!!");
        }
        IdentityProvidersResource identityProvidersResource = getRealmResource(realmName).identityProviders();
        logger.debug("identityProvidersResource:{}", identityProvidersResource);
        return identityProvidersResource;
    }

    public List<IdentityProviderRepresentation> findAllIdentityProviders(String realmName) {
        logger.info("Fetch all IdentityProvider for realmName:{}", realmName);

        IdentityProvidersResource identityProvidersResource = this.getIdentityProvidersResource(realmName);
        List<IdentityProviderRepresentation> identityProviders = identityProvidersResource.findAll();

        logger.info("identityProviders:{}", identityProviders);

        return identityProviders;
    }

    public IdentityProviderRepresentation getIdentityProviderById(String realmName, String internalId) {
        logger.info("Fetch IdentityProvider by id realmName:{}, internalId:{}", realmName, internalId);

        if (StringUtils.isBlank(realmName) || StringUtils.isBlank(internalId)) {
            throw new InvalidAttributeException("Realm name or IdentityProvider internalId is null!!!");
        }

        List<IdentityProviderRepresentation> identityProviders = findAllIdentityProviders(realmName);
        logger.debug("identityProviders:{}", identityProviders);
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
        logger.info("IdentityProvider fetched by id realmName:{}, internalId:{}, identityProvider:{}", realmName,
                internalId, identityProvider);
        return identityProvider;
    }

    public IdentityProviderRepresentation getIdentityProviderByName(String realmName, String alias) {
        logger.info("Get IdentityProvider by name realmName:{}, alias:{}", realmName, alias);

        if (StringUtils.isBlank(realmName) || StringUtils.isBlank(alias)) {
            throw new InvalidAttributeException("Realm name or IdentityProvider alias is null!!!");
        }

        List<IdentityProviderRepresentation> identityProviders = findAllIdentityProviders(realmName);
        logger.debug("identityProviders:{}", identityProviders);
        IdentityProviderRepresentation identityProvider = null;
        if (identityProviders == null || identityProviders.isEmpty()) {
            return identityProvider;
        }

        for (IdentityProviderRepresentation data : identityProviders) {
            if (alias.equals(data.getAlias())) {
                identityProvider = data;
                break;
            }
        }

        logger.debug("IdentityProvider fetched by name realmName:{}, alias:{}, identityProvider:{}", realmName, alias,
                identityProvider);
        return identityProvider;
    }

    public Map<String, String> validateSamlMetadata(String realmName, InputStream idpMetadataStream) {
        Map<String, String> config = null;
        try {
            logger.info("Verify Saml Idp Metadata realmName:{}, idpMetadataStream:{}", realmName, idpMetadataStream);

            if (idpMetadataStream == null) {
                throw new InvalidAttributeException("Idp Metedata file is null!!!");
            }

            MultipartFormDataOutput form = new MultipartFormDataOutput();
            form.addFormData("providerId", "saml", MediaType.TEXT_PLAIN_TYPE);
            logger.debug("SAML idpMetadataStream.available():{}", idpMetadataStream.available());

            byte[] content = idpMetadataStream.readAllBytes();
            logger.debug("content:{}", content);
            String body = new String(content, Charset.forName("utf-8"));
            form.addFormData("file", body, MediaType.APPLICATION_XML_TYPE, "saml-idp-metadata.xml");

            IdentityProvidersResource identityProvidersResource = this.getIdentityProvidersResource(realmName);
            if (identityProvidersResource == null) {
                return config;
            }
            config = identityProvidersResource.importFrom(form);
            logger.debug("IDP metadata importConfig config:{})", config);
            boolean valid = verifySamlIdpConfig(config);
            logger.debug("Is IDP metadata config valid:{})", valid);
            if (!valid) {
                throw new InvalidAttributeException("Idp Metedata file is not valid !!!");
            }

        } catch (Exception ex) {
            throw new ConfigurationException("Error while validating SAML IDP Metadata", ex);
        }

        return config;
    }

    public IdentityProviderRepresentation createIdentityProvider(String realmName,
            IdentityProviderRepresentation identityProviderRepresentation) {

        try {
            logger.info("Create new IdentityProvider under realmName:{}, identityProviderRepresentation:{})",
                    realmName, identityProviderRepresentation);

            if (StringUtils.isBlank(realmName)) {
                throw new InvalidAttributeException("Realm name is null!!!");
            }

            if (identityProviderRepresentation == null) {
                throw new InvalidAttributeException("IdentityProviderRepresentation is null!!!");
            }

            // validate IDP metadata
            logger.debug("IDP metadata config identityProviderRepresentation.getConfig():{})",
                    identityProviderRepresentation.getConfig());
            if (identityProviderRepresentation.getConfig() == null
                    || identityProviderRepresentation.getConfig().isEmpty()) {
                throw new InvalidAttributeException("Idp Metedata config is null!!!");
            }

            boolean valid = verifySamlIdpConfig(identityProviderRepresentation.getConfig());
            logger.debug("Is IDP metadata config valid:{})", valid);
           
            // create Identity Provider
            IdentityProvidersResource identityProvidersResource = this.getIdentityProvidersResource(realmName);
            if (identityProvidersResource == null) {
                throw new ConfigurationException(
                        "identityProvidersResource are null, could not create Identity Provider!!!");
            }
            logger.trace(
                    "IDP data identityProviderRepresentation.getAlias():{}, identityProviderRepresentation.getInternalId():{}, identityProviderRepresentation.getProviderId():{}, identityProviderRepresentation.getConfig():{}, identityProviderRepresentation.isEnabled():{}, identityProviderRepresentation.isLinkOnly():{}, identityProviderRepresentation.isStoreToken():{},identityProviderRepresentation.getFirstBrokerLoginFlowAlias():{}, identityProviderRepresentation.getPostBrokerLoginFlowAlias():{},identityProviderRepresentation.isTrustEmail():{}",
                    identityProviderRepresentation.getAlias(), identityProviderRepresentation.getInternalId(),
                    identityProviderRepresentation.getProviderId(), identityProviderRepresentation.getConfig(),
                    identityProviderRepresentation.isEnabled(), identityProviderRepresentation.isLinkOnly(),
                    identityProviderRepresentation.isStoreToken(),
                    identityProviderRepresentation.getFirstBrokerLoginFlowAlias(),
                    identityProviderRepresentation.getPostBrokerLoginFlowAlias(),
                    identityProviderRepresentation.isTrustEmail());
            Response response = identityProvidersResource.create(identityProviderRepresentation);

            logger.debug("IdentityProvider creation response:{}", response);
            if (response != null) {
                logger.debug("IdentityProvider creation response.getStatusInfo():{}, response.getEntity():{}",
                        response.getStatusInfo(), response.getEntity());

                String id = getCreatedId(response);
                logger.debug("IdentityProvider creation id():{}", id);

                List<IdentityProviderRepresentation> identityProvider = findAllIdentityProviders(realmName);
                if (identityProvider != null && !identityProvider.isEmpty()) {
                    identityProvider.stream()
                            .forEach(e -> System.out.println(e.getInternalId() + "::" + e.getDisplayName()));
                }

                identityProviderRepresentation = getIdentityProviderByName(realmName,
                        identityProviderRepresentation.getAlias());
                logger.debug("Final identityProviderRepresentation:{}", identityProviderRepresentation);

                response.close();

            }
        } catch (Exception ex) {
            throw new ConfigurationException("Error while creating SAML IDP ", ex);
        }

        return identityProviderRepresentation;
    }

    public IdentityProviderRepresentation updateIdentityProvider(String realmName,
            IdentityProviderRepresentation identityProviderRepresentation) {
        logger.info("Update IdentityProvider under realmName:{}, identityProviderRepresentation:{}", realmName,
                identityProviderRepresentation);

        // validations
        if (StringUtils.isBlank(realmName)) {
            throw new InvalidAttributeException("Realm name is null!!!");
        }

        if (identityProviderRepresentation == null) {
            throw new InvalidAttributeException("IdentityProviderRepresentation for updation is null!!!");
        }

        // validate IDP metadata
        logger.debug("IDP metadata config while update identityProviderRepresentation.getConfig():{}",
                identityProviderRepresentation.getConfig());
        if (identityProviderRepresentation.getConfig() == null
                || identityProviderRepresentation.getConfig().isEmpty()) {
            throw new InvalidAttributeException("Idp Metedata config is null!!!");
        }

        boolean valid = verifySamlIdpConfig(identityProviderRepresentation.getConfig());
        logger.debug("Is IDP metadata update config valid?:{})", valid);

        // validate IDP to update
        IdentityProvidersResource identityProvidersResource = this.getIdentityProvidersResource(realmName);
        if (identityProvidersResource == null) {
            throw new ConfigurationException(
                    "identityProvidersResource is null, could not update Identity Provider!!!");
        }

        IdentityProviderResource identityProviderResource = identityProvidersResource
                .get(identityProviderRepresentation.getAlias());
        logger.debug(
                "Is IDP resource present for update identityProviderRepresentation.getAlias():{}, identityProviderResource:{}",
                identityProviderRepresentation.getAlias(), identityProviderResource);
        if (identityProviderResource == null) {
            throw new InvalidAttributeException("IdentityProvider not found to update!!!");
        }

        // update
        identityProviderResource.update(identityProviderRepresentation);
        identityProviderRepresentation = identityProviderResource.toRepresentation();

        logger.info(
                "Updated IdentityProvider identityProviderRepresentation.getAlias():{} under realmName:{} is identityProviderRepresentation:{}",
                identityProviderRepresentation.getAlias(), realmName, identityProviderRepresentation);

        return identityProviderRepresentation;
    }

    public void deleteIdentityProvider(String realmName, String alias) {
        logger.info("IdentityProvider to delete realmName:{}, alias:{}", realmName, alias);
        if (StringUtils.isBlank(realmName) || StringUtils.isBlank(alias)) {
            throw new InvalidAttributeException("Realm name or IdentityProvider alias is null!!!");
        }

        IdentityProvidersResource identityProvidersResource = this.getIdentityProvidersResource(realmName);
        if (identityProvidersResource == null) {
            throw new ConfigurationException(
                    "IdentityProvidersResource is null, could not delete Identity Provider!!!");
        }

        logger.debug(
                "IdentityProviderResource fetched for delete realmName:{}, alias:{}, identityProvidersResource:{} ",
                realmName, alias, identityProvidersResource);

        IdentityProviderResource identityProviderResource = identityProvidersResource.get(alias);

        if (identityProviderResource == null) {
            throw new InvalidAttributeException("IdentityProvidersResource not found to delete!!!");
        }
        identityProviderResource.remove();
        logger.debug("Deleted IdentityProvider under realmName:{}, alias:{}", realmName, alias);

        IdentityProviderRepresentation identityProviderRepresentation = getIdentityProviderByName(realmName, alias);
        logger.debug("Checking identityProvider is deleted - identityProviderRepresentation:{}",
                identityProviderRepresentation);

        if (identityProviderResource != null) {
            throw new InvalidAttributeException("IdentityProviders could not be deleted!!!");
        }

        return;
    }

    public void getSAMLServiceProviderMetadata(String realmName, String alias) {
        // To-do

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
        logger.debug("verifySamlConfig - config:{}", config);
        if (config == null || config.isEmpty()) {
            return false;
        }
        logger.info("config.keySet().containsAll(Constants.SAML_IDP_CONFIG):{}",
                config.keySet().containsAll(Constants.SAML_IDP_CONFIG));
        
        return config.keySet().containsAll(Constants.SAML_IDP_CONFIG);
    }

}
