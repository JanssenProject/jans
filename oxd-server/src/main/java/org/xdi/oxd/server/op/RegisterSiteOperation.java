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
        if (response != null && !Strings.isNullOrEmpty(response.getClientId()) && !Strings.isNullOrEmpty(response.getClientSecret())) {
            return response;
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

        final RegisterRequest request = new RegisterRequest(applicationType, clientName != null ? clientName : "", Lists.newArrayList(redirectUris));
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