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
import io.jans.util.exception.InvalidAttributeException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

@ApplicationScoped
public class KeycloakService {

    public static final String REALM_NAME_NULL = "Realm name is null!!!";

    @Inject
    Logger logger;

    @Inject
    IdpClientFactory idpClientFactory;

    @Inject
    SamlConfigService samlConfigService;

    public List<IdentityProvider> findAllIdentityProviders(String realmName) throws IOException {
        logger.info("Fetch all IdentityProvider for realmName:{}, samlConfigService:{}", realmName, samlConfigService);

        if (StringUtils.isBlank(realmName)) {
            throw new InvalidAttributeException("IDP Realm name file is null!!!");
        }

        // Get token
        String token = this.getKcAccessToken(realmName);
        String idpUrl = getIdpUrl(realmName);
        logger.debug("Fetch all IdentityProvider for idpUrl:{}", idpUrl);

        String idpListAsString = idpClientFactory.getAllIdp(idpUrl, token);
        logger.info("Fetch all IdentityProvider for idpListAsString:{}", idpListAsString);

        List<IdentityProvider> identityProvider = createIdentityProviderList(idpListAsString);
        logger.info("Fetch all IdentityProvider for realmName:{}, identityProvider:{}", realmName, identityProvider);
        return identityProvider;
    }

    public Map<String, String> importSamlMetadata(String providerId, String realmName, InputStream idpMetadataStream)
            throws IOException {
        logger.info("Import config providerId:{}, realmName:{}, idpMetadataStream:{} ", providerId, realmName,
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
        String samlMetadataImportUrl = this.getSamlMetadataImportUrl(realmName);
        logger.info(" samlMetadataImportUrl:{}", samlMetadataImportUrl);

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
            IdentityProvider identityProvider) throws IOException {
        IdentityProvider idp = null;

        logger.info("Add/Update IdentityProvider under realmName:{}, isUpdate:{}, identityProvider:{})", realmName,
                isUpdate, identityProvider);

        if (StringUtils.isBlank(realmName)) {
            throw new InvalidAttributeException(REALM_NAME_NULL);
        }

        if (identityProvider == null) {
            throw new InvalidAttributeException("IdentityProvider object is null!!!");
        }

        // Get token
        String token = this.getKcAccessToken(realmName);
        String idpUrl = getIdpUrl(realmName);

        if (isUpdate) {
            idpUrl = idpUrl + "/" + identityProvider.getName();
            logger.info("Final URL for update IDP idpUrl:{}", idpUrl);
        }

        JSONObject jsonObject = createIdentityProviderJson(identityProvider);

        // Populate KC SAML config
        populateKcConfig(jsonObject);

        // Create KC JsonObject
        JSONObject kcJsonObject = createKcJSONObject(jsonObject);
        logger.info("Create new IdentityProvider - kcJsonObject:{}", kcJsonObject);

        String idpJson = idpClientFactory.createUpdateIdp(idpUrl, token, isUpdate, kcJsonObject);
        logger.debug("IdentityProvider response idpJson:{}", idpJson);

        idp = this.createIdentityProvider(idpJson);
        logger.debug("IdentityProvider idp:{}", idp);

        return identityProvider;
    }

    public boolean deleteIdentityProvider(String realmName, String idpName) throws IOException {
        boolean deleteStatus = false;

        logger.info("Delete IdentityProvider under realmName:{}, idpName:{})", realmName, idpName);

        if (StringUtils.isBlank(realmName)) {
            throw new InvalidAttributeException(REALM_NAME_NULL);
        }

        if (StringUtils.isBlank(idpName)) {
            throw new InvalidAttributeException("Name of IdentityProvider to be deleted is null!!!");
        }

        // Get token
        String token = this.getKcAccessToken(realmName);
        String idpUrl = getIdpUrl(realmName) + "/" + idpName;
        logger.info("IDP URL for delete is idpUrl:{}", idpUrl);

        deleteStatus = idpClientFactory.deleteIdp(idpUrl, token);
        logger.info("IdentityProvider delete response deleteStatus:{}", deleteStatus);

        return deleteStatus;
    }

    public String getSpMetadata(String realmName, String idpName) throws JsonProcessingException {
        String spMetadataJson = null;
        if (StringUtils.isBlank(realmName)) {
            throw new InvalidAttributeException(REALM_NAME_NULL);
        }

        if (StringUtils.isBlank(idpName)) {
            throw new InvalidAttributeException("Name of IdentityProvider is null!!!");
        }

        // Get token
        String token = this.getKcAccessToken(realmName);
        String idpUrl = getSpMetadataUrl(realmName, idpName);
        logger.info("IDP URL for delete is idpUrl:{}", idpUrl);

        spMetadataJson = idpClientFactory.getSpMetadata(idpUrl, token);
        logger.info("IdentityProvider delete response spMetadataJson:{}", spMetadataJson);
        return spMetadataJson;
    }

    private String getKcAccessToken(String realmName) throws JsonProcessingException {
        logger.info(" realmName:{}", realmName);

        String tokenUrl = getTokenUrl(realmName);

        return IdpClientFactory.getAccessToken(tokenUrl, samlConfigService.getClientId(),
                samlConfigService.getClientSecret(), samlConfigService.getGrantType(), samlConfigService.getScope(),
                samlConfigService.getUsername(), samlConfigService.getPassword(), samlConfigService.getServerUrl());
    }

    private String getIdpUrl(String realmName) {
        return samlConfigService.getIdpUrl(realmName);
    }

    private String getTokenUrl(String realmName) {
        return samlConfigService.getTokenUrl(realmName);
    }

    private String getSpMetadataUrl(String realm, String name) {
        return samlConfigService.getSpMetadataUrl(realm, name);
    }

    private String getSamlMetadataImportUrl(String realmName) {
        return samlConfigService.getIdpMetadataImportUrl(realmName);
    }

    private List<String> getKcAttributes() {
        return samlConfigService.getKcAttributes();
    }

    private List<String> getKcSamlConfig() {
        return samlConfigService.getKcSamlConfig();
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
        logger.info("jsonIdentityProviderList:{}", jsonIdentityProviderList);
        List<IdentityProvider> idpList = null;
        if (StringUtils.isBlank(jsonIdentityProviderList)) {
            return idpList;
        }

        JSONArray jsonArray = new JSONArray(jsonIdentityProviderList);
        int count = jsonArray.length(); // get totalCount of all jsonObjects
        idpList = new ArrayList<>();
        for (int i = 0; i < count; i++) { // iterate through jsonArray
            JSONObject jsonObject = jsonArray.getJSONObject(i); // get jsonObject @ i position
            logger.trace(" i:{},{}", i, jsonObject);
            if (jsonObject != null) {
                idpList.add(createIdentityProvider(jsonObject.toString()));
            }
        }
        logger.info("idpList:{}", idpList);
        return idpList;
    }

    private JSONObject createIdentityProviderJson(IdentityProvider identityProvider) throws IOException {
        logger.info("Create Json - identityProvider:{}", identityProvider);
        JSONObject jsonObj = null;
        if (identityProvider == null) {
            return jsonObj;
        }
        String json = Jackson.asJson(identityProvider);

        jsonObj = new JSONObject(json);
        jsonObj.put(Constants.INTERNAL_ID, identityProvider.getInum());
        jsonObj.put(Constants.ALIAS, identityProvider.getName());
        logger.info("jsonObj:{}", jsonObj);
        return jsonObj;

    }

    private IdentityProvider createIdentityProvider(String jsonIdentityProvider) throws IOException {
        logger.info("jsonIdentityProvider:{}", jsonIdentityProvider);
        IdentityProvider identityProvider = null;
        if (StringUtils.isBlank(jsonIdentityProvider)) {
            return identityProvider;
        }

        JSONObject jsonObj = new JSONObject(jsonIdentityProvider);
        jsonObj.put(Constants.INUM, Jackson.getElement(jsonIdentityProvider, Constants.INTERNAL_ID));
        jsonObj.put(Constants.NAME, Jackson.getElement(jsonIdentityProvider, Constants.ALIAS));

        ObjectMapper mapper = Jackson.createJsonMapper();
        identityProvider = mapper.readValue(jsonObj.toString(), IdentityProvider.class);
        logger.info("IDP - identityProvider:{}", identityProvider);

        return identityProvider;
    }

    private JSONObject populateKcConfig(JSONObject jsonObject) {
        logger.info("IDP - jsonObject:{}", jsonObject);
        List<String> kcSamlConfig = getKcSamlConfig();
        logger.info("kcSamlConfig:{}", kcSamlConfig);
        
        if (jsonObject == null || kcSamlConfig == null || kcSamlConfig.isEmpty()) {
            return jsonObject;
        }

        Map<String, String> config = new HashMap<>();
        for (String name : kcSamlConfig) {
            logger.trace("name:{}, jsonObject.has(name):{}", name, jsonObject.has(name));
            if (jsonObject.has(name)) {
                config.put(name, jsonObject.getString(name));
            }
        }

        logger.info("config:{}", config);
        jsonObject.put("config", config);

        // validate IDP metadata
        logger.debug("IDP metadata config config:{})", config);
        if ( config.isEmpty()) {
            throw new InvalidAttributeException("Idp Metedata config is null!!!");
        }

        boolean valid = verifySamlIdpConfig(config);
        logger.debug("Is IDP metadata config valid:{})", valid);

        logger.info("Post config IDP jsonObject:{}", jsonObject);

        return jsonObject;
    }

    private JSONObject createKcJSONObject(JSONObject jsonObject) {
        logger.info("For KC - jsonObject:{}", jsonObject);
        List<String> kcAttributes = getKcAttributes();

        if (jsonObject == null || kcAttributes == null || kcAttributes.isEmpty()) {
            return jsonObject;
        }

        JSONObject kcJSONObject = new JSONObject();
        for (String name : kcAttributes) {
            try {
                logger.trace("name:{}, jsonObject.get(name):{}", name, jsonObject.get(name));
                kcJSONObject.put(name, jsonObject.get(name));
            } catch (JSONException jex) {
                logger.error("JSONException for attribute:{}, is:{}", name, jex);
            }
        }

        logger.info("kcJSONObject:{}", kcJSONObject);

        return kcJSONObject;
    }

}
