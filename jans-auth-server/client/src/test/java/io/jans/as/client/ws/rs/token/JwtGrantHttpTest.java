package io.jans.as.client.ws.rs.token;

import io.jans.as.client.*;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.CryptoProviderException;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtType;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.security.KeyStoreException;
import java.security.cert.CertificateEncodingException;
import java.util.*;

import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Yuriy Z
 */
public class JwtGrantHttpTest extends BaseTest {

    /**
     * Test for the complete Jwt Grant Flow.
     */
    @Parameters({"userId", "redirectUris", "redirectUri"})
    @Test
    public void jwtGrantFlow(
            final String userId, final String redirectUris, final String redirectUri) throws Exception {
        showTitle("jwtGrantFlow");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        List<GrantType> grantTypes = Collections.singletonList(GrantType.JWT_BEARER);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");

        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
        AuthCryptoProvider cryptoProvider = cryptoContext.getCryptoProvider();

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, grantTypes, scopes, cryptoProvider);

        assertTrue(registerResponse.getGrantTypes().contains(GrantType.JWT_BEARER));

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization code at Authorization Challenge Endpoint
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        Date issuedAt = calendar.getTime();
        calendar.add(Calendar.MINUTE, 5);
        Date expirationTime = calendar.getTime();

        SignatureAlgorithm algorithm = SignatureAlgorithm.RS256;
        String keyId = cryptoContext.getKeyId(Algorithm.RS256);

        Jwt assertionJwt = new Jwt();

        assertionJwt.getHeader().setType(JwtType.JWT);
        assertionJwt.getHeader().setAlgorithm(algorithm);
        assertionJwt.getHeader().setKeyId(keyId);

        // Claims
        assertionJwt.getClaims().setClaim("iss", clientId);
        assertionJwt.getClaims().setClaim("sub", clientId);
        assertionJwt.getClaims().setClaim("aud", issuer);
        assertionJwt.getClaims().setJwtId(UUID.randomUUID());
        assertionJwt.getClaims().setExpirationTime(expirationTime);
        assertionJwt.getClaims().setIssuedAt(issuedAt);
        assertionJwt.getClaims().setClaim("uid", userId);

        // Signature
        String signature = cryptoProvider.sign(assertionJwt.getSigningInput(), keyId, clientSecret, algorithm);
        assertionJwt.setEncodedSignature(signature);
        String assertion = assertionJwt.toString();
        System.out.println("Assertion:");
        System.out.println(assertion);


        // 3. Request access token using the authorization code.
        TokenRequest tokenRequest = new TokenRequest(GrantType.JWT_BEARER);
        tokenRequest.setAssertion(assertion);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        tokenRequest.setScope(StringUtils.implode(scopes, " "));

        TokenClient tokenClient = newTokenClient(tokenRequest);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .status(200)
                .check();

        // Request user info
        // (it will work only if set AS configuration property jwtGrantAllowUserByUidInAssertion=true and provide uid in assertion payload)

//        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
//        userInfoClient.setExecutor(clientEngine(true));
//        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(tokenResponse.getAccessToken());
//
//        showClient(userInfoClient);
//        AssertBuilder.userInfoResponse(userInfoResponse)
//                .check();
    }

    public RegisterResponse registerClient(final String redirectUris, List<ResponseType> responseTypes, List<GrantType> grantTypes, List<String> scopes, AuthCryptoProvider cryptoProvider) throws CryptoProviderException, KeyStoreException, CertificateEncodingException {
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app: jwt grant",
                io.jans.as.model.util.StringUtils.spaceSeparatedToList(redirectUris));

        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setScope(scopes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setSubjectType(SubjectType.PUBLIC);
        registerRequest.setJwks(TestCryptoContext.getInstance().getJwksAsString());

        RegisterClient registerClient = newRegisterClient(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();
        return registerResponse;
    }
}
