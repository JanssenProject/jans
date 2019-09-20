package org.gluu.oxd.server.op;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.gluu.oxauth.client.RegisterClient;
import org.gluu.oxauth.client.RegisterRequest;
import org.gluu.oxauth.client.RegisterResponse;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.common.SubjectType;
import org.gluu.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.common.params.UpdateSiteParams;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.common.response.UpdateSiteResponse;
import org.gluu.oxd.server.HttpException;
import org.gluu.oxd.server.Utils;
import org.gluu.oxd.server.service.Rp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.util.List;
import java.util.Set;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 11/03/2016
 */

public class UpdateSiteOperation extends BaseOperation<UpdateSiteParams> {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateSiteOperation.class);

    /**
     * Base constructor
     *
     * @param command command
     */
    protected UpdateSiteOperation(Command command, final Injector injector) {
        super(command, injector, UpdateSiteParams.class);
    }

    @Override
    public IOpResponse execute(UpdateSiteParams params) {
        final Rp rp = getRp();

        LOG.info("Updating rp ... rp: " + rp);
        persistRp(rp, params);

        UpdateSiteResponse response = new UpdateSiteResponse();
        response.setOxdId(rp.getOxdId());
        return response;
    }

    private void persistRp(Rp rp, UpdateSiteParams params) {

        try {
            updateRegisteredClient(rp, params);
            getRpService().update(rp);

            LOG.info("RP updated: " + rp);
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist RP, params: " + params, e);
        }
    }

    private void updateRegisteredClient(Rp rp, UpdateSiteParams params) {
        if (StringUtils.isBlank(rp.getClientRegistrationClientUri())) {
            LOG.error("Registration client url is blank.");
            throw new HttpException(ErrorResponseCode.INVALID_REGISTRATION_CLIENT_URL);
        }

        final RegisterClient registerClient = new RegisterClient(rp.getClientRegistrationClientUri());
        registerClient.setRequest(createRegisterClientRequest(rp, params));
        registerClient.setExecutor(getHttpService().getClientExecutor());
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

        final RegisterRequest request = new RegisterRequest(rp.getClientRegistrationAccessToken());
        request.setHttpMethod(HttpMethod.PUT); // force update

        List<ResponseType> responseTypes = Lists.newArrayList();
        if (params.getResponseTypes() != null && !params.getResponseTypes().isEmpty()) {
            for (String type : params.getResponseTypes()) {
                responseTypes.add(ResponseType.fromString(type));
            }
            request.setResponseTypes(responseTypes);
            rp.setResponseTypes(params.getResponseTypes());

        }

        if (params.getRptAsJwt() != null) {
            request.setRptAsJwt(params.getRptAsJwt());
            rp.setRptAsJwt(params.getRptAsJwt());
        } else {
            request.setRptAsJwt(rp.getRptAsJwt());
        }

        List<GrantType> grantTypes = Lists.newArrayList();
        for (String grantType : params.getGrantType() != null ? params.getGrantType() : rp.getGrantType()) {
            GrantType t = GrantType.fromString(grantType);
            if (t != null) {
                grantTypes.add(t);
            }
        }

        request.setGrantTypes(grantTypes);
        rp.setGrantType(params.getGrantType());

        Set<String> redirectUris = Sets.newLinkedHashSet();
        if (params.getRedirectUris() != null && !params.getRedirectUris().isEmpty()) {
            if (!params.getRedirectUris().stream().allMatch(uri -> Utils.isValidUrl(uri))) {
                throw new HttpException(ErrorResponseCode.INVALID_REDIRECT_URI);
            }

            redirectUris.addAll(params.getRedirectUris());
            List<String> redirectUriList = Lists.newArrayList(redirectUris);
            request.setRedirectUris(redirectUriList);
            rp.setRedirectUris(redirectUriList);
            rp.setRedirectUri(redirectUriList.get(0));
        }

        if (params.getAcrValues() != null && !params.getAcrValues().isEmpty()) {
            rp.setAcrValues(params.getAcrValues());
            request.setDefaultAcrValues(params.getAcrValues());
        } else {
            request.setDefaultAcrValues(rp.getAcrValues());
        }

        if (params.getClaimsRedirectUri() != null && !params.getClaimsRedirectUri().isEmpty()) {
            rp.setClaimsRedirectUri(params.getClaimsRedirectUri());
            request.setClaimsRedirectUris(params.getClaimsRedirectUri());
        } else {
            request.setClaimsRedirectUris(rp.getClaimsRedirectUri());
        }
        if (params.getAccessTokenAsJwt() != null) {
            request.setAccessTokenAsJwt(params.getAccessTokenAsJwt());
            rp.setAccessTokenAsJwt(params.getAccessTokenAsJwt());
        } else {
            request.setAccessTokenAsJwt(rp.getAccessTokenAsJwt());
        }

        if (params.getAccessTokenSigningAlg() != null) {
            rp.setAccessTokenSigningAlg(params.getAccessTokenSigningAlg());
            request.setAccessTokenSigningAlg(SignatureAlgorithm.fromString(params.getAccessTokenSigningAlg()));
        } else {
            request.setAccessTokenSigningAlg(SignatureAlgorithm.fromString(rp.getAccessTokenSigningAlg()));
        }

        if (!Strings.isNullOrEmpty(params.getClientJwksUri())) {
            request.setJwksUri(params.getClientJwksUri());
        }

        if (params.getPostLogoutRedirectUris() != null && !params.getPostLogoutRedirectUris().isEmpty()) {
            request.setPostLogoutRedirectUris(Lists.newArrayList(params.getPostLogoutRedirectUris()));
        }

        if (params.getContacts() != null) {
            request.setContacts(params.getContacts());
            rp.setContacts(params.getContacts());
        } else {
            request.setContacts(rp.getContacts());
        }

        if (params.getScope() != null) {
            request.setScopes(params.getScope());
            rp.setScope(params.getScope());
        } else {
            request.setScopes(rp.getScope());
        }

        if (!Strings.isNullOrEmpty(params.getClientSectorIdentifierUri())) {
            request.setSectorIdentifierUri(params.getClientSectorIdentifierUri());
            rp.setSectorIdentifierUri(params.getClientSectorIdentifierUri());
        }

        if (params.getClientFrontchannelLogoutUris() != null && !params.getClientFrontchannelLogoutUris().isEmpty()) {
            rp.setFrontChannelLogoutUri(Lists.newArrayList(params.getClientFrontchannelLogoutUris()));
            request.setFrontChannelLogoutUris(Lists.newArrayList(params.getClientFrontchannelLogoutUris()));
        } else {
            request.setFrontChannelLogoutUris(rp.getFrontChannelLogoutUri());
        }

        if (params.getClientRequestUris() != null && !params.getClientRequestUris().isEmpty()) {
            request.setRequestUris(params.getClientRequestUris());
        }

        if (params.getClientTokenEndpointAuthSigningAlg() != null) {
            SignatureAlgorithm signatureAlgorithms = SignatureAlgorithm.fromString(params.getClientTokenEndpointAuthSigningAlg());
            if (signatureAlgorithms == null) {
                LOG.error("Received invalid algorithm in `client_token_endpoint_auth_signing_alg` property. Value: " + params.getClientTokenEndpointAuthSigningAlg() );
                throw new HttpException(ErrorResponseCode.INVALID_SIGNATURE_ALGORITHM);
            }
            rp.setTokenEndpointAuthSigningAlg(params.getClientTokenEndpointAuthSigningAlg());
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
            rp.setFrontChannelLogoutSessionRequired(params.getFrontChannelLogoutSessionRequired());
        } else {
            request.setFrontChannelLogoutSessionRequired(rp.getFrontChannelLogoutSessionRequired());
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
            rp.setRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims(params.getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims());
        } else {
            request.setRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims(rp.getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims());
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

        if (params.getTrustedClient() != null && params.getTrustedClient()) {
            request.addCustomAttribute("oxAuthTrustedClient", "true");
            rp.setTrustedClient(params.getTrustedClient());
        }

        if (StringUtils.isNotBlank(rp.getOxdId())) {
            request.addCustomAttribute("oxd_id", rp.getOxdId());
        }

        return request;
    }
}