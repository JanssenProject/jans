/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import io.jans.as.client.*;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.client.model.authorize.Claim;
import io.jans.as.client.model.authorize.ClaimValue;
import io.jans.as.client.model.authorize.JwtAuthorizationRequest;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.register.RegisterRequestParam;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static io.jans.as.client.client.Asserter.assertRegisterResponseClaimsNotNull;
import static io.jans.as.model.register.RegisterRequestParam.*;

/**
 * @author Javier Rojas Blum
 * @version March 23, 2022
 */
public class ClientLanguageMetadataTest extends BaseTest {

    final List<String> SCOPE = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name", "clientinfo");
    final String[] CLIENT_INFO_NAME_CLAIMS = new String[]{"name", "name#en-NZ", "name#en-CA", "name#en-GB",
            "name#es", "name#es-BO", "name#ja-Hani-JP", "name#ja-Jpan-JP", "name#ja-Kana-JP",
            "name#fr", "name#fr-FR", "name#fr-CA"};
    final String[] USER_INFO_CLIENT_NAME_CLAIMS = new String[]{
            CLIENT_NAME.getName(), "client_name#en-NZ", "client_name#en-CA", "client_name#en-GB",
            "client_name#ja-Hani-JP", "client_name#ja-Jpan-JP", "client_name#ja-Kana-JP",
            "client_name#es", "client_name#es-BO", "client_name#fr", "client_name#fr-FR", "client_name#fr-CA"};
    final String[] USER_INFO_LOGO_URI_CLAIMS = new String[]{
            LOGO_URI.getName(), "logo_uri#en-NZ", "logo_uri#en-CA", "logo_uri#en-GB",
            "logo_uri#ja-Hani-JP", "logo_uri#ja-Jpan-JP", "logo_uri#ja-Kana-JP",
            "logo_uri#es", "logo_uri#es-BO", "logo_uri#fr", "logo_uri#fr-FR", "logo_uri#fr-CA"};
    final String[] USER_INFO_CLIENT_URI_CLAIMS = new String[]{
            CLIENT_URI.getName(), "client_uri#en-NZ", "client_uri#en-CA", "client_uri#en-GB",
            "client_uri#ja-Hani-JP", "client_uri#ja-Jpan-JP", "client_uri#ja-Kana-JP",
            "client_uri#es", "client_uri#es-BO", "client_uri#fr", "client_uri#fr-FR", "client_uri#fr-CA"};
    final String[] USER_INFO_POLICY_URI_CLAIMS = new String[]{
            POLICY_URI.getName(), "policy_uri#en-NZ", "policy_uri#en-CA", "policy_uri#en-GB",
            "policy_uri#ja-Hani-JP", "policy_uri#ja-Jpan-JP", "policy_uri#ja-Kana-JP",
            "policy_uri#es", "policy_uri#es-BO", "policy_uri#fr", "policy_uri#fr-FR", "policy_uri#fr-CA"};
    final String[] USER_INFO_TOS_URI_CLAIMS = new String[]{
            TOS_URI.getName(), "tos_uri#en-NZ", "tos_uri#en-CA", "tos_uri#en-GB",
            "tos_uri#ja-Hani-JP", "tos_uri#ja-Jpan-JP", "tos_uri#ja-Kana-JP",
            "tos_uri#es", "tos_uri#es-BO", "tos_uri#fr", "tos_uri#fr-FR", "tos_uri#fr-CA"
    };

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void authorizationCodeFlow(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationCodeFlow");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterResponse registerResponse = getRegisterResponse(redirectUris, sectorIdentifierUri, responseTypes, SCOPE, null);

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationClientUri = registerResponse.getRegistrationClientUri();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();

        // 2. Client Read
        requestClientRead(registrationClientUri, registrationAccessToken);

        // 3. Request authorization and receive the authorization code.
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, SCOPE, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse)
                .responseTypes(responseTypes)
                .check();

        String authorizationCode = authorizationResponse.getCode();
        String idToken = authorizationResponse.getIdToken();

        // 4. Request access token using the authorization code.
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient = newTokenClient(tokenRequest);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();

        // 5. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSAClientEngine(jwksUri, SignatureAlgorithm.RS256)
                .claimsPresence(JwtClaimName.CODE_HASH)
                .notNullAuthenticationTime()
                .notNullJansOpenIDConnectVersion()
                .notNullAuthenticationContextClassReference()
                .notNullAuthenticationMethodReferences()
                .check();

        String accessToken = tokenResponse.getAccessToken();

        // 6. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setExecutor(clientEngine(true));
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL, JwtClaimName.BIRTHDATE, JwtClaimName.GENDER, JwtClaimName.MIDDLE_NAME)
                .claimsPresence(JwtClaimName.NICKNAME, JwtClaimName.PREFERRED_USERNAME, JwtClaimName.PROFILE)
                .claimsPresence(JwtClaimName.WEBSITE, JwtClaimName.EMAIL_VERIFIED, JwtClaimName.PHONE_NUMBER)
                .claimsPresence(JwtClaimName.PHONE_NUMBER_VERIFIED, JwtClaimName.ADDRESS, JwtClaimName.USER_NAME)
                .claimsNoPresence("org_name", "work_phone")
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri",
            "RS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void requestParameterMethodRS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodRS256");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = getRegisterResponse(redirectUris, sectorIdentifierUri, responseTypes, SCOPE, clientJwksUri);

        String clientId = registerResponse.getClientId();
        String registrationClientUri = registerResponse.getRegistrationClientUri();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();

        // 2. Client Read
        requestClientRead(registrationClientUri, registrationAccessToken);

        // 3. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, SCOPE, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.RS256, cryptoProvider);
        jwtAuthorizationRequest.setKeyId(keyId);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"basic"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse)
                .responseTypes(responseTypes)
                .check();

        String accessToken = authorizationResponse.getAccessToken();

        // 4. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(response3)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    private RegisterResponse getRegisterResponse(String redirectUris, String sectorIdentifierUri, List<ResponseType> responseTypes, List<String> scopes, String clientJwksUri) {
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setScope(scopes);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        if (clientJwksUri != null) {
            registerRequest.setJwksUri(clientJwksUri);
            registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS256);
        }

        registerRequest.setClientName("Nombre del cliente", new Locale("es"));
        registerRequest.setClientName("Nombre del caserito", new Locale("es", "BO"));
        registerRequest.setClientName("Client name", Locale.UK);
        registerRequest.setClientName("Client name", Locale.CANADA);
        registerRequest.setClientName("Client name", Locale.forLanguageTag("en-NZ"));
        registerRequest.setClientName("Nom du client", Locale.FRENCH);
        registerRequest.setClientName("Nom du client", Locale.CANADA_FRENCH);
        registerRequest.setClientName("Nom du client", Locale.FRANCE);
        registerRequest.setClientName("クライアント名", Locale.forLanguageTag("ja-Jpan-JP"));
        registerRequest.setClientName("カナ姓※", Locale.forLanguageTag("ja-Kana-JP"));
        registerRequest.setClientName("漢字姓※", Locale.forLanguageTag("ja-Hani-JP"));

        registerRequest.setLogoUri("https://gluu.org/wp-content/uploads/2020/12/logo.png");
        registerRequest.setLogoUri("https://gluu.org/wp-content/uploads/2020/12/logo.png?locale=es", new Locale("es"));
        registerRequest.setLogoUri("https://gluu.org/wp-content/uploads/2020/12/logo.png?locale=es-BO", new Locale("es", "BO"));
        registerRequest.setLogoUri("https://gluu.org/wp-content/uploads/2020/12/logo.png?locale=en-UK", Locale.UK);
        registerRequest.setLogoUri("https://gluu.org/wp-content/uploads/2020/12/logo.png?locale=en-CA", Locale.CANADA);
        registerRequest.setLogoUri("https://gluu.org/wp-content/uploads/2020/12/logo.png?locale=en-NZ", Locale.forLanguageTag("en-NZ"));
        registerRequest.setLogoUri("https://gluu.org/wp-content/uploads/2020/12/logo.png?locale=fr", Locale.FRENCH);
        registerRequest.setLogoUri("https://gluu.org/wp-content/uploads/2020/12/logo.png?locale=fr-CA", Locale.CANADA_FRENCH);
        registerRequest.setLogoUri("https://gluu.org/wp-content/uploads/2020/12/logo.png?locale=fr-FR", Locale.FRANCE);
        registerRequest.setLogoUri("https://gluu.org/wp-content/uploads/2020/12/logo.png?locale=ja-Jpan-JP", Locale.forLanguageTag("ja-Jpan-JP"));
        registerRequest.setLogoUri("https://gluu.org/wp-content/uploads/2020/12/logo.png?locale=ja-Kana-JP", Locale.forLanguageTag("ja-Kana-JP"));
        registerRequest.setLogoUri("https://gluu.org/wp-content/uploads/2020/12/logo.png?locale=ja-Hani-JP", Locale.forLanguageTag("ja-Hani-JP"));

        registerRequest.setClientUri("https://client-home-page/index.htm");
        registerRequest.setClientUri("https://client-home-page/index.htm?locale=es", new Locale("es"));
        registerRequest.setClientUri("https://client-home-page/index.htm?locale=es-BO", new Locale("es", "BO"));
        registerRequest.setClientUri("https://client-home-page/index.htm?locale=en-UK", Locale.UK);
        registerRequest.setClientUri("https://client-home-page/index.htm?locale=en-CA", Locale.CANADA);
        registerRequest.setClientUri("https://client-home-page/index.htm?locale=en-NZ", Locale.forLanguageTag("en-NZ"));
        registerRequest.setClientUri("https://client-home-page/index.htm?locale=fr", Locale.FRENCH);
        registerRequest.setClientUri("https://client-home-page/index.htm?locale=fr-CA", Locale.CANADA_FRENCH);
        registerRequest.setClientUri("https://client-home-page/index.htm?locale=fr-FR", Locale.FRANCE);
        registerRequest.setClientUri("https://client-home-page/index.htm?locale=ja-Jpan-JP", Locale.forLanguageTag("ja-Jpan-JP"));
        registerRequest.setClientUri("https://client-home-page/index.htm?locale=ja-Kana-JP", Locale.forLanguageTag("ja-Kana-JP"));
        registerRequest.setClientUri("https://client-home-page/index.htm?locale=ja-Hani-JP", Locale.forLanguageTag("ja-Hani-JP"));

        registerRequest.setPolicyUri("https://client-home-page/policy.htm");
        registerRequest.setPolicyUri("https://client-home-page/policy.htm?locale=es", new Locale("es"));
        registerRequest.setPolicyUri("https://client-home-page/policy.htm?locale=es-BO", new Locale("es", "BO"));
        registerRequest.setPolicyUri("https://client-home-page/policy.htm?locale=en-UK", Locale.UK);
        registerRequest.setPolicyUri("https://client-home-page/policy.htm?locale=en-CA", Locale.CANADA);
        registerRequest.setPolicyUri("https://client-home-page/policy.htm?locale=en-NZ", Locale.forLanguageTag("en-NZ"));
        registerRequest.setPolicyUri("https://client-home-page/policy.htm?locale=fr", Locale.FRENCH);
        registerRequest.setPolicyUri("https://client-home-page/policy.htm?locale=fr-CA", Locale.CANADA_FRENCH);
        registerRequest.setPolicyUri("https://client-home-page/policy.htm?locale=fr-FR", Locale.FRANCE);
        registerRequest.setPolicyUri("https://client-home-page/policy.htm?locale=ja-Jpan-JP", Locale.forLanguageTag("ja-Jpan-JP"));
        registerRequest.setPolicyUri("https://client-home-page/policy.htm?locale=ja-Kana-JP", Locale.forLanguageTag("ja-Kana-JP"));
        registerRequest.setPolicyUri("https://client-home-page/policy.htm?locale=ja-Hani-JP", Locale.forLanguageTag("ja-Hani-JP"));

        registerRequest.setTosUri("https://client-home-page/tos.htm");
        registerRequest.setTosUri("https://client-home-page/tos.htm?locale=es", new Locale("es"));
        registerRequest.setTosUri("https://client-home-page/tos.htm?locale=es-BO", new Locale("es", "BO"));
        registerRequest.setTosUri("https://client-home-page/tos.htm?locale=en-UK", Locale.UK);
        registerRequest.setTosUri("https://client-home-page/tos.htm?locale=en-CA", Locale.CANADA);
        registerRequest.setTosUri("https://client-home-page/tos.htm?locale=en-NZ", Locale.forLanguageTag("en-NZ"));
        registerRequest.setTosUri("https://client-home-page/tos.htm?locale=fr", Locale.FRENCH);
        registerRequest.setTosUri("https://client-home-page/tos.htm?locale=fr-CA", Locale.CANADA_FRENCH);
        registerRequest.setTosUri("https://client-home-page/tos.htm?locale=fr-FR", Locale.FRANCE);
        registerRequest.setTosUri("https://client-home-page/tos.htm?locale=ja-Jpan-JP", Locale.forLanguageTag("ja-Jpan-JP"));
        registerRequest.setTosUri("https://client-home-page/tos.htm?locale=ja-Kana-JP", Locale.forLanguageTag("ja-Kana-JP"));
        registerRequest.setTosUri("https://client-home-page/tos.htm?locale=ja-Hani-JP", Locale.forLanguageTag("ja-Hani-JP"));

        RegisterClient registerClient = newRegisterClient(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();
        return registerResponse;
    }

    private void requestClientRead(String registrationClientUri, String registrationAccessToken) {
        RegisterRequest registerRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient registerClient = new RegisterClient(registrationClientUri);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(response).ok()
                .notNullRegistrationClientUri()
                .check();
        assertRegisterResponseClaimsNotNull(response, APPLICATION_TYPE, POLICY_URI, SECTOR_IDENTIFIER_URI, SUBJECT_TYPE,
                ID_TOKEN_SIGNED_RESPONSE_ALG, CLIENT_NAME, LOGO_URI, RegisterRequestParam.SCOPE);

        assertRegisterResponseClaimsNotNull(response, USER_INFO_CLIENT_NAME_CLAIMS);
        assertRegisterResponseClaimsNotNull(response, USER_INFO_LOGO_URI_CLAIMS);
        assertRegisterResponseClaimsNotNull(response, USER_INFO_CLIENT_URI_CLAIMS);
        assertRegisterResponseClaimsNotNull(response, USER_INFO_POLICY_URI_CLAIMS);
        assertRegisterResponseClaimsNotNull(response, USER_INFO_TOS_URI_CLAIMS);
    }
}
