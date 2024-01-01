/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.jans.as.common.service.OrganizationService;
import io.jans.configapi.core.util.DataUtil;
import io.jans.configapi.util.AuthUtil;

import io.jans.configapi.plugin.saml.client.IdpClientFactory;
import io.jans.configapi.plugin.saml.model.IdentityProvider;
import io.jans.configapi.plugin.saml.util.Constants;
import io.jans.model.SearchRequest;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.util.exception.InvalidAttributeException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

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
    IdpClientFactory idpClientFactory;

    @Inject
    AuthUtil authUtil;

    public String getIdentityProviderDn() {
        return samlConfigService.getTrustedIdpDn();
    }

    public String getSpMetadataUrl(String realm, String name) {
        return samlConfigService.getSpMetadataUrl(realm, name);
    }

    public List<IdentityProvider> getAllIdentityProviders(String realmName) throws IOException {
        return keycloakService.findAllIdentityProviders(realmName);
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

    public List<IdentityProvider> getAllIdp(String realmName) throws IOException {
        log.info("Fetch all IDP from realm:{}", realmName);
        if (StringUtils.isBlank(realmName)) {
            realmName = Constants.REALM_MASTER;
        }
        return keycloakService.findAllIdentityProviders(realmName);

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

        // Set Inum
        String inum = identityProviderService.generateInumForIdentityProvider();
        identityProvider.setInum(inum);
        identityProvider.setDn(identityProviderService.getDnForIdentityProvider(inum));

        // common code
        identityProvider = processIdentityProvider(identityProvider, idpMetadataStream, false);
        log.debug("Create IdentityProvider identityProvider:{}", identityProvider);

        // Create IDP in Jans DB
        identityProviderService.addSamlIdentityProvider(identityProvider, idpMetadataStream);
        log.debug("Created IdentityProvider in Jans DB -  identityProvider:{}", identityProvider);

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

        // common code
        identityProvider = processIdentityProvider(identityProvider, idpMetadataStream, true);
        log.debug("Update IdentityProvider identityProvider:{}", identityProvider);

        // Update IDP in Jans DB
        updateIdentityProvider(identityProvider);
        log.error("Updated IdentityProvider dentityProvider:{}, identityProvider.getRealm():{}", identityProvider,
                identityProvider.getRealm());
        return identityProvider;
    }

    public void deleteIdentityProvider(IdentityProvider identityProvider) {
        boolean status = false;
        log.error("Delete dentityProvider:{}, samlConfigService.isSamlEnabled():{}", identityProvider,
                samlConfigService.isSamlEnabled());
        // validate
        if (identityProvider == null) {
            throw new InvalidAttributeException("IdentityProvider object for delete is null!!!");
        }

        if (samlConfigService.isSamlEnabled()) {
            // Delete IDP in KC
            status = keycloakService.deleteIdentityProvider(identityProvider.getRealm(), identityProvider.getName());
        }
        log.error("Delete IDP status:{},)", status);
        // Delete in Jans DB
        if (status) {
            identityProviderService.removeIdentityProvider(identityProvider);
        }
    }

    public void processUnprocessedIdpMetadataFiles() {
        identityProviderService.processUnprocessedIdpMetadataFiles();
    }

    public String getSpMetadata(IdentityProvider identityProvider) throws JsonProcessingException {

        if (identityProvider == null) {
            throw new InvalidAttributeException("IdentityProvider object is null!!!");
        }
        return keycloakService.getSpMetadata(identityProvider.getRealm(), identityProvider.getName());

    }

    private IdentityProvider updateIdentityProvider(IdentityProvider identityProvider) throws IOException {
        log.info("Update IdentityProvider with IDP metadata file in identityProvider:{}", identityProvider);

        // Update IDP in Jans DB
        identityProviderService.updateIdentityProvider(identityProvider);
        log.debug("Updated IdentityProvider in Jans DB -  identityProvider:{}", identityProvider);

        return identityProvider;
    }

    private IdentityProvider setSamlIdentityProviderDefaultValue(IdentityProvider identityProvider, boolean update) {
        log.info("Setting default value for identityProvider:{}, update:{}", identityProvider, update);
        if (identityProvider == null) {
            return identityProvider;
        }

        // Set default Realm in-case null
        if (StringUtils.isBlank(identityProvider.getRealm())) {
            identityProvider.setRealm(Constants.REALM_MASTER);
        }

        if (StringUtils.isBlank(identityProvider.getProviderId())) {
            identityProvider.setProviderId(Constants.SAML);
        }
        return identityProvider;
    }

    private IdentityProvider processIdentityProvider(IdentityProvider identityProvider, InputStream idpMetadataStream,
            boolean isUpdate) throws IOException {
        log.error("Common processing for identityProvider:{}, idpMetadataStream:{}, isUpdate:{}", identityProvider,
                idpMetadataStream, isUpdate);

        if (identityProvider == null) {
            return identityProvider;
        }

        // Set default Value for SAML IDP
        setSamlIdentityProviderDefaultValue(identityProvider, isUpdate);

        // SAML IDP Metadata handling
        if (idpMetadataStream != null && idpMetadataStream.available() > 0) {
            Map<String, String> config = validateSamlMetadata(identityProvider.getProviderId(),
                    identityProvider.getRealm(), idpMetadataStream);
            log.error("Validated metadata to create IDP - config:{}", config);
            populateIdpMetadataElements(identityProvider, config);
        }

        // ensure individual metadata elements are present
        boolean validConfig = validateIdpMetadataElements(identityProvider);
        log.error("Is metadata individual elements for IDP creation present:{}", validConfig);

        if (samlConfigService.isSamlEnabled()) {
            // Create IDP in KC
            log.error("Create/Update IDP Service idpMetadataStream:{}, identityProvider.getRealm():{}",
                    idpMetadataStream, identityProvider.getRealm());
            identityProvider = keycloakService.createUpdateIdentityProvider(identityProvider.getRealm(), isUpdate,
                    identityProvider);

            log.error("Newly created identityProvider:{}", identityProvider);

            // set KC SP MetadataURL name
            if (identityProvider != null) {
                String spMetadataUrl = getSpMetadataUrl(identityProvider.getRealm(), identityProvider.getName());
                log.error(" Setting KC SP Metadata URL - spMetadataUrl:{} ", spMetadataUrl);
                identityProvider.setSpMetaDataURL(spMetadataUrl);
            }
        }
        return identityProvider;
    }

    private Map<String, String> validateSamlMetadata(String prorviderId, String realmName,
            InputStream idpMetadataStream) throws JsonProcessingException {
        return keycloakService.importSamlMetadata(prorviderId, realmName, idpMetadataStream);
    }

    private boolean validateIdpMetadataElements(IdentityProvider identityProvider) {
        log.error("identityProvider:{}, samlConfigService.getIdpMetadataMandatoryAttributes():{}", identityProvider,
                samlConfigService.getIdpMetadataMandatoryAttributes());
        boolean isValid = false;
        if (identityProvider == null || samlConfigService.getIdpMetadataMandatoryAttributes().isEmpty()) {
            isValid = true;
            return isValid;
        }

        List<String> missingElements = null;
        for (String attribute : samlConfigService.getIdpMetadataMandatoryAttributes()) {
            log.error("attribute:{}, getValue(identityProvider, attribute):{}", attribute,
                    getValue(identityProvider, attribute));
            if (StringUtils.isBlank(getValue(identityProvider, attribute))) {
                if (missingElements == null) {
                    missingElements = new ArrayList<>();
                }
                missingElements.add(attribute);
            }
        }

        log.info("missingElements:{}", missingElements);

        if (missingElements != null && !missingElements.isEmpty()) {
            isValid = false;
            log.error("IDP elements are missing:{}, isValid:{} !", missingElements, isValid);
            throw new InvalidAttributeException("IDP mandatory attribute missing - " + missingElements + " !!!");
        }
        isValid = true;
        log.info("validateIdpMetadataElements - isValid:{}", isValid);
        return isValid;
    }

    private IdentityProvider populateIdpMetadataElements(IdentityProvider identityProvider,
            Map<String, String> config) {
        log.error("identityProvider:{}, config:{}, samlConfigService.getKcSamlConfig():{}", identityProvider, config,
                samlConfigService.getKcSamlConfig());

        if (identityProvider == null || config == null || samlConfigService.getKcSamlConfig().isEmpty()) {
            return identityProvider;
        }

        for (String attribute : samlConfigService.getKcSamlConfig()) {
            log.error("attribute:{}, config.get(attribute):{}", attribute,
                    config.get(attribute));
                DataUtil.invokeReflectionSetter(identityProvider, attribute, config.get(attribute));        
        }

        log.error("validateIdpMetadataElements - identityProvider:{}", identityProvider);
        return identityProvider;
    }

    private String getValue(IdentityProvider identityProvider, String property) {
        log.error("Get Field Value - identityProvider:{}, property:{}", identityProvider, property);
        String value = null;
        try {
            value = (String) DataUtil.getValue(identityProvider, property);
            log.error("Field Value - property:{}, value:{}", property, value);
        } catch (Exception ex) {
            log.error("Error while getting value of config ", ex);
        }
        return value;
    }

}
