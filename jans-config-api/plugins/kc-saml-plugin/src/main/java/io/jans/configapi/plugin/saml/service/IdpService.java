/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.jans.as.common.service.OrganizationService;
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
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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

        // Set default Value for SAML IDP
        setSamlIdentityProviderDefaultValue(identityProvider, false);

        // SAML IDP Metadata handling
        if (idpMetadataStream != null && idpMetadataStream.available() > 0) {

            Map<String, String> config = keycloakService.importSamlMetadata(Constants.SAML, identityProvider.getRealm(),
                    idpMetadataStream);
            log.info("Validated metadata to create IDP - config:{}", config);
            identityProvider.setConfig(config);
        } else {
            // ensure individual metadata elements are present
            boolean validConfig = validateIdpMetadataElements(identityProvider);
            log.info("Is metadata individual elements for IDP creation present:{}", validConfig);
        }

        if (samlConfigService.isSamlEnabled()) {
            // Create IDP in KC
            log.debug("IDP Service idpMetadataStream:{}, identityProvider.getRealm():{}", idpMetadataStream,
                    identityProvider.getRealm());
            identityProvider = keycloakService.createIdentityProvider(identityProvider.getRealm(), identityProvider);

            // log.debug("Newly created kcIdp:{}", kcIdp);

            // set KC SP MetadataURL name
            if (identityProvider != null) {
                String spMetadataUrl = getSpMetadataUrl(identityProvider.getRealm(), identityProvider.getName());
                log.info(" Setting KC SP Metadata URL - spMetadataUrl:{} ", spMetadataUrl);
                identityProvider.setSpMetaDataURL(spMetadataUrl);
            }
        }

        // Create IDP in Jans DB
        log.debug("Create IdentityProvider identityProvider:{})", identityProvider);
        identityProviderService.addSamlIdentityProvider(identityProvider, idpMetadataStream);
        log.debug("Created IdentityProvider in Jans DB -  identityProvider:{})", identityProvider);

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

        // Set default Value for SAML IDP
        setSamlIdentityProviderDefaultValue(identityProvider, true);

        // SAML IDP Metadata handling
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

        if (samlConfigService.isSamlEnabled()) {
            // Update IDP in KC

            identityProvider = keycloakService.updateIdentityProvider(identityProvider.getRealm(), identityProvider);
            log.debug("Updated identityProvider:{}", identityProvider);

            // set KC SP MetadataURL name
            if (identityProvider != null) {
                String spMetadataUrl = getSpMetadataUrl(identityProvider.getRealm(), identityProvider.getName());
                log.info(" Updating KC SP Metadata URL - spMetadataUrl:{} ", spMetadataUrl);
                identityProvider.setSpMetaDataURL(spMetadataUrl);
            }
        }

        // Update IDP in Jans DB
        updateIdentityProvider(identityProvider);
        log.debug("Updated IdentityProvider dentityProvider:{}, , identityProvider.getRealm():{})", identityProvider,
                identityProvider.getRealm());
        return identityProvider;
    }

    public void deleteIdentityProvider(IdentityProvider identityProvider) {

        if (samlConfigService.isSamlEnabled()) {
            // Delete IDP in KC
            // keycloakService.deleteIdentityProvider(identityProvider.getRealm(),
            // identityProvider.getName());
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

    private IdentityProvider setSamlIdentityProviderDefaultValue(IdentityProvider identityProvider, boolean update) {
        log.info("setting default value for identityProvider:{}, update:{}", identityProvider, update);
        if (identityProvider == null) {
            return identityProvider;
        }

        // Set default Realm in-case null
        if (StringUtils.isBlank(identityProvider.getRealm())) {
            identityProvider.setRealm(Constants.REALM_MASTER);
        }

        if (!update) {
            identityProvider.setProviderId(Constants.SAML);
        }
        return identityProvider;
    }

    private Map<String, String> validateSamlMetadata(String realmName, InputStream idpMetadataStream)
            throws JsonProcessingException {
        return keycloakService.importSamlMetadata(null, realmName, idpMetadataStream);
    }

    private boolean validateIdpMetadataElements(IdentityProvider identityProvider) {
        log.info("identityProvider:{}, samlConfigService.getIdpMetadataMandatoryAttributes():{}", identityProvider,
                samlConfigService.getIdpMetadataMandatoryAttributes());
        boolean isValid = false;
        if (identityProvider == null || identityProvider.getConfig() == null || identityProvider.getConfig().isEmpty()
                || samlConfigService.getIdpMetadataMandatoryAttributes().isEmpty()) {
            isValid = true;
            return isValid;
        }

        List<String> missingElements = null;
        for (String attribute : samlConfigService.getIdpMetadataMandatoryAttributes()) {
            log.info("attribute:{}", attribute);
            if (StringUtils.isBlank(identityProvider.getConfig().get(attribute))) {
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
}
