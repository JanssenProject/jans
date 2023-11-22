/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.service;

import io.jans.configapi.plugin.keycloak.idp.broker.configuration.KeycloakConfig;
import io.jans.configapi.plugin.keycloak.idp.broker.util.Constants;
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
        logger.error("Get RealmResource for realm:{})", realm);
        if (StringUtils.isBlank(realm)) {
            realm = Constants.REALM_MASTER;
        }
        RealmResource realmResource = keycloakConfig.getInstance().realm(realm);
        logger.error("realmResource:{})", realmResource);
        return realmResource;
    }

    public List<RealmRepresentation> getAllRealms() {
        logger.error("Get All KC Realms");
        List<RealmRepresentation> realmRepresentation = keycloakConfig.getInstance().realms().findAll();

        logger.error("realmRepresentation:{})", realmRepresentation);
        return realmRepresentation;
    }

    public RealmRepresentation getRealmByName(String realmName) {
        logger.error("Get RealmResource for realmName:{})", realmName);

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
        logger.error("realmRepresentation:{})", realmRepresentation);
        return realmRepresentation;
    }

    public RealmRepresentation createNewRealm(RealmRepresentation realmRepresentation) {
        logger.error("Create realmRepresentation:{})", realmRepresentation);
        if (realmRepresentation == null) {
            new InvalidAttributeException("RealmRepresentation is null");
        }
        keycloakConfig.getInstance().realms().create(realmRepresentation);

        realmRepresentation = getRealmByName(realmRepresentation.getDisplayName());
        logger.error("realmRepresentation:{})", realmRepresentation);
        return realmRepresentation;
    }

    public RealmRepresentation updateRealm(RealmRepresentation realmRepresentation) {
        logger.error("Updade realmRepresentation:{})", realmRepresentation);
        if (realmRepresentation == null) {
            new InvalidAttributeException("RealmRepresentation is null");
        }
        RealmResource realmResource = this.getRealmResource(realmRepresentation.getRealm());
        logger.error("realmResource:{})", realmResource);
        realmResource.update(realmRepresentation);
        realmRepresentation = realmResource.toRepresentation();
        logger.error("realmRepresentation:{})", realmRepresentation);
        return realmRepresentation;
    }

    public void deleteRealm(String realmName) {
        logger.error("Delete Realm by name realmName:{})", realmName);

        if (StringUtils.isBlank(realmName)) {
            new InvalidAttributeException("Realm name is null!!!");
        }
        keycloakConfig.getInstance().realm(realmName).remove();
        return;
    }

    public List<IdentityProviderRepresentation> findAllIdentityProviders(String realmName) {
        logger.error("Fetch all IdentityProvider for realmName:{})", realmName);

        if (StringUtils.isBlank(realmName)) {
            new InvalidAttributeException("Realm name is null!!!");
        }

        IdentityProvidersResource identityProvidersResource = getRealmResource(realmName).identityProviders();
        logger.error("identityProvidersResource:{})", identityProvidersResource);
        List<IdentityProviderRepresentation> identityProviders = identityProvidersResource.findAll();

        logger.error("identityProviders:{}", identityProviders);

        return identityProviders;
    }

    public IdentityProviderRepresentation getIdentityProviderById(String realmName, String internalId) {
        logger.error("Fetch IdentityProvider by id realmName:{}, internalId:{})", realmName, internalId);

        if (StringUtils.isBlank(realmName) || StringUtils.isBlank(internalId)) {
            new InvalidAttributeException("Realm name or IdentityProvider internalId is null!!!");
        }

        List<IdentityProviderRepresentation> identityProviders = findAllIdentityProviders(realmName);
        logger.error("identityProviders:{}", identityProviders);
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
        logger.error("IdentityProvider fetched by id realmName:{}, internalId:{}, identityProvider:{})", realmName,
                internalId, identityProvider);
        return identityProvider;
    }

    public IdentityProvidersResource getIdentityProviderResource(String realmName) {
        logger.error("Get IdentityProviderResource by name realmName:{}", realmName);

        if (StringUtils.isBlank(realmName)) {
            new InvalidAttributeException("Realm name is null!!!");
        }
               
        IdentityProvidersResource identityProvidersResource = getRealmResource(realmName).identityProviders();    
        logger.error("IdentityProviderResource fetched by name realmName:{}, identityProvidersResource:{})",
                realmName,identityProvidersResource);
        return identityProvidersResource;
    }

    public IdentityProviderRepresentation getIdentityProviderByName(String realmName, String alias) {
        logger.error("Get IdentityProvider by name realmName:{}, alias:{})", realmName, alias);

        if (StringUtils.isBlank(realmName) || StringUtils.isBlank(alias)) {
            new InvalidAttributeException("Realm name or IdentityProvider alias is null!!!");
        }

        List<IdentityProviderRepresentation> identityProviders = findAllIdentityProviders(realmName);
        logger.error("identityProviders:{}", identityProviders);
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

        logger.error("IdentityProvider fetched by name realmName:{}, alias:{}, identityProvider:{})", realmName, alias,
                identityProvider);
        return identityProvider;
    }

      
    public Map<String, String> validateSamlMetadata(String realmName,InputStream idpMetadataStream) {
        Map<String, String> config = null;
        try {
            logger.error("Verify Saml Idp Metadata realmName:{}, idpMetadataStream:{})",realmName, idpMetadataStream);

            if (idpMetadataStream == null) {
                new InvalidAttributeException("Idp Metedata file is null!!!");
            }
            
            MultipartFormDataOutput form = new MultipartFormDataOutput();
            form.addFormData("providerId", "saml", MediaType.TEXT_PLAIN_TYPE);            
            logger.error("SAML idpMetadataStream.available():{}", idpMetadataStream.available());            

            byte[] content = idpMetadataStream.readAllBytes();
            logger.error("content:{}", content);
            String body = new String(content, Charset.forName("utf-8"));
            form.addFormData("file", body, MediaType.APPLICATION_XML_TYPE, "saml-idp-metadata.xml");

            IdentityProvidersResource identityProvidersResource = getRealmResource(realmName).identityProviders();
            logger.error("identityProvidersResource:{})", identityProvidersResource);

           config = identityProvidersResource.importFrom(form);
            logger.error("IDP metadata importConfig config:{})", config);
            boolean valid = verifySamlIdpConfig(config);
            logger.error("Is IDP metadata config valid:{})", valid);
            if (!valid) {
                new InvalidAttributeException("Idp Metedata file is not valid !!!");
            }
            
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ConfigurationException("Error while validating SAML IDP Metadata", ex);
        }

        return config;
    }

    public IdentityProviderRepresentation createIdentityProvider(String realmName,
            IdentityProviderRepresentation identityProviderRepresentation) {

        try {
            logger.error("Create new IdentityProvider under realmName:{}, identityProviderRepresentation:{})", realmName, identityProviderRepresentation);
            
            if (StringUtils.isBlank(realmName)) {
                new InvalidAttributeException("Realm name is null!!!");
            }

            if (identityProviderRepresentation == null) {
                new InvalidAttributeException("IdentityProviderRepresentation is null!!!");
            }

            //validate IDP metadata
            logger.error("IDP metadata config identityProviderRepresentation.getConfig():{})", identityProviderRepresentation.getConfig());
            if (identityProviderRepresentation.getConfig() == null || identityProviderRepresentation.getConfig().isEmpty()) {
                new InvalidAttributeException("Idp Metedata config is null!!!");
            }
           
            boolean valid = verifySamlIdpConfig(identityProviderRepresentation.getConfig());
            logger.error("Is IDP metadata config valid:{})", valid);
            if (!valid) {
                new InvalidAttributeException("Idp Metedata file is not valid !!!");
            }
            
            //create Identity Provider
            IdentityProvidersResource identityProvidersResource = getRealmResource(realmName).identityProviders();
            logger.error("identityProvidersResource:{})", identityProvidersResource);
            logger.error("IDP data identityProviderRepresentation.getAlias():{}, identityProviderRepresentation.getInternalId():{}, identityProviderRepresentation.getProviderId():{}, identityProviderRepresentation.getConfig():{}, identityProviderRepresentation.isEnabled():{}, identityProviderRepresentation.isLinkOnly():{}, identityProviderRepresentation.isStoreToken():{},identityProviderRepresentation.getFirstBrokerLoginFlowAlias():{}, identityProviderRepresentation.getPostBrokerLoginFlowAlias():{},identityProviderRepresentation.isTrustEmail():{}", identityProviderRepresentation.getAlias(),identityProviderRepresentation.getInternalId(), identityProviderRepresentation.getProviderId(), identityProviderRepresentation.getConfig(),identityProviderRepresentation.isEnabled(), identityProviderRepresentation.isLinkOnly(), identityProviderRepresentation.isStoreToken(), identityProviderRepresentation.getFirstBrokerLoginFlowAlias(), identityProviderRepresentation.getPostBrokerLoginFlowAlias(), identityProviderRepresentation.isTrustEmail() );
            Response response = identityProvidersResource.create(identityProviderRepresentation);

            logger.error("IdentityProvider creation response:{}", response);
            if (response != null) {
                logger.error("IdentityProvider creation response.getStatusInfo():{}, response.getEntity():{}",
                        response.getStatusInfo(), response.getEntity());
                String id = getCreatedId(response);
                logger.error("IdentityProvider creation id():{}", id);
               
                List<IdentityProviderRepresentation> identityProvider = findAllIdentityProviders(realmName);
                if (identityProvider != null && !identityProvider.isEmpty()) {
                    identityProvider.stream()
                            .forEach(e -> System.out.println(e.getInternalId() + "::" + e.getDisplayName()));
                }
                
                
                identityProviderRepresentation = getIdentityProviderByName(realmName,identityProviderRepresentation.getAlias());
                logger.error("Final identityProviderRepresentation:{}", identityProviderRepresentation);
               
                response.close();

            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ConfigurationException("Error while configuring SAML IDP ", ex);
        }

        return identityProviderRepresentation;
    }

    public IdentityProviderRepresentation updateIdentityProvider(String realmName,
            IdentityProviderRepresentation identityProviderRepresentation) {
        logger.error("Update IdentityProvider under realmName:{}, identityProviderRepresentation:{})", realmName,
                identityProviderRepresentation);

        //validations
        if (StringUtils.isBlank(realmName)) {
            new InvalidAttributeException("Realm name is null!!!");
        }

        if (identityProviderRepresentation == null) {
            new InvalidAttributeException("IdentityProviderRepresentation for updation is null!!!");
        }

       //validate IDP metadata
        logger.error("IDP metadata config while update identityProviderRepresentation.getConfig():{})", identityProviderRepresentation.getConfig());
        if (identityProviderRepresentation.getConfig() == null || identityProviderRepresentation.getConfig().isEmpty()) {
            new InvalidAttributeException("Idp Metedata config is null!!!");
        }
    
        boolean valid = verifySamlIdpConfig(identityProviderRepresentation.getConfig());
        logger.error("Is IDP metadata config valid?:{})", valid);
        if (!valid) {
            new InvalidAttributeException("Idp Metedata file is not valid !!!");
        }
        
        //validate IDP to update
        IdentityProvidersResource identityProvidersResource = getRealmResource(realmName).identityProviders();
        logger.error("IdentityProviderResource fetched for update realmName:{}, identityProviderRepresentation.getAlias():{}, identityProvidersResource:{})",
                realmName, identityProviderRepresentation.getAlias(), identityProvidersResource);

        if(identityProvidersResource==null) {
            new InvalidAttributeException("IdentityProvidersResource not found!!!");
        }
        
        IdentityProviderResource identityProviderResource =  identityProvidersResource.get(identityProviderRepresentation.getAlias());

        logger.error("Is IDP resource present for update identityProviderRepresentation.getAlias():{}, identityProviderResource:{}",identityProviderRepresentation.getAlias(), identityProviderResource);
        if(identityProviderResource==null) {
            new InvalidAttributeException("IdentityProvider not found to update!!!");
        }
        
        //update
        identityProviderResource.update(identityProviderRepresentation);
        identityProviderRepresentation = identityProviderResource.toRepresentation();

        
        logger.error("Updated IdentityProvider identityProviderRepresentation.getAlias():{} under realmName:{} is identityProviderRepresentation:{})", identityProviderRepresentation.getAlias(), realmName,
                identityProviderRepresentation);

        return identityProviderRepresentation;
    }

    public void deleteIdentityProvider(String realmName, String alias) {
        logger.error("IdentityProvider to delete realmName:{}, alias:{})", realmName, alias);
        if (StringUtils.isBlank(realmName) || StringUtils.isBlank(alias)) {
            new InvalidAttributeException("Realm name or IdentityProvider alias is null!!!");
        }
        IdentityProvidersResource identityProvidersResource = getRealmResource(realmName).identityProviders();
        logger.error("IdentityProviderResource fetched for delete realmName:{}, alias:{}, identityProvidersResource:{} ",
                realmName, alias, identityProvidersResource);

        if(identityProvidersResource==null) {
            new InvalidAttributeException("IdentityProvidersResource not found !!!");
        }
        
        IdentityProviderResource identityProviderResource =  identityProvidersResource.get(alias);
        
        if(identityProviderResource==null) {
            new InvalidAttributeException("IdentityProvidersResource not found to delete!!!");
        }
        identityProviderResource.remove();
        logger.error("Deleted IdentityProvider under realmName:{}, alias:{})", realmName, alias);
        
        IdentityProviderRepresentation identityProviderRepresentation = getIdentityProviderByName(realmName,alias);
        logger.error("Checking identityProvider is deleted - identityProviderRepresentation:{}", identityProviderRepresentation);
        
        if(identityProviderResource!=null) {
            new InvalidAttributeException("IdentityProviders could not be deleted!!!");
        }
        
        return;
    }

    public void getSAMLServiceProviderMetadata (String realmName, String alias) {
        //To-do
        
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
        logger.error("verifySamlConfig - config:{}", config);
        if (config == null || config.isEmpty()) {
            return false;
        }
        logger.error("config.keySet().containsAll(Constants.SAML_IDP_CONFIG):{}",
                config.keySet().containsAll(Constants.SAML_IDP_CONFIG));
        return config.keySet().containsAll(Constants.SAML_IDP_CONFIG);
    }

}
