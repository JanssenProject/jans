package org.xdi.oxd.server.op;

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

public class RegisterSiteOperation extends BaseOperation {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterSiteOperation.class);

    private SiteConfigurationService siteService;
    private SiteConfiguration siteConfiguration;

    /**
     * Base constructor
     *
     * @param p_command command
     */
    protected RegisterSiteOperation(Command p_command, final Injector injector) {
        super(p_command, injector);
        siteService = getInjector().getInstance(SiteConfigurationService.class);
    }

    @Override
    public CommandResponse execute() {
        try {
            String siteId = UUID.randomUUID().toString();

            LOG.info("Create site");
            persistSiteConfiguration(siteId);

            RegisterSiteResponse opResponse = new RegisterSiteResponse();
            opResponse.setSiteId(siteId);
            return okResponse(opResponse);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return CommandResponse.INTERNAL_ERROR_RESPONSE;
    }

    private void persistSiteConfiguration(String siteId) {
        final RegisterSiteParams params = asParams(RegisterSiteParams.class);

        try {
            siteConfiguration = createSiteConfiguration(siteId, params);

            if (!hasClient()) {
                final RegisterResponse registerResponse = registerClient();
                siteConfiguration.setClientId(registerResponse.getClientId());
                siteConfiguration.setClientSecret(registerResponse.getClientSecret());
                siteConfiguration.setClientRegistrationAccessToken(registerResponse.getRegistrationAccessToken());
                siteConfiguration.setClientRegistrationClientUri(registerResponse.getRegistrationClientUri());
                siteConfiguration.setClientIdIssuedAt(registerResponse.getClientIdIssuedAt());
                siteConfiguration.setClientSecretExpiresAt(registerResponse.getClientSecretExpiresAt());
            }

            siteService.createNewFile(siteConfiguration);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist site configuration, params: " + params, e);
        }
    }

    private boolean hasClient() {
        final RegisterSiteParams params = asParams(RegisterSiteParams.class);
        return !Strings.isNullOrEmpty(params.getClientId()) && !Strings.isNullOrEmpty(params.getClientSecret());
    }

    private RegisterResponse registerClient() {
        final RegisterClient registerClient = new RegisterClient(getDiscoveryService().getConnectDiscoveryResponse().getRegistrationEndpoint());
        registerClient.setRequest(createRegisterClientRequest());
        registerClient.setExecutor(getHttpService().getClientExecutor());
        final RegisterResponse response = registerClient.exec();
        if (response != null) {
            if (!Strings.isNullOrEmpty(response.getClientId()) && !Strings.isNullOrEmpty(response.getClientSecret())) {
                return response;
            } else {
                LOG.error("ClientId: " + response.getClientId() + ", clientSecret: " + response.getClientSecret());
            }
        } else {
            LOG.error("RegisterClient response is null.");
        }
        throw new RuntimeException("Failed to register client for site.");
    }

    private RegisterRequest createRegisterClientRequest() {
        final RegisterSiteParams params = asParams(RegisterSiteParams.class);
        final SiteConfiguration fallback = siteService.defaultSiteConfiguration();

        ApplicationType applicationType = null;
        if (!Strings.isNullOrEmpty(params.getApplicationType()) && ApplicationType.fromString(params.getApplicationType()) != null) {
            applicationType = ApplicationType.fromString(params.getApplicationType());
        }
        if (applicationType == null) {
            applicationType = ApplicationType.fromString(fallback.getApplicationType());
        }

        List<ResponseType> responseTypes = Lists.newArrayList();
        if (params.getResponseTypes() != null && !params.getResponseTypes().isEmpty()) {
            for (String type : params.getResponseTypes()) {
                responseTypes.add(ResponseType.fromString(type));
            }
        }
        if (responseTypes.isEmpty()) {
            for (String type : fallback.getResponseTypes()) {
                responseTypes.add(ResponseType.fromString(type));
            }
        }

        String clientName = "oxD client for site: " + siteConfiguration.getOxdId();

        Set<String> redirectUris = Sets.newHashSet();
        redirectUris.add(params.getAuthorizationRedirectUri());
        if (params.getRedirectUris() != null && !params.getRedirectUris().isEmpty()) {
            redirectUris.addAll(params.getRedirectUris());
        }

        final RegisterRequest request = new RegisterRequest(applicationType, clientName, Lists.newArrayList(redirectUris));
        request.setResponseTypes(responseTypes);
        request.setJwksUri(params.getClientJwksUri());
        request.setPostLogoutRedirectUris(Lists.newArrayList(params.getLogoutRedirectUri()));
        request.setContacts(params.getContacts());

        request.setGrantTypes(grantTypes());

        if (StringUtils.isNotBlank(params.getClientTokenEndpointAuthMethod())) {
            final AuthenticationMethod authenticationMethod = AuthenticationMethod.fromString(params.getClientTokenEndpointAuthMethod());
            if (authenticationMethod != null) {
                request.setTokenEndpointAuthMethod(authenticationMethod);
            }
        }

        if (params.getClientRequestUris() != null && !params.getClientRequestUris().isEmpty()) {
            request.setRequestUris(params.getClientRequestUris());
        }

        siteConfiguration.setResponseTypes(asString(responseTypes));
        siteConfiguration.setLogoutRedirectUri(params.getLogoutRedirectUri());
        siteConfiguration.setContacts(params.getContacts());
        siteConfiguration.setRedirectUris(Lists.newArrayList(redirectUris));
        return request;
    }

    private List<GrantType> grantTypes() {
        List<GrantType> grantTypes = Lists.newArrayList();
        grantTypes.add(GrantType.AUTHORIZATION_CODE);
        grantTypes.add(GrantType.IMPLICIT);
        grantTypes.add(GrantType.REFRESH_TOKEN);
        return grantTypes;
    }

    private List<String> asString(List<ResponseType> responseTypes) {
        List<String> list = Lists.newArrayList();
        for (ResponseType r : responseTypes) {
            list.add(r.getValue());
        }
        return list;
    }

    private SiteConfiguration createSiteConfiguration(String siteId, RegisterSiteParams params) {

        final SiteConfiguration siteConf = new SiteConfiguration(siteService.defaultSiteConfiguration());
        siteConf.setOxdId(siteId);
        siteConf.setAuthorizationRedirectUri(params.getAuthorizationRedirectUri());
        siteConf.setRedirectUris(params.getRedirectUris());

        if (params.getAcrValues() != null && !params.getAcrValues().isEmpty()) {
            siteConf.setAcrValues(params.getAcrValues());
        }
        if (!Strings.isNullOrEmpty(params.getApplicationType())) {
            siteConf.setApplicationType(params.getApplicationType());
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

        if (params.getGrantType() != null && !params.getGrantType().isEmpty()) {
            siteConf.setGrantType(params.getGrantType());
        }

        if (params.getResponseTypes() != null && !params.getResponseTypes().isEmpty()) {
            siteConf.setResponseTypes(params.getResponseTypes());
        }

        if (params.getScope() != null && !params.getScope().isEmpty()) {
            siteConf.setScope(params.getScope());
        }

        if (params.getUiLocales() != null && !params.getUiLocales().isEmpty()) {
            siteConf.setUiLocales(params.getUiLocales());
        }

        return siteConf;
    }
}