/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.service;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.common.InumService;
import io.jans.as.common.service.OrganizationService;
import io.jans.as.common.util.AttributeConstants;
import io.jans.configapi.util.AuthUtil;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.plugin.saml.client.IdpClientFactory;
import io.jans.configapi.plugin.saml.mapper.IdentityProviderMapper;
import io.jans.configapi.plugin.saml.model.IdentityProvider;
import io.jans.configapi.plugin.saml.service.SamlService;
import io.jans.configapi.plugin.saml.timer.MetadataValidationTimer;
import io.jans.configapi.plugin.saml.util.Constants;

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

import jakarta.ws.rs.core.Response;

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
    SamlConfigService samlConfigService;

    @Inject
    OrganizationService organizationService;

    @Inject
    IdentityProviderService identityProviderService;

    @Inject
    KeycloakService keycloakService;

    @Inject
    IdentityProviderMapper identityProviderMapper;

    @Inject
    IdpClientFactory idpClientFactory;

    @Inject
    AuthUtil authUtil;

    public String getIdentityProviderDn() {
        return samlConfigService.getTrustedIdpDn();
    }

    public String getSpMetadataUrl(String realm, String name) {
        return samlConfigService.getSpMetadataUrl(realm, name);
    }

    public List<IdentityProvider> getAllIdentityProviders() {
        return this.identityProviderService.getAllIdentityProvider(0);
    }

    public IdentityProvider getIdentityProviderByInum(String inum) {
        return identityProviderService.getIdentityProviderByInum(inum);
    }

    public List<IdentityProvider> getIdentityProviderByName(String name) {
        return identityProviderService.getIdentityProviderByName(name);
    }

    public PagedResult<IdentityProvider> getIdentityProviders(SearchRequest searchRequest) {
        return identityProviderService.getIdentityProvider(searchRequest);
    }

    public IdentityProvider createSamlIdentityProvider(IdentityProvider identityProvider, InputStream idpMetadataStream)
            throws IOException {
        log.info(
                "Create IdentityProvider with IDP metadata file in identityProvider:{}, idpMetadataStream:{}, samlConfigService.isSamlEnabled():{}",
                identityProvider, idpMetadataStream, samlConfigService.isSamlEnabled());

        // validate
        if (identityProvider == null) {
            throw new InvalidAttributeException("IdentityProvider object is null!!!");
        }

        if (idpMetadataStream != null && idpMetadataStream.available() > 0) {
            // validate metadata and set in config
            Map<String, String> config = validateSamlMetadata(identityProvider.getRealm(), idpMetadataStream);
            log.info("Validated metadata to create IDP - config:{}", config);
            identityProvider.setConfig(config);
        } else {
            // ensure individual metadata elements are present
            boolean validConfig = validateIdpMetadataElements(identityProvider);
            log.info("Is metadata individual elements for IDP creation present:{}", validConfig);
        }

        // Create IDP in Jans DB
        log.debug("Create IdentityProvider identityProvider:{})", identityProvider);
        identityProviderService.addSamlIdentityProvider(identityProvider, idpMetadataStream);
        log.debug("Created IdentityProvider in Jans DB -  identityProvider:{})", identityProvider);

        if (samlConfigService.isSamlEnabled()) {
            // Create IDP in KC
            IdentityProviderRepresentation kcIdp = this.convertToIdentityProviderRepresentation(identityProvider);
            log.debug("converted kcIdp:{}", kcIdp);

            log.debug("IDP Service idpMetadataStream:{}, identityProvider.getRealm():{}", idpMetadataStream,
                    identityProvider.getRealm());
            kcIdp = keycloakService.createIdentityProvider(identityProvider.getRealm(), kcIdp);
            log.debug("Newly created kcIdp:{}", kcIdp);
            identityProvider = this.convertToIdentityProvider(identityProvider, kcIdp);
            log.debug("Final created identityProvider:{}", identityProvider);

            // set KC SP MetadataURL name
            if (identityProvider != null) {
                String spMetadataUrl = getSpMetadataUrl(identityProvider.getRealm(), identityProvider.getName());
                log.info(" Setting KC SP Metadata URL - spMetadataUrl:{} ", spMetadataUrl);
                identityProvider.setSpMetaDataURL(spMetadataUrl);
            }
        }
        return identityProvider;
    }

    public IdentityProvider updateSamlIdentityProvider(IdentityProvider identityProvider, InputStream idpMetadataStream)
            throws IOException {
        log.debug(
                "Update IdentityProvider with IDP metadata file in - identityProvider:{}, idpMetadataStream:{}, samlConfigService.isSamlEnabled():{}",
                identityProvider, idpMetadataStream, samlConfigService.isSamlEnabled());

        // validate
        if (identityProvider == null) {
            throw new InvalidAttributeException("IdentityProvider object for update is null!!!");
        }

        if (idpMetadataStream != null && idpMetadataStream.available() > 0) {
            // validate metadata and set in config
            Map<String, String> config = validateSamlMetadata(identityProvider.getRealm(), idpMetadataStream);
            log.debug("Validated metadata to update IDP - config:{}", config);
            identityProvider.setConfig(config);
        } else {
            // ensure individual metadata elements are present
            boolean validConfig = validateIdpMetadataElements(identityProvider);
            log.info("Is metadata individual for update elements present:{}", validConfig);
        }

        // validate metadata and set in config
        Map<String, String> config = validateSamlMetadata(identityProvider.getRealm(), idpMetadataStream);
        log.debug("Validated metadata to update config:{}", config);
        identityProvider.setConfig(config);

        // Update IDP in Jans DB
        updateIdentityProvider(identityProvider);
        log.debug("Updated IdentityProvider dentityProvider:{}, , identityProvider.getRealm():{})", identityProvider,
                identityProvider.getRealm());

        if (samlConfigService.isSamlEnabled()) {
            // Update IDP in KC
            IdentityProviderRepresentation kcIdp = this.convertToIdentityProviderRepresentation(identityProvider);
            log.debug("converted kcIdp:{}", kcIdp);

            kcIdp = keycloakService.updateIdentityProvider(identityProvider.getRealm(), kcIdp);
            log.debug("Updated kcIdp:{}", kcIdp);
            identityProvider = this.convertToIdentityProvider(identityProvider, kcIdp);

            // set KC SP MetadataURL name
            if (identityProvider != null) {
                String spMetadataUrl = getSpMetadataUrl(identityProvider.getRealm(), identityProvider.getName());
                log.info(" Updating KC SP Metadata URL - spMetadataUrl:{} ", spMetadataUrl);
                identityProvider.setSpMetaDataURL(spMetadataUrl);
            }
        }
        return identityProvider;
    }

    public void deleteIdentityProvider(IdentityProvider identityProvider) {

        if (samlConfigService.isSamlEnabled()) {
            // Delete IDP in KC
            keycloakService.deleteIdentityProvider(identityProvider.getRealm(), identityProvider.getName());
        }
        // Delete in Jans DB
        identityProviderService.removeIdentityProvider(identityProvider);
    }

    public void processUnprocessedIdpMetadataFiles() {
        identityProviderService.processUnprocessedIdpMetadataFiles();
    }

    public Response getSpMetadata(IdentityProvider identityProvider) {
        Response response = null;
        if (identityProvider == null) {
            return response;
        }
        return idpClientFactory
                .getSpMetadata(getSpMetadataUrl(identityProvider.getRealm(), identityProvider.getName()));

    }

    private IdentityProvider updateIdentityProvider(IdentityProvider identityProvider) throws IOException {
        log.info("Update IdentityProvider with IDP metadata file in identityProvider:{}", identityProvider);

        // Update IDP in Jans DB
        identityProviderService.updateIdentityProvider(identityProvider);
        log.debug("Updated IdentityProvider in Jans DB -  identityProvider:{})", identityProvider);

        return identityProvider;
    }

    private Map<String, String> validateSamlMetadata(String realmName, InputStream idpMetadataStream) {
        return keycloakService.validateSamlMetadata(realmName, idpMetadataStream);
    }

    private IdentityProvider convertToIdentityProvider(IdentityProvider identityProvider,
            IdentityProviderRepresentation kcIdp) {
        log.debug("identityProvider:{}, kcIdp:{}", identityProvider, kcIdp);

        IdentityProvider idp = this.convertToIdentityProvider(kcIdp);
        log.info("convertToIdentityProvider - idp:{}", idp);

        if (idp != null && identityProvider != null) {
            idp.setRealm(identityProvider.getRealm());
            idp.setSpMetaDataFN(identityProvider.getSpMetaDataFN());
            idp.setSpMetaDataURL(identityProvider.getSpMetaDataURL());
            idp.setSpMetaDataLocation(identityProvider.getSpMetaDataLocation());
            idp.setIdpMetaDataFN(identityProvider.getIdpMetaDataFN());
            idp.setIdpMetaDataLocation(identityProvider.getIdpMetaDataLocation());
            idp.setIdpMetaDataURL(identityProvider.getIdpMetaDataURL());
            idp.setStatus(identityProvider.getStatus());
            idp.setValidationStatus(identityProvider.getValidationStatus());
            idp.setValidationLog(identityProvider.getValidationLog());
        }

        return idp;
    }

    private IdentityProvider convertToIdentityProvider(IdentityProviderRepresentation kcIdp) {
        log.debug("kcIdp:{}", kcIdp);
        IdentityProvider idp = null;
        if (kcIdp == null) {
            return idp;
        }
        idp = identityProviderMapper.kcIdentityProviderToIdentityProvider(kcIdp);
        log.info("convertToIdentityProvider - idp:{}", idp);

        return idp;
    }

    private IdentityProviderRepresentation convertToIdentityProviderRepresentation(IdentityProvider idp) {
        log.info("idp:{}", idp);
        IdentityProviderRepresentation kcIdp = null;
        if (idp == null) {
            return kcIdp;
        }
        kcIdp = identityProviderMapper.identityProviderToKCIdentityProvider(idp);
        log.debug("convert IdentityProviderRepresentation - kcIdp:{}", kcIdp);

        log.trace(
                "convert IDP data kcIdp.getAlias():{}, kcIdp.getInternalId():{}, kcIdp.getProviderId():{}, kcIdp.getConfig():{}, kcIdp.isEnabled():{}, kcIdp.isLinkOnly():{}, kcIdp.isStoreToken():{},kcIdp.getFirstBrokerLoginFlowAlias():{}, kcIdp.getPostBrokerLoginFlowAlias():{},kcIdp.isTrustEmail():{}",
                kcIdp.getAlias(), kcIdp.getInternalId(), kcIdp.getProviderId(), kcIdp.getConfig(), kcIdp.isEnabled(),
                kcIdp.isLinkOnly(), kcIdp.isStoreToken(), kcIdp.getFirstBrokerLoginFlowAlias(),
                kcIdp.getPostBrokerLoginFlowAlias(), kcIdp.isTrustEmail());

        return kcIdp;
    }

    private boolean validateIdpMetadataElements(IdentityProvider identityProvider) {
        log.info("identityProvider:{}, samlConfigService.getIdpMetadataMandatoryAttributes():{}", identityProvider, samlConfigService.getIdpMetadataMandatoryAttributes());
        boolean isValid = true;
        if (identityProvider == null || identityProvider.getConfig() == null
                || identityProvider.getConfig().isEmpty() || samlConfigService.getIdpMetadataMandatoryAttributes().isEmpty()) {
            return isValid;
        }

        List<String> missingElements = null;
        for(String attribute: samlConfigService.getIdpMetadataMandatoryAttributes()) {
            log.info("attribute:{}", attribute);
            if(StringUtils.isBlank(identityProvider.getConfig().get(attribute))){
                if(missingElements == null) {
                    missingElements = new ArrayList<>();
                }
                missingElements.add(attribute);
            }           
        }
      
        log.info("missingElements:{}", missingElements);

        if (missingElements != null && !missingElements.isEmpty()) {
            log.error("IDP elements are missing:{} !", missingElements);
            throw new InvalidAttributeException("IDP mandatory attribute missing - "+missingElements+" !!!");
        }
        isValid = true;
        log.info("validateIdpMetadataElements - isValid:{}", isValid);
        return isValid;
    }
}
