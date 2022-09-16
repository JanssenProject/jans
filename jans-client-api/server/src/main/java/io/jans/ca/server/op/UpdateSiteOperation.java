package io.jans.ca.server.op;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.params.UpdateSiteParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.UpdateSiteResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.Utils;
import io.jans.ca.server.configuration.model.Rp;
import io.jans.ca.server.mapper.RegisterRequestMapper;
import io.jans.ca.server.service.RpService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequestScoped
@Named
public class UpdateSiteOperation extends BaseOperation<UpdateSiteParams> {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateSiteOperation.class);

    private Rp rp;

    @Inject
    RpService rpService;

    @Override
    public IOpResponse execute(UpdateSiteParams params, HttpServletRequest httpServletRequest) {
        rp = getRp(params);

        LOG.info("Updating rp ... rp: " + rp);
        persistRp(rp, params);

        UpdateSiteResponse response = new UpdateSiteResponse();
        response.setRpId(rp.getRpId());
        return response;
    }

    @Override
    public Class<UpdateSiteParams> getParameterClass() {
        return UpdateSiteParams.class;
    }

    @Override
    public String getReturnType() {
        return MediaType.APPLICATION_JSON;
    }

    private void persistRp(Rp rp, UpdateSiteParams params) {

        try {
            RegisterRequest registerRequest = createRegisterClientRequest(rp, params);
            updateRegisteredClient(rp, registerRequest);
            RegisterRequestMapper.fillRp(rp, registerRequest);
            rpService.update(rp);

            LOG.info("RP updated: " + rp);
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist RP, params: " + params, e);
        }
    }

    private void updateRegisteredClient(Rp rp, RegisterRequest registerRequest) {
        if (StringUtils.isBlank(rp.getClientRegistrationClientUri())) {
            LOG.error("Registration client url is blank.");
            throw new HttpException(ErrorResponseCode.INVALID_REGISTRATION_CLIENT_URL);
        }

        final RegisterClient registerClient = rpService.createRegisterClient(rp.getClientRegistrationClientUri(), registerRequest);
        final RegisterResponse response = registerClient.exec();
        if (response != null) {
            if (response.getStatus() == 200) {
                LOG.trace("Client updated successfully. for rp - client_id: " + rp.getClientId());
                return;
            } else {
                LOG.error("Response is not OK (200).");
            }
        } else {
            LOG.error("RegisterClient response is null.");
        }
        if (!Strings.isNullOrEmpty(response.getErrorDescription())) {
            LOG.error(response.getErrorDescription());
        }

        throw new RuntimeException("Failed to update client for rp. Details:" + response.getEntity());
    }

    private RegisterRequest createRegisterClientRequest(Rp rp, UpdateSiteParams params) {

        final RegisterRequest request = RegisterRequestMapper.createRegisterRequest(rp);
        request.setHttpMethod(HttpMethod.PUT); // force update

        if (params.getResponseTypes() != null && !params.getResponseTypes().isEmpty()) {
            request.setResponseTypesStrings(params.getResponseTypes());
        }

        if (params.getRptAsJwt() != null) {
            request.setRptAsJwt(params.getRptAsJwt());
        }

        if (params.getGrantType() != null && !params.getGrantType().isEmpty()) {
            request.setGrantTypes(params.getGrantType().stream().map(item -> GrantType.fromString(item)).collect(Collectors.toList()));
        }


        Set<String> redirectUris = Sets.newLinkedHashSet();
        if (params.getRedirectUris() != null && !params.getRedirectUris().isEmpty()) {
            if (!params.getRedirectUris().stream().allMatch(uri -> Utils.isValidUrl(uri))) {
                throw new HttpException(ErrorResponseCode.INVALID_REDIRECT_URI);
            }

            redirectUris.addAll(params.getRedirectUris());
            List<String> redirectUriList = Lists.newArrayList(redirectUris);
            request.setRedirectUris(redirectUriList);
        }

        if (params.getAcrValues() != null && !params.getAcrValues().isEmpty()) {
            request.setDefaultAcrValues(params.getAcrValues());
        }

        if (params.getClaimsRedirectUri() != null && !params.getClaimsRedirectUri().isEmpty()) {
            request.setClaimsRedirectUris(params.getClaimsRedirectUri());
        }

        if (params.getAccessTokenAsJwt() != null) {
            request.setAccessTokenAsJwt(params.getAccessTokenAsJwt());
        }

        if (params.getAccessTokenSigningAlg() != null) {
            SignatureAlgorithm signatureAlgorithms = SignatureAlgorithm.fromString(params.getAccessTokenSigningAlg());
            if (signatureAlgorithms == null) {
                LOG.error("Received invalid algorithm in `access_token_signing_alg` property. Value: " + params.getAccessTokenSigningAlg());
                throw new HttpException(ErrorResponseCode.INVALID_SIGNATURE_ALGORITHM);
            }
            request.setAccessTokenSigningAlg(signatureAlgorithms);
        }

        if (!Strings.isNullOrEmpty(params.getClientJwksUri())) {
            request.setJwksUri(params.getClientJwksUri());
        }

        if (params.getPostLogoutRedirectUris() != null && !params.getPostLogoutRedirectUris().isEmpty()) {
            request.setPostLogoutRedirectUris(Lists.newArrayList(params.getPostLogoutRedirectUris()));
        }

        if (params.getContacts() != null) {
            request.setContacts(params.getContacts());
        }

        if (params.getScope() != null) {
            request.setScope(params.getScope());
        }

        if (!Strings.isNullOrEmpty(params.getClientSectorIdentifierUri())) {
            request.setSectorIdentifierUri(params.getClientSectorIdentifierUri());
        }

        if (!Strings.isNullOrEmpty(params.getClientFrontchannelLogoutUri())) {
            request.setFrontChannelLogoutUri(params.getClientFrontchannelLogoutUri());
        }

        if (params.getClientRequestUris() != null && !params.getClientRequestUris().isEmpty()) {
            request.setRequestUris(params.getClientRequestUris());
        }

        if (params.getClientTokenEndpointAuthSigningAlg() != null) {
            SignatureAlgorithm signatureAlgorithms = SignatureAlgorithm.fromString(params.getClientTokenEndpointAuthSigningAlg());
            if (signatureAlgorithms == null) {
                LOG.error("Received invalid algorithm in `client_token_endpoint_auth_signing_alg` property. Value: " + params.getClientTokenEndpointAuthSigningAlg());
                throw new HttpException(ErrorResponseCode.INVALID_SIGNATURE_ALGORITHM);
            }
            request.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.fromString(params.getClientTokenEndpointAuthSigningAlg()));
        }

        if (!Strings.isNullOrEmpty(params.getClientName())) {
            request.setClientName(params.getClientName());
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

            if (signatureAlgorithms == SignatureAlgorithm.NONE && !getJansConfigurationService().find().getAcceptIdTokenWithoutSignature()) {
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

        if (StringUtils.isNotBlank(rp.getRpId())) {
            request.addCustomAttribute("rp_id", rp.getRpId());
        }

        return request;
    }
}