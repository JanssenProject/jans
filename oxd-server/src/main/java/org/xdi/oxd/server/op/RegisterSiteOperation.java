package org.xdi.oxd.server.op;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.RegisterClient;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.client.RegisterResponse;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.ErrorResponseCode;
import org.xdi.oxd.common.ErrorResponseException;
import org.xdi.oxd.common.params.RegisterSiteParams;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.server.service.SiteConfiguration;
import org.xdi.oxd.server.service.SiteConfigurationService;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 24/09/2015
 */

public class RegisterSiteOperation extends BaseOperation<RegisterSiteParams> {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterSiteOperation.class);

    private SiteConfiguration siteConfiguration;

    /**
     * Base constructor
     *
     * @param command command
     */
    protected RegisterSiteOperation(Command command, final Injector injector) {
        super(command, injector, RegisterSiteParams.class);
    }

    @Override
    public CommandResponse execute(RegisterSiteParams params) {
        try {
            validateParametersAndFallbackIfNeeded(params);

            String siteId = UUID.randomUUID().toString();

            LOG.info("Creating site configuration ...");
            persistSiteConfiguration(siteId, params);

            LOG.info("Site configuration created: " + siteConfiguration);

            RegisterSiteResponse opResponse = new RegisterSiteResponse();
            opResponse.setOxdId(siteId);
            opResponse.setOpHost(params.getOpHost());
            return okResponse(opResponse);
        } catch (ErrorResponseException e) {
            throw e;
        }  catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return CommandResponse.INTERNAL_ERROR_RESPONSE;
    }

    private void validateParametersAndFallbackIfNeeded(RegisterSiteParams params) {
        SiteConfiguration fallback = getSiteService().defaultSiteConfiguration();

        // op_host
        if (Strings.isNullOrEmpty(params.getOpHost())) {
            LOG.warn("op_host is not set for parameter: " + params + ". Look up at " + SiteConfigurationService.DEFAULT_SITE_CONFIG_JSON + " for fallback op_host");
            String fallbackOpHost = fallback.getOpHost();
            if (Strings.isNullOrEmpty(fallbackOpHost)) {
                throw new ErrorResponseException(ErrorResponseCode.INVALID_OP_HOST);
            }
            LOG.warn("Fallback to op_host: " + fallbackOpHost + ", from " + SiteConfigurationService.DEFAULT_SITE_CONFIG_JSON);
            params.setOpHost(fallbackOpHost);
        }

        // grant_type
        List<String> grantTypes = Lists.newArrayList();

        if (params.getGrantType() != null && !params.getGrantType().isEmpty()) {
            grantTypes.addAll(params.getGrantType());
        }

        if (grantTypes.isEmpty() && fallback.getGrantType() != null && !fallback.getGrantType().isEmpty()) {
            grantTypes.addAll(fallback.getGrantType());
        }

        if (grantTypes.isEmpty()) {
            grantTypes.add(GrantType.AUTHORIZATION_CODE.getValue());
        }

        params.setGrantType(grantTypes);

        // authorization_redirect_uri
        if (Strings.isNullOrEmpty(params.getAuthorizationRedirectUri())) {
            params.setAuthorizationRedirectUri(fallback.getAuthorizationRedirectUri());
        }
        if (Strings.isNullOrEmpty(params.getAuthorizationRedirectUri())) {
            throw new ErrorResponseException(ErrorResponseCode.INVALID_AUTHORIZATION_REDIRECT_URI);
        }

        //post_logout_redirect_uri
        if (Strings.isNullOrEmpty(params.getPostLogoutRedirectUri()) && !Strings.isNullOrEmpty(fallback.getPostLogoutRedirectUri())) {
            params.setPostLogoutRedirectUri(fallback.getPostLogoutRedirectUri());
        }

        // response_type
        List<String> responseTypes = Lists.newArrayList();
        if (params.getResponseTypes() != null && !params.getResponseTypes().isEmpty()) {
            responseTypes.addAll(params.getResponseTypes());
        }
        if (responseTypes.isEmpty() && fallback.getResponseTypes() != null && !fallback.getResponseTypes().isEmpty()) {
            responseTypes.addAll(fallback.getResponseTypes());
        }
        if (responseTypes.isEmpty()) {
            responseTypes.add("code");
        }
        params.setResponseTypes(responseTypes);

        // redirect_uris
        Set<String> redirectUris = Sets.newHashSet();
        redirectUris.add(params.getAuthorizationRedirectUri());
        if (params.getRedirectUris() != null && !params.getRedirectUris().isEmpty()) {
            redirectUris.addAll(params.getRedirectUris());
            if (!Strings.isNullOrEmpty(params.getPostLogoutRedirectUri())) {
                redirectUris.add(params.getPostLogoutRedirectUri());
            }
        }
        params.setRedirectUris(Lists.newArrayList(redirectUris));

        // scope
        if (params.getScope() == null || params.getScope().isEmpty()) {
            params.setScope(fallback.getScope());
        }
        if (params.getScope() == null || params.getScope().isEmpty()) {
            throw new ErrorResponseException(ErrorResponseCode.INVALID_SCOPE);
        }

        // acr_values
        if (params.getAcrValues() == null || params.getAcrValues().isEmpty()) {
            params.setAcrValues(fallback.getAcrValues());
        }

        // client_jwks_uri
        if (Strings.isNullOrEmpty(params.getClientJwksUri()) && !Strings.isNullOrEmpty(fallback.getClientJwksUri())) {
            params.setClientJwksUri(fallback.getClientJwksUri());
        }

        // contacts
        if (params.getContacts() == null || params.getContacts().isEmpty()) {
            params.setContacts(fallback.getContacts());
        }

        // ui_locales
        if (params.getUiLocales() == null || params.getUiLocales().isEmpty()) {
            params.setUiLocales(fallback.getUiLocales());
        }

        // claims_locales
        if (params.getClaimsLocales() == null || params.getClaimsLocales().isEmpty()) {
            params.setClaimsLocales(fallback.getClaimsLocales());
        }
    }

    private void persistSiteConfiguration(String siteId, RegisterSiteParams params) {

        try {
            siteConfiguration = createSiteConfiguration(siteId, params);

            if (!hasClient(params)) {
                final RegisterResponse registerResponse = registerClient(params);
                siteConfiguration.setClientId(registerResponse.getClientId());
                siteConfiguration.setClientSecret(registerResponse.getClientSecret());
                siteConfiguration.setClientRegistrationAccessToken(registerResponse.getRegistrationAccessToken());
                siteConfiguration.setClientRegistrationClientUri(registerResponse.getRegistrationClientUri());
                siteConfiguration.setClientIdIssuedAt(registerResponse.getClientIdIssuedAt());
                siteConfiguration.setClientSecretExpiresAt(registerResponse.getClientSecretExpiresAt());
            }

            getSiteService().createNewFile(siteConfiguration);
        } catch (IOException e) {
            LOG.error("Failed to persist site configuration, params: " + params, e);
            throw new RuntimeException(e);
        }
    }

    private boolean hasClient(RegisterSiteParams params) {
        return !Strings.isNullOrEmpty(params.getClientId()) && !Strings.isNullOrEmpty(params.getClientSecret());
    }

    private RegisterResponse registerClient(RegisterSiteParams params) {
        final String registrationEndpoint = getDiscoveryService().getConnectDiscoveryResponse(params.getOpHost()).getRegistrationEndpoint();
        if (Strings.isNullOrEmpty(registrationEndpoint)) {
            LOG.error("This OP (" + params.getOpHost() + ") does not provide registration_endpoint. It means that oxd is not able dynamically register client. " +
                    "Therefore it is required to obtain/register client manually on OP site and provide client_id and client_secret to oxd register_site command.");
            throw new ErrorResponseException(ErrorResponseCode.NO_UMA_RESOURCES_TO_PROTECT);
        }

        final RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(createRegisterClientRequest(params));
        registerClient.setExecutor(getHttpService().getClientExecutor());
        final RegisterResponse response = registerClient.exec();
        if (response != null) {
            if (!Strings.isNullOrEmpty(response.getClientId()) && !Strings.isNullOrEmpty(response.getClientSecret())) {
                LOG.trace("Registered client for site - client_id: " + response.getClientId());
                return response;
            } else {
                LOG.error("ClientId: " + response.getClientId() + ", clientSecret: " + response.getClientSecret());
            }
        } else {
            LOG.error("RegisterClient response is null.");
        }
        if (!Strings.isNullOrEmpty(response.getErrorDescription())) {
            LOG.error(response.getErrorDescription());
        }

        throw new RuntimeException("Failed to register client for site. Details:" + response.getEntity());
    }

    private RegisterRequest createRegisterClientRequest(RegisterSiteParams params) {
        List<ResponseType> responseTypes = Lists.newArrayList();
        for (String type : params.getResponseTypes()) {
            responseTypes.add(ResponseType.fromString(type));
        }

        String clientName = "oxD client for site: " + siteConfiguration.getOxdId();
        if (!Strings.isNullOrEmpty(params.getClientName())) {
            clientName = params.getClientName();
        }

        final RegisterRequest request = new RegisterRequest(ApplicationType.WEB, clientName, params.getRedirectUris());
        request.setResponseTypes(responseTypes);
        request.setJwksUri(params.getClientJwksUri());
        request.setPostLogoutRedirectUris(params.getPostLogoutRedirectUri() != null ? Lists.newArrayList(params.getPostLogoutRedirectUri()) : Lists.<String>newArrayList());
        request.setContacts(params.getContacts());
        request.setScopes(params.getScope());
        request.setDefaultAcrValues(params.getAcrValues());

        List<GrantType> grantTypes = Lists.newArrayList();
        for (String grantType : params.getGrantType()) {
            grantTypes.add(GrantType.fromString(grantType));
        }
        request.setGrantTypes(grantTypes);

        if (params.getClientLogoutUri() != null) {
           request.setLogoutUris(Lists.newArrayList(params.getClientLogoutUri()));
        }

        if (StringUtils.isNotBlank(params.getClientTokenEndpointAuthMethod())) {
            final AuthenticationMethod authenticationMethod = AuthenticationMethod.fromString(params.getClientTokenEndpointAuthMethod());
            if (authenticationMethod != null) {
                request.setTokenEndpointAuthMethod(authenticationMethod);
            }
        }

        if (params.getClientRequestUris() != null && !params.getClientRequestUris().isEmpty()) {
            request.setRequestUris(params.getClientRequestUris());
        }

        if (!Strings.isNullOrEmpty(params.getClientSectorIdentifierUri())) {
            request.setSectorIdentifierUri(params.getClientSectorIdentifierUri());
        }

        siteConfiguration.setResponseTypes(params.getResponseTypes());
        siteConfiguration.setPostLogoutRedirectUri(params.getPostLogoutRedirectUri());
        siteConfiguration.setContacts(params.getContacts());
        siteConfiguration.setRedirectUris(Lists.newArrayList(params.getRedirectUris()));
        return request;
    }

    private SiteConfiguration createSiteConfiguration(String siteId, RegisterSiteParams params) {

        Preconditions.checkState(!Strings.isNullOrEmpty(params.getOpHost()), "op_host contains blank value. Please specify valid OP public address.");

        final SiteConfiguration siteConf = new SiteConfiguration(getSiteService().defaultSiteConfiguration());
        siteConf.setOxdId(siteId);
        siteConf.setOpHost(params.getOpHost());
        siteConf.setAuthorizationRedirectUri(params.getAuthorizationRedirectUri());
        siteConf.setRedirectUris(params.getRedirectUris());
        siteConf.setApplicationType("web");

        if (!Strings.isNullOrEmpty(params.getPostLogoutRedirectUri())) {
           siteConf.setPostLogoutRedirectUri(params.getPostLogoutRedirectUri());
        }

        if (params.getAcrValues() != null && !params.getAcrValues().isEmpty()) {
            siteConf.setAcrValues(params.getAcrValues());
        }

        if (params.getClaimsLocales() != null && !params.getClaimsLocales().isEmpty()) {
            siteConf.setClaimsLocales(params.getClaimsLocales());
        }

        if (!Strings.isNullOrEmpty(params.getClientId()) && !Strings.isNullOrEmpty(params.getClientSecret())) {
            siteConf.setClientId(params.getClientId());
            siteConf.setClientSecret(params.getClientSecret());
        }

        if (params.getContacts() != null && !params.getContacts().isEmpty()) {
            siteConf.setContacts(params.getContacts());
        }

        siteConf.setGrantType(params.getGrantType());
        siteConf.setResponseTypes(params.getResponseTypes());

        if (params.getScope() != null && !params.getScope().isEmpty()) {
            siteConf.setScope(params.getScope());
        }

        if (params.getUiLocales() != null && !params.getUiLocales().isEmpty()) {
            siteConf.setUiLocales(params.getUiLocales());
        }

        return siteConf;
    }
}