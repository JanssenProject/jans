package org.gluu.oxd.server.op;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.oxauth.client.RegisterClient;
import org.gluu.oxauth.client.RegisterRequest;
import org.gluu.oxauth.client.RegisterResponse;
import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.register.ApplicationType;
import org.gluu.oxauth.model.uma.UmaMetadata;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.common.params.RegisterSiteParams;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.common.response.RegisterSiteResponse;
import org.gluu.oxd.server.HttpException;
import org.gluu.oxd.server.Utils;
import org.gluu.oxd.server.model.UmaResource;
import org.gluu.oxd.server.service.Rp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 */

public class RegisterSiteOperation extends BaseOperation<RegisterSiteParams> {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterSiteOperation.class);

    private Rp rp;

    /**
     * Base constructor
     *
     * @param command command
     */
    protected RegisterSiteOperation(Command command, final Injector injector) {
        super(command, injector, RegisterSiteParams.class);
    }

    public RegisterSiteResponse execute_(RegisterSiteParams params) {
        validateParametersAndFallbackIfNeeded(params);

        String oxdId = UUID.randomUUID().toString();

        LOG.info("Creating RP ...");
        persistRp(oxdId, params);

        LOG.info("RP created: " + rp);

        RegisterSiteResponse response = new RegisterSiteResponse();
        response.setOxdId(oxdId);
        response.setOpHost(params.getOpHost());
        response.setClientId(rp.getClientId());
        response.setClientName(rp.getClientName());
        response.setClientSecret(rp.getClientSecret());
        response.setClientRegistrationAccessToken(rp.getClientRegistrationAccessToken());
        response.setClientRegistrationClientUri(rp.getClientRegistrationClientUri());
        response.setClientIdIssuedAt(Utils.date(rp.getClientIdIssuedAt()));
        response.setClientSecretExpiresAt(Utils.date(rp.getClientSecretExpiresAt()));
        return response;
    }

    @Override
    public IOpResponse execute(RegisterSiteParams params) {
        try {
            return execute_(params);
        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        throw HttpException.internalError();
    }

    private void validateParametersAndFallbackIfNeeded(RegisterSiteParams params) {
        Rp fallback = getConfigurationService().defaultRp();

        // op_host
        if (Strings.isNullOrEmpty(params.getOpHost())) {
            LOG.warn("'op_host' is not set for parameter: " + params + ". Look up at configuration file for fallback of 'op_host'");
            String fallbackOpHost = fallback.getOpHost();
            if (Strings.isNullOrEmpty(fallbackOpHost)) {
                throw new HttpException(ErrorResponseCode.INVALID_OP_HOST);
            }
            LOG.warn("Fallback to op_host: " + fallbackOpHost + ", from configuration file.");
            params.setOpHost(fallbackOpHost);
        }

        // grant_type
        List<String> grantTypes = Lists.newArrayList();

        if (params.getGrantTypes() != null && !params.getGrantTypes().isEmpty()) {
            grantTypes.addAll(params.getGrantTypes());
        }

        if (grantTypes.isEmpty() && fallback.getGrantType() != null && !fallback.getGrantType().isEmpty()) {
            grantTypes.addAll(fallback.getGrantType());
        }

        params.setGrantTypes(grantTypes);

        // authorization_redirect_uri
        if (Strings.isNullOrEmpty(params.getAuthorizationRedirectUri())) {
            params.setAuthorizationRedirectUri(fallback.getAuthorizationRedirectUri());
        }
        if (!Utils.isValidUrl(params.getAuthorizationRedirectUri())) {
            throw new HttpException(ErrorResponseCode.INVALID_AUTHORIZATION_REDIRECT_URI);
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
        final Boolean autoRegister = getConfigurationService().getConfiguration().getUma2AuthRegisterClaimsGatheringEndpointAsRedirectUriOfClient();
        if (autoRegister != null && autoRegister && !redirectUris.isEmpty()) {
            String first = redirectUris.iterator().next();
            if (first.contains(params.getOpHost())) {
                final UmaMetadata discovery = getDiscoveryService().getUmaDiscovery(params.getOpHost(), params.getOpDiscoveryPath());
                String autoRedirectUri = discovery.getClaimsInteractionEndpoint() + "?authentication=true";

                LOG.trace("Register claims interaction endpoint as redirect_uri: " + autoRedirectUri);
                redirectUris.add(autoRedirectUri);
            } else {
                LOG.trace("Skip auto registration of claims interaction endpoint as redirect_uri because OP host for different uri's is different which will not pass AS redirect_uri's validation (same host must be present).");
            }
        }
        params.setRedirectUris(Lists.newArrayList(redirectUris));

        // claims_redirect_uri
        Set<String> claimsRedirectUris = Sets.newHashSet();
        if (params.getClaimsRedirectUri() != null && !params.getClaimsRedirectUri().isEmpty()) {
            claimsRedirectUris.addAll(params.getClaimsRedirectUri());
        }
        params.setClaimsRedirectUri(Lists.newArrayList(claimsRedirectUris));

        // scope
        if (params.getScope() == null || params.getScope().isEmpty()) {
            params.setScope(fallback.getScope());
        }
        if (params.getScope() == null || params.getScope().isEmpty()) {
            throw new HttpException(ErrorResponseCode.INVALID_SCOPE);
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

    private void persistRp(String siteId, RegisterSiteParams params) {

        try {
            rp = createRp(siteId, params);

            if (!hasClient(params)) {
                final RegisterResponse registerResponse = registerClient(params);
                rp.setClientId(registerResponse.getClientId());
                rp.setClientSecret(registerResponse.getClientSecret());
                rp.setClientRegistrationAccessToken(registerResponse.getRegistrationAccessToken());
                rp.setClientRegistrationClientUri(registerResponse.getRegistrationClientUri());
                rp.setClientIdIssuedAt(registerResponse.getClientIdIssuedAt());
                rp.setClientSecretExpiresAt(registerResponse.getClientSecretExpiresAt());
            }

            getRpService().create(rp);
        } catch (IOException e) {
            LOG.error("Failed to persist site configuration, params: " + params, e);
            throw new RuntimeException(e);
        }
    }

    private boolean hasClient(RegisterSiteParams params) {
        return !Strings.isNullOrEmpty(params.getClientId()) && !Strings.isNullOrEmpty(params.getClientSecret());
    }

    private RegisterResponse registerClient(RegisterSiteParams params) {
        final String registrationEndpoint = getDiscoveryService().getConnectDiscoveryResponse(params.getOpHost(), params.getOpDiscoveryPath()).getRegistrationEndpoint();
        if (Strings.isNullOrEmpty(registrationEndpoint)) {
            LOG.error("This OP (" + params.getOpHost() + ") does not provide registration_endpoint. It means that oxd is not able dynamically register client. " +
                    "Therefore it is required to obtain/register client manually on OP site and provide client_id and client_secret to oxd register_site command.");
            throw new HttpException(ErrorResponseCode.NO_REGISTRATION_ENDPOINT);
        }

        final RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(createRegisterClientRequest(params));
        registerClient.setExecutor(getHttpService().getClientExecutor());
        final RegisterResponse response = registerClient.exec();
        if (response != null) {
            if (!Strings.isNullOrEmpty(response.getClientId()) && !Strings.isNullOrEmpty(response.getClientSecret())) {
                LOG.trace("Registered client for site - client_id: " + response.getClientId() + ", claims: " + response.getClaims() + ", registration_client_uri:" + response.getRegistrationClientUri());
                return response;
            } else {
                LOG.error("ClientId: " + response.getClientId() + ", clientSecret: " + response.getClientSecret());
            }
        } else {
            LOG.error("RegisterClient response is null.");
        }
        if (response != null && !Strings.isNullOrEmpty(response.getErrorDescription())) {
            LOG.error(response.getErrorDescription());
        }

        throw new RuntimeException("Failed to register client for site. Details: " + (response != null ? response.getEntity() : "response is null"));
    }

    private RegisterRequest createRegisterClientRequest(RegisterSiteParams params) {
        List<ResponseType> responseTypes = Lists.newArrayList();
        for (String type : params.getResponseTypes()) {
            responseTypes.add(ResponseType.fromString(type));
        }

        String clientName = "oxd client for rp: " + rp.getOxdId();
        if (!Strings.isNullOrEmpty(params.getClientName())) {
            clientName = params.getClientName();
            rp.setClientName(clientName);
        }

        final RegisterRequest request = new RegisterRequest(ApplicationType.WEB, clientName, params.getRedirectUris());
        request.setResponseTypes(responseTypes);
        request.setJwksUri(params.getClientJwksUri());
        request.setClaimsRedirectUris(params.getClaimsRedirectUri() != null ? params.getClaimsRedirectUri() : new ArrayList<String>());
        request.setPostLogoutRedirectUris(params.getPostLogoutRedirectUri() != null ? Lists.newArrayList(params.getPostLogoutRedirectUri()) : Lists.<String>newArrayList());
        request.setContacts(params.getContacts());
        request.setScopes(params.getScope());
        request.setDefaultAcrValues(params.getAcrValues());

        if (StringUtils.isNotBlank(params.getClientTokenEndpointAuthSigningAlg())) {
            SignatureAlgorithm signatureAlgorithms = SignatureAlgorithm.fromString(params.getClientTokenEndpointAuthSigningAlg());
            if (signatureAlgorithms == null) {
                LOG.error("Received invalid algorithm in `client_token_endpoint_auth_signing_alg` property. Value: " + params.getClientTokenEndpointAuthSigningAlg() );
                throw new HttpException(ErrorResponseCode.INVALID_ALGORITHM);
            }
            request.setTokenEndpointAuthSigningAlg(signatureAlgorithms);
            rp.setTokenEndpointAuthSigningAlg(params.getClientTokenEndpointAuthSigningAlg());
        }

        if (params.getTrustedClient() != null && params.getTrustedClient()) {
            request.addCustomAttribute("oxAuthTrustedClient", "true");
        }

        List<GrantType> grantTypes = Lists.newArrayList();
        for (String grantType : params.getGrantTypes()) {
            grantTypes.add(GrantType.fromString(grantType));
        }
        request.setGrantTypes(grantTypes);

        if (params.getClientFrontchannelLogoutUris() != null) {
            rp.setFrontChannelLogoutUri(params.getClientFrontchannelLogoutUris());
            request.setFrontChannelLogoutUris(Lists.newArrayList(params.getClientFrontchannelLogoutUris()));
        } else {
            if (rp.getFrontChannelLogoutUri() != null) {
                request.setFrontChannelLogoutUris(rp.getFrontChannelLogoutUri());
            }
        }

        if (StringUtils.isNotBlank(params.getClientTokenEndpointAuthMethod())) {
            final AuthenticationMethod authenticationMethod = AuthenticationMethod.fromString(params.getClientTokenEndpointAuthMethod());
            if (authenticationMethod != null) {
                request.setTokenEndpointAuthMethod(authenticationMethod);
                rp.setTokenEndpointAuthMethod(params.getClientTokenEndpointAuthMethod());
            }
        }

        if (params.getClientRequestUris() != null && !params.getClientRequestUris().isEmpty()) {
            request.setRequestUris(params.getClientRequestUris());
        }

        if (!Strings.isNullOrEmpty(params.getClientSectorIdentifierUri())) {
            request.setSectorIdentifierUri(params.getClientSectorIdentifierUri());
        }

        request.setAccessTokenAsJwt(params.getAccessTokenAsJwt());
        request.setAccessTokenSigningAlg(SignatureAlgorithm.fromString(params.getAccessTokenSigningAlg()));
        request.setRptAsJwt(params.getRptAsJwt());

        rp.setRptAsJwt(params.getRptAsJwt());
        rp.setAccessTokenAsJwt(params.getAccessTokenAsJwt());
        rp.setAccessTokenSigningAlg(params.getAccessTokenSigningAlg());
        rp.setResponseTypes(params.getResponseTypes());
        rp.setPostLogoutRedirectUri(params.getPostLogoutRedirectUri());
        rp.setContacts(params.getContacts());
        rp.setRedirectUris(Lists.newArrayList(params.getRedirectUris()));
        return request;
    }

    private Rp createRp(String siteId, RegisterSiteParams params) {

        Preconditions.checkState(!Strings.isNullOrEmpty(params.getOpHost()), "op_host contains blank value. Please specify valid OP public address.");

        final Rp rp = new Rp(getConfigurationService().defaultRp());
        rp.setOxdId(siteId);
        rp.setOpHost(params.getOpHost());
        rp.setOpDiscoveryPath(params.getOpDiscoveryPath());
        rp.setAuthorizationRedirectUri(params.getAuthorizationRedirectUri());
        rp.setRedirectUris(params.getRedirectUris());
        rp.setClaimsRedirectUri(params.getClaimsRedirectUri());
        rp.setApplicationType("web");
        rp.setUmaProtectedResources(new ArrayList<UmaResource>());
        rp.setFrontChannelLogoutUri(params.getClientFrontchannelLogoutUris());

        if (!Strings.isNullOrEmpty(params.getPostLogoutRedirectUri())) {
            rp.setPostLogoutRedirectUri(params.getPostLogoutRedirectUri());
        }

        if (params.getAcrValues() != null && !params.getAcrValues().isEmpty()) {
            rp.setAcrValues(params.getAcrValues());
        }

        if (params.getClaimsLocales() != null && !params.getClaimsLocales().isEmpty()) {
            rp.setClaimsLocales(params.getClaimsLocales());
        }

        if (!Strings.isNullOrEmpty(params.getClientId()) && !Strings.isNullOrEmpty(params.getClientSecret())) {
            rp.setClientId(params.getClientId());
            rp.setClientSecret(params.getClientSecret());
            rp.setClientRegistrationAccessToken(params.getClientRegistrationAccessToken());
            rp.setClientRegistrationClientUri(params.getClientRegistrationClientUri());
        }

        if (params.getContacts() != null && !params.getContacts().isEmpty()) {
            rp.setContacts(params.getContacts());
        }

        rp.setGrantType(params.getGrantTypes());
        rp.setResponseTypes(params.getResponseTypes());

        if (params.getScope() != null && !params.getScope().isEmpty()) {
            rp.setScope(params.getScope());
        }

        if (params.getUiLocales() != null && !params.getUiLocales().isEmpty()) {
            rp.setUiLocales(params.getUiLocales());
        }

        return rp;
    }
}