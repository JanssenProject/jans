package org.gluu.oxd.server.mapper;

import com.google.common.base.Strings;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
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

    private RegisterRequestMapper() {
    }

    public static void fillRp(Rp rp, RegisterRequest request) {

        if (!Strings.isNullOrEmpty(request.getClientName())) {
            rp.setClientName(request.getClientName());
        }

        rp.setApplicationType(safeToString(request.getApplicationType()));
        rp.setTokenEndpointAuthSigningAlg(safeToString(request.getTokenEndpointAuthSigningAlg()));

        if (CollectionUtils.isNotEmpty(request.getGrantTypes())) {
            rp.setGrantType(request.getGrantTypes().stream().map(item -> item.getValue()).collect(Collectors.toList()));
        }

        rp.setFrontChannelLogoutUri(request.getFrontChannelLogoutUri());
        rp.setTokenEndpointAuthMethod(safeToString(request.getTokenEndpointAuthMethod()));

        if (CollectionUtils.isNotEmpty(request.getRequestUris())) {
            rp.setRequestUris(request.getRequestUris());
        }

        if (!Strings.isNullOrEmpty(request.getSectorIdentifierUri())) {
            rp.setSectorIdentifierUri(request.getSectorIdentifierUri());
        }

        if (CollectionUtils.isNotEmpty(request.getRedirectUris())) {
            rp.setRedirectUris(request.getRedirectUris());
            rp.setRedirectUri(request.getRedirectUris().get(0));
        }

        rp.setAccessTokenAsJwt(request.getAccessTokenAsJwt());
        rp.setAccessTokenSigningAlg(safeToString(request.getAccessTokenSigningAlg()));
        rp.setRptAsJwt(request.getRptAsJwt());

        if (CollectionUtils.isNotEmpty(request.getResponseTypes_())) {
            rp.setResponseTypes(request.getResponseTypes_());
        }

        if (CollectionUtils.isNotEmpty(request.getDefaultAcrValues())) {
            rp.setAcrValues(request.getDefaultAcrValues());
        }

        if (CollectionUtils.isNotEmpty(request.getContacts())) {
            rp.setContacts(request.getContacts());
        }

        if (CollectionUtils.isNotEmpty(request.getPostLogoutRedirectUris())) {
            rp.setPostLogoutRedirectUris(request.getPostLogoutRedirectUris());
        }

        if (CollectionUtils.isNotEmpty(request.getScope())) {
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
        rp.setFrontChannelLogoutSessionRequired(request.getFrontChannelLogoutSessionRequired());

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

        rp.setSubjectType(safeToString(request.getSubjectType()));
        rp.setRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims(request.getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims());
        rp.setIdTokenSignedResponseAlg(safeToString(request.getIdTokenSignedResponseAlg()));
        rp.setIdTokenEncryptedResponseAlg(safeToString(request.getIdTokenEncryptedResponseAlg()));
        rp.setIdTokenEncryptedResponseEnc(safeToString(request.getIdTokenEncryptedResponseEnc()));
        rp.setUserInfoSignedResponseAlg(safeToString(request.getUserInfoSignedResponseAlg()));
        rp.setUserInfoEncryptedResponseAlg(safeToString(request.getUserInfoEncryptedResponseAlg()));
        rp.setUserInfoEncryptedResponseEnc(safeToString(request.getUserInfoEncryptedResponseEnc()));
        rp.setRequestObjectSigningAlg(safeToString(request.getRequestObjectSigningAlg()));
        rp.setRequestObjectEncryptionAlg(safeToString(request.getRequestObjectEncryptionAlg()));
        rp.setRequestObjectEncryptionEnc(safeToString(request.getRequestObjectEncryptionEnc()));
        rp.setDefaultMaxAge(safeToNumber(request.getDefaultMaxAge()));
        rp.setRequireAuthTime(request.getRequireAuthTime());

        if (!Strings.isNullOrEmpty(request.getInitiateLoginUri())) {
            rp.setInitiateLoginUri(request.getInitiateLoginUri());
        }

        if (CollectionUtils.isNotEmpty(request.getAuthorizedOrigins())) {
            rp.setAuthorizedOrigins(request.getAuthorizedOrigins());
        }

        rp.setAccessTokenLifetime(safeToNumber(request.getAccessTokenLifetime()));

        if (!Strings.isNullOrEmpty(request.getSoftwareId())) {
            rp.setSoftwareId(request.getSoftwareId());
        }

        if (!Strings.isNullOrEmpty(request.getSoftwareVersion())) {
            rp.setSoftwareVersion(request.getSoftwareVersion());
        }

        if (!Strings.isNullOrEmpty(request.getSoftwareStatement())) {
            rp.setSoftwareStatement(request.getSoftwareStatement());
        }

        if (MapUtils.isNotEmpty(request.getCustomAttributes())) {
            request.getCustomAttributes().entrySet().stream().forEach(e -> {
                rp.addCustomAttribute(e.getKey(), e.getValue());
            });
        }

        if (!Strings.isNullOrEmpty(request.getJwksUri())) {
            rp.setClientJwksUri(request.getJwksUri());
        }

        if (CollectionUtils.isNotEmpty(request.getClaimsRedirectUris())) {
            rp.setClaimsRedirectUri(request.getClaimsRedirectUris());
        }

        if (request.getAllowSpontaneousScopes() != null) {
            rp.setAllowSpontaneousScopes(request.getAllowSpontaneousScopes());
        }

        if (CollectionUtils.isNotEmpty(request.getSpontaneousScopes())) {
            rp.setSpontaneousScopes(request.getSpontaneousScopes());
        }
    }

    public static RegisterRequest createRegisterRequest(Rp rp) {
        final RegisterRequest request = new RegisterRequest(rp.getClientRegistrationAccessToken());

        if (!Strings.isNullOrEmpty(rp.getClientName())) {
            request.setClientName(rp.getClientName());
        }

        request.setApplicationType(ApplicationType.fromString(rp.getApplicationType()));
        request.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.fromString(rp.getTokenEndpointAuthSigningAlg()));

        if (CollectionUtils.isNotEmpty(rp.getGrantType())) {
            request.setGrantTypes(rp.getGrantType().stream().map(item -> GrantType.fromString(item)).collect(Collectors.toList()));
        }

        request.setFrontChannelLogoutUri(rp.getFrontChannelLogoutUri());
        request.setTokenEndpointAuthMethod(AuthenticationMethod.fromString(rp.getTokenEndpointAuthMethod()));

        if (CollectionUtils.isNotEmpty(rp.getRequestUris())) {
            request.setRequestUris(rp.getRequestUris());
        }

        if (!Strings.isNullOrEmpty(rp.getSectorIdentifierUri())) {
            request.setSectorIdentifierUri(rp.getSectorIdentifierUri());
        }

        if (CollectionUtils.isNotEmpty(rp.getRedirectUris())) {
            request.setRedirectUris(rp.getRedirectUris());
        }

        request.setAccessTokenAsJwt(rp.getAccessTokenAsJwt());
        request.setAccessTokenSigningAlg(SignatureAlgorithm.fromString(rp.getAccessTokenSigningAlg()));
        request.setRptAsJwt(rp.getRptAsJwt());

        if (CollectionUtils.isNotEmpty(rp.getResponseTypes())) {
            request.setResponseTypes_(rp.getResponseTypes());
        }

        if (CollectionUtils.isNotEmpty(rp.getAcrValues())) {
            request.setDefaultAcrValues(rp.getAcrValues());
        }

        if (CollectionUtils.isNotEmpty(rp.getContacts())) {
            request.setContacts(rp.getContacts());
        }

        if (CollectionUtils.isNotEmpty(rp.getPostLogoutRedirectUris())) {
            request.setPostLogoutRedirectUris(rp.getPostLogoutRedirectUris());
        }

        if (CollectionUtils.isNotEmpty(rp.getScope())) {
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

        request.setFrontChannelLogoutSessionRequired(rp.getFrontChannelLogoutSessionRequired());

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

        request.setRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims(rp.getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims());
        request.setIdTokenSignedResponseAlg(SignatureAlgorithm.fromString(rp.getIdTokenSignedResponseAlg()));
        request.setIdTokenEncryptedResponseAlg(KeyEncryptionAlgorithm.fromName(rp.getIdTokenEncryptedResponseAlg()));
        request.setIdTokenEncryptedResponseEnc(BlockEncryptionAlgorithm.fromName(rp.getIdTokenEncryptedResponseEnc()));
        request.setUserInfoSignedResponseAlg(SignatureAlgorithm.fromString(rp.getUserInfoSignedResponseAlg()));
        request.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.fromName(rp.getUserInfoEncryptedResponseAlg()));
        request.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.fromName(rp.getUserInfoEncryptedResponseEnc()));
        request.setRequestObjectSigningAlg(SignatureAlgorithm.fromString(rp.getRequestObjectSigningAlg()));
        request.setRequestObjectEncryptionAlg(KeyEncryptionAlgorithm.fromName(rp.getRequestObjectEncryptionAlg()));
        request.setRequestObjectEncryptionEnc(BlockEncryptionAlgorithm.fromName(rp.getRequestObjectEncryptionEnc()));

        request.setDefaultMaxAge(safeToNumber(rp.getDefaultMaxAge()));
        request.setRequireAuthTime(rp.getRequireAuthTime());

        if (!Strings.isNullOrEmpty(rp.getInitiateLoginUri())) {
            request.setInitiateLoginUri(rp.getInitiateLoginUri());
        }

        if (CollectionUtils.isNotEmpty(rp.getAuthorizedOrigins())) {
            request.setAuthorizedOrigins(rp.getAuthorizedOrigins());
        }

        request.setAccessTokenLifetime(safeToNumber(rp.getAccessTokenLifetime()));

        if (!Strings.isNullOrEmpty(rp.getSoftwareId())) {
            request.setSoftwareId(rp.getSoftwareId());
        }

        if (!Strings.isNullOrEmpty(rp.getSoftwareVersion())) {
            request.setSoftwareVersion(rp.getSoftwareVersion());
        }

        if (!Strings.isNullOrEmpty(rp.getSoftwareStatement())) {
            request.setSoftwareStatement(rp.getSoftwareStatement());
        }

        if (MapUtils.isNotEmpty(rp.getCustomAttributes())) {
            rp.getCustomAttributes().entrySet().stream().forEach(e -> {
                request.addCustomAttribute(e.getKey(), e.getValue());
            });
        }

        if (!Strings.isNullOrEmpty(rp.getClientJwksUri())) {
            request.setJwksUri(rp.getClientJwksUri());
        }

        if (CollectionUtils.isNotEmpty(rp.getClaimsRedirectUri())) {
            request.setClaimsRedirectUris(rp.getClaimsRedirectUri());
        }

        if (rp.getAllowSpontaneousScopes() != null) {
            request.setAllowSpontaneousScopes(rp.getAllowSpontaneousScopes());
        }

        if (CollectionUtils.isNotEmpty(rp.getSpontaneousScopes())) {
            request.setSpontaneousScopes(rp.getSpontaneousScopes());
        }

        return request;
    }

    public static String safeToString(Object obj) {
        if (obj == null)
            return null;
        return obj.toString();
    }

    public static Integer safeToNumber(Object obj) {
        if (obj == null || !NumberUtils.isNumber(obj.toString()))
            return null;
        return Integer.valueOf(obj.toString());
    }
}
