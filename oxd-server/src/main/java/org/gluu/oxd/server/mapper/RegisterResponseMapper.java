package org.gluu.oxd.server.mapper;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.client.RegisterRequest;
import org.gluu.oxauth.client.RegisterResponse;
import org.gluu.oxd.common.Jackson2;
import org.gluu.oxd.server.service.Rp;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

public class RegisterResponseMapper {

    public static boolean fillRp(Rp rp, RegisterResponse response) throws IOException {

        Rp rpFromOP = createRp(response);
        boolean isRpChanged = false;

        if (!StringUtils.equals(rpFromOP.getClientSecret(), rp.getClientSecret())) {
            rp.setClientSecret(rpFromOP.getClientSecret());
            isRpChanged = true;
        }

        if (!Objects.equal(rpFromOP.getClientSecretExpiresAt(), rp.getClientSecretExpiresAt())) {
            rp.setClientSecretExpiresAt(rpFromOP.getClientSecretExpiresAt());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getClientName(), rp.getClientName())) {
            rp.setClientName(rpFromOP.getClientName());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getTokenEndpointAuthSigningAlg(), rp.getTokenEndpointAuthSigningAlg())) {
            rp.setTokenEndpointAuthSigningAlg(rpFromOP.getTokenEndpointAuthSigningAlg());
            isRpChanged = true;
        }

        if (!isListsEqualIgnoringOrder(rpFromOP.getGrantType(), rp.getGrantType())) {
            rp.setGrantType(rpFromOP.getGrantType());
            isRpChanged = true;
        }

        if (!isListsEqualIgnoringOrder(rpFromOP.getFrontChannelLogoutUris(), rp.getFrontChannelLogoutUris())) {
            rp.setFrontChannelLogoutUris(rpFromOP.getFrontChannelLogoutUris());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getTokenEndpointAuthMethod(), rp.getTokenEndpointAuthMethod())) {
            rp.setTokenEndpointAuthMethod(rpFromOP.getTokenEndpointAuthMethod());
            isRpChanged = true;
        }

        if (!isListsEqualIgnoringOrder(rpFromOP.getRequestUris(), rp.getRequestUris())) {
            rp.setRequestUris(rpFromOP.getRequestUris());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getSectorIdentifierUri(), rp.getSectorIdentifierUri())) {
            rp.setSectorIdentifierUri(rpFromOP.getSectorIdentifierUri());
            isRpChanged = true;
        }

        if (!isListsEqualIgnoringOrder(rpFromOP.getRedirectUris(), rp.getRedirectUris())) {
            rp.setRedirectUris(rpFromOP.getRedirectUris());
            isRpChanged = true;
        }

        if (!Objects.equal(rpFromOP.getAccessTokenAsJwt(), rp.getAccessTokenAsJwt())) {
            rp.setAccessTokenAsJwt(rpFromOP.getAccessTokenAsJwt());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getAccessTokenSigningAlg(), rp.getAccessTokenSigningAlg())) {
            rp.setAccessTokenSigningAlg(rpFromOP.getAccessTokenSigningAlg());
            isRpChanged = true;
        }

        if (!Objects.equal(rpFromOP.getRptAsJwt(), rp.getRptAsJwt())) {
            rp.setRptAsJwt(rpFromOP.getRptAsJwt());
            isRpChanged = true;
        }

        if (!isListsEqualIgnoringOrder(rpFromOP.getResponseTypes(), rp.getResponseTypes())) {
            rp.setResponseTypes(rpFromOP.getResponseTypes());
            isRpChanged = true;
        }

        if (!isListsEqualIgnoringOrder(rpFromOP.getAcrValues(), rp.getAcrValues())) {
            rp.setAcrValues(rpFromOP.getAcrValues());
            isRpChanged = true;
        }

        if (!isListsEqualIgnoringOrder(rpFromOP.getContacts(), rp.getContacts())) {
            rp.setContacts(rpFromOP.getContacts());
            isRpChanged = true;
        }

        if (!isListsEqualIgnoringOrder(rpFromOP.getPostLogoutRedirectUris(), rp.getPostLogoutRedirectUris())) {
            rp.setPostLogoutRedirectUris(rpFromOP.getPostLogoutRedirectUris());
            isRpChanged = true;
        }

        if (!isListsEqualIgnoringOrder(rpFromOP.getScope(), rp.getScope())) {
            rp.setScope(rpFromOP.getScope());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getLogoUri(), rp.getLogoUri())) {
            rp.setLogoUri(rpFromOP.getLogoUri());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getClientUri(), rp.getClientUri())) {
            rp.setClientUri(rpFromOP.getClientUri());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getPolicyUri(), rp.getPolicyUri())) {
            rp.setPolicyUri(rpFromOP.getPolicyUri());
            isRpChanged = true;
        }

        if (!Objects.equal(rpFromOP.getFrontChannelLogoutSessionRequired(), rp.getFrontChannelLogoutSessionRequired())) {
            rp.setFrontChannelLogoutSessionRequired(rpFromOP.getFrontChannelLogoutSessionRequired());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getTosUri(), rp.getTosUri())) {
            rp.setTosUri(rpFromOP.getTosUri());
            isRpChanged = true;
        }

        if (!isJsonStringEqual(rpFromOP.getJwks(), rp.getJwks())) {
            rp.setJwks(rpFromOP.getJwks());
            isRpChanged = true;
        }
        if (!StringUtils.equals(rpFromOP.getIdTokenBindingCnf(), rp.getIdTokenBindingCnf())) {
            rp.setIdTokenBindingCnf(rpFromOP.getIdTokenBindingCnf());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getTlsClientAuthSubjectDn(), rp.getTlsClientAuthSubjectDn())) {
            rp.setTlsClientAuthSubjectDn(rpFromOP.getTlsClientAuthSubjectDn());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getSubjectType(), rp.getSubjectType())) {
            rp.setSubjectType(rpFromOP.getSubjectType());
            isRpChanged = true;
        }

        if (!Objects.equal(rpFromOP.getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims(), rp.getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims())) {
            rp.setRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims(rpFromOP.getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getIdTokenSignedResponseAlg(), rp.getIdTokenSignedResponseAlg())) {
            rp.setIdTokenSignedResponseAlg(rpFromOP.getIdTokenSignedResponseAlg());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getIdTokenEncryptedResponseAlg(), rp.getIdTokenEncryptedResponseAlg())) {
            rp.setIdTokenEncryptedResponseAlg(rpFromOP.getIdTokenEncryptedResponseAlg());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getIdTokenEncryptedResponseEnc(), rp.getIdTokenEncryptedResponseEnc())) {
            rp.setIdTokenEncryptedResponseEnc(rpFromOP.getIdTokenEncryptedResponseEnc());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getUserInfoSignedResponseAlg(), rp.getUserInfoSignedResponseAlg())) {
            rp.setUserInfoSignedResponseAlg(rpFromOP.getUserInfoSignedResponseAlg());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getUserInfoEncryptedResponseAlg(), rp.getUserInfoEncryptedResponseAlg())) {
            rp.setUserInfoEncryptedResponseAlg(rpFromOP.getUserInfoEncryptedResponseAlg());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getUserInfoEncryptedResponseEnc(), rp.getUserInfoEncryptedResponseEnc())) {
            rp.setUserInfoEncryptedResponseEnc(rpFromOP.getUserInfoEncryptedResponseEnc());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getRequestObjectSigningAlg(), rp.getRequestObjectSigningAlg())) {
            rp.setRequestObjectSigningAlg(rpFromOP.getRequestObjectSigningAlg());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getRequestObjectSigningAlg(), rp.getRequestObjectSigningAlg())) {
            rp.setRequestObjectSigningAlg(rpFromOP.getRequestObjectSigningAlg());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getRequestObjectEncryptionAlg(), rp.getRequestObjectEncryptionAlg())) {
            rp.setRequestObjectEncryptionAlg(rpFromOP.getRequestObjectEncryptionAlg());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getRequestObjectEncryptionEnc(), rp.getRequestObjectEncryptionEnc())) {
            rp.setRequestObjectEncryptionEnc(rpFromOP.getRequestObjectEncryptionEnc());
            isRpChanged = true;
        }

        if (!Objects.equal(rpFromOP.getDefaultMaxAge(), rp.getDefaultMaxAge())) {
            rp.setDefaultMaxAge(rpFromOP.getDefaultMaxAge());
            isRpChanged = true;
        }

        if (!Objects.equal(rpFromOP.getRequireAuthTime(), rp.getRequireAuthTime())) {
            rp.setRequireAuthTime(rpFromOP.getRequireAuthTime());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getInitiateLoginUri(), rp.getInitiateLoginUri())) {
            rp.setInitiateLoginUri(rpFromOP.getInitiateLoginUri());
            isRpChanged = true;
        }

        if (!isListsEqualIgnoringOrder(rpFromOP.getAuthorizedOrigins(), rp.getAuthorizedOrigins())) {
            rp.setAuthorizedOrigins(rpFromOP.getAuthorizedOrigins());
            isRpChanged = true;
        }

        if (!Objects.equal(rpFromOP.getAccessTokenLifetime(), rp.getAccessTokenLifetime())) {
            rp.setAccessTokenLifetime(rpFromOP.getAccessTokenLifetime());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getSoftwareId(), rp.getSoftwareId())) {
            rp.setSoftwareId(rpFromOP.getSoftwareId());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getSoftwareVersion(), rp.getSoftwareVersion())) {
            rp.setSoftwareVersion(rpFromOP.getSoftwareVersion());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getSoftwareStatement(), rp.getSoftwareStatement())) {
            rp.setSoftwareStatement(rpFromOP.getSoftwareStatement());
            isRpChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getClientJwksUri(), rp.getClientJwksUri())) {
            rp.setClientJwksUri(rpFromOP.getClientJwksUri());
            isRpChanged = true;
        }

        if (!isListsEqualIgnoringOrder(rpFromOP.getClaimsRedirectUri(), rp.getClaimsRedirectUri())) {
            rp.setClaimsRedirectUri(rpFromOP.getClaimsRedirectUri());
            isRpChanged = true;
        }

        return isRpChanged;
    }

    public static Rp createRp(RegisterResponse response) {
        Rp rpFromRegisterResponse = new Rp();
        RegisterRequest request = RegisterRequest.fromJson(response.getEntity(), false);

        RegisterRequestMapper.fillRp(rpFromRegisterResponse, request);
        rpFromRegisterResponse.setClientId(response.getClientId());
        rpFromRegisterResponse.setClientSecret(response.getClientSecret());
        rpFromRegisterResponse.setClientSecretExpiresAt(response.getClientSecretExpiresAt());

        return rpFromRegisterResponse;
    }

    public static boolean isListsEqualIgnoringOrder(List<String> oxdRp, List<String> oxAuthRp) {
        if (oxdRp == null || oxdRp.isEmpty()) {
            return oxAuthRp == null || oxAuthRp.isEmpty();
        } else if (oxAuthRp != null) {
            return new HashSet<>(oxdRp).equals(new HashSet<>(oxAuthRp));
        }
        return false;
    }

    public static boolean isJsonStringEqual(String oxdParam, String oxAuthparam) throws IOException {
        return Strings.isNullOrEmpty(oxdParam) ? Strings.isNullOrEmpty(oxAuthparam) : Jackson2.createJsonMapperWithoutEmptyAttributes().readTree(oxdParam).equals(Jackson2.createJsonMapperWithoutEmptyAttributes().readTree(oxAuthparam));
    }
}
