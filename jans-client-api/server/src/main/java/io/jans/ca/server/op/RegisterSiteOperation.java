package io.jans.ca.server.op;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.params.RegisterSiteParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.Utils;
import io.jans.ca.server.configuration.model.Rp;
import io.jans.ca.server.mapper.RegisterRequestMapper;
import io.jans.ca.server.service.DiscoveryService;
import io.jans.ca.server.service.RpService;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @Inject
    RpService rpService;
    @Inject
    DiscoveryService discoveryService;


    public RegisterSiteResponse execute_(RegisterSiteParams params) {
        validateParametersAndFallbackIfNeeded(params);

        String rpId = UUID.randomUUID().toString();

        LOG.info("Creating RP ...");
        persistRp(rpId, params);

        LOG.info("RP created: " + rp);

        RegisterSiteResponse response = new RegisterSiteResponse();
        response.setRpId(rpId);
        response.setOpHost(rp.getOpHost());
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
    public IOpResponse execute(RegisterSiteParams params, HttpServletRequest httpRequest) {
        try {
            return execute_(params);
        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        throw HttpException.internalError();
    }

    @Override
    public Class<RegisterSiteParams> getParameterClass() {
        return RegisterSiteParams.class;
    }

    @Override
    public String getReturnType() {
        return MediaType.APPLICATION_JSON;
    }

    private void validateParametersAndFallbackIfNeeded(RegisterSiteParams params) {
        if (StringUtils.isNotBlank(params.getClientId()) && StringUtils.isBlank(params.getClientSecret())) {
            throw new HttpException(ErrorResponseCode.INVALID_CLIENT_SECRET_REQUIRED);
        }

        if (StringUtils.isNotBlank(params.getClientSecret()) && StringUtils.isBlank(params.getClientId())) {
            throw new HttpException(ErrorResponseCode.INVALID_CLIENT_ID_REQUIRED);
        }

        Rp fallback = rpService.defaultRp();

        //op_configuration_endpoint
        LOG.info("Either 'op_configuration_endpoint' or 'op_host' should be set. jans_client_api will now check which of these parameter is available.");
        if (StringUtils.isBlank(params.getOpConfigurationEndpoint())) {
            LOG.warn("'op_configuration_endpoint' is not set for parameter: " + params + ". Look up at configuration file for fallback of 'op_configuration_endpoint'.");
            String fallbackOpConfigurationEndpoint = fallback.getOpConfigurationEndpoint();
            if (StringUtils.isNotBlank(fallbackOpConfigurationEndpoint)) {
                LOG.warn("Fallback to op_configuration_endpoint: " + fallbackOpConfigurationEndpoint + ", from configuration file.");
                params.setOpConfigurationEndpoint(fallbackOpConfigurationEndpoint);
            }
        }

        // op_host
        if (Strings.isNullOrEmpty(params.getOpHost()) && Strings.isNullOrEmpty(params.getOpConfigurationEndpoint())) {
            LOG.error("Either 'op_configuration_endpoint' or 'op_host' should be set. Parameter: " + params);
            throw new HttpException(ErrorResponseCode.INVALID_OP_HOST_AND_CONFIGURATION_ENDPOINT);
        }

        // grant_type
        List<String> grantTypes = Lists.newArrayList();

        if (params.getGrantTypes() != null && !params.getGrantTypes().isEmpty()) {
            grantTypes.addAll(params.getGrantTypes());
        }

        if (grantTypes.isEmpty() && fallback.getGrantType() != null && !fallback.getGrantType().isEmpty()) {
            grantTypes.addAll(fallback.getGrantType());
        }
        boolean addCredentials = jansConfigurationService.find().getAddClientCredentialsGrantTypeAutomaticallyDuringClientRegistration().booleanValue();
        if (!grantTypes.contains(GrantType.CLIENT_CREDENTIALS.getValue()) && addCredentials) {
            grantTypes.add(GrantType.CLIENT_CREDENTIALS.getValue());
        }

        params.setGrantTypes(grantTypes);

        //post_logout_redirect_uri
        if (params.getPostLogoutRedirectUris() != null && params.getPostLogoutRedirectUris().isEmpty()
                && fallback.getPostLogoutRedirectUris() != null && !fallback.getPostLogoutRedirectUris().isEmpty()) {
            params.setPostLogoutRedirectUris(fallback.getPostLogoutRedirectUris());
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
        if (params.getRedirectUris() == null || params.getRedirectUris().isEmpty()) {
            params.setRedirectUris(fallback.getRedirectUris());
        }
        Set<String> redirectUris = Sets.newLinkedHashSet();
        if (params.getRedirectUris() != null && !params.getRedirectUris().isEmpty() && params.getRedirectUris().stream().allMatch(uri -> Utils.isValidUrl(uri))) {
            redirectUris.addAll(params.getRedirectUris());
        } else {
            throw new HttpException(ErrorResponseCode.INVALID_REDIRECT_URI);
        }
        final Boolean autoRegister = jansConfigurationService.find().getUma2AutoRegisterClaimsGatheringEndpointAsRedirectUriOfClient();
        if (autoRegister != null && autoRegister && !redirectUris.isEmpty()) {
            String first = redirectUris.iterator().next();
            if (first.contains(discoveryService.getConnectDiscoveryResponse(params.getOpConfigurationEndpoint(), params.getOpHost(), params.getOpDiscoveryPath()).getIssuer())) {
                final UmaMetadata discovery = discoveryService.getUmaDiscovery(params.getOpConfigurationEndpoint(), params.getOpHost(), params.getOpDiscoveryPath());
                String autoRedirectUri = discovery.getClaimsInteractionEndpoint() + "?authentication=true";

                LOG.trace("Register claims interaction endpoint as redirect_uri: " + autoRedirectUri);
                redirectUris.add(autoRedirectUri);
            } else {
                LOG.trace("Skip auto registration of claims interaction endpoint as redirect_uri because OP host for different uri's is different which will not pass AS redirect_uri's validation (same host must be present).");
            }
        }
        params.setRedirectUris(Lists.newArrayList(redirectUris));

        // claims_redirect_uri
        if ((params.getClaimsRedirectUri() == null || params.getClaimsRedirectUri().isEmpty()) && (fallback.getClaimsRedirectUri() != null && !fallback.getClaimsRedirectUri().isEmpty())) {
            params.setClaimsRedirectUri(fallback.getClaimsRedirectUri());
        }
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
        if ((params.getClaimsLocales() == null || params.getClaimsLocales().isEmpty()) && (fallback.getClaimsLocales() != null && !fallback.getClaimsLocales().isEmpty())) {
            params.setClaimsLocales(fallback.getClaimsLocales());
        }

        //client_name
        if (StringUtils.isBlank(params.getClientName()) && StringUtils.isNotBlank(fallback.getClientName())) {
            params.setClientName(fallback.getClientName());
        }

        //client_jwks_uri
        if (StringUtils.isBlank(params.getClientJwksUri()) && StringUtils.isNotBlank(fallback.getClientJwksUri())) {
            params.setClientJwksUri(fallback.getClientJwksUri());
        }

        //token_endpoint_auth_method
        if (StringUtils.isBlank(params.getClientTokenEndpointAuthMethod()) && StringUtils.isNotBlank(fallback.getTokenEndpointAuthMethod())) {
            params.setClientTokenEndpointAuthMethod(fallback.getTokenEndpointAuthMethod());
        }

        //token_endpoint_auth_signing_alg
        if (StringUtils.isBlank(params.getClientTokenEndpointAuthSigningAlg()) && StringUtils.isNotBlank(fallback.getTokenEndpointAuthSigningAlg())) {
            params.setClientTokenEndpointAuthSigningAlg(fallback.getTokenEndpointAuthSigningAlg());
        }

        //request_uris
        if ((params.getClientRequestUris() == null || params.getClientRequestUris().isEmpty()) && (fallback.getRequestUris() != null && !fallback.getRequestUris().isEmpty())) {
            params.setClientRequestUris(fallback.getRequestUris());
        }

        //front_channel_logout_uris
        if (StringUtils.isBlank(params.getClientFrontchannelLogoutUri()) && StringUtils.isNotBlank(fallback.getFrontChannelLogoutUri())) {
            params.setClientFrontchannelLogoutUri(fallback.getFrontChannelLogoutUri());
        }

        //sector_identifier_uri
        if (StringUtils.isBlank(params.getClientSectorIdentifierUri()) && StringUtils.isNotBlank(fallback.getSectorIdentifierUri())) {
            params.setClientSectorIdentifierUri(fallback.getSectorIdentifierUri());
        }

        //client_id
        if (StringUtils.isBlank(params.getClientId()) && StringUtils.isNotBlank(fallback.getClientId())) {
            params.setClientId(fallback.getClientId());
        }

        //client_secret
        if (StringUtils.isBlank(params.getClientSecret()) && StringUtils.isNotBlank(fallback.getClientSecret())) {
            params.setClientSecret(fallback.getClientSecret());
        }

        //access_token_signing_alg
        if (StringUtils.isBlank(params.getAccessTokenSigningAlg()) && StringUtils.isNotBlank(fallback.getAccessTokenSigningAlg())) {
            params.setAccessTokenSigningAlg(fallback.getAccessTokenSigningAlg());
        }

        //logo_uri
        if (StringUtils.isBlank(params.getLogoUri()) && StringUtils.isNotBlank(fallback.getLogoUri())) {
            params.setLogoUri(fallback.getLogoUri());
        }

        //client_uri
        if (StringUtils.isBlank(params.getClientUri()) && StringUtils.isNotBlank(fallback.getClientUri())) {
            params.setClientUri(fallback.getClientUri());
        }

        //policy_uri
        if (StringUtils.isBlank(params.getPolicyUri()) && StringUtils.isNotBlank(fallback.getPolicyUri())) {
            params.setPolicyUri(fallback.getPolicyUri());
        }

        //tos_uri
        if (StringUtils.isBlank(params.getTosUri()) && StringUtils.isNotBlank(fallback.getTosUri())) {
            params.setTosUri(fallback.getTosUri());
        }

        //jwks
        if (StringUtils.isBlank(params.getJwks()) && StringUtils.isNotBlank(fallback.getJwks())) {
            params.setJwks(fallback.getJwks());
        }

        //id_token_binding_cnf
        if (StringUtils.isBlank(params.getIdTokenBindingCnf()) && StringUtils.isNotBlank(fallback.getIdTokenBindingCnf())) {
            params.setIdTokenBindingCnf(fallback.getIdTokenBindingCnf());
        }

        //tls_client_auth_subject_dn
        if (StringUtils.isBlank(params.getTlsClientAuthSubjectDn()) && StringUtils.isNotBlank(fallback.getTlsClientAuthSubjectDn())) {
            params.setTlsClientAuthSubjectDn(fallback.getTlsClientAuthSubjectDn());
        }

        //id_token_signed_response_alg
        if (StringUtils.isBlank(params.getIdTokenSignedResponseAlg()) && StringUtils.isNotBlank(fallback.getIdTokenSignedResponseAlg())) {
            params.setIdTokenSignedResponseAlg(fallback.getIdTokenSignedResponseAlg());
        }

        //id_token_encrypted_response_alg
        if (StringUtils.isBlank(params.getIdTokenEncryptedResponseAlg()) && StringUtils.isNotBlank(fallback.getIdTokenEncryptedResponseAlg())) {
            params.setIdTokenEncryptedResponseAlg(fallback.getIdTokenEncryptedResponseAlg());
        }

        //id_token_encrypted_response_enc
        if (StringUtils.isBlank(params.getIdTokenEncryptedResponseEnc()) && StringUtils.isNotBlank(fallback.getIdTokenEncryptedResponseEnc())) {
            params.setIdTokenEncryptedResponseEnc(fallback.getIdTokenEncryptedResponseEnc());
        }

        //user_info_signed_response_alg
        if (StringUtils.isBlank(params.getUserInfoSignedResponseAlg()) && StringUtils.isNotBlank(fallback.getUserInfoSignedResponseAlg())) {
            params.setUserInfoSignedResponseAlg(fallback.getUserInfoSignedResponseAlg());
        }

        //user_info_encrypted_response_alg
        if (StringUtils.isBlank(params.getUserInfoEncryptedResponseAlg()) && StringUtils.isNotBlank(fallback.getUserInfoEncryptedResponseAlg())) {
            params.setUserInfoEncryptedResponseAlg(fallback.getUserInfoEncryptedResponseAlg());
        }

        //user_info_encrypted_response_enc
        if (StringUtils.isBlank(params.getUserInfoEncryptedResponseEnc()) && StringUtils.isNotBlank(fallback.getUserInfoEncryptedResponseEnc())) {
            params.setUserInfoEncryptedResponseEnc(fallback.getUserInfoEncryptedResponseEnc());
        }

        //request_object_signing_alg
        if (StringUtils.isBlank(params.getRequestObjectSigningAlg()) && StringUtils.isNotBlank(fallback.getRequestObjectSigningAlg())) {
            params.setRequestObjectSigningAlg(fallback.getRequestObjectSigningAlg());
        }

        //request_object_encryption_alg
        if (StringUtils.isBlank(params.getRequestObjectEncryptionAlg()) && StringUtils.isNotBlank(fallback.getRequestObjectEncryptionAlg())) {
            params.setRequestObjectEncryptionAlg(fallback.getRequestObjectEncryptionAlg());
        }

        //request_object_encryption_enc
        if (StringUtils.isBlank(params.getRequestObjectEncryptionEnc()) && StringUtils.isNotBlank(fallback.getRequestObjectEncryptionEnc())) {
            params.setRequestObjectEncryptionEnc(fallback.getRequestObjectEncryptionEnc());
        }

        //default_max_age
        if (params.getDefaultMaxAge() == null && fallback.getDefaultMaxAge() != null) {
            params.setDefaultMaxAge(fallback.getDefaultMaxAge());
        }

        //initiate_login_uri
        if (StringUtils.isBlank(params.getInitiateLoginUri()) && StringUtils.isNotBlank(fallback.getInitiateLoginUri())) {
            params.setInitiateLoginUri(fallback.getInitiateLoginUri());
        }

        //authorized_origins
        if ((params.getAuthorizedOrigins() == null || params.getAuthorizedOrigins().isEmpty()) && (fallback.getAuthorizedOrigins() != null && !fallback.getAuthorizedOrigins().isEmpty())) {
            params.setAuthorizedOrigins(fallback.getAuthorizedOrigins());
        }

        //access_token_lifetime
        if (params.getAccessTokenLifetime() == null && fallback.getAccessTokenLifetime() != null) {
            params.setAccessTokenLifetime(fallback.getAccessTokenLifetime());
        }

        //software_id
        if (StringUtils.isBlank(params.getSoftwareId()) && StringUtils.isNotBlank(fallback.getSoftwareId())) {
            params.setSoftwareId(fallback.getSoftwareId());
        }

        //software_version
        if (StringUtils.isBlank(params.getSoftwareVersion()) && StringUtils.isNotBlank(fallback.getSoftwareVersion())) {
            params.setSoftwareVersion(fallback.getSoftwareVersion());
        }

        //software_statement
        if (StringUtils.isBlank(params.getSoftwareStatement()) && StringUtils.isNotBlank(fallback.getSoftwareStatement())) {
            params.setSoftwareStatement(fallback.getSoftwareStatement());
        }

        //custom_attributes
        if ((params.getCustomAttributes() == null || params.getCustomAttributes().isEmpty()) && (fallback.getCustomAttributes() != null && !fallback.getCustomAttributes().isEmpty())) {
            params.setCustomAttributes(fallback.getCustomAttributes());
        }

        //access_token_as_jwt
        if (params.getAccessTokenAsJwt() == null) {
            params.setAccessTokenAsJwt(fallback.getAccessTokenAsJwt());
        }

        //rpt_as_jwt
        if (params.getRptAsJwt() == null) {
            params.setRptAsJwt(fallback.getRptAsJwt());
        }

        //front_channel_logout_session_required
        if (params.getFrontChannelLogoutSessionRequired() == null) {
            params.setFrontChannelLogoutSessionRequired(fallback.getFrontChannelLogoutSessionRequired());
        }

        //run_introspection_script_beforeaccess_token_as_jwt_creation_and_include_claims
        if (params.getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims() == null) {
            params.setRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims(fallback.getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims());
        }

        //require_auth_time
        if (params.getRequireAuthTime() == null) {
            params.setRequireAuthTime(fallback.getRequireAuthTime());
        }
    }

    private void persistRp(String rpId, RegisterSiteParams params) {

        try {
            final RegisterRequest registerRequest = createRegisterClientRequest(params, rpId);
            rp = createRp(registerRequest);
            rp.setRpId(rpId);
            rp.setApplicationType("web");
            rp.setOpHost(discoveryService.getConnectDiscoveryResponse(params.getOpConfigurationEndpoint(), params.getOpHost(), params.getOpDiscoveryPath()).getIssuer());
            rp.setOpDiscoveryPath(params.getOpDiscoveryPath());
            rp.setOpConfigurationEndpoint(params.getOpConfigurationEndpoint());
            rp.setUiLocales(params.getUiLocales());
            rp.setSyncClientFromOp(params.getSyncClientFromOp());
            rp.setSyncClientPeriodInSeconds(params.getSyncClientPeriodInSeconds());

            if (!hasClient(params)) {
                final RegisterResponse registerResponse = registerClient(params, registerRequest);

                rp.setClientId(registerResponse.getClientId());
                rp.setClientSecret(registerResponse.getClientSecret());
                rp.setClientRegistrationAccessToken(registerResponse.getRegistrationAccessToken());
                rp.setClientRegistrationClientUri(registerResponse.getRegistrationClientUri());
                rp.setClientIdIssuedAt(registerResponse.getClientIdIssuedAt());
                rp.setClientSecretExpiresAt(registerResponse.getClientSecretExpiresAt());
            } else {
                rp.setClientId(params.getClientId());
                rp.setClientSecret(params.getClientSecret());
            }

            rpService.create(rp);
        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Failed to persist site configuration, params: " + params, e);
            throw new RuntimeException(e);
        }
    }

    private boolean hasClient(RegisterSiteParams params) {
        return !Strings.isNullOrEmpty(params.getClientId()) && !Strings.isNullOrEmpty(params.getClientSecret());
    }

    private RegisterResponse registerClient(RegisterSiteParams params, RegisterRequest request) {
        String opHostEndpoint = Strings.isNullOrEmpty(params.getOpConfigurationEndpoint()) ? params.getOpHost() : params.getOpConfigurationEndpoint();
        Preconditions.checkState(!Strings.isNullOrEmpty(opHostEndpoint), "Both op_configuration_endpoint and op_host contains blank value. Please specify valid OP public address.");

        final String registrationEndpoint = discoveryService.getConnectDiscoveryResponse(params.getOpConfigurationEndpoint(), params.getOpHost(), params.getOpDiscoveryPath()).getRegistrationEndpoint();
        if (Strings.isNullOrEmpty(registrationEndpoint)) {
            LOG.error("This OP (" + opHostEndpoint + ") does not provide registration_endpoint. It means that jans_client_api is not able dynamically register client. " +
                    "Therefore it is required to obtain/register client manually on OP site and provide client_id and client_secret to jans_client_api register_site command.");
            throw new HttpException(ErrorResponseCode.NO_REGISTRATION_ENDPOINT);
        }

        final RegisterClient registerClient = rpService.createRegisterClient(registrationEndpoint, request);
        final RegisterResponse response = registerClient.exec();
        if (response != null) {
            if (!Strings.isNullOrEmpty(response.getClientId()) && !Strings.isNullOrEmpty(response.getClientSecret())) {
                LOG.trace("Registered client for site - client_id: " + response.getClientId() + ", claims: " + response.getClaims() + ", registration_client_uri:" + response.getRegistrationClientUri());
                return response;
            }
            LOG.error("ClientId: " + response.getClientId() + ", clientSecret: " + response.getClientSecret());
            if (Strings.isNullOrEmpty(response.getClientId())) {
                LOG.error("`client_id` is not returned from OP host. Please check OP log file for error (oxauth.log).");
                throw new HttpException(ErrorResponseCode.NO_CLIENT_ID_RETURNED);
            }

            if (Strings.isNullOrEmpty(response.getClientSecret())) {
                LOG.error("`client_secret` is not returned from OP host. Please check: 1) OP log file for error (oxauth.log) 2) whether `returnClientSecretOnRead` configuration property is set to true on OP host.");
                throw new HttpException(ErrorResponseCode.NO_CLIENT_SECRET_RETURNED);
            }

        } else {
            LOG.error("RegisterClient response is null.");
        }
        if (response != null && !Strings.isNullOrEmpty(response.getErrorDescription())) {
            LOG.error(response.getErrorDescription());
        }

        throw new RuntimeException("Failed to register client for site. Details: " + (response != null ? response.getEntity() : "response is null"));
    }

    private RegisterRequest createRegisterClientRequest(RegisterSiteParams params, String rpId) {
        String clientName = "jans_client_api client for rp: " + rpId;
        if (!Strings.isNullOrEmpty(params.getClientName())) {
            clientName = params.getClientName();
        }

        final RegisterRequest request = new RegisterRequest(ApplicationType.WEB, clientName, params.getRedirectUris());
        request.setResponseTypesStrings(params.getResponseTypes());
        request.setJwksUri(params.getClientJwksUri());
        request.setClaimsRedirectUris(params.getClaimsRedirectUri() != null ? params.getClaimsRedirectUri() : new ArrayList<String>());
        request.setPostLogoutRedirectUris(params.getPostLogoutRedirectUris() != null ? params.getPostLogoutRedirectUris() : Lists.newArrayList());
        request.setContacts(params.getContacts());
        request.setScope(params.getScope());
        request.setDefaultAcrValues(params.getAcrValues());

        if (StringUtils.isNotBlank(params.getClientTokenEndpointAuthSigningAlg())) {
            SignatureAlgorithm signatureAlgorithms = SignatureAlgorithm.fromString(params.getClientTokenEndpointAuthSigningAlg());
            if (signatureAlgorithms == null) {
                LOG.error("Received invalid algorithm in `client_token_endpoint_auth_signing_alg` property. Value: " + params.getClientTokenEndpointAuthSigningAlg());
                throw new HttpException(ErrorResponseCode.INVALID_SIGNATURE_ALGORITHM);
            }
            request.setTokenEndpointAuthSigningAlg(signatureAlgorithms);
        }

        if (StringUtils.isNotBlank(rpId)) {
            request.addCustomAttribute("rp_id", rpId);
        }

        List<GrantType> grantTypes = Lists.newArrayList();
        for (String grantType : params.getGrantTypes()) {
            grantTypes.add(GrantType.fromString(grantType));
        }
        request.setGrantTypes(grantTypes);

        if (StringUtils.isNotBlank(params.getClientFrontchannelLogoutUri())) {
            request.setFrontChannelLogoutUri(params.getClientFrontchannelLogoutUri());
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

        request.setAccessTokenAsJwt(params.getAccessTokenAsJwt());
        request.setAccessTokenSigningAlg(SignatureAlgorithm.fromString(params.getAccessTokenSigningAlg()));
        request.setRptAsJwt(params.getRptAsJwt());

        if (!Strings.isNullOrEmpty(params.getLogoUri())) {
            request.setLogoUri(params.getLogoUri());
        }

        if (!Strings.isNullOrEmpty(params.getClientUri())) {
            request.setClientUri(params.getClientUri());
        }

        if (!Strings.isNullOrEmpty(params.getPolicyUri())) {
            request.setPolicyUri(params.getPolicyUri());
        }

        if (params.getFrontChannelLogoutSessionRequired() != null) {
            request.setFrontChannelLogoutSessionRequired(params.getFrontChannelLogoutSessionRequired());
        }

        if (!Strings.isNullOrEmpty(params.getTosUri())) {
            request.setTosUri(params.getTosUri());
        }

        if (!Strings.isNullOrEmpty(params.getJwks())) {
            request.setJwks(params.getJwks());
        }

        if (!Strings.isNullOrEmpty(params.getIdTokenBindingCnf())) {
            request.setIdTokenTokenBindingCnf(params.getIdTokenBindingCnf());
        }

        if (!Strings.isNullOrEmpty(params.getTlsClientAuthSubjectDn())) {
            request.setTlsClientAuthSubjectDn(params.getTlsClientAuthSubjectDn());
        }

        if (!Strings.isNullOrEmpty(params.getSubjectType())) {
            SubjectType subjectType = SubjectType.fromString(params.getSubjectType());
            if (subjectType == null) {
                LOG.error("Received invalid values in `subject_type` property. Value: " + params.getSubjectType());
                throw new HttpException(ErrorResponseCode.INVALID_SUBJECT_TYPE);
            }
            request.setSubjectType(subjectType);
        }

        if (params.getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims() != null) {
            request.setRunIntrospectionScriptBeforeJwtCreation(params.getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims());
        }

        if (!Strings.isNullOrEmpty(params.getIdTokenSignedResponseAlg())) {
            SignatureAlgorithm signatureAlgorithms = SignatureAlgorithm.fromString(params.getIdTokenSignedResponseAlg());
            if (signatureAlgorithms == null) {
                LOG.error("Received invalid algorithm in `id_token_signed_response_alg` property. Value: " + params.getIdTokenSignedResponseAlg());
                throw new HttpException(ErrorResponseCode.INVALID_SIGNATURE_ALGORITHM);
            }

            boolean acceptIdTokenWithoutSignature = jansConfigurationService.find().getAcceptIdTokenWithoutSignature().booleanValue();
            if (signatureAlgorithms == SignatureAlgorithm.NONE && !acceptIdTokenWithoutSignature) {
                LOG.error("`ID_TOKEN` without signature is not allowed. To allow `ID_TOKEN` without signature set `accept_id_token_without_signature` field to 'true' in client-api-server.yml.");
                throw new HttpException(ErrorResponseCode.ID_TOKEN_WITHOUT_SIGNATURE_NOT_ALLOWED);
            }

            request.setIdTokenSignedResponseAlg(signatureAlgorithms);
        }

        if (!Strings.isNullOrEmpty(params.getIdTokenEncryptedResponseAlg())) {
            KeyEncryptionAlgorithm keyEncryptionAlgorithms = KeyEncryptionAlgorithm.fromName(params.getIdTokenEncryptedResponseAlg());
            if (keyEncryptionAlgorithms == null) {
                LOG.error("Received invalid algorithm in `id_token_encrypted_response_alg` property. Value: " + params.getIdTokenEncryptedResponseAlg());
                throw new HttpException(ErrorResponseCode.INVALID_KEY_ENCRYPTION_ALGORITHM);
            }
            request.setIdTokenEncryptedResponseAlg(keyEncryptionAlgorithms);
        }

        if (!Strings.isNullOrEmpty(params.getIdTokenEncryptedResponseEnc())) {
            BlockEncryptionAlgorithm blockEncryptionAlgorithms = BlockEncryptionAlgorithm.fromName(params.getIdTokenEncryptedResponseEnc());
            if (blockEncryptionAlgorithms == null) {
                LOG.error("Received invalid algorithm in `id_token_encrypted_response_enc` property. Value: " + params.getIdTokenEncryptedResponseEnc());
                throw new HttpException(ErrorResponseCode.INVALID_BLOCK_ENCRYPTION_ALGORITHM);
            }
            request.setIdTokenEncryptedResponseEnc(blockEncryptionAlgorithms);
        }

        if (!Strings.isNullOrEmpty(params.getUserInfoSignedResponseAlg())) {
            SignatureAlgorithm signatureAlgorithms = SignatureAlgorithm.fromString(params.getUserInfoSignedResponseAlg());
            if (signatureAlgorithms == null) {
                LOG.error("Received invalid algorithm in `user_info_signed_response_alg` property. Value: " + params.getUserInfoSignedResponseAlg());
                throw new HttpException(ErrorResponseCode.INVALID_SIGNATURE_ALGORITHM);
            }
            request.setUserInfoSignedResponseAlg(signatureAlgorithms);
        }

        if (!Strings.isNullOrEmpty(params.getUserInfoEncryptedResponseAlg())) {
            KeyEncryptionAlgorithm keyEncryptionAlgorithms = KeyEncryptionAlgorithm.fromName(params.getUserInfoEncryptedResponseAlg());
            if (keyEncryptionAlgorithms == null) {
                LOG.error("Received invalid algorithm in `user_info_encrypted_response_alg` property. Value: " + params.getUserInfoEncryptedResponseAlg());
                throw new HttpException(ErrorResponseCode.INVALID_KEY_ENCRYPTION_ALGORITHM);
            }
            request.setUserInfoEncryptedResponseAlg(keyEncryptionAlgorithms);
        }

        if (!Strings.isNullOrEmpty(params.getUserInfoEncryptedResponseEnc())) {
            BlockEncryptionAlgorithm blockEncryptionAlgorithms = BlockEncryptionAlgorithm.fromName(params.getUserInfoEncryptedResponseEnc());
            if (blockEncryptionAlgorithms == null) {
                LOG.error("Received invalid algorithm in `user_info_encrypted_response_enc` property. Value: " + params.getUserInfoEncryptedResponseEnc());
                throw new HttpException(ErrorResponseCode.INVALID_BLOCK_ENCRYPTION_ALGORITHM);
            }
            request.setUserInfoEncryptedResponseEnc(blockEncryptionAlgorithms);
        }

        if (!Strings.isNullOrEmpty(params.getRequestObjectSigningAlg())) {
            SignatureAlgorithm signatureAlgorithms = SignatureAlgorithm.fromString(params.getRequestObjectSigningAlg());
            if (signatureAlgorithms == null) {
                LOG.error("Received invalid algorithm in `request_object_signing_alg` property. Value: " + params.getRequestObjectSigningAlg());
                throw new HttpException(ErrorResponseCode.INVALID_SIGNATURE_ALGORITHM);
            }
            request.setRequestObjectSigningAlg(signatureAlgorithms);
        }

        if (!Strings.isNullOrEmpty(params.getRequestObjectEncryptionAlg())) {
            KeyEncryptionAlgorithm keyEncryptionAlgorithms = KeyEncryptionAlgorithm.fromName(params.getRequestObjectEncryptionAlg());
            if (keyEncryptionAlgorithms == null) {
                LOG.error("Received invalid algorithm in `request_object_encryption_alg` property. Value: " + params.getRequestObjectEncryptionAlg());
                throw new HttpException(ErrorResponseCode.INVALID_KEY_ENCRYPTION_ALGORITHM);
            }
            request.setRequestObjectEncryptionAlg(keyEncryptionAlgorithms);
        }

        if (!Strings.isNullOrEmpty(params.getRequestObjectEncryptionEnc())) {
            BlockEncryptionAlgorithm blockEncryptionAlgorithms = BlockEncryptionAlgorithm.fromName(params.getRequestObjectEncryptionEnc());
            if (blockEncryptionAlgorithms == null) {
                LOG.error("Received invalid algorithm in `request_object_encryption_enc` property. Value: " + params.getRequestObjectEncryptionEnc());
                throw new HttpException(ErrorResponseCode.INVALID_BLOCK_ENCRYPTION_ALGORITHM);
            }
            request.setRequestObjectEncryptionEnc(blockEncryptionAlgorithms);
        }

        if (params.getDefaultMaxAge() != null && NumberUtils.isNumber(params.getDefaultMaxAge().toString())) {
            request.setDefaultMaxAge(params.getDefaultMaxAge());
        }

        if (!Strings.isNullOrEmpty(params.getInitiateLoginUri())) {
            request.setInitiateLoginUri(params.getInitiateLoginUri());
        }

        if (params.getAuthorizedOrigins() != null && !params.getAuthorizedOrigins().isEmpty()) {
            request.setAuthorizedOrigins(params.getAuthorizedOrigins());
        }

        if (params.getAccessTokenLifetime() != null && NumberUtils.isNumber(params.getAccessTokenLifetime().toString())) {
            request.setAccessTokenLifetime(params.getAccessTokenLifetime());
        }

        if (!Strings.isNullOrEmpty(params.getSoftwareId())) {
            request.setSoftwareId(params.getSoftwareId());
        }

        if (!Strings.isNullOrEmpty(params.getSoftwareVersion())) {
            request.setSoftwareVersion(params.getSoftwareVersion());
        }

        if (!Strings.isNullOrEmpty(params.getSoftwareStatement())) {
            request.setSoftwareStatement(params.getSoftwareStatement());
        }

        if (params.getAllowSpontaneousScopes() != null) {
            request.setAllowSpontaneousScopes(params.getAllowSpontaneousScopes());
        }

        if (CollectionUtils.isNotEmpty(params.getSpontaneousScopes())) {
            request.setSpontaneousScopes(params.getSpontaneousScopes());
        }

        if (params.getCustomAttributes() != null && !params.getCustomAttributes().isEmpty()) {
            params.getCustomAttributes().entrySet().removeIf(entry -> entry.getKey().contains("oxAuthTrustedClient"));
            params.getCustomAttributes().entrySet().stream().forEach(e -> {
                request.addCustomAttribute(e.getKey(), e.getValue());
            });
        }

        return request;
    }

    private Rp createRp(RegisterRequest registerRequest) {
        final Rp rp = new Rp();

        RegisterRequestMapper.fillRp(rp, registerRequest);

        return rp;
    }
}