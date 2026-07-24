/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.core.cedarling.service;

import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import io.jans.core.cedarling.service.security.api.ProtectedCedarlingApi;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;

/**
 * Unit tests for the abstract {@link CedarlingProtectionService} base class, exercised through the test-only
 * {@link DummyCedarlingProtectionService} subclass.
 *
 * <p>The base class implements the transport-agnostic part of the authorization filter:
 * <ol>
 *   <li>Permission resolution – at least one {@code @ProtectedCedarlingApi} annotation
 *       must be found on the JAX-RS resource class, method, or its interfaces.</li>
 *   <li>Cedarling policy evaluation – the Cedarling engine must grant access
 *       for every declared permission.</li>
 * </ol>
 *
 * <p>JWT validation (issuer, expiry, signature) is a concern of production subclasses (e.g. the Lock Server one) and
 * is tested there. {@link DummyCedarlingProtectionService} only adds the bearer-token presence check and delegates
 * straight to {@link CedarlingProtectionService#isValid}.
 *
 * <p>A {@code null} return value from {@code processAuthorization} means
 * "pass-through" – the JAX-RS filter will allow the request to continue.
 *
 * @author Author Date: 12/05/2026
 */
@ExtendWith(MockitoExtension.class)
class CedarlingProtectionServiceTest {

    // ─── Constants ──────────────────────────────────────────────────────────────

    /** Minimal three-part placeholder used wherever a JWT string is required. */
    private static final String DUMMY_JWT = "header.payload.signature";

    // ─── Mocks & SUT ────────────────────────────────────────────────────────────

    @Mock private Logger                        log;
    @Mock private CedarlingAuthorizationService authorizationService;

    @InjectMocks
    private DummyCedarlingProtectionService service;

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
    // 3. Cedarling authorization decisions
    // ===========================================================================

    @Nested
    @DisplayName("Cedarling policy evaluation")
    class CedarlingAuthorizationTests {

        @Test
        @DisplayName("Cedarling grants access → null (pass-through)")
        void cedarlingGrants_returnsNull() {
            when(authorizationService.authorize(anyMap(), anyString(), anyMap(), anyMap()))
                    .thenReturn(true);

            Response response = service.processAuthorization(
                    "Bearer " + DUMMY_JWT,
                    mockResourceInfo(SecuredResource.class, "securedMethod"));

            assertNull(response, "Granted access must pass through (null response)");
        }

        @Test
        @DisplayName("Cedarling denies access → 403")
        void cedarlingDenies_returns403() {
            when(authorizationService.authorize(anyMap(), anyString(), anyMap(), anyMap()))
                    .thenReturn(false);

            Response response = service.processAuthorization(
                    "Bearer " + DUMMY_JWT,
                    mockResourceInfo(SecuredResource.class, "securedMethod"));

            assertStatus(FORBIDDEN, response);
        }

        /**
         * The bearer token must reach Cedarling with the {@code Bearer } prefix stripped —
         * Cedarling itself performs the (test-disabled) JWT validation and the scope-based
         * decision, so the token cannot simply be ignored.
         */
        @Test
        @DisplayName("bearer token is forwarded to Cedarling without the 'Bearer ' prefix")
        void bearerToken_forwardedToCedarling() {
            when(authorizationService.authorize(anyMap(), anyString(), anyMap(), anyMap()))
                    .thenReturn(true);

            service.processAuthorization(
                    "Bearer " + DUMMY_JWT,
                    mockResourceInfo(SecuredResource.class, "securedMethod"));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, String>> tokensCaptor = ArgumentCaptor.forClass(Map.class);
            verify(authorizationService).authorize(tokensCaptor.capture(), anyString(), anyMap(), anyMap());

            assertEquals(
                    DUMMY_JWT,
                    tokensCaptor.getValue().get(CedarlingAuthorizationService.CEDARLING_JANS_ACCESS_TOKEN),
                    "Raw token (without 'Bearer ' prefix) must be passed to Cedarling");
        }

        /**
         * When a resource has multiple permissions (class-level + method-level) and the
         * first call to {@code authorizationService.authorize()} returns {@code false},
         * the service must short-circuit and not evaluate further permissions.
         */
        @Test
        @DisplayName("first permission denied short-circuits remaining checks")
        void multiplePermissions_firstDenied_doesNotCheckRemainder() {
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

        @Test
        @DisplayName("both permissions granted returns null")
        void multiplePermissions_allGranted_returnsNull() {
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

    // ===========================================================================
    // 4. Unexpected runtime exception → 500 Internal Server Error
    // ===========================================================================

    @Nested
    @DisplayName("Exception handling")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("unexpected exception returns 500 with exception message")
        void unexpectedException_returns500() {
            when(authorizationService.authorize(anyMap(), anyString(), anyMap(), anyMap()))
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

    // ===========================================================================
    // Helper methods
    // ===========================================================================

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
}
