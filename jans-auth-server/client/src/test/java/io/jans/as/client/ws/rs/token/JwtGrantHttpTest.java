package io.jans.as.client.ws.rs.token;

import io.jans.as.client.*;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.client.util.KeyGeneratorContext;
import io.jans.as.client.util.TestPropFile;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.CryptoProviderException;
import io.jans.as.model.jwk.*;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtType;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.Base64Util;
import io.jans.as.model.util.StringUtils;
import io.jans.util.security.SecurityProviderUtility;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

import static io.jans.as.model.jwk.JWKParameter.*;

/**
 * @author Yuriy Z
 */
public class JwtGrantHttpTest extends BaseTest {

    /**
     * Test for the complete Jwt Grant Flow.
     */
    @Parameters({"userId", "redirectUris", "redirectUri", "dnName", "keyStoreFile", "keyStoreSecret", "RS256_keyId"})
    @Test
    public void jwtGrantFlow(
            final String userId, final String redirectUris, final String redirectUri, final String dnName,
            final String keyStoreFile, final String keyStoreSecret, String keyId) throws Exception {
        showTitle("jwtGrantFlow");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        List<GrantType> grantTypes = Collections.singletonList(GrantType.JWT_BEARER);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, grantTypes, scopes, cryptoProvider);

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization code at Authorization Challenge Endpoint
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        Date issuedAt = calendar.getTime();
        calendar.add(Calendar.MINUTE, 5);
        Date expirationTime = calendar.getTime();

        SignatureAlgorithm algorithm = SignatureAlgorithm.RS256;

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

    private JSONWebKeySet generateJwks(AuthCryptoProvider cryptoProvider) throws CryptoProviderException, KeyStoreException, CertificateEncodingException {
        final KeyOpsType keyOpsType = KeyOpsType.CONNECT;

        final KeyGeneratorContext context = new KeyGeneratorContext();
        context.setKeyLength(2048);
        context.setExpirationDays(0);
        context.setExpirationHours(10);
        context.calculateExpiration();
        context.setTestPropFile(new TestPropFile(null));
        context.setKeyOpsType(keyOpsType);
        context.setCryptoProvider(cryptoProvider);
        context.calculateExpiration();

        JSONWebKeySet jwks = new JSONWebKeySet();

        final KeyStore keyStore = cryptoProvider.getKeyStore();
        for (String kid : Collections.list(keyStore.aliases())) {
            final PublicKey publicKey = cryptoProvider.getPublicKey(kid);
            final Certificate cert = keyStore.getCertificate(kid);
            final String algName = org.apache.commons.lang3.StringUtils.substringAfterLast(kid, "_");
            Algorithm algorithm = Algorithm.fromString(algName);

            if (publicKey == null || cert == null || algorithm == null) {
                continue;
            }

            SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(algorithm.getParamName());
            JSONObject result = generateJwk(algorithm, kid, publicKey, Use.SIGNATURE, (X509Certificate) cert, context.getExpirationForKeyOpsType(keyOpsType));

            JSONWebKey key = new JSONWebKey();

            key.setName(algorithm.getOutName());
            key.setDescr(algorithm.getDescription());
            key.setKid(result.getString(KEY_ID));
            key.setUse(Use.SIGNATURE);
            key.setAlg(algorithm);
            key.setKty(signatureAlgorithm.getFamily().getKeyType());
            key.setExp(result.optLong(EXPIRATION_TIME));
            key.setCrv(signatureAlgorithm.getCurve());
            key.setN(result.optString(MODULUS));
            key.setE(result.optString(EXPONENT));
            key.setX(result.optString(X));
            key.setY(result.optString(Y));
            key.setKeyOpsType(Collections.singletonList(keyOpsType));

            JSONArray x5c = result.optJSONArray(CERTIFICATE_CHAIN);
            key.setX5c(StringUtils.toList(x5c));

            //System.out.println("Generated jwk for " + kid);
            jwks.getKeys().add(key);
        }

        return jwks;
    }

    private static JSONObject generateJwk(Algorithm algorithm, String alias, PublicKey publicKey, Use use, X509Certificate cert, Long expirationTime) throws CertificateEncodingException {
        JSONObject jsonObject = new JSONObject();

        algorithm.fill(jsonObject);

        jsonObject.put(JWKParameter.KEY_ID, alias);
        jsonObject.put(JWKParameter.EXPIRATION_TIME, expirationTime);
        if (publicKey instanceof RSAPublicKey) {
            RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
            jsonObject.put(JWKParameter.MODULUS, Base64Util.base64urlencodeUnsignedBigInt(rsaPublicKey.getModulus()));
            jsonObject.put(JWKParameter.EXPONENT, Base64Util.base64urlencodeUnsignedBigInt(rsaPublicKey.getPublicExponent()));
        } else if (publicKey instanceof ECPublicKey) {
            ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
            if (use == Use.SIGNATURE) {
                SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(algorithm.getParamName());
                jsonObject.put(JWKParameter.CURVE, signatureAlgorithm.getCurve().getName());
            } else if (use == Use.ENCRYPTION) {
                KeyEncryptionAlgorithm keyEncryptionAlgorithm = KeyEncryptionAlgorithm.fromName(algorithm.getParamName());
                jsonObject.put(JWKParameter.CURVE, keyEncryptionAlgorithm.getCurve().getName());
            }
            jsonObject.put(JWKParameter.X, Base64Util.base64urlencodeUnsignedBigInt(ecPublicKey.getW().getAffineX()));
            jsonObject.put(JWKParameter.Y, Base64Util.base64urlencodeUnsignedBigInt(ecPublicKey.getW().getAffineY()));
        }
        if (SecurityProviderUtility.isBcProvMode() && use == Use.SIGNATURE
                && publicKey instanceof org.bouncycastle.jcajce.interfaces.EdDSAPublicKey) {
            org.bouncycastle.jcajce.interfaces.EdDSAPublicKey edDSAPublicKey = (org.bouncycastle.jcajce.interfaces.EdDSAPublicKey) publicKey;
            SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(algorithm.getParamName());
            jsonObject.put(JWKParameter.CURVE, signatureAlgorithm.getCurve().getName());
            jsonObject.put(JWKParameter.X, Base64Util.base64urlencode(edDSAPublicKey.getEncoded()));
            // EdDSA keys (EdDSAPublicKey, EDDSAPrivateKey) don't use BigInteger, but only byte[],
            // so Base64Util.base64urlencode, but not Base64Util.base64urlencodeUnsignedBigInt is used.
        }
        JSONArray x5c = new JSONArray();
        x5c.put(Base64.encodeBase64String(cert.getEncoded()));
        jsonObject.put(JWKParameter.CERTIFICATE_CHAIN, x5c);
        return jsonObject;
    }

    public RegisterResponse registerClient(final String redirectUris, List<ResponseType> responseTypes, List<GrantType> grantTypes, List<String> scopes, AuthCryptoProvider cryptoProvider) throws CryptoProviderException, KeyStoreException, CertificateEncodingException {
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app: jwt grant",
                io.jans.as.model.util.StringUtils.spaceSeparatedToList(redirectUris));

        final JSONWebKeySet jwks = generateJwks(cryptoProvider);

        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setScope(scopes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setSubjectType(SubjectType.PUBLIC);
        registerRequest.setJwks(jwks.toString());

        RegisterClient registerClient = newRegisterClient(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();
        return registerResponse;
    }

}
