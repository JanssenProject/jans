package org.gluu.oxd.server.mapper;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.client.RegisterResponse;
import org.gluu.oxauth.model.common.SubjectType;
import org.gluu.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxd.common.Jackson2;
import org.gluu.oxd.server.service.Rp;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.gluu.oxauth.model.register.RegisterRequestParam.*;

public class RegisterResponseMapper {

    public static boolean hasClientMetadataChangedAtOp(Rp rp, RegisterResponse response) throws IOException {

        Rp rpFromOP = setRpObjFrmRegisterResponseObj(response);
        boolean isOpClientMetadataChanged = false;

        if (!StringUtils.equals(rpFromOP.getClientSecret(), rp.getClientSecret())) {
            rp.setClientSecret(rpFromOP.getClientSecret());
            isOpClientMetadataChanged = true;
        }

        if (!compareParams(rpFromOP.getClientSecretExpiresAt(), rp.getClientSecretExpiresAt())) {
            rp.setClientSecretExpiresAt(rpFromOP.getClientSecretExpiresAt());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getClientName(), rp.getClientName())) {
            rp.setClientName(rpFromOP.getClientName());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getTokenEndpointAuthSigningAlg(), rp.getTokenEndpointAuthSigningAlg())) {
            rp.setTokenEndpointAuthSigningAlg(rpFromOP.getTokenEndpointAuthSigningAlg());
            isOpClientMetadataChanged = true;
        }

        if (!compareParams(rpFromOP.getGrantType(), rp.getGrantType())) {
            rp.setGrantType(rpFromOP.getGrantType());
            isOpClientMetadataChanged = true;
        }

        if (!compareParams(rpFromOP.getFrontChannelLogoutUris(), rp.getFrontChannelLogoutUris())) {
            rp.setFrontChannelLogoutUris(rpFromOP.getFrontChannelLogoutUris());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getTokenEndpointAuthMethod(), rp.getTokenEndpointAuthMethod())) {
            rp.setTokenEndpointAuthMethod(rpFromOP.getTokenEndpointAuthMethod());
            isOpClientMetadataChanged = true;
        }

        if (!compareParams(rpFromOP.getRequestUris(), rp.getRequestUris())) {
            rp.setRequestUris(rpFromOP.getRequestUris());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getSectorIdentifierUri(), rp.getSectorIdentifierUri())) {
            rp.setSectorIdentifierUri(rpFromOP.getSectorIdentifierUri());
            isOpClientMetadataChanged = true;
        }

        if (!compareParams(rpFromOP.getRedirectUris(), rp.getRedirectUris())) {
            rp.setRedirectUris(rpFromOP.getRedirectUris());
            isOpClientMetadataChanged = true;
        }

        if (!compareParams(rpFromOP.getAccessTokenAsJwt(), rp.getAccessTokenAsJwt())) {
            rp.setAccessTokenAsJwt(rpFromOP.getAccessTokenAsJwt());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getAccessTokenSigningAlg(), rp.getAccessTokenSigningAlg())) {
            rp.setAccessTokenSigningAlg(rpFromOP.getAccessTokenSigningAlg());
            isOpClientMetadataChanged = true;
        }

        if (!compareParams(rpFromOP.getRptAsJwt(), rp.getRptAsJwt())) {
            rp.setRptAsJwt(rpFromOP.getRptAsJwt());
            isOpClientMetadataChanged = true;
        }

        if (!compareParams(rpFromOP.getResponseTypes(), rp.getResponseTypes())) {
            rp.setResponseTypes(rpFromOP.getResponseTypes());
            isOpClientMetadataChanged = true;
        }

        if (!compareParams(rpFromOP.getAcrValues(), rp.getAcrValues())) {
            rp.setAcrValues(rpFromOP.getAcrValues());
            isOpClientMetadataChanged = true;
        }

        if (!compareParams(rpFromOP.getContacts(), rp.getContacts())) {
            rp.setContacts(rpFromOP.getContacts());
            isOpClientMetadataChanged = true;
        }

        if (!compareParams(rpFromOP.getPostLogoutRedirectUris(), rp.getPostLogoutRedirectUris())) {
            rp.setPostLogoutRedirectUris(rpFromOP.getPostLogoutRedirectUris());
            isOpClientMetadataChanged = true;
        }

        if (!compareParams(rpFromOP.getScope(), rp.getScope())) {
            rp.setScope(rpFromOP.getScope());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getLogoUri(), rp.getLogoUri())) {
            rp.setLogoUri(rpFromOP.getLogoUri());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getClientUri(), rp.getClientUri())) {
            rp.setClientUri(rpFromOP.getClientUri());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getPolicyUri(), rp.getPolicyUri())) {
            rp.setPolicyUri(rpFromOP.getPolicyUri());
            isOpClientMetadataChanged = true;
        }

        if (!compareParams(rpFromOP.getFrontChannelLogoutSessionRequired(), rp.getFrontChannelLogoutSessionRequired())) {
            rp.setFrontChannelLogoutSessionRequired(rpFromOP.getFrontChannelLogoutSessionRequired());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getTosUri(), rp.getTosUri())) {
            rp.setTosUri(rpFromOP.getTosUri());
            isOpClientMetadataChanged = true;
        }

        if (!compareJsonNodeParams(rpFromOP.getJwks(), rp.getJwks())) {
            rp.setJwks(rpFromOP.getJwks());
            isOpClientMetadataChanged = true;
        }
        if (!StringUtils.equals(rpFromOP.getIdTokenBindingCnf(), rp.getIdTokenBindingCnf())) {
            rp.setIdTokenBindingCnf(rpFromOP.getIdTokenBindingCnf());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getTlsClientAuthSubjectDn(), rp.getTlsClientAuthSubjectDn())) {
            rp.setTlsClientAuthSubjectDn(rpFromOP.getTlsClientAuthSubjectDn());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getSubjectType(), rp.getSubjectType())) {
            rp.setSubjectType(rpFromOP.getSubjectType());
            isOpClientMetadataChanged = true;
        }

        if (!compareParams(rpFromOP.getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims(), rp.getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims())) {
            rp.setRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims(rpFromOP.getRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getIdTokenSignedResponseAlg(), rp.getIdTokenSignedResponseAlg())) {
            rp.setIdTokenSignedResponseAlg(rpFromOP.getIdTokenSignedResponseAlg());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getIdTokenEncryptedResponseAlg(), rp.getIdTokenEncryptedResponseAlg())) {
            rp.setIdTokenEncryptedResponseAlg(rpFromOP.getIdTokenEncryptedResponseAlg());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getIdTokenEncryptedResponseEnc(), rp.getIdTokenEncryptedResponseEnc())) {
            rp.setIdTokenEncryptedResponseEnc(rpFromOP.getIdTokenEncryptedResponseEnc());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getUserInfoSignedResponseAlg(), rp.getUserInfoSignedResponseAlg())) {
            rp.setUserInfoSignedResponseAlg(rpFromOP.getUserInfoSignedResponseAlg());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getUserInfoEncryptedResponseAlg(), rp.getUserInfoEncryptedResponseAlg())) {
            rp.setUserInfoEncryptedResponseAlg(rpFromOP.getUserInfoEncryptedResponseAlg());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getUserInfoEncryptedResponseEnc(), rp.getUserInfoEncryptedResponseEnc())) {
            rp.setUserInfoEncryptedResponseEnc(rpFromOP.getUserInfoEncryptedResponseEnc());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getRequestObjectSigningAlg(), rp.getRequestObjectSigningAlg())) {
            rp.setRequestObjectSigningAlg(rpFromOP.getRequestObjectSigningAlg());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getRequestObjectSigningAlg(), rp.getRequestObjectSigningAlg())) {
            rp.setRequestObjectSigningAlg(rpFromOP.getRequestObjectSigningAlg());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getRequestObjectEncryptionAlg(), rp.getRequestObjectEncryptionAlg())) {
            rp.setRequestObjectEncryptionAlg(rpFromOP.getRequestObjectEncryptionAlg());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getRequestObjectEncryptionEnc(), rp.getRequestObjectEncryptionEnc())) {
            rp.setRequestObjectEncryptionEnc(rpFromOP.getRequestObjectEncryptionEnc());
            isOpClientMetadataChanged = true;
        }

        if (!compareParams(rpFromOP.getDefaultMaxAge(), rp.getDefaultMaxAge())) {
            rp.setDefaultMaxAge(rpFromOP.getDefaultMaxAge());
            isOpClientMetadataChanged = true;
        }

        if (!compareParams(rpFromOP.getRequireAuthTime(), rp.getRequireAuthTime())) {
            rp.setRequireAuthTime(rpFromOP.getRequireAuthTime());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getInitiateLoginUri(), rp.getInitiateLoginUri())) {
            rp.setInitiateLoginUri(rpFromOP.getInitiateLoginUri());
            isOpClientMetadataChanged = true;
        }

        if (!compareParams(rpFromOP.getAuthorizedOrigins(), rp.getAuthorizedOrigins())) {
            rp.setAuthorizedOrigins(rpFromOP.getAuthorizedOrigins());
            isOpClientMetadataChanged = true;
        }

        if (!compareParams(rpFromOP.getAccessTokenLifetime(), rp.getAccessTokenLifetime())) {
            rp.setAccessTokenLifetime(rpFromOP.getAccessTokenLifetime());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getSoftwareId(), rp.getSoftwareId())) {
            rp.setSoftwareId(rpFromOP.getSoftwareId());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getSoftwareVersion(), rp.getSoftwareVersion())) {
            rp.setSoftwareVersion(rpFromOP.getSoftwareVersion());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getSoftwareStatement(), rp.getSoftwareStatement())) {
            rp.setSoftwareStatement(rpFromOP.getSoftwareStatement());
            isOpClientMetadataChanged = true;
        }

        if (!StringUtils.equals(rpFromOP.getClientJwksUri(), rp.getClientJwksUri())) {
            rp.setClientJwksUri(rpFromOP.getClientJwksUri());
            isOpClientMetadataChanged = true;
        }

        if (!compareParams(rpFromOP.getClaimsRedirectUri(), rp.getClaimsRedirectUri())) {
            rp.setClaimsRedirectUri(rpFromOP.getClaimsRedirectUri());
            isOpClientMetadataChanged = true;
        }

        if (isOpClientMetadataChanged) {
            rp.setLastSynced(new Date());
        }

        return isOpClientMetadataChanged;
    }

    private static Rp setRpObjFrmRegisterResponseObj(RegisterResponse response) {
        Rp rpFromRegisterResponse = new Rp();
        Map<String, String> respClaims = response.getClaims();

        rpFromRegisterResponse.setClientName(respClaims.get(CLIENT_NAME.toString()));

        if (!Strings.isNullOrEmpty(respClaims.get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString())) &&
                SignatureAlgorithm.fromString(respClaims.get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString())) != null) {
            rpFromRegisterResponse.setTokenEndpointAuthSigningAlg(respClaims.get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        }

        rpFromRegisterResponse.setClientSecret(response.getClientSecret());
        rpFromRegisterResponse.setClientSecretExpiresAt(response.getClientSecretExpiresAt());
        rpFromRegisterResponse.setGrantType(convertStringToList(respClaims.get(GRANT_TYPES.toString())));
        rpFromRegisterResponse.setFrontChannelLogoutUris(convertStringToList(respClaims.get(FRONT_CHANNEL_LOGOUT_URI.toString())));
        rpFromRegisterResponse.setTokenEndpointAuthMethod(respClaims.get(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        rpFromRegisterResponse.setRequestUris(convertStringToList(respClaims.get(REQUEST_URIS.toString())));
        rpFromRegisterResponse.setSectorIdentifierUri(respClaims.get(SECTOR_IDENTIFIER_URI.toString()));
        rpFromRegisterResponse.setRedirectUris(convertStringToList(respClaims.get(REDIRECT_URIS.toString())));

        if (!Strings.isNullOrEmpty(respClaims.get(ACCESS_TOKEN_AS_JWT.toString()))) {
            rpFromRegisterResponse.setAccessTokenAsJwt(Boolean.valueOf(respClaims.get(ACCESS_TOKEN_AS_JWT.toString())));
        }

        if (!Strings.isNullOrEmpty(respClaims.get(ACCESS_TOKEN_SIGNING_ALG.toString())) &&
                SignatureAlgorithm.fromString(respClaims.get(ACCESS_TOKEN_SIGNING_ALG.toString())) != null) {
            rpFromRegisterResponse.setAccessTokenSigningAlg(respClaims.get(ACCESS_TOKEN_SIGNING_ALG.toString()));
        }

        if (!Strings.isNullOrEmpty(respClaims.get(RPT_AS_JWT.toString()))) {
            rpFromRegisterResponse.setRptAsJwt(Boolean.valueOf(respClaims.get(RPT_AS_JWT.toString())));
        }

        rpFromRegisterResponse.setResponseTypes(convertStringToList(respClaims.get(RESPONSE_TYPES.toString())));
        rpFromRegisterResponse.setAcrValues(convertStringToList(respClaims.get(DEFAULT_ACR_VALUES.toString())));
        rpFromRegisterResponse.setContacts(convertStringToList(respClaims.get(CONTACTS.toString())));
        rpFromRegisterResponse.setPostLogoutRedirectUris(convertStringToList(respClaims.get(POST_LOGOUT_REDIRECT_URIS.toString())));
        rpFromRegisterResponse.setScope(convertSpaceSeparatedStringToList(respClaims.get(SCOPE.toString())));
        rpFromRegisterResponse.setLogoUri(respClaims.get(LOGO_URI.toString()));
        rpFromRegisterResponse.setClientUri(respClaims.get(CLIENT_URI.toString()));
        rpFromRegisterResponse.setPolicyUri(respClaims.get(POLICY_URI.toString()));

        if (!Strings.isNullOrEmpty(respClaims.get(FRONT_CHANNEL_LOGOUT_SESSION_REQUIRED.toString()))) {
            rpFromRegisterResponse.setFrontChannelLogoutSessionRequired(Boolean.valueOf(respClaims.get(FRONT_CHANNEL_LOGOUT_SESSION_REQUIRED.toString())));
        }

        rpFromRegisterResponse.setTosUri(respClaims.get(TOS_URI.toString()));
        rpFromRegisterResponse.setJwks(respClaims.get(JWKS.toString()));
        rpFromRegisterResponse.setIdTokenBindingCnf(respClaims.get(ID_TOKEN_TOKEN_BINDING_CNF.toString()));
        rpFromRegisterResponse.setTlsClientAuthSubjectDn(respClaims.get(TLS_CLIENT_AUTH_SUBJECT_DN.toString()));

        if (!Strings.isNullOrEmpty(respClaims.get(SUBJECT_TYPE.toString())) &&
                SubjectType.fromString(respClaims.get(SUBJECT_TYPE.toString())) != null) {
            rpFromRegisterResponse.setSubjectType(respClaims.get(SUBJECT_TYPE.toString()));
        }

        if (!Strings.isNullOrEmpty(respClaims.get(RUN_INTROSPECTION_SCRIPT_BEFORE_ACCESS_TOKEN_CREATION_AS_JWT_AND_INCLUDE_CLAIMS.toString()))) {
            rpFromRegisterResponse.setRunIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims(Boolean.valueOf(respClaims.get(RUN_INTROSPECTION_SCRIPT_BEFORE_ACCESS_TOKEN_CREATION_AS_JWT_AND_INCLUDE_CLAIMS.toString())));
        }

        if (!Strings.isNullOrEmpty(respClaims.get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString())) &&
                SignatureAlgorithm.fromString(respClaims.get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString())) != null) {
            rpFromRegisterResponse.setIdTokenSignedResponseAlg(respClaims.get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        }

        if (!Strings.isNullOrEmpty(respClaims.get(ID_TOKEN_ENCRYPTED_RESPONSE_ALG.toString())) &&
                KeyEncryptionAlgorithm.fromName(respClaims.get(ID_TOKEN_ENCRYPTED_RESPONSE_ALG.toString())) != null) {
            rpFromRegisterResponse.setIdTokenEncryptedResponseAlg(respClaims.get(ID_TOKEN_ENCRYPTED_RESPONSE_ALG.toString()));
        }

        if (!Strings.isNullOrEmpty(respClaims.get(ID_TOKEN_ENCRYPTED_RESPONSE_ENC.toString())) &&
                BlockEncryptionAlgorithm.fromName(respClaims.get(ID_TOKEN_ENCRYPTED_RESPONSE_ENC.toString())) != null) {
            rpFromRegisterResponse.setIdTokenEncryptedResponseEnc(respClaims.get(ID_TOKEN_ENCRYPTED_RESPONSE_ENC.toString()));
        }

        if (!Strings.isNullOrEmpty(respClaims.get(USERINFO_SIGNED_RESPONSE_ALG.toString())) &&
                SignatureAlgorithm.fromString(respClaims.get(USERINFO_SIGNED_RESPONSE_ALG.toString())) != null) {
            rpFromRegisterResponse.setUserInfoSignedResponseAlg(respClaims.get(USERINFO_SIGNED_RESPONSE_ALG.toString()));
        }

        if (!Strings.isNullOrEmpty(respClaims.get(USERINFO_ENCRYPTED_RESPONSE_ALG.toString())) &&
                KeyEncryptionAlgorithm.fromName(respClaims.get(USERINFO_ENCRYPTED_RESPONSE_ALG.toString())) != null) {
            rpFromRegisterResponse.setUserInfoEncryptedResponseAlg(respClaims.get(USERINFO_ENCRYPTED_RESPONSE_ALG.toString()));
        }

        if (!Strings.isNullOrEmpty(respClaims.get(USERINFO_ENCRYPTED_RESPONSE_ENC.toString())) &&
                BlockEncryptionAlgorithm.fromName(respClaims.get(USERINFO_ENCRYPTED_RESPONSE_ENC.toString())) != null) {
            rpFromRegisterResponse.setUserInfoEncryptedResponseEnc(respClaims.get(USERINFO_ENCRYPTED_RESPONSE_ENC.toString()));
        }

        if (!Strings.isNullOrEmpty(respClaims.get(REQUEST_OBJECT_SIGNING_ALG.toString())) &&
                SignatureAlgorithm.fromString(respClaims.get(REQUEST_OBJECT_SIGNING_ALG.toString())) != null) {
            rpFromRegisterResponse.setRequestObjectSigningAlg(respClaims.get(REQUEST_OBJECT_SIGNING_ALG.toString()));
        }

        if (!Strings.isNullOrEmpty(respClaims.get(REQUEST_OBJECT_ENCRYPTION_ALG.toString())) &&
                KeyEncryptionAlgorithm.fromName(respClaims.get(REQUEST_OBJECT_ENCRYPTION_ALG.toString())) != null) {
            rpFromRegisterResponse.setRequestObjectEncryptionAlg(respClaims.get(REQUEST_OBJECT_ENCRYPTION_ALG.toString()));
        }

        if (!Strings.isNullOrEmpty(respClaims.get(REQUEST_OBJECT_ENCRYPTION_ENC.toString())) &&
                BlockEncryptionAlgorithm.fromName(respClaims.get(REQUEST_OBJECT_ENCRYPTION_ENC.toString())) != null) {
            rpFromRegisterResponse.setRequestObjectEncryptionEnc(respClaims.get(REQUEST_OBJECT_ENCRYPTION_ENC.toString()));
        }

        rpFromRegisterResponse.setDefaultMaxAge(safeToNumber(respClaims.get(DEFAULT_MAX_AGE.toString())));

        if (!Strings.isNullOrEmpty(respClaims.get(REQUIRE_AUTH_TIME.toString()))) {
            rpFromRegisterResponse.setRequireAuthTime(Boolean.valueOf(respClaims.get(REQUIRE_AUTH_TIME.toString())));
        }

        rpFromRegisterResponse.setInitiateLoginUri(respClaims.get(INITIATE_LOGIN_URI.toString()));
        rpFromRegisterResponse.setAuthorizedOrigins(convertStringToList(respClaims.get(AUTHORIZED_ORIGINS.toString())));
        rpFromRegisterResponse.setAccessTokenLifetime(safeToNumber(respClaims.get(ACCESS_TOKEN_LIFETIME.toString())));
        rpFromRegisterResponse.setSoftwareId(respClaims.get(SOFTWARE_ID.toString()));
        rpFromRegisterResponse.setSoftwareVersion(respClaims.get(SOFTWARE_VERSION.toString()));
        rpFromRegisterResponse.setSoftwareStatement(respClaims.get(SOFTWARE_STATEMENT.toString()));
        rpFromRegisterResponse.setClientJwksUri(respClaims.get(JWKS_URI.toString()));
        rpFromRegisterResponse.setClaimsRedirectUri(convertStringToList(respClaims.get(CLAIMS_REDIRECT_URIS.toString())));

        return rpFromRegisterResponse;
    }

    private static List<String> convertStringToList(String input) {
        if (input == null)
            return null;

        String commaSeparatedString = input.replaceAll("\\[", "").replaceAll("\\]", "").replace("\"", "");
        return Lists.newArrayList(commaSeparatedString.split(","));
    }

    private static List<String> convertSpaceSeparatedStringToList(String input) {
        if (input == null)
            return null;

        return Lists.newArrayList(input.split(" "));
    }

    public static Integer safeToNumber(String num) {
        if (num == null)
            return null;
        try {
            return Integer.valueOf(num);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static boolean compareParams(List<String> oxdRp, List<String> oxAuthRp) {
        if (oxdRp == null || oxdRp.isEmpty()) {
            return oxAuthRp == null || oxdRp.isEmpty();
        } else if (oxAuthRp != null) {
            return new HashSet<>(oxdRp).equals(new HashSet<>(oxAuthRp));
        }
        return false;
    }

    public static boolean compareJsonNodeParams(String oxdParam, String oxAuthparam) throws IOException {
        return oxdParam == null ? oxAuthparam == null : Jackson2.createJsonMapperWithoutEmptyAttributes().readTree(oxdParam).equals(Jackson2.createJsonMapperWithoutEmptyAttributes().readTree(oxAuthparam));
    }

    public static boolean compareParams(Boolean oxdParam, Boolean oxAuthparam) {
        return oxdParam == null ? oxAuthparam == null : oxdParam.equals(oxAuthparam);
    }

    public static boolean compareParams(Integer oxdParam, Integer oxAuthparam) {
        return oxdParam == null ? oxAuthparam == null : oxdParam.equals(oxAuthparam);
    }

    public static boolean compareParams(Date oxdParam, Date oxAuthparam) {
        return oxdParam == null ? oxAuthparam == null : oxdParam.equals(oxAuthparam);
    }
}
