package org.gluu.oxd.server.mapper;

import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.gluu.oxauth.client.RegisterRequest;
import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.common.SubjectType;
import org.gluu.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.register.ApplicationType;
import org.gluu.oxd.server.service.Rp;

import java.util.stream.Collectors;

public class RegisterRequestMapper {

    public void fillRp(Rp rp, RegisterRequest request) {

        if (!Strings.isNullOrEmpty(request.getClientName())) {
            rp.setClientName(request.getClientName());
        }

        if (request.getApplicationType() != null) {
            rp.setApplicationType(request.getApplicationType().toString());
        }

        if (request.getTokenEndpointAuthSigningAlg() != null && StringUtils.isNotBlank(request.getTokenEndpointAuthSigningAlg().toString())) {
            rp.setTokenEndpointAuthSigningAlg(request.getTokenEndpointAuthSigningAlg().toString());
        }

        if (request.getGrantTypes() != null) {
            rp.setGrantType(request.getGrantTypes().stream().map(item -> item.getValue()).collect(Collectors.toList()));
        }

        if (request.getFrontChannelLogoutUris() != null) {
            rp.setFrontChannelLogoutUris(request.getFrontChannelLogoutUris());
        }

        if (request.getTokenEndpointAuthMethod() != null && StringUtils.isNotBlank(request.getTokenEndpointAuthMethod().toString())) {
            rp.setTokenEndpointAuthMethod(request.getTokenEndpointAuthMethod().toString());
        }

        if (request.getRequestUris() != null && !request.getRequestUris().isEmpty()) {
            rp.setClientRequestUris(request.getRequestUris());
        }

        if (!Strings.isNullOrEmpty(request.getSectorIdentifierUri())) {
            rp.setClientSectorIdentifierUri(request.getSectorIdentifierUri());
        }

        if (request.getRedirectUris() != null && !request.getRedirectUris().isEmpty()) {
            rp.setRedirectUris(request.getRedirectUris());
            rp.setRedirectUri(request.getRedirectUris().get(0));
        }

        if (request.getAccessTokenAsJwt() != null) {
            rp.setAccessTokenAsJwt(request.getAccessTokenAsJwt());
        }

        if (request.getAccessTokenSigningAlg() != null && StringUtils.isNotBlank(request.getAccessTokenSigningAlg().toString())) {
            rp.setAccessTokenSigningAlg(request.getAccessTokenSigningAlg().toString());
        }

        if (request.getRptAsJwt() != null) {
            rp.setRptAsJwt(request.getRptAsJwt());
        }

        if (request.getResponseTypes() != null) {
            rp.setResponseTypes(request.getResponseTypes().stream().map(item -> item.getValue()).collect(Collectors.toList()));
        }

        if (request.getDefaultAcrValues() != null && !request.getDefaultAcrValues().isEmpty()) {
            rp.setAcrValues(request.getDefaultAcrValues());
        }

        if (request.getContacts() != null && !request.getContacts().isEmpty()) {
            rp.setContacts(request.getContacts());
        }

        if (request.getPostLogoutRedirectUris() != null && !request.getPostLogoutRedirectUris().isEmpty()) {
            rp.setPostLogoutRedirectUris(request.getPostLogoutRedirectUris());
        }

        if (request.getScope() != null && !request.getScope().isEmpty()) {
            rp.setScope(request.getScope());
        }

        if (!Strings.isNullOrEmpty(request.getLogoUri())) {
            rp.setLogoUri(request.getLogoUri());
        }

        if (!Strings.isNullOrEmpty(request.getClientUri())) {
            rp.setClientUri(request.getClientUri());
        }

        if (!Strings.isNullOrEmpty(request.getPolicyUri())) {
            rp.setPolicyUri(request.getPolicyUri());
        }

        if (request.getFrontChannelLogoutSessionRequired() != null) {
            rp.setFrontChannelLogoutSessionRequired(request.getFrontChannelLogoutSessionRequired());
        }

        if (!Strings.isNullOrEmpty(request.getTosUri())) {
            rp.setTosUri(request.getTosUri());
        }

        if (!Strings.isNullOrEmpty(request.getJwks())) {
            rp.setJwks(request.getJwks());
        }

        if (!Strings.isNullOrEmpty(request.getIdTokenTokenBindingCnf())) {
            rp.setIdTokenBindingCnf(request.getIdTokenTokenBindingCnf());
        }

        if (!Strings.isNullOrEmpty(request.getTlsClientAuthSubjectDn())) {
            rp.setTlsClientAuthSubjectDn(request.getTlsClientAuthSubjectDn());
        }

        if (request.getSubjectType() != null && StringUtils.isNotBlank(request.getSubjectType().toString())) {
            rp.setSubjectType(request.getSubjectType().toString());
        }

        if (request.getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims() != null) {
            rp.setRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims(request.getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims());
        }

        if (request.getIdTokenSignedResponseAlg() != null && StringUtils.isNotBlank(request.getIdTokenSignedResponseAlg().toString())) {
            rp.setIdTokenSignedResponseAlg(request.getIdTokenSignedResponseAlg().toString());
        }

        if (request.getIdTokenEncryptedResponseAlg() != null && StringUtils.isNotBlank(request.getIdTokenEncryptedResponseAlg().toString())) {
            rp.setIdTokenEncryptedResponseAlg(request.getIdTokenEncryptedResponseAlg().toString());
        }

        if (request.getIdTokenEncryptedResponseEnc() != null && StringUtils.isNotBlank(request.getIdTokenEncryptedResponseEnc().toString())) {
            rp.setIdTokenEncryptedResponseEnc(request.getIdTokenEncryptedResponseEnc().toString());
        }

        if (request.getUserInfoSignedResponseAlg() != null && StringUtils.isNotBlank(request.getUserInfoSignedResponseAlg().toString())) {
            rp.setUserInfoSignedResponseAlg(request.getUserInfoSignedResponseAlg().toString());
        }

        if (request.getUserInfoEncryptedResponseAlg() != null && StringUtils.isNotBlank(request.getUserInfoEncryptedResponseAlg().toString())) {
            rp.setUserInfoEncryptedResponseAlg(request.getUserInfoEncryptedResponseAlg().toString());
        }

        if (request.getUserInfoEncryptedResponseEnc() != null && StringUtils.isNotBlank(request.getUserInfoEncryptedResponseEnc().toString())) {
            rp.setUserInfoEncryptedResponseEnc(request.getUserInfoEncryptedResponseEnc().toString());
        }

        if (request.getRequestObjectSigningAlg() != null && StringUtils.isNotBlank(request.getRequestObjectSigningAlg().toString())) {
            rp.setRequestObjectSigningAlg(request.getRequestObjectSigningAlg().toString());
        }

        if (request.getRequestObjectEncryptionAlg() != null && StringUtils.isNotBlank(request.getRequestObjectEncryptionAlg().toString())) {
            rp.setRequestObjectEncryptionAlg(request.getRequestObjectEncryptionAlg().toString());
        }

        if (request.getRequestObjectEncryptionEnc() != null && StringUtils.isNotBlank(request.getRequestObjectEncryptionEnc().toString())) {
            rp.setRequestObjectEncryptionEnc(request.getRequestObjectEncryptionEnc().toString());
        }

        if (request.getDefaultMaxAge() != null && NumberUtils.isNumber(request.getDefaultMaxAge().toString())) {
            rp.setDefaultMaxAge(request.getDefaultMaxAge());
        }

        if (request.getRequireAuthTime() != null) {
            rp.setRequireAuthTime(request.getRequireAuthTime());
        }

        if (!Strings.isNullOrEmpty(request.getInitiateLoginUri())) {
            rp.setInitiateLoginUri(request.getInitiateLoginUri());
        }

        if (request.getAuthorizedOrigins() != null && !request.getAuthorizedOrigins().isEmpty()) {
            rp.setAuthorizedOrigins(request.getAuthorizedOrigins());
        }

        if (request.getAccessTokenLifetime() != null && NumberUtils.isNumber(request.getAccessTokenLifetime().toString())) {
            rp.setAccessTokenLifetime(request.getAccessTokenLifetime());
        }

        if (!Strings.isNullOrEmpty(request.getSoftwareId())) {
            rp.setSoftwareId(request.getSoftwareId());
        }

        if (!Strings.isNullOrEmpty(request.getSoftwareVersion())) {
            rp.setSoftwareVersion(request.getSoftwareVersion());
        }

        if (!Strings.isNullOrEmpty(request.getSoftwareStatement())) {
            rp.setSoftwareStatement(request.getSoftwareStatement());
        }

        if (request.getCustomAttributes() != null && !request.getCustomAttributes().isEmpty()) {
            request.getCustomAttributes().entrySet().stream().forEach(e -> {
                rp.addCustomAttribute(e.getKey(), e.getValue());
            });
        }

        if (!Strings.isNullOrEmpty(request.getJwksUri())) {
            rp.setClientJwksUri(request.getJwksUri());
        }

        if (request.getClaimsRedirectUris() != null && !request.getClaimsRedirectUris().isEmpty()) {
            rp.setClaimsRedirectUri(request.getClaimsRedirectUris());
        }

    }

    public RegisterRequest createRegisterRequest(Rp rp) {
        final RegisterRequest request = new RegisterRequest(rp.getClientRegistrationAccessToken());

        if (!Strings.isNullOrEmpty(rp.getClientName())) {
            request.setClientName(rp.getClientName());
        }

        if (!Strings.isNullOrEmpty(rp.getApplicationType())) {
            request.setApplicationType(ApplicationType.fromString(rp.getApplicationType()));
        }

        if (!Strings.isNullOrEmpty(rp.getTokenEndpointAuthSigningAlg())) {
            request.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.fromString(rp.getTokenEndpointAuthSigningAlg()));
        }

        if (rp.getGrantType() != null) {
            request.setGrantTypes(rp.getGrantType().stream().map(item -> GrantType.fromString(item)).collect(Collectors.toList()));
        }

        if (rp.getFrontChannelLogoutUris() != null) {
            request.setFrontChannelLogoutUris(rp.getFrontChannelLogoutUris());
        }

        if (!Strings.isNullOrEmpty(rp.getTokenEndpointAuthMethod())) {
            request.setTokenEndpointAuthMethod(AuthenticationMethod.fromString(rp.getTokenEndpointAuthMethod()));
        }

        if (rp.getClientRequestUris() != null && !rp.getClientRequestUris().isEmpty()) {
            request.setRequestUris(rp.getClientRequestUris());
        }

        if (!Strings.isNullOrEmpty(rp.getClientSectorIdentifierUri())) {
            request.setSectorIdentifierUri(rp.getClientSectorIdentifierUri());
        }

        if (rp.getRedirectUris() != null && !rp.getRedirectUris().isEmpty()) {
            request.setRedirectUris(rp.getRedirectUris());
        }

        if (rp.getAccessTokenAsJwt() != null) {
            request.setAccessTokenAsJwt(rp.getAccessTokenAsJwt());
        }

        if (!Strings.isNullOrEmpty(rp.getAccessTokenSigningAlg())) {
            request.setAccessTokenSigningAlg(SignatureAlgorithm.fromString(rp.getAccessTokenSigningAlg()));
        }

        if (rp.getRptAsJwt() != null) {
            request.setRptAsJwt(rp.getRptAsJwt());
        }

        if (rp.getResponseTypes() != null) {
            request.setResponseTypes(rp.getResponseTypes().stream().map(item -> ResponseType.fromString(item)).collect(Collectors.toList()));
        }

        if (rp.getAcrValues() != null && !rp.getAcrValues().isEmpty()) {
            request.setDefaultAcrValues(rp.getAcrValues());
        }

        if (rp.getContacts() != null && !rp.getContacts().isEmpty()) {
            request.setContacts(rp.getContacts());
        }

        if (rp.getPostLogoutRedirectUris() != null && !rp.getPostLogoutRedirectUris().isEmpty()) {
            request.setPostLogoutRedirectUris(rp.getPostLogoutRedirectUris());
        }

        if (rp.getScope() != null && !rp.getScope().isEmpty()) {
            request.setScope(rp.getScope());
        }

        if (!Strings.isNullOrEmpty(rp.getLogoUri())) {
            request.setLogoUri(rp.getLogoUri());
        }

        if (!Strings.isNullOrEmpty(rp.getClientUri())) {
            request.setClientUri(rp.getClientUri());
        }

        if (!Strings.isNullOrEmpty(rp.getPolicyUri())) {
            request.setPolicyUri(rp.getPolicyUri());
        }

        if (rp.getFrontChannelLogoutSessionRequired() != null) {
            request.setFrontChannelLogoutSessionRequired(rp.getFrontChannelLogoutSessionRequired());
        }

        if (!Strings.isNullOrEmpty(rp.getTosUri())) {
            request.setTosUri(rp.getTosUri());
        }

        if (!Strings.isNullOrEmpty(rp.getJwks())) {
            request.setJwks(rp.getJwks());
        }

        if (!Strings.isNullOrEmpty(rp.getIdTokenBindingCnf())) {
            request.setIdTokenTokenBindingCnf(rp.getIdTokenBindingCnf());
        }

        if (!Strings.isNullOrEmpty(rp.getTlsClientAuthSubjectDn())) {
            request.setTlsClientAuthSubjectDn(rp.getTlsClientAuthSubjectDn());
        }

        if (!Strings.isNullOrEmpty(rp.getSubjectType())) {
            request.setSubjectType(SubjectType.fromString(rp.getSubjectType()));
        }

        if (rp.getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims() != null) {
            request.setRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims(rp.getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims());
        }

        if (!Strings.isNullOrEmpty(rp.getIdTokenSignedResponseAlg())) {
            request.setIdTokenSignedResponseAlg(SignatureAlgorithm.fromString(rp.getIdTokenSignedResponseAlg()));
        }
        if (!Strings.isNullOrEmpty(rp.getIdTokenEncryptedResponseAlg())) {
            request.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.fromName(rp.getIdTokenEncryptedResponseAlg()));
        }

        if (!Strings.isNullOrEmpty(rp.getIdTokenEncryptedResponseEnc())) {
            request.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.fromName(rp.getIdTokenEncryptedResponseEnc()));
        }

        if (!Strings.isNullOrEmpty(rp.getUserInfoSignedResponseAlg())) {
            request.setUserInfoSignedResponseAlg(SignatureAlgorithm.fromString(rp.getUserInfoSignedResponseAlg()));
        }

        if (!Strings.isNullOrEmpty(rp.getUserInfoEncryptedResponseAlg())) {
            request.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.fromName(rp.getUserInfoEncryptedResponseAlg().toString()));
        }

        if (!Strings.isNullOrEmpty(rp.getUserInfoEncryptedResponseEnc())) {
            request.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.fromName(rp.getUserInfoEncryptedResponseEnc()));
        }

        if (!Strings.isNullOrEmpty(rp.getRequestObjectSigningAlg())) {
            request.setRequestObjectSigningAlg(SignatureAlgorithm.fromString(rp.getRequestObjectSigningAlg()));
        }

        if (!Strings.isNullOrEmpty(rp.getRequestObjectEncryptionAlg())) {
            request.setRequestObjectEncryptionAlg(KeyEncryptionAlgorithm.fromName(rp.getRequestObjectEncryptionAlg()));
        }

        if (!Strings.isNullOrEmpty(rp.getRequestObjectEncryptionEnc())) {
            request.setRequestObjectEncryptionEnc(BlockEncryptionAlgorithm.fromName(rp.getRequestObjectEncryptionEnc()));
        }

        if (rp.getDefaultMaxAge() != null && NumberUtils.isNumber(rp.getDefaultMaxAge().toString())) {
            request.setDefaultMaxAge(rp.getDefaultMaxAge());
        }

        if (rp.getRequireAuthTime() != null) {
            request.setRequireAuthTime(rp.getRequireAuthTime());
        }

        if (!Strings.isNullOrEmpty(rp.getInitiateLoginUri())) {
            request.setInitiateLoginUri(rp.getInitiateLoginUri());
        }

        if (rp.getAuthorizedOrigins() != null && !rp.getAuthorizedOrigins().isEmpty()) {
            request.setAuthorizedOrigins(rp.getAuthorizedOrigins());
        }

        if (rp.getAccessTokenLifetime() != null && NumberUtils.isNumber(rp.getAccessTokenLifetime().toString())) {
            request.setAccessTokenLifetime(rp.getAccessTokenLifetime());
        }

        if (!Strings.isNullOrEmpty(rp.getSoftwareId())) {
            request.setSoftwareId(rp.getSoftwareId());
        }

        if (!Strings.isNullOrEmpty(rp.getSoftwareVersion())) {
            request.setSoftwareVersion(rp.getSoftwareVersion());
        }

        if (!Strings.isNullOrEmpty(rp.getSoftwareStatement())) {
            request.setSoftwareStatement(rp.getSoftwareStatement());
        }

        if (rp.getCustomAttributes() != null && !rp.getCustomAttributes().isEmpty()) {
            rp.getCustomAttributes().entrySet().stream().forEach(e -> {
                request.addCustomAttribute(e.getKey(), e.getValue());
            });
        }

        if (!Strings.isNullOrEmpty(rp.getClientJwksUri())) {
            request.setJwksUri(rp.getClientJwksUri());
        }

        if (rp.getClaimsRedirectUri() != null && !rp.getClaimsRedirectUri().isEmpty()) {
            request.setClaimsRedirectUris(rp.getClaimsRedirectUri());
        }

        return request;
    }
}
