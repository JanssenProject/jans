/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.service;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.common.InumService;
import io.jans.as.common.service.OrganizationService;
import io.jans.as.common.util.AttributeConstants;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.plugin.keycloak.idp.broker.service.SamlService;
import io.jans.configapi.plugin.keycloak.idp.broker.timer.IdpMetadataValidationTimer;
import io.jans.configapi.plugin.keycloak.idp.broker.model.IdentityProvider;
import io.jans.configapi.plugin.keycloak.idp.broker.service.IdpConfigService;
import io.jans.configapi.plugin.keycloak.idp.broker.util.Constants;
import io.jans.configapi.plugin.keycloak.idp.broker.mapper.IdentityProviderMapper;

import io.jans.model.GluuStatus;
import io.jans.model.SearchRequest;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import io.jans.util.exception.InvalidAttributeException;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;

import org.keycloak.representations.idm.IdentityProviderRepresentation;

@ApplicationScoped
public class IdpService {

    @Inject
    Logger log;

    @Inject
    PersistenceEntryManager persistenceEntryManager;

    @Inject
    IdpConfigService idpConfigService;

    @Inject
    OrganizationService organizationService;

    @Inject
    IdentityProviderService identityProviderService;

    @Inject
    KeycloakService keycloakService;

    @Inject
    IdentityProviderMapper identityProviderMapper;

    public IdentityProvider getIdentityProviderByInum(String realmName, String inum) {
        IdentityProvider idp = null;
        try {
            log.error("IDP to be fetched from realmName:{}, inum:{}, idpConfigService.isIdpEnabled():{}", realmName,
                    inum, idpConfigService.isIdpEnabled());
            if (idpConfigService.isIdpEnabled()) {
                IdentityProviderRepresentation kcIdp = keycloakService.getIdentityProviderById(realmName, inum);
                idp = this.convertToIdentityProvider(kcIdp);
            } else {
                identityProviderService.getAllIdentityProviderByInum(inum);
            }
        } catch (Exception ex) {
            log.error("Failed to fetch IdentityProvider entry", ex);
        }
        return idp;
    }

    public List<IdentityProvider> getAllIdentityProviders(String realmName) {
        log.error("All IDP to be fetched from realmName:{}, idpConfigService.isIdpEnabled():{}", realmName,
                idpConfigService.isIdpEnabled());
        if (idpConfigService.isIdpEnabled()) {
            List<IdentityProviderRepresentation> kcIdpList = keycloakService.findAllIdentityProviders(realmName);
            log.error("kcIdpList:{}", kcIdpList);
            return this.convertToIdentityProviderList(kcIdpList);
        } else {
            return this.identityProviderService.getAllIdentityProvider(0);
        }
    }

    public IdentityProvider getIdentityProviderByName(String realmName, String alias) {
        log.error("Get IdentityProvider by name realmName:{}, alias:{}, idpConfigService.isIdpEnabled():{}", realmName,
                alias, idpConfigService.isIdpEnabled());
        if (idpConfigService.isIdpEnabled()) {
            IdentityProviderRepresentation kcIdp = keycloakService.getIdentityProviderByName(realmName, alias);
            log.error("kcIdp:{}", kcIdp);
            return this.convertToIdentityProvider(kcIdp);
        } else {
            return this.getIdentityProviderByName(realmName, alias);
        }
    }
    
    public IdentityProvider createSamlIdentityProvider(IdentityProvider identityProvider,
            InputStream idpMetadataStream) throws IOException {
        log.error(
                "Create IdentityProvider with IDP metadata file in identityProvider:{}, idpMetadataStream:{}, idpConfigService.isIdpEnabled():{}",
                identityProvider, idpMetadataStream, idpConfigService.isIdpEnabled());
        
        //validate        
        if (identityProvider == null) {
            new InvalidAttributeException("IdentityProvider  is null!!!");
        }
        
        if (idpMetadataStream == null) {
            new InvalidAttributeException("Idp Metedata file is null!!!");
        }
       
        //validate metadata and set in config
        Map<String, String> config = validateSamlMetadata(identityProvider.getRealm(),idpMetadataStream);
        log.error("Validated metadata to create IDP - config:{}", config);
        identityProvider.setConfig(config);
        
        // Create IDP in Jans DB
        log.error("Create IdentityProvider identityProvider:{})", identityProvider);
        identityProviderService.addSamlIdentityProvider(identityProvider, idpMetadataStream);
        log.error("Created IdentityProvider in Jans DB -  identityProvider:{})", identityProvider);

        if (idpConfigService.isIdpEnabled()) {
            // Create IDP in KC
            IdentityProviderRepresentation kcIdp = this.convertToIdentityProviderRepresentation(identityProvider);
            log.error("converted kcIdp:{}", kcIdp);

            log.error("IDP Service idpMetadataStream:{}", idpMetadataStream);    
            log.error("IDP Service idpMetadataStream.available():{}", idpMetadataStream.available());    
            kcIdp = keycloakService.createIdentityProvider(identityProvider.getRealm(), kcIdp);
            log.error("kcIdp:{}", kcIdp);
            identityProvider = this.convertToIdentityProvider(kcIdp);
            log.error("Final created identityProvider:{}", identityProvider);

        }
        return identityProvider;
    }

    public IdentityProvider updateSamlIdentityProvider(IdentityProvider identityProvider, InputStream idpMetadataStream)
            throws IOException {
        log.error(
                "Update IdentityProvider with IDP metadata file in - identityProvider:{}, idpMetadataStream:{}, idpConfigService.isIdpEnabled():{}",
                identityProvider, idpMetadataStream, idpConfigService.isIdpEnabled());

        //validate metadata and set in config
        Map<String, String> config = validateSamlMetadata(identityProvider.getRealm(),idpMetadataStream);
        log.error("Validated metadata to update config:{}", config);
        identityProvider.setConfig(config);
       
        //Update IDP in Jans DB
        updateIdentityProvider(identityProvider);
        log.error("Updated IdentityProvider dentityProvider:{})", identityProvider);

        if (idpConfigService.isIdpEnabled()) {
            // Update IDP in KC
            IdentityProviderRepresentation kcIdp = this.convertToIdentityProviderRepresentation(identityProvider);
            log.error("converted kcIdp:{}", kcIdp);

            kcIdp = keycloakService.updateIdentityProvider(identityProvider.getRealm(), kcIdp);
            log.error("kcIdp:{}", kcIdp);
            identityProvider = this.convertToIdentityProvider(kcIdp);
        }
        return identityProvider;
    }
    
    private IdentityProvider updateIdentityProvider(IdentityProvider identityProvider)
            throws IOException {
        log.error(
                "Update IdentityProvider with IDP metadata file in identityProvider:{}", identityProvider);

        // Update IDP in Jans DB
        identityProviderService.updateIdentityProvider(identityProvider);
        log.error("Updated IdentityProvider in Jans DB -  identityProvider:{})", identityProvider);

        return identityProvider;
    }
    
    private Map<String, String> validateSamlMetadata(String realmName,InputStream idpMetadataStream) {       
        return keycloakService.validateSamlMetadata(realmName,idpMetadataStream);        
    }

    private List<IdentityProvider> convertToIdentityProviderList(List<IdentityProviderRepresentation> kcIdpList) {
        log.error("kcIdpList:{}", kcIdpList);
        List<IdentityProvider> idpList = null;
        if (kcIdpList == null || kcIdpList.isEmpty()) {
            return idpList;
        }
        idpList = kcIdpList.stream()
                .map(element -> identityProviderMapper.kcIdentityProviderToIdentityProvider(element))
                .collect(Collectors.toList());
        log.error("idpList:{}", idpList);

        return idpList;
    }

    private IdentityProvider convertToIdentityProvider(IdentityProviderRepresentation kcIdp) {
        log.error("kcIdp:{}", kcIdp);
        IdentityProvider idp = null;
        if (kcIdp == null) {
            return idp;
        }
        idp = identityProviderMapper.kcIdentityProviderToIdentityProvider(kcIdp);
        log.error("convertToIdentityProvider - idp:{}", idp);

        return idp;
    }

    private IdentityProviderRepresentation convertToIdentityProviderRepresentation(IdentityProvider idp) {
        log.error("idp:{}", idp);
        IdentityProviderRepresentation kcIdp = null;
        if (idp == null) {
            return kcIdp;
        }
        kcIdp = identityProviderMapper.identityProviderToKCIdentityProvider(idp);
        log.error("convert IdentityProviderRepresentation - kcIdp:{}", kcIdp);
        
        log.error("convert IDP data kcIdp.getAlias():{}, kcIdp.getInternalId():{}, kcIdp.getProviderId():{}, kcIdp.getConfig():{}, kcIdp.isEnabled():{}, kcIdp.isLinkOnly():{}, kcIdp.isStoreToken():{},kcIdp.getFirstBrokerLoginFlowAlias():{}, kcIdp.getPostBrokerLoginFlowAlias():{},kcIdp.isTrustEmail():{}", kcIdp.getAlias(),kcIdp.getInternalId(), kcIdp.getProviderId(), kcIdp.getConfig(),kcIdp.isEnabled(), kcIdp.isLinkOnly(), kcIdp.isStoreToken(), kcIdp.getFirstBrokerLoginFlowAlias(), kcIdp.getPostBrokerLoginFlowAlias(), kcIdp.isTrustEmail() );

        return kcIdp;
    }

}
