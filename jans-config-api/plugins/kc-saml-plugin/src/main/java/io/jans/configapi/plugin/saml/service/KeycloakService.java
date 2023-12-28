/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.service;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.jans.util.exception.InvalidAttributeException;
import io.jans.configapi.plugin.saml.service.SamlConfigService;
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

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.StatusType;
import jakarta.ws.rs.WebApplicationException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class KeycloakService {

    private String KC_IMPORT_CONFIG = "/admin/realms/%s/identity-provider/import-config";

    @Inject
    Logger logger;

    @Inject
    IdpClientFactory idpClientFactory;

    @Inject
    SamlConfigService samlConfigService;

    public List<IdentityProvider> findAllIdentityProviders(String realmName) throws JsonProcessingException {
        logger.error("Fetch all IdentityProvider for realmName:{}, samlConfigService:{}", realmName, samlConfigService);

        if (StringUtils.isBlank(realmName)) {
            realmName = Constants.REALM_MASTER;
        }

        // Get token
        String token = this.getKcAccessToken(realmName);
        logger.error(" token:{}", token);

        String idpUrl = getIdpUrl(realmName);
        logger.error("Fetch all IdentityProvider for idpUrl:{}", idpUrl);

        List<IdentityProvider> identityProvider = idpClientFactory.getAllIdp(idpUrl, token);
        logger.info("Fetch all IdentityProvider for realmName:{}, identityProvider:{}", realmName, identityProvider);
        return identityProvider;
    }

    public Map<String, String> importSamlMetadata(String providerId, String realmName, InputStream idpMetadataStream)
            throws JsonProcessingException {
        logger.error("Import config providerId:{}, realmName:{}, idpMetadataStream:{} ", providerId, realmName,
                idpMetadataStream);

        if (StringUtils.isBlank(providerId)) {
            providerId = "saml";
        }

        if (StringUtils.isBlank(realmName)) {
            realmName = Constants.REALM_MASTER;
        }

        if (idpMetadataStream == null) {
            throw new InvalidAttributeException("Idp Metedata file is null!!!");
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

    private String getKcAccessToken(String realmName) throws JsonProcessingException {
        logger.error(" realmName:{}", realmName);

        String tokenUrl = getTokenUrl(realmName);
        logger.error("tokenUrl:{}", tokenUrl);

        String token = idpClientFactory.getAccessToken(tokenUrl, samlConfigService.getClientId(),
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
        StringBuilder sb = new StringBuilder();
        sb.append(samlConfigService.getServerUrl()).append(KC_IMPORT_CONFIG);
        return String.format(sb.toString(), realmName);
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
