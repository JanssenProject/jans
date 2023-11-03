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
    ConfigurationFactory configurationFactory;

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
            log.error("IDP to be fetched from realmName:{}, inum:{}", realmName, inum);
            IdentityProviderRepresentation kcIdp = keycloakService.getIdentityProviderById(realmName, inum);
            idp = this.convertToIdentityProvider(kcIdp);
        } catch (Exception ex) {
            log.error("Failed to fetch IdentityProvider entry", ex);
        }
        return idp;
    }

    public List<IdentityProvider> getAllIdentityProviders(String realmName) {
        log.error("All IDP to be fetched from realmName:{}", realmName);
        List<IdentityProviderRepresentation> kcIdpList = keycloakService.findAllIdentityProviders(realmName);
        log.error("kcIdpList:{}", kcIdpList);
        
        return this.convertToIdentityProviderList(kcIdpList);
    }
    
    public IdentityProvider getIdentityProviderByName(String realmName, String alias) {
        log.info("Get IdentityProvider by name realmName:{}, alias:{}", realmName, alias);
        IdentityProviderRepresentation kcIdp = keycloakService.getIdentityProviderByName(realmName,alias);
        log.error("kcIdp:{}", kcIdp);

        return this.convertToIdentityProvider(kcIdp);
    }
    
    public IdentityProvider createIdentityProvider(String realmName, IdentityProvider identityProvider) throws IOException{
        log.info("Create IdentityProvider in realmName:{}, identityProvider:{}", realmName, identityProvider);
        
        //Create IDP in Jans DB
        identityProviderService.addSamlIdentityProvider(identityProvider);
        log.info("Created IdentityProvider in Jans DB -  identityProvider:{}", identityProvider);
        
        // Create IDP in KC
        IdentityProviderRepresentation kcIdp = this.convertToIdentityProviderRepresentation(identityProvider);
        log.error("converted kcIdp:{}", kcIdp);
        
        kcIdp = keycloakService.createIdentityProvider(realmName,kcIdp);
        log.error("kcIdp:{}", kcIdp);
     
        return this.convertToIdentityProvider(kcIdp);
    }
    
    public IdentityProvider createIdentityProvider(IdentityProvider identityProvider, InputStream idpMetadataStream) throws IOException{
        log.info("Create IdentityProvider with IDP metadata file in identityProvider:{}, idpMetadataStream:{}", identityProvider, idpMetadataStream);
        
        //Create IDP in Jans DB
        identityProviderService.addSamlIdentityProvider(identityProvider, idpMetadataStream);
        log.info("Create IdentityProvider in Jans DB -  identityProvider:{})", identityProvider);
        
        // Create IDP in KC
        IdentityProviderRepresentation kcIdp = this.convertToIdentityProviderRepresentation(identityProvider);
        log.error("converted kcIdp:{}", kcIdp);
        
        kcIdp = keycloakService.createIdentityProvider(identityProvider.getRealm(),kcIdp, idpMetadataStream);
        log.error("kcIdp:{}", kcIdp);

        return this.convertToIdentityProvider(kcIdp);
    }
    
    
    public IdentityProvider createIdentityProvider(String realmName, IdentityProvider identityProvider, InputStream idpMetadataStream) throws IOException{
        log.info("Create IdentityProvider with IDP metadata file in realmName:{}, identityProvider:{}, idpMetadataStream:{}", realmName, identityProvider, idpMetadataStream);
        
        //Create IDP in Jans DB
        identityProviderService.addSamlIdentityProvider(identityProvider, idpMetadataStream);
        log.info("Create IdentityProvider in Jans DB -  identityProvider:{})", identityProvider);
        
        // Create IDP in KC
        IdentityProviderRepresentation kcIdp = this.convertToIdentityProviderRepresentation(identityProvider);
        log.error("converted kcIdp:{}", kcIdp);
        
        kcIdp = keycloakService.createIdentityProvider(realmName,kcIdp,idpMetadataStream);
        log.error("kcIdp:{}", kcIdp);

        return this.convertToIdentityProvider(kcIdp);
    }
    
    public IdentityProvider updateProvider(IdentityProvider identityProvider, InputStream idpMetadataStream) throws IOException{
        log.info("Update IdentityProvider with IDP metadata file in identityProvider:{}, idpMetadataStream:{}", identityProvider, idpMetadataStream);
        
        //Create IDP in Jans DB
        identityProviderService.addSamlIdentityProvider(identityProvider, idpMetadataStream);
        log.info("Update IdentityProvider in Jans DB -  identityProvider:{})", identityProvider);
        
        // Create IDP in KC
        IdentityProviderRepresentation kcIdp = this.convertToIdentityProviderRepresentation(identityProvider);
        log.error("converted kcIdp:{}", kcIdp);
        
        kcIdp = keycloakService.createIdentityProvider(identityProvider.getRealm(),kcIdp, idpMetadataStream);
        log.error("kcIdp:{}", kcIdp);

        return this.convertToIdentityProvider(kcIdp);
    }
    
    private List<IdentityProvider> convertToIdentityProviderList(List<IdentityProviderRepresentation> kcIdpList){
        log.error("kcIdpList:{}", kcIdpList);
        List<IdentityProvider> idpList = null;
        if(kcIdpList==null || kcIdpList.isEmpty()) {
            return idpList;
        }
        idpList = kcIdpList.stream().map(element -> identityProviderMapper.kcIdentityProviderToIdentityProvider(element)).collect(Collectors.toList());
        log.error("idpList:{}", idpList);
        
        return idpList;
    }
    
    private IdentityProvider convertToIdentityProvider(IdentityProviderRepresentation kcIdp){
        log.error("kcIdp:{}", kcIdp);
        IdentityProvider idp = null;
        if(kcIdp==null) {
            return idp;
        }
        idp = identityProviderMapper.kcIdentityProviderToIdentityProvider(kcIdp);
        log.error("idpList:{}", idp);
        
        return idp;
    }
    
    private IdentityProviderRepresentation convertToIdentityProviderRepresentation(IdentityProvider idp){
        log.error("idp:{}", idp);
        IdentityProviderRepresentation kcIdp = null;
        if(idp==null) {
            return kcIdp;
        }
        kcIdp = identityProviderMapper.identityProviderToKCIdentityProvider(idp);
        log.error("kcIdp:{}", kcIdp);
        
        return kcIdp;
    }

  

}
