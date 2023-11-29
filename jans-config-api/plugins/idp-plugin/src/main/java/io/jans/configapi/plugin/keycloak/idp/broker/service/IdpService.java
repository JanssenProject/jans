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
import io.jans.configapi.plugin.keycloak.idp.broker.client.IdpClientFactory;
import io.jans.configapi.plugin.keycloak.idp.broker.mapper.IdentityProviderMapper;
import io.jans.configapi.plugin.keycloak.idp.broker.model.IdentityProvider;
import io.jans.configapi.plugin.keycloak.idp.broker.service.IdpConfigService;
import io.jans.configapi.plugin.keycloak.idp.broker.service.SamlService;
import io.jans.configapi.plugin.keycloak.idp.broker.timer.IdpMetadataValidationTimer;
import io.jans.configapi.plugin.keycloak.idp.broker.util.Constants;

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
    IdpConfigService idpConfigService;

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

    public String getIdentityProviderDn() {
        return idpConfigService.getTrustedIdpDn();
    }

    public String getSpMetadataUrl(String realm, String name) {
        return idpConfigService.getSpMetadataUrl(realm, name);
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
                "Create IdentityProvider with IDP metadata file in identityProvider:{}, idpMetadataStream:{}, idpConfigService.isIdpEnabled():{}",
                identityProvider, idpMetadataStream, idpConfigService.isIdpEnabled());

        // validate
        if (identityProvider == null) {
            throw new InvalidAttributeException("IdentityProvider object is null!!!");
        }

        if (idpMetadataStream == null) {
            throw new InvalidAttributeException("Idp Metedata file is null!!!");
        }

        // validate metadata and set in config
        Map<String, String> config = validateSamlMetadata(identityProvider.getRealm(), idpMetadataStream);
        log.debug("Validated metadata to create IDP - config:{}", config);
        identityProvider.setConfig(config);

        // Create IDP in Jans DB
        log.debug("Create IdentityProvider identityProvider:{})", identityProvider);
        identityProviderService.addSamlIdentityProvider(identityProvider, idpMetadataStream);
        log.debug("Created IdentityProvider in Jans DB -  identityProvider:{})", identityProvider);

        if (idpConfigService.isIdpEnabled()) {
            // Create IDP in KC
            IdentityProviderRepresentation kcIdp = this.convertToIdentityProviderRepresentation(identityProvider);
            log.debug("converted kcIdp:{}", kcIdp);

            log.debug("IDP Service idpMetadataStream:{}, idpMetadataStream.available():{}", idpMetadataStream,
                    idpMetadataStream.available());
            kcIdp = keycloakService.createIdentityProvider(identityProvider.getRealm(), kcIdp);
            log.debug("Newly created kcIdp:{}", kcIdp);
            identityProvider = this.convertToIdentityProvider(kcIdp);
            log.debug("Final created identityProvider:{}", identityProvider);

        }
        return identityProvider;
    }

    public IdentityProvider updateSamlIdentityProvider(IdentityProvider identityProvider, InputStream idpMetadataStream)
            throws IOException {
        log.debug(
                "Update IdentityProvider with IDP metadata file in - identityProvider:{}, idpMetadataStream:{}, idpConfigService.isIdpEnabled():{}",
                identityProvider, idpMetadataStream, idpConfigService.isIdpEnabled());

        // validate
        if (identityProvider == null) {
            throw new InvalidAttributeException("IdentityProvider object for update is null!!!");
        }

        if (idpMetadataStream == null) {
            throw new InvalidAttributeException("Idp Metedata file for update is null!!!");
        }

        // validate metadata and set in config
        Map<String, String> config = validateSamlMetadata(identityProvider.getRealm(), idpMetadataStream);
        log.debug("Validated metadata to update config:{}", config);
        identityProvider.setConfig(config);

        // Update IDP in Jans DB
        updateIdentityProvider(identityProvider);
        log.debug("Updated IdentityProvider dentityProvider:{})", identityProvider);

        if (idpConfigService.isIdpEnabled()) {
            // Update IDP in KC
            IdentityProviderRepresentation kcIdp = this.convertToIdentityProviderRepresentation(identityProvider);
            log.debug("converted kcIdp:{}", kcIdp);

            kcIdp = keycloakService.updateIdentityProvider(identityProvider.getRealm(), kcIdp);
            log.debug("Updated kcIdp:{}", kcIdp);
            identityProvider = this.convertToIdentityProvider(kcIdp);
        }
        return identityProvider;
    }

    public void deleteIdentityProvider(IdentityProvider identityProvider) {

        if (idpConfigService.isIdpEnabled()) {
            // Delete IDP in KC
            keycloakService.deleteIdentityProvider(identityProvider.getRealm(), identityProvider.getName());
        }
        // Delete in Jans DB
        identityProviderService.removeIdentityProvider(identityProvider);
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

}
