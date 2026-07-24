/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.cedarling.service;

import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.model.jwt.JwtHeader;
import io.jans.core.cedarling.model.CedarlingConfiguration;
import io.jans.core.cedarling.service.CedarlingAuthorizationService;
import io.jans.core.cedarling.service.security.api.ProtectedCedarlingApi;
import io.jans.lock.service.CedarlingProtectionService;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;

/**
 * Unit tests for {@link CedarlingProtectionService#processAuthorization}.
 *
 * <p>The method under test implements a multi-step authorization filter:
 * <ol>
 *   <li>Presence check – bearer token must be present.</li>
 *   <li>Permission resolution – at least one {@code @ProtectedCedarlingApi} annotation
 *       must be found on the JAX-RS resource class, method, or its interfaces.</li>
 *   <li>JWT validation – token must be parseable as a JWT.</li>
 *   <li>Issuer check – JWT {@code iss} claim must match the OIDC discovery document.</li>
 *   <li>Expiry check – JWT {@code exp} claim must be in the future.</li>
 *   <li>Algorithm check – HMAC-family algorithms are rejected.</li>
 *   <li>Signature verification – cryptographic signature must be valid.</li>
 *   <li>Cedarling policy evaluation – the Cedarling engine must grant access
 *       for every declared permission.</li>
 * </ol>
 *
 * <p>A {@code null} return value from {@code processAuthorization} means
 * "pass-through" – the JAX-RS filter will allow the request to continue.
 *
 * @author Author Date: 12/05/2026
 */
@ExtendWith(MockitoExtension.class)
class CedarlingProtectionServiceTest {

    // ─── Constants ──────────────────────────────────────────────────────────────

    private static final String TEST_ISSUER   = "https://idp.example.com";
    private static final String TEST_JWKS_URI = "https://idp.example.com/jwks";
    /** Minimal three-part placeholder used wherever a JWT string is required. */
    private static final String DUMMY_JWT     = "header.payload.signature";

    // ─── Mocks & SUT ────────────────────────────────────────────────────────────

    @Mock private Logger                        log;
    @Mock private CedarlingConfiguration        cedarConf;
    @Mock private CedarlingAuthorizationService authorizationService;
    /** Injected as a mock so that JWKS HTTP calls can be stubbed without networking. */
    @Mock private ObjectMapper                  mapper;

    @InjectMocks
    private CedarlingProtectionService service;

    // ─── JAX-RS Resource Fixtures ───────────────────────────────────────────────

    /**
     * A plain resource class that carries no {@code @ProtectedCedarlingApi}
     * annotation and implements no interfaces.  Used to simulate "empty
     * permissions" scenarios.
     */
    static class PlainResource {
        public void plainMethod() { /* no-op */ }
    }

    /**
     * An interface-level {@code @ProtectedCedarlingApi} annotation.
     * The service resolves permissions by walking the resource class's
     * interface hierarchy, so placing the annotation here is the most
     * common real-world pattern.
     */
    @ProtectedCedarlingApi(
            action   = "Jans::Action::\"POST\"",
            resource = "Jans::HTTP_Request",
            id       = "lock_audit_log_write",
            path     = "/audit/log/bulk"
    )
    interface SecuredInterface {
        void securedMethod();
    }

    /** Resource class that inherits its permission from {@link SecuredInterface}. */
    static class SecuredResource implements SecuredInterface {
        @Override
        public void securedMethod() { /* no-op */ }
    }

    /**
     * A resource class that has a {@code @ProtectedCedarlingApi} on the class
     * itself AND on the method, producing two permissions.  Used to verify
     * short-circuit behaviour when the first permission is denied.
     */
    @ProtectedCedarlingApi(
            action   = "Jans::Action::\"POST\"",
            resource = "Jans::HTTP_Request",
            id       = "lock_audit_health_write",
            path     = "/audit/health/bulk"
    )
    static class MultiPermissionResource {
        @ProtectedCedarlingApi(
                action   = "Jans::Action::\"POST\"",
                resource = "Jans::HTTP_Request",
                id       = "lock_audit_log_write",
                path     = "/audit/log/bulk"
        )
        public void multiMethod() { /* no-op */ }
    }

    // ─── Setup ──────────────────────────────────────────────────────────────────

    /**
     * Injects the fields that are normally initialised in the {@code @PostConstruct}
     * lifecycle method.  We bypass the real {@code init()} to avoid network calls
     * during unit tests.
     */
    @BeforeEach
    void setUp() throws Exception {
        // Inject the mocked ObjectMapper (avoids real HTTP for JWKS fetches)
        injectField("mapper", mapper);

        // Build a minimal OpenID configuration stub
        OpenIdConfigurationResponse oidcConfig = mock(OpenIdConfigurationResponse.class);
        lenient().when(oidcConfig.getIssuer()).thenReturn(TEST_ISSUER);
        lenient().when(oidcConfig.getJwksUri()).thenReturn(TEST_JWKS_URI);
        injectField("oidcConfig", oidcConfig);
    }

    // ===========================================================================
    // 1. Missing / blank bearer token → 401 Unauthorized
    // ===========================================================================

    @Nested
    @DisplayName("Bearer token presence check")
    class BearerTokenPresenceTests {

        @Test
        @DisplayName("null token returns 401")
        void nullToken_returns401() {
            Response response = service.processAuthorization(null, mock(ResourceInfo.class));

            assertStatus(UNAUTHORIZED, response);
        }

        @Test
        @DisplayName("empty string token returns 401")
        void emptyToken_returns401() {
            Response response = service.processAuthorization("", mock(ResourceInfo.class));

            assertStatus(UNAUTHORIZED, response);
        }
    }

    // ===========================================================================
    // 2. No @ProtectedCedarlingApi annotation resolved → 500 Internal Server Error
    // ===========================================================================

    @Nested
    @DisplayName("Permission annotation resolution")
    class PermissionResolutionTests {

        @Test
        @DisplayName("resource with no annotation on class, method, or interfaces returns 500")
        void noAnnotation_returns500() {
            // PlainResource has no @ProtectedCedarlingApi and no interfaces,
            // so the permissions list stays empty.
            Response response = service.processAuthorization(
                    "Bearer some-token-value",
                    mockResourceInfo(PlainResource.class, "plainMethod"));

            assertStatus(INTERNAL_SERVER_ERROR, response);
            assertNotNull(response.getEntity(), "Error detail should be present in response body");
        }
    }

    // ===========================================================================
    // 3. Token is not a valid JWT → 403 Forbidden
    // ===========================================================================

    @Nested
    @DisplayName("JWT parsing")
    class JwtParsingTests {

        @Test
        @DisplayName("plain text token (not JWT-encoded) returns 403")
        void plainTextToken_returns403() {
            // Jwt.parse() will throw InvalidJwtException for a non-JWT string,
            // causing tokenAsJwt() to return null.
            Response response = service.processAuthorization(
                    "Bearer i-am-not-a-jwt",
                    mockResourceInfo(SecuredResource.class, "securedMethod"));

            assertStatus(FORBIDDEN, response);
            assertTrue(
                    response.getEntity().toString().contains("JWT"),
                    "Response body should mention JWT");
        }

        @Test
        @DisplayName("random base64 string that looks like JWT parts returns 403")
        void malformedBase64Token_returns403() {
            Response response = service.processAuthorization(
                    "Bearer aaa.bbb.ccc",
                    mockResourceInfo(SecuredResource.class, "securedMethod"));

            assertStatus(FORBIDDEN, response);
        }
    }

    // ===========================================================================
    // 4. JWT issuer mismatch → 403 Forbidden
    // ===========================================================================

    @Nested
    @DisplayName("JWT issuer validation")
    class IssuerValidationTests {

        @Test
        @DisplayName("JWT with wrong issuer returns 403")
        void wrongIssuer_returns403() throws Exception {
            try (MockedStatic<Jwt> jwtStatic = mockStatic(Jwt.class)) {
                Jwt jwt = buildMockJwt("https://untrusted-idp.example.com", futureEpoch(), SignatureAlgorithm.RS256);
                jwtStatic.when(() -> Jwt.parse(anyString())).thenReturn(jwt);

                Response response = service.processAuthorization(
                        "Bearer " + DUMMY_JWT,
                        mockResourceInfo(SecuredResource.class, "securedMethod"));

                assertStatus(FORBIDDEN, response);
                assertTrue(
                        response.getEntity().toString().contains("issuer"),
                        "Response body should mention 'issuer'");
            }
        }
    }

    // ===========================================================================
    // 5. Expired JWT → 403 Forbidden
    // ===========================================================================

    @Nested
    @DisplayName("JWT expiry validation")
    class ExpiryValidationTests {

        @Test
        @DisplayName("expired JWT returns 403")
        void expiredJwt_returns403() throws Exception {
            try (MockedStatic<Jwt> jwtStatic = mockStatic(Jwt.class)) {
                Jwt jwt = buildMockJwt(TEST_ISSUER, pastEpoch(), SignatureAlgorithm.RS256);
                jwtStatic.when(() -> Jwt.parse(anyString())).thenReturn(jwt);

                Response response = service.processAuthorization(
                        "Bearer " + DUMMY_JWT,
                        mockResourceInfo(SecuredResource.class, "securedMethod"));

                assertStatus(FORBIDDEN, response);
                assertTrue(
                        response.getEntity().toString().contains("Expired"),
                        "Response body should mention expiry");
            }
        }

        @Test
        @DisplayName("JWT with exp claim absent (defaults to epoch 0) returns 403")
        void missingExpClaim_returns403() throws Exception {
            try (MockedStatic<Jwt> jwtStatic = mockStatic(Jwt.class)) {
                // When getClaimAsInteger returns null, the service defaults to 0
                // (epoch 0 = 1970-01-01), so the token is immediately expired.
                Jwt jwt = buildMockJwtNullExp(TEST_ISSUER, SignatureAlgorithm.RS256);
                jwtStatic.when(() -> Jwt.parse(anyString())).thenReturn(jwt);

                Response response = service.processAuthorization(
                        "Bearer " + DUMMY_JWT,
                        mockResourceInfo(SecuredResource.class, "securedMethod"));

                assertStatus(FORBIDDEN, response);
            }
        }
    }

    // ===========================================================================
    // 6. HMAC-family algorithm → 500 Internal Server Error
    // ===========================================================================

    @Nested
    @DisplayName("Signing algorithm validation")
    class AlgorithmValidationTests {

        @Test
        @DisplayName("HS256 (HMAC) algorithm returns 500 with HMAC message")
        void hmacAlgorithmHS256_returns500() throws Exception {
            try (MockedStatic<Jwt> jwtStatic = mockStatic(Jwt.class)) {
                Jwt jwt = buildMockJwt(TEST_ISSUER, futureEpoch(), SignatureAlgorithm.HS256);
                jwtStatic.when(() -> Jwt.parse(anyString())).thenReturn(jwt);

                Response response = service.processAuthorization(
                        "Bearer " + DUMMY_JWT,
                        mockResourceInfo(SecuredResource.class, "securedMethod"));

                assertStatus(INTERNAL_SERVER_ERROR, response);
                assertTrue(
                        response.getEntity().toString().contains("HMAC"),
                        "Response body should explain that HMAC is not allowed");
            }
        }

        @Test
        @DisplayName("HS384 (HMAC) algorithm also returns 500")
        void hmacAlgorithmHS384_returns500() throws Exception {
            try (MockedStatic<Jwt> jwtStatic = mockStatic(Jwt.class)) {
                Jwt jwt = buildMockJwt(TEST_ISSUER, futureEpoch(), SignatureAlgorithm.HS384);
                jwtStatic.when(() -> Jwt.parse(anyString())).thenReturn(jwt);

                Response response = service.processAuthorization(
                        "Bearer " + DUMMY_JWT,
                        mockResourceInfo(SecuredResource.class, "securedMethod"));

                assertStatus(INTERNAL_SERVER_ERROR, response);
            }
        }
    }

    // ===========================================================================
    // 7. Signature verification fails → 403 Forbidden
    // ===========================================================================

    @Nested
    @DisplayName("JWT signature verification")
    class SignatureVerificationTests {

        @Test
        @DisplayName("invalid signature returns 403")
        void invalidSignature_returns403() throws Exception {
            try (MockedStatic<Jwt> jwtStatic = mockStatic(Jwt.class);
                 MockedConstruction<AuthCryptoProvider> cryptoMock = mockConstruction(
                         AuthCryptoProvider.class,
                         (mock, ctx) -> when(
                                 mock.verifySignature(any(), any(), any(), any(), any(), any()))
                                 .thenReturn(false))) {

                Jwt jwt = buildMockJwt(TEST_ISSUER, futureEpoch(), SignatureAlgorithm.RS256);
                jwtStatic.when(() -> Jwt.parse(anyString())).thenReturn(jwt);
                when(mapper.readValue(any(URL.class), eq(Map.class))).thenReturn(Map.of());

                Response response = service.processAuthorization(
                        "Bearer " + DUMMY_JWT,
                        mockResourceInfo(SecuredResource.class, "securedMethod"));

                assertStatus(FORBIDDEN, response);
            }
        }
    }

    // ===========================================================================
    // 8. Cedarling authorization decisions
    // ===========================================================================

    @Nested
    @DisplayName("Cedarling policy evaluation")
    class CedarlingAuthorizationTests {

        @Test
        @DisplayName("valid JWT + Cedarling grants access → null (pass-through)")
        void validJwtAndCedarlingGrants_returnsNull() throws Exception {
            try (MockedStatic<Jwt> jwtStatic = mockStatic(Jwt.class);
                 MockedConstruction<AuthCryptoProvider> cryptoMock = mockConstruction(
                         AuthCryptoProvider.class,
                         (mock, ctx) -> when(
                                 mock.verifySignature(any(), any(), any(), any(), any(), any()))
                                 .thenReturn(true))) {

                Jwt jwt = buildMockJwt(TEST_ISSUER, futureEpoch(), SignatureAlgorithm.RS256);
                jwtStatic.when(() -> Jwt.parse(anyString())).thenReturn(jwt);
                when(mapper.readValue(any(URL.class), eq(Map.class))).thenReturn(Map.of());
                when(authorizationService.authorize(anyMap(), anyString(), anyMap(), anyMap()))
                        .thenReturn(true);

                Response response = service.processAuthorization(
                        "Bearer " + DUMMY_JWT,
                        mockResourceInfo(SecuredResource.class, "securedMethod"));

                assertNull(response, "Granted access must pass through (null response)");
            }
        }

        @Test
        @DisplayName("valid JWT + Cedarling denies access → 403")
        void validJwtAndCedarlingDenies_returns403() throws Exception {
            try (MockedStatic<Jwt> jwtStatic = mockStatic(Jwt.class);
                 MockedConstruction<AuthCryptoProvider> cryptoMock = mockConstruction(
                         AuthCryptoProvider.class,
                         (mock, ctx) -> when(
                                 mock.verifySignature(any(), any(), any(), any(), any(), any()))
                                 .thenReturn(true))) {

                Jwt jwt = buildMockJwt(TEST_ISSUER, futureEpoch(), SignatureAlgorithm.RS256);
                jwtStatic.when(() -> Jwt.parse(anyString())).thenReturn(jwt);
                when(mapper.readValue(any(URL.class), eq(Map.class))).thenReturn(Map.of());
                when(authorizationService.authorize(anyMap(), anyString(), anyMap(), anyMap()))
                        .thenReturn(false);

                Response response = service.processAuthorization(
                        "Bearer " + DUMMY_JWT,
                        mockResourceInfo(SecuredResource.class, "securedMethod"));

                assertStatus(FORBIDDEN, response);
            }
        }

        /**
         * When a resource has multiple permissions (class-level + method-level) and the
         * first call to {@code authorizationService.authorize()} returns {@code false},
         * the service must short-circuit and not evaluate further permissions.
         */
        @Test
        @DisplayName("first permission denied short-circuits remaining checks")
        void multiplePermissions_firstDenied_doesNotCheckRemainder() throws Exception {
            try (MockedStatic<Jwt> jwtStatic = mockStatic(Jwt.class);
                 MockedConstruction<AuthCryptoProvider> cryptoMock = mockConstruction(
                         AuthCryptoProvider.class,
                         (mock, ctx) -> when(
                                 mock.verifySignature(any(), any(), any(), any(), any(), any()))
                                 .thenReturn(true))) {

                Jwt jwt = buildMockJwt(TEST_ISSUER, futureEpoch(), SignatureAlgorithm.RS256);
                jwtStatic.when(() -> Jwt.parse(anyString())).thenReturn(jwt);
                when(mapper.readValue(any(URL.class), eq(Map.class))).thenReturn(Map.of());

                // First call is denied; second should never be reached
                when(authorizationService.authorize(anyMap(), anyString(), anyMap(), anyMap()))
                        .thenReturn(false);

                Response response = service.processAuthorization(
                        "Bearer " + DUMMY_JWT,
                        mockResourceInfo(MultiPermissionResource.class, "multiMethod"));

                assertStatus(FORBIDDEN, response);
                // MultiPermissionResource has 2 permissions; the service must stop after
                // the first denied result — so authorize() is called exactly once.
                verify(authorizationService, times(1))
                        .authorize(anyMap(), anyString(), anyMap(), anyMap());
            }
        }

        @Test
        @DisplayName("both permissions granted returns null")
        void multiplePermissions_allGranted_returnsNull() throws Exception {
            try (MockedStatic<Jwt> jwtStatic = mockStatic(Jwt.class);
                 MockedConstruction<AuthCryptoProvider> cryptoMock = mockConstruction(
                         AuthCryptoProvider.class,
                         (mock, ctx) -> when(
                                 mock.verifySignature(any(), any(), any(), any(), any(), any()))
                                 .thenReturn(true))) {

                Jwt jwt = buildMockJwt(TEST_ISSUER, futureEpoch(), SignatureAlgorithm.RS256);
                jwtStatic.when(() -> Jwt.parse(anyString())).thenReturn(jwt);
                when(mapper.readValue(any(URL.class), eq(Map.class))).thenReturn(Map.of());
                when(authorizationService.authorize(anyMap(), anyString(), anyMap(), anyMap()))
                        .thenReturn(true);

                Response response = service.processAuthorization(
                        "Bearer " + DUMMY_JWT,
                        mockResourceInfo(MultiPermissionResource.class, "multiMethod"));

                assertNull(response, "Granted access must pass through (null response)");
                // Exactly 2 permissions → authorize() called twice
                verify(authorizationService, times(2))
                        .authorize(anyMap(), anyString(), anyMap(), anyMap());
            }
        }
    }

    // ===========================================================================
    // 9. Unexpected runtime exception → 500 Internal Server Error
    // ===========================================================================

    @Nested
    @DisplayName("Exception handling")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("unexpected exception returns 500 with exception message")
        void unexpectedException_returns500() throws Exception {
            try (MockedStatic<Jwt> jwtStatic = mockStatic(Jwt.class)) {
                jwtStatic.when(() -> Jwt.parse(anyString()))
                         .thenThrow(new RuntimeException("Unexpected adapter failure"));

                Response response = service.processAuthorization(
                        "Bearer " + DUMMY_JWT,
                        mockResourceInfo(SecuredResource.class, "securedMethod"));

                assertStatus(INTERNAL_SERVER_ERROR, response);
                assertEquals(
                        "Unexpected adapter failure",
                        response.getEntity().toString(),
                        "Exception message must be surfaced in the response body");
            }
        }

        @Test
        @DisplayName("JWKS fetch failure returns 500")
        void jwksFetchFailure_returns500() throws Exception {
            try (MockedStatic<Jwt> jwtStatic = mockStatic(Jwt.class)) {
                Jwt jwt = buildMockJwt(TEST_ISSUER, futureEpoch(), SignatureAlgorithm.RS256);
                jwtStatic.when(() -> Jwt.parse(anyString())).thenReturn(jwt);

                // Simulate an I/O error when fetching the JWKS endpoint
                when(mapper.readValue(any(URL.class), eq(Map.class)))
                        .thenThrow(new java.io.IOException("Connection refused"));

                Response response = service.processAuthorization(
                        "Bearer " + DUMMY_JWT,
                        mockResourceInfo(SecuredResource.class, "securedMethod"));

                assertStatus(INTERNAL_SERVER_ERROR, response);
            }
        }
    }

    // ===========================================================================
    // Helper methods
    // ===========================================================================

    /**
     * Injects a value into a private field of the service under test using reflection.
     *
     * @param fieldName the exact name of the private field
     * @param value     the value to inject
     */
    private void injectField(String fieldName, Object value) throws Exception {
        Field field = CedarlingProtectionService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(service, value);
    }

    /**
     * Creates a {@link ResourceInfo} mock that returns the specified class and method.
     *
     * @param resourceClass the JAX-RS resource class to return
     * @param methodName    the public method name to return
     * @return a configured {@link ResourceInfo} mock
     */
    @SuppressWarnings("unchecked")
    private static ResourceInfo mockResourceInfo(Class<?> resourceClass, String methodName) {
        ResourceInfo ri = mock(ResourceInfo.class);
        try {
            when(ri.getResourceClass()).thenReturn((Class) resourceClass);
            when(ri.getResourceMethod()).thenReturn(resourceClass.getMethod(methodName));
        } catch (NoSuchMethodException e) {
            fail("Test fixture error – method not found: " + resourceClass.getSimpleName() + "#" + methodName);
        }
        return ri;
    }

    /**
     * Asserts that the {@link Response} has the expected HTTP status code.
     *
     * @param expected expected status
     * @param actual   actual response (must not be null)
     */
    private static void assertStatus(Response.Status expected, Response actual) {
        assertNotNull(actual, "Expected a response but got null (pass-through)");
        assertEquals(
                expected.getStatusCode(),
                actual.getStatus(),
                String.format("Expected HTTP %d (%s) but got %d",
                        expected.getStatusCode(), expected, actual.getStatus()));
    }

    /**
     * Builds a fully mocked {@link Jwt} with the specified issuer, expiry, and
     * signing algorithm.  All fields needed by the service under test are stubbed.
     *
     * @param issuer    value for the {@code iss} JWT claim
     * @param expEpoch  value for the {@code exp} JWT claim (epoch seconds)
     * @param algorithm signing algorithm to report via the JWT header
     * @return a mocked {@link Jwt}
     * @throws InvalidJwtException 
     */
    private static Jwt buildMockJwt(String issuer, int expEpoch, SignatureAlgorithm algorithm) throws InvalidJwtException {
        Jwt jwt = mock(Jwt.class);

        // Stub JWT header
        JwtHeader header = mock(JwtHeader.class);
        lenient().when(header.getSignatureAlgorithm()).thenReturn(algorithm);
        lenient().when(header.getKeyId()).thenReturn("test-key-id");
        lenient().when(jwt.getHeader()).thenReturn(header);

        // Stub JWT claims
        JwtClaims claims = mock(JwtClaims.class);
        lenient().when(claims.getClaimAsString(JwtClaimName.ISSUER)).thenReturn(issuer);
        lenient().when(claims.getClaimAsInteger(JwtClaimName.EXPIRATION_TIME)).thenReturn(expEpoch);
        lenient().when(jwt.getClaims()).thenReturn(claims);

        // Stub signing material (needed for signature verification)
        lenient().when(jwt.getSigningInput()).thenReturn("signing.input");
        lenient().when(jwt.getEncodedSignature()).thenReturn("encoded-signature");

        return jwt;
    }

    /**
     * Same as {@link #buildMockJwt} but returns {@code null} for the {@code exp}
     * claim, which the service defaults to epoch 0 (i.e. immediately expired).
     *
     * @param issuer    value for the {@code iss} JWT claim
     * @param algorithm signing algorithm to report via the JWT header
     * @return a mocked {@link Jwt} with a null {@code exp} claim
     * @throws InvalidJwtException 
     */
    private static Jwt buildMockJwtNullExp(String issuer, SignatureAlgorithm algorithm) throws InvalidJwtException {
        Jwt jwt = mock(Jwt.class);

        JwtHeader header = mock(JwtHeader.class);
        lenient().when(header.getSignatureAlgorithm()).thenReturn(algorithm);
        lenient().when(header.getKeyId()).thenReturn("test-key-id");
        lenient().when(jwt.getHeader()).thenReturn(header);

        JwtClaims claims = mock(JwtClaims.class);
        lenient().when(claims.getClaimAsString(JwtClaimName.ISSUER)).thenReturn(issuer);
        // null triggers Optional.orElse(0) in the service → token is expired
        lenient().when(claims.getClaimAsInteger(JwtClaimName.EXPIRATION_TIME)).thenReturn(null);
        lenient().when(jwt.getClaims()).thenReturn(claims);

        lenient().when(jwt.getSigningInput()).thenReturn("signing.input");
        lenient().when(jwt.getEncodedSignature()).thenReturn("encoded-signature");

        return jwt;
    }

    /**
     * Returns an {@code exp} value (epoch seconds) 1 hour in the future.
     * Tokens with this value are not yet expired.
     */
    private static int futureEpoch() {
        return (int) (System.currentTimeMillis() / 1000) + 3600;
    }

    /**
     * Returns an {@code exp} value (epoch seconds) 1 hour in the past.
     * Tokens with this value are expired.
     */
    private static int pastEpoch() {
        return (int) (System.currentTimeMillis() / 1000) - 3600;
    }
}