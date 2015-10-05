package org.xdi.oxd.server.op;

import com.google.common.base.Strings;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.RegisterClient;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.client.RegisterResponse;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.RegisterSiteParams;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.server.service.SiteConfiguration;
import org.xdi.oxd.server.service.SiteConfigurationService;

import java.io.IOException;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 24/09/2015
 */

public class RegisterSiteOperation extends BaseOperation {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterSiteOperation.class);

    private SiteConfigurationService siteService;

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
            final SiteConfiguration siteConfiguration = createSiteConfiguration(siteId, params);

            if (!hasClient()) {
                final RegisterResponse registerResponse = registerClient();
                siteConfiguration.setClientId(registerResponse.getClientId());
                siteConfiguration.setClientSecret(registerResponse.getClientSecret());
                siteConfiguration.setClientRegistrationAccessToken(registerResponse.getRegistrationAccessToken());
                siteConfiguration.setClientRegistrationClientUri(registerResponse.getRegistrationClientUri());
                siteConfiguration.setClientIdIssuedAt(registerResponse.getClientIdIssuedAt());
                siteConfiguration.setClientSecretExpiresAt(registerResponse.getClientSecretExpiresAt());
            }

            siteService.persist(siteConfiguration);
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
        if (response != null && !Strings.isNullOrEmpty(response.getClientId()) && !Strings.isNullOrEmpty(response.getClientSecret())) {
            return response;
        }
        throw new RuntimeException("Failed to register client for site.");
    }

    private RegisterRequest createRegisterClientRequest() {
        final RegisterSiteParams params = asParams(RegisterSiteParams.class);

        final ApplicationType applicationType = ApplicationType.fromString(params.getApplicationType());

//        final List<ResponseType> responseTypes = ResponseType.fromString(responseTypeString, " ");
//        final RegisterRequest request = new RegisterRequest(applicationType, params.getClientName(), params.getRedirectUrl());
//        request.setResponseTypes(responseTypes);
//        request.setJwksUri(params.getJwksUri());
//        request.setPostLogoutRedirectUris(org.xdi.oxauth.model.util.StringUtils.spaceSeparatedToList(params.getLogoutRedirectUrl()));
//
//        if (StringUtils.isNotBlank(params.getContacts())) {
//            request.setContacts(org.xdi.oxauth.model.util.StringUtils.spaceSeparatedToList(params.getContacts()));
//        }
//
//        if (StringUtils.isNotBlank(params.getGrantTypes())) {
//            request.setGrantTypes(grantTypes(params.getGrantTypes()));
//        }
//
//        if (StringUtils.isNotBlank(params.getTokenEndpointAuthMethod())) {
//            final AuthenticationMethod authenticationMethod = AuthenticationMethod.fromString(params.getTokenEndpointAuthMethod());
//            if (authenticationMethod != null) {
//                request.setTokenEndpointAuthMethod(authenticationMethod);
//            }
//        }
//
//        if (params.getRequestUris() != null && !params.getRequestUris().isEmpty()) {
//            request.setRequestUris(params.getRequestUris());
//        }
//        return request;
        return null; // todo
    }

    private SiteConfiguration createSiteConfiguration(String siteId, RegisterSiteParams params) {
        final SiteConfiguration siteConf = new SiteConfiguration();
        siteConf.setOxdId(siteId);
        siteConf.setAcrValues(params.getAcrValues());
        siteConf.setApplicationType(params.getApplicationType());
        siteConf.setAuthorizationRedirectUri(params.getAuthorizationRedirectUri());
        siteConf.setClaimsLocales(params.getClaimsLocales());
        siteConf.setClientId(params.getClientId());
        siteConf.setClientSecret(params.getClientSecret());
        siteConf.setContacts(params.getContacts());
        siteConf.setGrantType(params.getGrantType());
        siteConf.setRedirectUris(params.getRedirectUris());
        siteConf.setResponseTypes(params.getResponseTypes());
        siteConf.setScope(params.getScope());
        siteConf.setUiLocales(params.getUiLocales());

        return siteConf;
    }
}