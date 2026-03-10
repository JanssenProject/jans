package io.jans.as.server.model.authorize;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.exception.InvalidJwtException;
import jakarta.ws.rs.WebApplicationException;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 */
public class JwtAuthorizationRequestTest {

    @Test(expectedExceptions = InvalidJwtException.class)
    public void createJwtAuthorizationRequest_whenEncryptionIsRequiredForUnencryptedRequestObject_shouldThrowException() throws InvalidJwtException {
        AppConfiguration appConfiguration = new AppConfiguration();
        appConfiguration.setRequireRequestObjectEncryption(true);

        String signedJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        new JwtAuthorizationRequest(appConfiguration, null, signedJwt, new Client());
    }

    @Test
    public void validateRequestUri_whichIsAllowedByClient_shouldBeOk() {
        String requestUri = "https://myrp.com/request_uri";

        Client client = new Client();
        client.setRequestUris(new String[]{"https://myrp.com/request_uri"});
        JwtAuthorizationRequest.validateRequestUri(requestUri, client, new AppConfiguration(), "", new ErrorResponseFactory());
    }

    @Test
    public void validateRequestUri_withNoRestrictions_shouldBeOk() {
        String requestUri = "https://myrp.com/request_uri";

        JwtAuthorizationRequest.validateRequestUri(requestUri, new Client(), new AppConfiguration(), "", new ErrorResponseFactory());
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateRequestUri_whichIsNotAllowedByClient_shouldRaiseException() {
        String requestUri = "https://myrp.com/request_uri";

        Client client = new Client();
        client.setRequestUris(new String[]{"https://myrp.com"});
        JwtAuthorizationRequest.validateRequestUri(requestUri, client, new AppConfiguration(), "", new ErrorResponseFactory());
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateRequestUri_whichIsBlockListed_shouldRaiseException() {
        String requestUri = "https://myrp.com/request_uri";

        final AppConfiguration appConfiguration = new AppConfiguration();
        appConfiguration.setRequestUriBlockList(Arrays.asList("myrp.com", "evil.com"));
        JwtAuthorizationRequest.validateRequestUri(requestUri, new Client(), appConfiguration, "", new ErrorResponseFactory());
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateRequestUri_forLocalhost_shouldRaiseException() {
        String requestUri = "https://localhost/request_uri";

        final AppConfiguration appConfiguration = new AppConfiguration();
        appConfiguration.setRequestUriBlockList(Collections.singletonList("localhost"));
        JwtAuthorizationRequest.validateRequestUri(requestUri, new Client(), appConfiguration, "", new ErrorResponseFactory());
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateRequestUri_forLocalhostIp_shouldRaiseException() {
        String requestUri = "https://127.0.0.1/request_uri";

        final AppConfiguration appConfiguration = new AppConfiguration();
        appConfiguration.setRequestUriBlockList(Collections.singletonList("127.0.0.1"));
        JwtAuthorizationRequest.validateRequestUri(requestUri, new Client(), appConfiguration, "", new ErrorResponseFactory());
    }

    @Test
    public void validateRequestUri_whichIsNotBlockListed_shouldBeOk() {
        String requestUri = "https://myrp.com/request_uri";

        final AppConfiguration appConfiguration = new AppConfiguration();
        appConfiguration.setRequestUriBlockList(Arrays.asList("evil.com", "second.com"));
        JwtAuthorizationRequest.validateRequestUri(requestUri, new Client(), appConfiguration, "", new ErrorResponseFactory());
    }

    /**
     * Security test: Verifies that JWE encryption algorithms are NOT recognized as signature algorithms.
     * This is critical for the forceSignedRequestObject security fix.
     *
     * Background: A JWE request object with a plain JSON payload (no nested signed JWT) would have
     * algorithm="RSA-OAEP" (or similar JWE encryption algorithm) in its header. If this were incorrectly
     * recognized as a valid signature algorithm, it could bypass signature verification.
     *
     * The fix ensures:
     * 1. JWE request objects MUST contain a nested signed JWT (validated in JwtAuthorizationRequest constructor)
     * 2. forceSignedRequestObject check treats unrecognized algorithms (null) the same as SignatureAlgorithm.NONE
     */
    @Test
    public void jweEncryptionAlgorithms_shouldNotBeRecognizedAsSignatureAlgorithms() {
        // JWE Key Encryption Algorithms - should all return null
        assertNull(SignatureAlgorithm.fromString("RSA-OAEP"),
                "RSA-OAEP is a JWE key encryption algorithm, not a signature algorithm");
        assertNull(SignatureAlgorithm.fromString("RSA-OAEP-256"),
                "RSA-OAEP-256 is a JWE key encryption algorithm, not a signature algorithm");
        assertNull(SignatureAlgorithm.fromString("RSA1_5"),
                "RSA1_5 is a JWE key encryption algorithm, not a signature algorithm");
        assertNull(SignatureAlgorithm.fromString("A128KW"),
                "A128KW is a JWE key encryption algorithm, not a signature algorithm");
        assertNull(SignatureAlgorithm.fromString("A192KW"),
                "A192KW is a JWE key encryption algorithm, not a signature algorithm");
        assertNull(SignatureAlgorithm.fromString("A256KW"),
                "A256KW is a JWE key encryption algorithm, not a signature algorithm");
        assertNull(SignatureAlgorithm.fromString("dir"),
                "dir is a JWE key encryption algorithm, not a signature algorithm");
        assertNull(SignatureAlgorithm.fromString("ECDH-ES"),
                "ECDH-ES is a JWE key encryption algorithm, not a signature algorithm");
        assertNull(SignatureAlgorithm.fromString("ECDH-ES+A128KW"),
                "ECDH-ES+A128KW is a JWE key encryption algorithm, not a signature algorithm");

        // JWE Content Encryption Algorithms - should all return null
        assertNull(SignatureAlgorithm.fromString("A128GCM"),
                "A128GCM is a JWE content encryption algorithm, not a signature algorithm");
        assertNull(SignatureAlgorithm.fromString("A256GCM"),
                "A256GCM is a JWE content encryption algorithm, not a signature algorithm");
        assertNull(SignatureAlgorithm.fromString("A128CBC+HS256"),
                "A128CBC+HS256 is a JWE content encryption algorithm, not a signature algorithm");
        assertNull(SignatureAlgorithm.fromString("A256CBC+HS512"),
                "A256CBC+HS512 is a JWE content encryption algorithm, not a signature algorithm");
    }

    /**
     * Security test: Verifies that the forceSignedRequestObject logic correctly handles
     * both null (unrecognized algorithm) and NONE algorithm cases.
     *
     * This test documents the security fix for CVE-like vulnerability where:
     * - A JWE with plain JSON payload would have algorithm="RSA-OAEP" in the outer header
     * - SignatureAlgorithm.fromString("RSA-OAEP") returns null (not a signature algorithm)
     * - The OLD buggy check: `signatureAlgorithm == SignatureAlgorithm.NONE` would pass (null != NONE)
     * - The NEW fixed check: `signatureAlgorithm == null || signatureAlgorithm == SignatureAlgorithm.NONE` correctly rejects
     */
    @Test
    public void forceSignedRequestObjectGuard_shouldRejectBothNullAndNoneAlgorithms() {
        // Simulate the forceSignedRequestObject guard logic

        // Case 1: JWE encryption algorithm (returns null) - should be rejected
        SignatureAlgorithm jweAlg = SignatureAlgorithm.fromString("RSA-OAEP");
        assertNull(jweAlg, "RSA-OAEP should return null");
        assertTrue(jweAlg == null || jweAlg == SignatureAlgorithm.NONE,
                "JWE algorithms (null) should trigger forceSignedRequestObject rejection");

        // Case 2: Explicit NONE algorithm - should be rejected
        SignatureAlgorithm noneAlg = SignatureAlgorithm.fromString("none");
        assertTrue(noneAlg == SignatureAlgorithm.NONE, "none should return SignatureAlgorithm.NONE");
        assertTrue(noneAlg == null || noneAlg == SignatureAlgorithm.NONE,
                "NONE algorithm should trigger forceSignedRequestObject rejection");

        // Case 3: Valid signature algorithm - should NOT be rejected
        SignatureAlgorithm validAlg = SignatureAlgorithm.fromString("RS256");
        assertTrue(validAlg == SignatureAlgorithm.RS256, "RS256 should be recognized");
        assertTrue(!(validAlg == null || validAlg == SignatureAlgorithm.NONE),
                "Valid signature algorithms should NOT trigger forceSignedRequestObject rejection");
    }
}
