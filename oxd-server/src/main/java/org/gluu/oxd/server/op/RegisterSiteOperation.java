package org.gluu.oxd.server.op;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.gluu.oxauth.client.RegisterClient;
import org.gluu.oxauth.client.RegisterRequest;
import org.gluu.oxauth.client.RegisterResponse;
import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.common.SubjectType;
import org.gluu.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
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
        if (StringUtils.isNotBlank(params.getClientId()) && StringUtils.isBlank(params.getClientSecret())) {
            throw new HttpException(ErrorResponseCode.INVALID_CLIENT_SECRET_REQUIRED);
        }

        if (StringUtils.isNotBlank(params.getClientSecret()) && StringUtils.isBlank(params.getClientId())) {
            throw new HttpException(ErrorResponseCode.INVALID_CLIENT_ID_REQUIRED);
        }

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

        if (!grantTypes.contains(GrantType.CLIENT_CREDENTIALS.getValue()) && getConfigurationService().getConfiguration().getAddClientCredentialsGrantTypeAutomaticallyDuringClientRegistration()) {
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
        Set<String> redirectUris = Sets.newLinkedHashSet();
        if (params.getRedirectUris() != null && !params.getRedirectUris().isEmpty() && params.getRedirectUris().stream().allMatch(uri -> Utils.isValidUrl(uri))) {
            redirectUris.addAll(params.getRedirectUris());
        } else {
            throw new HttpException(ErrorResponseCode.INVALID_REDIRECT_URI);
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
        } catch (Exception e) {
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

        final RegisterClient registerClient = getOpClientFactory().createRegisterClient(registrationEndpoint);
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
        request.setPostLogoutRedirectUris(params.getPostLogoutRedirectUris() != null ? params.getPostLogoutRedirectUris() : Lists.newArrayList());
        request.setContacts(params.getContacts());
        request.setScope(params.getScope());
        request.setDefaultAcrValues(params.getAcrValues());

        if (StringUtils.isNotBlank(params.getClientTokenEndpointAuthSigningAlg())) {
            SignatureAlgorithm signatureAlgorithms = SignatureAlgorithm.fromString(params.getClientTokenEndpointAuthSigningAlg());
            if (signatureAlgorithms == null) {
                LOG.error("Received invalid algorithm in `client_token_endpoint_auth_signing_alg` property. Value: " + params.getClientTokenEndpointAuthSigningAlg() );
                throw new HttpException(ErrorResponseCode.INVALID_SIGNATURE_ALGORITHM);
            }
            request.setTokenEndpointAuthSigningAlg(signatureAlgorithms);
            rp.setTokenEndpointAuthSigningAlg(params.getClientTokenEndpointAuthSigningAlg());
        }

        if (params.getTrustedClient() != null && params.getTrustedClient()) {
            request.addCustomAttribute("oxAuthTrustedClient", "true");
            rp.setTrustedClient(params.getTrustedClient());
        }

        if (StringUtils.isNotBlank(rp.getOxdId())) {
            request.addCustomAttribute("oxd_id", rp.getOxdId());
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

        if (params.getScope() != null && !params.getScope().isEmpty()) {
            rp.setScope(params.getScope());
        }

        if (params.getUiLocales() != null && !params.getUiLocales().isEmpty()) {
            rp.setUiLocales(params.getUiLocales());
        }

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
            rp.setFrontChannelLogoutSessionRequired(params.getFrontChannelLogoutSessionRequired());
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
                LOG.error("Received invalid values in `subject_type` property. Value: " + params.getSubjectType() );
                throw new HttpException(ErrorResponseCode.INVALID_SUBJECT_TYPE);
            }
            request.setSubjectType(subjectType);
        }

        if (params.getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims() != null) {
            request.setRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims(params.getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims());
        }

        if (!Strings.isNullOrEmpty(params.getIdTokenSignedResponseAlg())) {
            SignatureAlgorithm signatureAlgorithms = SignatureAlgorithm.fromString(params.getIdTokenSignedResponseAlg());
            if (signatureAlgorithms == null) {
                LOG.error("Received invalid algorithm in `id_token_signed_response_alg` property. Value: " + params.getIdTokenSignedResponseAlg() );
                throw new HttpException(ErrorResponseCode.INVALID_SIGNATURE_ALGORITHM);
            }
            request.setIdTokenSignedResponseAlg(signatureAlgorithms);
        }

        if (!Strings.isNullOrEmpty(params.getIdTokenEncryptedResponseAlg())) {
            KeyEncryptionAlgorithm keyEncryptionAlgorithms = KeyEncryptionAlgorithm.fromName(params.getIdTokenEncryptedResponseAlg());
            if (keyEncryptionAlgorithms == null) {
                LOG.error("Received invalid algorithm in `id_token_encrypted_response_alg` property. Value: " + params.getIdTokenEncryptedResponseAlg() );
                throw new HttpException(ErrorResponseCode.INVALID_KEY_ENCRYPTION_ALGORITHM);
            }
            request.setIdTokenEncryptedResponseAlg(keyEncryptionAlgorithms);
        }

        if (!Strings.isNullOrEmpty(params.getIdTokenEncryptedResponseEnc())) {
            BlockEncryptionAlgorithm blockEncryptionAlgorithms = BlockEncryptionAlgorithm.fromName(params.getIdTokenEncryptedResponseEnc());
            if (blockEncryptionAlgorithms == null) {
                LOG.error("Received invalid algorithm in `id_token_encrypted_response_enc` property. Value: " + params.getIdTokenEncryptedResponseEnc() );
                throw new HttpException(ErrorResponseCode.INVALID_BLOCK_ENCRYPTION_ALGORITHM);
            }
            request.setIdTokenEncryptedResponseEnc(blockEncryptionAlgorithms);
        }

        if (!Strings.isNullOrEmpty(params.getUserInfoSignedResponseAlg())) {
            SignatureAlgorithm signatureAlgorithms = SignatureAlgorithm.fromString(params.getUserInfoSignedResponseAlg());
            if (signatureAlgorithms == null) {
                LOG.error("Received invalid algorithm in `user_info_signed_response_alg` property. Value: " + params.getUserInfoSignedResponseAlg() );
                throw new HttpException(ErrorResponseCode.INVALID_SIGNATURE_ALGORITHM);
            }
            request.setUserInfoSignedResponseAlg(signatureAlgorithms);
        }

        if (!Strings.isNullOrEmpty(params.getUserInfoEncryptedResponseAlg())) {
            KeyEncryptionAlgorithm keyEncryptionAlgorithms = KeyEncryptionAlgorithm.fromName(params.getUserInfoEncryptedResponseAlg());
            if (keyEncryptionAlgorithms == null) {
                LOG.error("Received invalid algorithm in `user_info_encrypted_response_alg` property. Value: " + params.getUserInfoEncryptedResponseAlg() );
                throw new HttpException(ErrorResponseCode.INVALID_KEY_ENCRYPTION_ALGORITHM);
            }
            request.setUserInfoEncryptedResponseAlg(keyEncryptionAlgorithms);
        }

        if (!Strings.isNullOrEmpty(params.getUserInfoEncryptedResponseEnc())) {
            BlockEncryptionAlgorithm blockEncryptionAlgorithms = BlockEncryptionAlgorithm.fromName(params.getUserInfoEncryptedResponseEnc());
            if (blockEncryptionAlgorithms == null) {
                LOG.error("Received invalid algorithm in `user_info_encrypted_response_enc` property. Value: " + params.getUserInfoEncryptedResponseEnc() );
                throw new HttpException(ErrorResponseCode.INVALID_BLOCK_ENCRYPTION_ALGORITHM);
            }
            request.setUserInfoEncryptedResponseEnc(blockEncryptionAlgorithms);
        }

        if (!Strings.isNullOrEmpty(params.getRequestObjectSigningAlg())) {
            SignatureAlgorithm signatureAlgorithms = SignatureAlgorithm.fromString(params.getRequestObjectSigningAlg());
            if (signatureAlgorithms == null) {
                LOG.error("Received invalid algorithm in `request_object_signing_alg` property. Value: " + params.getRequestObjectSigningAlg() );
                throw new HttpException(ErrorResponseCode.INVALID_SIGNATURE_ALGORITHM);
            }
            request.setRequestObjectSigningAlg(signatureAlgorithms);
        }

        if (!Strings.isNullOrEmpty(params.getRequestObjectEncryptionAlg())) {
            KeyEncryptionAlgorithm keyEncryptionAlgorithms = KeyEncryptionAlgorithm.fromName(params.getRequestObjectEncryptionAlg());
            if (keyEncryptionAlgorithms == null) {
                LOG.error("Received invalid algorithm in `request_object_encryption_alg` property. Value: " + params.getRequestObjectEncryptionAlg() );
                throw new HttpException(ErrorResponseCode.INVALID_KEY_ENCRYPTION_ALGORITHM);
            }
            request.setRequestObjectEncryptionAlg(keyEncryptionAlgorithms);
        }

        if (!Strings.isNullOrEmpty(params.getRequestObjectEncryptionEnc())) {
            BlockEncryptionAlgorithm blockEncryptionAlgorithms = BlockEncryptionAlgorithm.fromName(params.getRequestObjectEncryptionEnc());
            if (blockEncryptionAlgorithms == null) {
                LOG.error("Received invalid algorithm in `request_object_encryption_enc` property. Value: " + params.getRequestObjectEncryptionEnc() );
                throw new HttpException(ErrorResponseCode.INVALID_BLOCK_ENCRYPTION_ALGORITHM);
            }
            request.setRequestObjectEncryptionEnc(blockEncryptionAlgorithms);
        }

        if (params.getDefaultMaxAge() != null && NumberUtils.isNumber(params.getDefaultMaxAge().toString())) {
            request.setDefaultMaxAge(params.getDefaultMaxAge());
        }

        if (params.getRequireAuthTime() != null) {
            request.setRequireAuthTime(params.getRequireAuthTime());
            rp.setRequireAuthTime(params.getRequireAuthTime());
        } else {
            request.setRequireAuthTime(rp.getRequireAuthTime());
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

        if (params.getCustomAttributes() != null && !params.getCustomAttributes().isEmpty()) {
            params.getCustomAttributes().entrySet().stream().forEach(e -> {
                request.addCustomAttribute(e.getKey(), e.getValue());
            });
        }

        rp.setRptAsJwt(params.getRptAsJwt());
        rp.setAccessTokenAsJwt(params.getAccessTokenAsJwt());
        rp.setAccessTokenSigningAlg(params.getAccessTokenSigningAlg());
        rp.setResponseTypes(params.getResponseTypes());
        rp.setPostLogoutRedirectUris(params.getPostLogoutRedirectUris());
        rp.setContacts(params.getContacts());
        rp.setRedirectUris(Lists.newArrayList(params.getRedirectUris()));
        rp.setRedirectUri(params.getRedirectUris().get(0));
        return request;
    }

    private Rp createRp(String siteId, RegisterSiteParams params) {

        Preconditions.checkState(!Strings.isNullOrEmpty(params.getOpHost()), "op_host contains blank value. Please specify valid OP public address.");

        final Rp rp = new Rp(getConfigurationService().defaultRp());
        rp.setOxdId(siteId);
        rp.setOpHost(params.getOpHost());
        rp.setOpDiscoveryPath(params.getOpDiscoveryPath());
        rp.setRedirectUris(params.getRedirectUris());
        rp.setClaimsRedirectUri(params.getClaimsRedirectUri());
        rp.setApplicationType("web");
        rp.setUmaProtectedResources(new ArrayList<UmaResource>());
        rp.setFrontChannelLogoutUri(params.getClientFrontchannelLogoutUris());

        if (params.getPostLogoutRedirectUris() != null && !params.getPostLogoutRedirectUris().isEmpty()) {
            rp.setPostLogoutRedirectUris(params.getPostLogoutRedirectUris());
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