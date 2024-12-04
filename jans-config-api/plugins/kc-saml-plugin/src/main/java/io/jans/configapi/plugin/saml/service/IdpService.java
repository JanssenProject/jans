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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
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

    public String getRealm() {
        String realm = samlConfigService.getRealm();
        log.debug("realm:{}", realm);
        if (StringUtils.isBlank(realm)) {
            realm = Constants.REALM_MASTER;
        }
        log.debug("Final realm:{}", realm);
        return realm;
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
        List<IdentityProvider> list = null;
        try {
            list = identityProviderService.getIdentityProviderByName(name);
        } catch (Exception ex) {
            log.error("Error while finding IDP with name:{} is:{}", name, ex);
        }
        return list;
    }

    public PagedResult<IdentityProvider> getIdentityProviders(SearchRequest searchRequest) {
        return identityProviderService.getIdentityProvider(searchRequest);
    }

    public List<IdentityProvider> getAllIdp(String realmName) throws IOException {
        log.info("Fetch all IDP from realm:{}", realmName);
        if (StringUtils.isBlank(realmName)) {
            realmName = getRealm();
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
        ByteArrayOutputStream bos = getByteArrayOutputStream(idpMetadataStream);
        identityProvider = processIdentityProvider(identityProvider, getInputStream(bos), false);
        log.debug("Create IdentityProvider identityProvider:{}", identityProvider);

        try {
            // Create IDP in Jans DB
            identityProviderService.addSamlIdentityProvider(identityProvider, getInputStream(bos));
            log.debug("Created IdentityProvider in Jans DB -  identityProvider:{}", identityProvider);
        } catch (Exception ex) {
            log.error("Deleting KC IDP as error while persisting identityProvider:{}", identityProvider);
            deleteIdentityProvider(identityProvider, false);
            throw ex;
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

        // common code
        ByteArrayOutputStream bos = getByteArrayOutputStream(idpMetadataStream);
        identityProvider = processIdentityProvider(identityProvider, getInputStream(bos), true);
        log.debug("Update IdentityProvider identityProvider:{}", identityProvider);

        // Update IDP in Jans DB
        updateIdentityProvider(identityProvider, getInputStream(bos));
        log.info("Updated IdentityProvider - identityProvider:{}", identityProvider);
        return identityProvider;
    }

    public void deleteIdentityProvider(IdentityProvider identityProvider, boolean deleteInDB) throws IOException {
        boolean status = false;
        log.info("Delete dentityProvider:{}, deleteInDB:{}, samlConfigService.isSamlEnabled():{}", identityProvider,
                deleteInDB, samlConfigService.isSamlEnabled());
        // validate
        if (identityProvider == null) {
            throw new InvalidAttributeException("IdentityProvider object for delete is null!!!");
        }

        if (samlConfigService.isSamlEnabled()) {
            // Delete IDP in KC
            status = keycloakService.deleteIdentityProvider(identityProvider.getRealm(), identityProvider.getName());
        }
        log.info("Delete IDP status:{}, deleteInDB:{}", status, deleteInDB);
        // Delete in Jans DB
        if (status && deleteInDB) {
            log.info("Deleting IDP in DB - identityProvider.getInum():{}, identityProvider.getName():{}",
                    identityProvider.getInum(), identityProvider.getName());
            identityProviderService.removeIdentityProvider(identityProvider);
            log.info("IDP successfully deleted in DB - identityProvider.getInum():{}, identityProvider.getName():{}",
                    identityProvider.getInum(), identityProvider.getName());
        }
    }

    
    public String getSpMetadata(IdentityProvider identityProvider) throws JsonProcessingException {

        if (identityProvider == null) {
            throw new InvalidAttributeException("IdentityProvider object is null!!!");
        }
        return keycloakService.getSpMetadata(identityProvider.getRealm(), identityProvider.getName());

    }

    private IdentityProvider updateIdentityProvider(IdentityProvider identityProvider, InputStream idpMetadataStream)
            throws IOException {
        log.info("Update IdentityProvider with IDP metadata file in identityProvider:{}, idpMetadataStream:{} ",
                identityProvider, idpMetadataStream);

        // Update IDP in Jans DB
        identityProviderService.updateIdentityProvider(identityProvider, idpMetadataStream);
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
            identityProvider.setRealm(getRealm());
        }

        if (StringUtils.isBlank(identityProvider.getProviderId())) {
            identityProvider.setProviderId(Constants.SAML);
        }
        
        if(!update) {
            //While creating set store token to be true
            identityProvider.setStoreToken(true);
            identityProvider.setAddReadTokenRoleOnCreate(true);

            if (StringUtils.isBlank(identityProvider.getNameIDPolicyFormat())) {
                identityProvider.setNameIDPolicyFormat(Constants.NAME_ID_POLICY_FORMAT_DEFAULT_VALUE);
            }

            if (StringUtils.isBlank(identityProvider.getPrincipalAttribute())) {
                identityProvider.setPrincipalAttribute(Constants.PRINCIPAL_ATTRIBUTE_DEFAULT_VALUE);
            }

            if (StringUtils.isBlank(identityProvider.getPrincipalType())) {
                identityProvider.setPrincipalType(Constants.PRINCIPAL_TYPE_DEFAULT_VALUE);
            }
        }

        log.info("After setting default value for identityProvider:{}, update:{}", identityProvider, update);
        return identityProvider;
    }
    
    private Map<String, String> setSamlIdpConfigDefaultValue(Map<String, String> config, IdentityProvider identityProvider, boolean update) {
        log.info("Setting default config value for config:{}, identityProvider:{}, update:{}, samlConfigService.isSetConfigDefaultValue():{}", config, identityProvider, update, samlConfigService.isSetConfigDefaultValue());
        if (identityProvider == null || config == null || config.isEmpty()) {
            return config;
        }

        // Set default values               
        if(!update && samlConfigService.isSetConfigDefaultValue()) {
             if (StringUtils.isBlank(identityProvider.getNameIDPolicyFormat())) {
                identityProvider.setNameIDPolicyFormat(Constants.NAME_ID_POLICY_FORMAT_DEFAULT_VALUE);
              }
             config.put(Constants.NAME_ID_POLICY_FORMAT, identityProvider.getNameIDPolicyFormat());

            if (StringUtils.isBlank(identityProvider.getPrincipalAttribute())) {
                identityProvider.setPrincipalAttribute(Constants.PRINCIPAL_ATTRIBUTE_DEFAULT_VALUE);
            }
            config.put(Constants.PRINCIPAL_ATTRIBUTE, identityProvider.getPrincipalAttribute());

            if (StringUtils.isBlank(identityProvider.getPrincipalType())) {
                identityProvider.setPrincipalType(Constants.PRINCIPAL_TYPE_DEFAULT_VALUE);
            }
            config.put(Constants.PRINCIPAL_TYPE, identityProvider.getPrincipalType());
        }

        log.info("After setting config default value for identityProvider:{}, update:{}", identityProvider, update);
        return config;
    }


    private IdentityProvider processIdentityProvider(IdentityProvider identityProvider, InputStream idpMetadataStream,
            boolean isUpdate) throws IOException {
        log.info("Common processing for identityProvider:{}, idpMetadataStream:{}, isUpdate:{}", identityProvider,
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
            log.debug("Validated metadata to create IDP - config:{}", config);
            setSamlIdpConfigDefaultValue(config, identityProvider, isUpdate);
            populateIdpMetadataElements(identityProvider, config);
        }

        // ensure individual metadata elements are present
        boolean validConfig = validateIdpMetadataElements(identityProvider);
        log.info("Is metadata individual elements for IDP creation present:{}", validConfig);

        if (samlConfigService.isSamlEnabled()) {
            // Create IDP in KC
            log.info("Create/Update IDP Service idpMetadataStream:{}, identityProvider.getRealm():{}",
                    idpMetadataStream, identityProvider.getRealm());
            identityProvider = keycloakService.createUpdateIdentityProvider(identityProvider.getRealm(), isUpdate,
                    identityProvider);

            log.info("Newly created identityProvider in KC:{}", identityProvider);

            // set KC SP MetadataURL name
            if (identityProvider != null) {
                String spMetadataUrl = getSpMetadataUrl(identityProvider.getRealm(), identityProvider.getName());
                log.debug(" Setting KC SP Metadata URL - spMetadataUrl:{} ", spMetadataUrl);
                identityProvider.setSpMetaDataURL(spMetadataUrl);
            }
        }
        return identityProvider;
    }

    private Map<String, String> validateSamlMetadata(String prorviderId, String realmName,
            InputStream idpMetadataStream) throws IOException {
        return keycloakService.importSamlMetadata(prorviderId, realmName, idpMetadataStream);
    }

    private boolean validateIdpMetadataElements(IdentityProvider identityProvider) {
        log.info("identityProvider:{}, samlConfigService.getIdpMetadataMandatoryAttributes():{}", identityProvider,
                samlConfigService.getIdpMetadataMandatoryAttributes());
        boolean isValid = false;
        if (identityProvider == null || samlConfigService.getIdpMetadataMandatoryAttributes() == null || samlConfigService.getIdpMetadataMandatoryAttributes().isEmpty()) {
            isValid = true;
            return isValid;
        }

        List<String> missingElements = null;
        for (String attribute : samlConfigService.getIdpMetadataMandatoryAttributes()) {
            log.debug("attribute:{}, getValue(identityProvider, attribute):{}", attribute,
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
            log.debug("IDP elements are missing:{}, isValid:{} !", missingElements, isValid);
            throw new InvalidAttributeException("IDP mandatory attribute missing - " + missingElements + " !!!");
        }
        isValid = true;
        log.info("validateIdpMetadataElements - isValid:{}", isValid);
        return isValid;
    }

    private IdentityProvider populateIdpMetadataElements(IdentityProvider identityProvider,
            Map<String, String> config) {
        log.info("identityProvider:{}, config:{}, samlConfigService.getKcSamlConfig():{}", identityProvider, config,
                samlConfigService.getKcSamlConfig());

        if (identityProvider == null || config == null || samlConfigService.getKcSamlConfig().isEmpty()) {
            return identityProvider;
        }

        for (String attribute : samlConfigService.getKcSamlConfig()) {
            log.trace("attribute:{}, config.get(attribute):{}", attribute, config.get(attribute));
            DataUtil.invokeReflectionSetter(identityProvider, attribute, config.get(attribute));
        }

        log.info("validateIdpMetadataElements - identityProvider:{}", identityProvider);
        return identityProvider;
    }

    private String getValue(IdentityProvider identityProvider, String property) {
        log.debug("Get Field Value - identityProvider:{}, property:{}", identityProvider, property);
        String value = null;
        try {
            value = (String) DataUtil.getValue(identityProvider, property);
            log.debug("Field Value - property:{}, value:{}", property, value);
        } catch (Exception ex) {
            log.error("Error while getting value of config ", ex);
        }
        return value;
    }

    private ByteArrayOutputStream getByteArrayOutputStream(InputStream input) throws IOException {
        return authUtil.getByteArrayOutputStream(input);
    }

    private InputStream getInputStream(ByteArrayOutputStream output) {
        log.debug("Get InputStream for output:{}", output);
        InputStream input = null;
        if (output == null) {
            return input;
        }

        input = new ByteArrayInputStream(output.toByteArray());
        log.debug("From ByteArrayOutputStream InputStream is:{}", input);
        return input;
    }

}
