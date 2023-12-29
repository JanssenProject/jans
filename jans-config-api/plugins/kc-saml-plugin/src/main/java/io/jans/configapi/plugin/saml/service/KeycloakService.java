/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.plugin.saml.client.IdpClientFactory;
import io.jans.configapi.plugin.saml.model.IdentityProvider;
import io.jans.configapi.plugin.saml.util.Constants;
import io.jans.util.exception.ConfigurationException;
import io.jans.util.exception.InvalidAttributeException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

@ApplicationScoped
public class KeycloakService {

    // private String KC_IMPORT_CONFIG =
    // "/admin/realms/%s/identity-provider/import-config";

    @Inject
    Logger logger;

    @Inject
    IdpClientFactory idpClientFactory;

    @Inject
    SamlConfigService samlConfigService;

    public List<IdentityProvider> findAllIdentityProviders(String realmName) throws IOException {
        logger.error("Fetch all IdentityProvider for realmName:{}, samlConfigService:{}", realmName, samlConfigService);

        if (StringUtils.isBlank(realmName)) {
            throw new InvalidAttributeException("IDP Realm name file is null!!!");
        }

        // Get token
        String token = this.getKcAccessToken(realmName);

        String idpUrl = getIdpUrl(realmName);
        logger.error("Fetch all IdentityProvider for idpUrl:{}", idpUrl);

        String idpListAsString = idpClientFactory.getAllIdp(idpUrl, token);
        logger.error("Fetch all IdentityProvider for idpListAsString:{}", idpListAsString);

        List<IdentityProvider> identityProvider = createIdentityProviderList(idpListAsString);
        logger.info("Fetch all IdentityProvider for realmName:{}, identityProvider:{}", realmName, identityProvider);
        return identityProvider;
    }

    public Map<String, String> importSamlMetadata(String providerId, String realmName, InputStream idpMetadataStream)
            throws JsonProcessingException {
        logger.error("Import config providerId:{}, realmName:{}, idpMetadataStream:{} ", providerId, realmName,
                idpMetadataStream);

        if (StringUtils.isBlank(providerId)) {
            throw new InvalidAttributeException("IDP ProviderId is null!!!");
        }

        if (StringUtils.isBlank(realmName)) {
            throw new InvalidAttributeException("IDP Realm name file is null!!!");
        }

        if (idpMetadataStream == null) {
            throw new InvalidAttributeException("IDP Metedata file is null!!!");
        }

        // Get token
        String token = this.getKcAccessToken(realmName);
        logger.error(" token:{}", token);

        String samlMetadataImportUrl = this.getSamlMetadataImportUrl(realmName);
        logger.error(" samlMetadataImportUrl:{}", samlMetadataImportUrl);

        Map<String, String> config = idpClientFactory.extractSamlMetadata(samlMetadataImportUrl, token, providerId,
                realmName, idpMetadataStream);
        logger.info("Import SAML response for realmName:{}, config:{}", realmName, config);

        boolean valid = verifySamlIdpConfig(config);
        logger.debug("Is IDP metadata config valid:{}", valid);
        if (!valid) {
            throw new InvalidAttributeException("Idp Metedata file is not valid !!!");
        }

        return config;
    }

    public IdentityProvider createUpdateIdentityProvider(String realmName, boolean isUpdate,
            IdentityProvider identityProvider) {
        IdentityProvider idp = null;
        try {
            logger.error("Add/Update IdentityProvider under realmName:{}, isUpdate:{}, identityProvider:{})", realmName,
                    isUpdate, identityProvider);

            if (StringUtils.isBlank(realmName)) {
                throw new InvalidAttributeException("Realm name is null!!!");
            }

            if (identityProvider == null) {
                throw new InvalidAttributeException("IdentityProvider object is null!!!");
            }

            // validate IDP metadata
            logger.debug("IDP metadata config identityProvider.getConfig():{})", identityProvider.getConfig());
            if (identityProvider.getConfig() == null || identityProvider.getConfig().isEmpty()) {
                throw new InvalidAttributeException("Idp Metedata config is null!!!");
            }

            boolean valid = verifySamlIdpConfig(identityProvider.getConfig());
            logger.debug("Is IDP metadata config valid:{})", valid);

            // Get token
            String token = this.getKcAccessToken(realmName);
            logger.error(" token:{}", token);

            String idpUrl = getIdpUrl(realmName);
            logger.error("IDP URL idpUrl:{}", idpUrl);

            if (isUpdate) {
                idpUrl = idpUrl + "/" + identityProvider.getName();
                logger.error("Final URL for update IDP idpUrl:{}", idpUrl);
            }

            String idpJson = this.createIdentityProviderJsonString(identityProvider);
            logger.error("IdentityProvider JSON idpJson:{}", idpJson);

            idpJson = idpClientFactory.createUpdateIdp(idpUrl, token, isUpdate, idpJson);
            logger.error("IdentityProvider response idpJson:{}", idpJson);

            idp = this.createIdentityProvider(idpJson);
            logger.error("IdentityProvider idp:{}", idp);

        } catch (Exception ex) {
            throw new ConfigurationException("Error while Add/Update SAML IDP ", ex);
        }

        return idp;
    }

    public boolean deleteIdentityProvider(String realmName, String idpName) {
        boolean deleteStatus = false;
        try {
            logger.error("Delete IdentityProvider under realmName:{}, idpName:{})", realmName, idpName);

            if (StringUtils.isBlank(realmName)) {
                throw new InvalidAttributeException("Realm name is null!!!");
            }

            if (StringUtils.isBlank(idpName)) {
                throw new InvalidAttributeException("Name of IdentityProvider to be deleted is null!!!");
            }

            // Get token
            String token = this.getKcAccessToken(realmName);
            logger.error(" token:{}", token);

            String idpUrl = getIdpUrl(realmName) + "/" + idpName;
            logger.error("IDP URL for delete is idpUrl:{}", idpUrl);

            deleteStatus = idpClientFactory.deleteIdp(idpUrl, token);
            logger.error("IdentityProvider delete response deleteStatus:{}", deleteStatus);

        } catch (Exception ex) {
            throw new ConfigurationException("Error while Deleting SAML IDP ", ex);
        }

        return deleteStatus;
    }

    public String getSpMetadata(String realmName, String idpName) throws JsonProcessingException {
        String spMetadataJson = null;
        if (StringUtils.isBlank(realmName)) {
            throw new InvalidAttributeException("Realm name is null!!!");
        }

        if (StringUtils.isBlank(idpName)) {
            throw new InvalidAttributeException("Name of IdentityProvider is null!!!");
        }

        // Get token
        String token = this.getKcAccessToken(realmName);
        logger.error(" token:{}", token);

        String idpUrl = getSpMetadataUrl(realmName, idpName);
        logger.error("IDP URL for delete is idpUrl:{}", idpUrl);

        spMetadataJson = idpClientFactory.getSpMetadata(idpUrl, token);
        logger.error("IdentityProvider delete response spMetadataJson:{}", spMetadataJson);
        return spMetadataJson;
    }

    private String getKcAccessToken(String realmName) throws JsonProcessingException {
        logger.error(" realmName:{}", realmName);

        String tokenUrl = getTokenUrl(realmName);
        logger.error("tokenUrl:{}", tokenUrl);

        String token = IdpClientFactory.getAccessToken(tokenUrl, samlConfigService.getClientId(),
                samlConfigService.getClientSecret(), samlConfigService.getGrantType(), samlConfigService.getScope(),
                samlConfigService.getUsername(), samlConfigService.getPassword(), samlConfigService.getServerUrl());
        logger.error(" token:{}", token);
        return token;
    }

    private String getIdpUrl(String realmName) {
        return samlConfigService.getIdpUrl(realmName);
    }

    private String getTokenUrl(String realmName) {
        return samlConfigService.getTokenUrl(realmName);
    }

    private String getSamlMetadataImportUrl(String realmName) {
        return samlConfigService.getIdpMetadataImportUrl(realmName);
    }

    private String getSpMetadataUrl(String realm, String name) {
        return samlConfigService.getSpMetadataUrl(realm, name);
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

    private List<IdentityProvider> createIdentityProviderList(String jsonIdentityProviderList) throws IOException {
        logger.error("jsonIdentityProviderList:{}", jsonIdentityProviderList);
        List<IdentityProvider> idpList = null;
        if (StringUtils.isBlank(jsonIdentityProviderList)) {
            return idpList;
        }

        JSONArray jsonArray = new JSONArray(jsonIdentityProviderList);
        int count = jsonArray.length(); // get totalCount of all jsonObjects
        idpList = new ArrayList<>();
        for (int i = 0; i < count; i++) { // iterate through jsonArray
            JSONObject jsonObject = jsonArray.getJSONObject(i); // get jsonObject @ i position
            logger.error(" i:{},{}", i, jsonObject);
            if (jsonObject != null) {
                idpList.add(createIdentityProvider(jsonObject.toString()));
            }
        }
        logger.error("idpList:{}", idpList);
        return idpList;
    }

    private String createIdentityProviderJsonString(IdentityProvider identityProvider) throws IOException {
        logger.error("identityProvider:{}", identityProvider);
        String identityProviderJson = null;
        if (identityProvider == null) {
            return identityProviderJson;
        }
        String json = Jackson.asJson(identityProvider);
        logger.error("json:{}", json);

        JSONArray jsonArray = new JSONArray(json);
        JSONObject jsonObj = new JSONObject();
        jsonObj.put(Constants.INTERNAL_ID, identityProvider.getInum());
        jsonObj.put(Constants.ALIAS, identityProvider.getName());
        jsonArray.put(jsonObj);
        identityProviderJson = jsonArray.toString();
        logger.error("identityProviderJson:{}", identityProviderJson);

        return identityProviderJson;

    }

    private IdentityProvider createIdentityProvider(String jsonIdentityProvider) throws IOException {
        logger.error("jsonIdentityProvider:{}", jsonIdentityProvider);
        IdentityProvider identityProvider = null;
        if (StringUtils.isBlank(jsonIdentityProvider)) {
            return identityProvider;
        }
        JSONArray jsonArray = new JSONArray(jsonIdentityProvider);
        JSONObject jsonObj = new JSONObject();
        jsonObj.put(Constants.INUM, Jackson.getElement(jsonIdentityProvider, Constants.INTERNAL_ID));
        jsonObj.put(Constants.NAME, Jackson.getElement(jsonIdentityProvider, Constants.ALIAS));
        jsonArray.put(jsonObj);

        ObjectMapper mapper = Jackson.createJsonMapper();
        identityProvider = mapper.readValue(jsonArray.toString(), IdentityProvider.class);
        logger.error("identityProvider:{}", identityProvider);

        return identityProvider;
    }

}
