/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */
package io.jans.core.cedarling.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.core.cedarling.BaseCedarlingTest;
import io.jans.core.cedarling.config.BootstrapConfig;
import io.jans.core.cedarling.model.CedarlingConfiguration;
import io.jans.core.cedarling.model.CedarlingPermission;
import io.jans.core.cedarling.model.LogLevel;
import io.jans.core.cedarling.model.LogType;
import io.jans.core.cedarling.service.policy.PolicyStoreFileProvider;

/**
 * Integration tests for {@link CedarlingAuthorizationService}.
 *
 * <p>These tests exercise the Cedarling policy engine end-to-end using real JWT access
 * tokens and a real policy store loaded from {@code src/test/resources/lock_policy_store.json}.
 *
 * <h3>Token matrix</h3>
 * <pre>
 * ┌────────┬──────────────────────────────────────────────────────────────┬──────────────────────────────┐
 * │ Token  │ OAuth scopes                                                 │ Allowed endpoints            │
 * ├────────┼──────────────────────────────────────────────────────────────┼──────────────────────────────┤
 * │ JWT 1  │ lock/health.write, lock/telemetry.write, lock/log.write      │ /log, /health, /telemetry    │
 * │ JWT 2  │ lock/health.write                                            │ /health only                 │
 * │ JWT 3  │ lock/log.write                                               │ /log only                    │
 * └────────┴──────────────────────────────────────────────────────────────┴──────────────────────────────┘
 * </pre>
 *
 * <p><strong>JWT expiry:</strong> The tokens are real tokens whose {@code exp} claim is in
 * the past.  Before each test run the payloads are patched (base64-decode → update exp →
 * base64-encode) to a value 1 hour in the future.  Because the adapter is initialised with
 * {@code jwtSigValidation(false)} the modified signature is still accepted by Cedarling.
 *
 * <p><strong>Prerequisites:</strong> The native Cedarling UniFFI library must be on the
 * dynamic-library path ({@code LD_LIBRARY_PATH} / {@code java.library.path}).
 * 
 * @author Yuriy Movchan Date: 12/05/2026
 */
@TestInstance(Lifecycle.PER_CLASS)
@DisplayName("CedarlingAuthorizationService – Integration Tests")
class CedarlingAuthorizationServiceIntegrationTest extends BaseCedarlingTest {

	static {
		Configurator.setRootLevel(Level.INFO);
	}

	private static final Logger log = LoggerFactory.getLogger(CedarlingAuthorizationServiceIntegrationTest.class);

    // ─── Raw JWT strings (exp claims are patched at runtime) ────────────────────

    /**
     * JWT 1 – contains all three scopes:
     * {@code health.write}, {@code telemetry.write}, {@code log.write}.
     */
    private static final String RAW_JWT_1 =
            "eyJraWQiOiJjb25uZWN0XzNjMzJjMTkzLTY5ZjYtNDBkYS1iNmQyLTE3ODY0YmJkYzU1MV9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9"
            + ".eyJhdWQiOiIwZTk5NDAzMC1lZTg4LTRmMTQtYWUwOC04YjBkYTEwNDQwMjkiLCJzdWIiOiIwZTk5NDAzMC1lZTg4LTRmMTQtYWUwOC04YjBkYTEwNDQwMjkiLCJ4NXQjUzI1NiI6IiIsIm5iZiI6MTc3ODYxNDA2Niwic2NvcGUiOlsiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2xvY2svaGVhbHRoLndyaXRlIiwiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2xvY2svdGVsZW1ldHJ5LndyaXRlIiwiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2xvY2svbG9nLndyaXRlIl0sImlzcyI6Imh0dHBzOi8vamFucy1teXNxbC1sb2NrLXNlcnZlci5qYW5zLmluZm8iLCJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiZXhwIjoxNzc4NjE0MzY2LCJpYXQiOjE3Nzg2MTQwNjYsImNsaWVudF9pZCI6IjBlOTk0MDMwLWVlODgtNGYxNC1hZTA4LThiMGRhMTA0NDAyOSIsImp0aSI6IkhNdjluYnBiUkRLRTAzNVRUTkgwT3ciLCJzdGF0dXMiOnsic3RhdHVzX2xpc3QiOnsiaWR4Ijo0MDAsInVyaSI6Imh0dHBzOi8vamFucy1teXNxbC1sb2NrLXNlcnZlci5qYW5zLmluZm8vamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9fX0"
            + ".bNXIL4f1lqvLoS49iMSZJORD2mJ9MWYCM5a8nyAiLqKy_fEqvqb-g1X6SgVeS2dJ9aFV-KRrfcjl0zSSQq6mBn-1pAostlMgV-lkOBi7rCbJUMwmdN7Bv7Op8EyuD44_4hHRYhAXOXYv1CcjkyXtv-A9gDxNjHvhHVvpjaizcIMXVRrPxTTQgZF7r7n0t13La2E0vOxzzsgcWQjJukAY8HYybtoRL4JFswBIWPcgET9Btg9mZghDMlvs0yiLVQfiGUZYcmxCCEQinjtutKgONP0Gv6xVMdsXMUpgXGZi6PCiEaEWButMwBauc9RJWEHbd7C4muKoAQ6_tFNuS_eoRw";

    /**
     * JWT 2 – contains only {@code health.write} scope.
     */
    private static final String RAW_JWT_2 =
            "eyJraWQiOiJjb25uZWN0XzNjMzJjMTkzLTY5ZjYtNDBkYS1iNmQyLTE3ODY0YmJkYzU1MV9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9"
            + ".eyJhdWQiOiIwZTk5NDAzMC1lZTg4LTRmMTQtYWUwOC04YjBkYTEwNDQwMjkiLCJzdWIiOiIwZTk5NDAzMC1lZTg4LTRmMTQtYWUwOC04YjBkYTEwNDQwMjkiLCJ4NXQjUzI1NiI6IiIsIm5iZiI6MTc3ODYxNDE2MSwic2NvcGUiOlsiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2xvY2svaGVhbHRoLndyaXRlIl0sImlzcyI6Imh0dHBzOi8vamFucy1teXNxbC1sb2NrLXNlcnZlci5qYW5zLmluZm8iLCJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiZXhwIjoxNzc4NjE0NDYxLCJpYXQiOjE3Nzg2MTQxNjEsImNsaWVudF9pZCI6IjBlOTk0MDMwLWVlODgtNGYxNC1hZTA4LThiMGRhMTA0NDAyOSIsImp0aSI6InRVMGVQb0haU0RPSjJ5Z0EyZFRnc3ciLCJzdGF0dXMiOnsic3RhdHVzX2xpc3QiOnsiaWR4Ijo0MDEsInVyaSI6Imh0dHBzOi8vamFucy1teXNxbC1sb2NrLXNlcnZlci5qYW5zLmluZm8vamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9fX0"
            + ".iIN4rKz4wnGgijhNWJqY_RqYNx_7zT0hdevnU6wqRwiLp3rQG3c4ouv8P6X4CbiaxERzABbrjsS-4JcW2H2oLpAsuJGhJtr-HExe3iLs_OQ2_4NDwo0k2KJ5e_zGP6Wykr6mQ8WhvGIfURk1aLirLCsegKhH1b26tSp6i8z7z-etNLwGjVPDfw6vV01kYJ0_O_tSf0HuLkGTPf34ld86CUNbPf2cE9Q4uqX_3xVTtMW0ffmOhDo8Qs2dL96xs8O6ah-Rvp6UVjcD4A1qbVImN6USE70nEndmtDR_rvfsCBiL-htkgChTDZymceTcOn00NOvWB2I00rvSy7FdWwNAFQ";

    /**
     * JWT 3 – contains only {@code log.write} scope.
     */
    private static final String RAW_JWT_3 =
            "eyJraWQiOiJjb25uZWN0XzNjMzJjMTkzLTY5ZjYtNDBkYS1iNmQyLTE3ODY0YmJkYzU1MV9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9"
            + ".eyJhdWQiOiIwZTk5NDAzMC1lZTg4LTRmMTQtYWUwOC04YjBkYTEwNDQwMjkiLCJzdWIiOiIwZTk5NDAzMC1lZTg4LTRmMTQtYWUwOC04YjBkYTEwNDQwMjkiLCJ4NXQjUzI1NiI6IiIsIm5iZiI6MTc3ODYxNzc2MSwic2NvcGUiOlsiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2xvY2svbG9nLndyaXRlIl0sImlzcyI6Imh0dHBzOi8vamFucy1teXNxbC1sb2NrLXNlcnZlci5qYW5zLmluZm8iLCJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiZXhwIjoxNzc4NjE4MDYxLCJpYXQiOjE3Nzg2MTc3NjEsImNsaWVudF9pZCI6IjBlOTk0MDMwLWVlODgtNGYxNC1hZTA4LThiMGRhMTA0NDAyOSIsImp0aSI6IjVxUWRHYWl4VEgybkQ5Z0Y2WDFPaXciLCJzdGF0dXMiOnsic3RhdHVzX2xpc3QiOnsiaWR4Ijo1MDAsInVyaSI6Imh0dHBzOi8vamFucy1teXNxbC1sb2NrLXNlcnZlci5qYW5zLmluZm8vamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9fX0"
            + ".Q7xieptgb5r9eXqjI5BCSDv_ITtzZXbsXoyqcjsYw0PonF6z3c5XjiSPPrXVUU9dY_HQUrd4ib3U7oIQrKtfXcjJ2pMNuTZ0vPRCcZM_XqqbV3IewUbztabDKDNpK0pSaNZy9V1SslHjW_vQoVDnclJL-w2usyXlMVnFub92GV3ldBZ9cB4UYVRovrzG_UxCa8FI-WkikYoET-vIiHbS5yP3EXlRKwP2pWwhHKwhAC7sjbnYW8ApgYVAmvAnWqwPcaY_Bl-UobDHGBr0b0FhLtMIZvGevo1KdQE5dJwiflZOgiUZiYJU9uJ-tklD2gd5Pq-7g1-DW9Fvsmo2WVDcHw";

    // ─── @ProtectedCedarlingApi annotation parameter constants ──────────────────

    /** Cedar action that maps to HTTP POST requests. */
    private static final String ACTION_POST      = "Jans::Action::\"POST\"";

    /** Cedar entity type for HTTP requests (note: trailing quote is intentional,
     *  it matches the value used in the real @ProtectedCedarlingApi annotations). */
    private static final String RESOURCE_TYPE    = "Jans::HTTP_Request";

    // Permission IDs – must match the {@code id} attribute of each annotation
    private static final String ID_LOG           = "lock_audit_log_write";
    private static final String ID_HEALTH        = "lock_audit_health_write";
    private static final String ID_TELEMETRY     = "lock_audit_telemetry_write";

    // Endpoint paths – must match the {@code path} attribute of each annotation
    private static final String PATH_LOG         = "/audit/log/bulk";
    private static final String PATH_HEALTH      = "/audit/health/bulk";
    private static final String PATH_TELEMETRY   = "/audit/health/bulk";

    /**
     * The token map key expected by Cedarling's native authorize input.
     * Taken verbatim from {@code CedarlingAuthorizationService.CEDARLING_JANS_ACCESS_TOKEN}.
     */
    private static final String ACCESS_TOKEN_KEY = CedarlingAuthorizationService.CEDARLING_JANS_ACCESS_TOKEN;

    // ─── Service under test + runtime-patched tokens ────────────────────────────

    private CedarlingAuthorizationService authService;

    /**
     * JWT tokens with their {@code exp} claims patched to one hour in the future.
     * Patched once before all tests to keep the value stable across the entire run.
     */
    private String jwt1;
    private String jwt2;
    private String jwt3;

    // ─── Lifecycle ──────────────────────────────────────────────────────────────

    /**
     * Performs one-time initialisation before any test in this class runs:
     * <ol>
     *   <li>Loads {@code lock_policy_store.json} from the test classpath.</li>
     *   <li>Creates a fully wired {@link CedarlingAuthorizationService} via field
     *       injection (bypassing CDI) and calls its {@code @PostConstruct} method.</li>
     *   <li>Patches the {@code exp} claims of all three JWT tokens.</li>
     * </ol>
     */
    @BeforeAll
    void setUpServiceAndTokens() throws Exception {
        // ── 1. Load policy store from test resources ──────────────────────────
		String policyStoreFn = System.getProperty("user.dir") + "/target/test-classes/test-policy-store";
		PolicyStoreFileProvider mockPolicyStoreProvider = mock(PolicyStoreFileProvider.class);
		when(mockPolicyStoreProvider.getPolicyStorePath()).thenReturn(policyStoreFn);

        // ── 2. Build mocked CDI dependencies ─────────────────────────────────
        Logger                    svcLog              = LoggerFactory.getLogger(CedarlingAuthorizationService.class);
        CedarlingConfiguration    cedarConf        = mock(CedarlingConfiguration.class);

        // ── 3. Wire the service manually (CDI is not available in unit tests) ─
		authService = new CedarlingAuthorizationService() {
			@Override
			protected BootstrapConfig prepareBootstrapConfig(CedarlingConfiguration cedarConf) {
				// Delegate to the standard builder; JWT validation is disabled for tests
				BootstrapConfig bootstrapConfig = BootstrapConfig.builder().applicationName("Lock Server - Test Edition").policyStoreLocalFn(policyStoreFn).jwtStatusValidation(false)
						.jwtSigValidation(false).logType(LogType.STD_OUT).logLevel(LogLevel.TRACE).lock(false).build();

				log.info("Cedarling bootstrap configuration: {}", bootstrapConfig.toJsonConfig());
				return bootstrapConfig;
			}
		};

		when(cedarConf.isEnabled()).thenReturn(true);

		injectField(authService, "log", svcLog);
		injectField(authService, "cedarConf", cedarConf);
		injectField(authService, "cedarlingPolicyStoreFileProvider", mockPolicyStoreProvider);

        // Trigger @PostConstruct – initialises CedarlingAdapter with the real policy
        authService.init();

        // ── 4. Patch exp claims so the tokens are not expired during the test run
        jwt1 = withFutureExp(RAW_JWT_1);
        jwt2 = withFutureExp(RAW_JWT_2);
        jwt3 = withFutureExp(RAW_JWT_3);
    }

    /** Calls the adapter's {@code close()} after the entire test class completes. */
    @AfterAll
    void tearDown() {
        if (authService != null) {
            authService.destroy();
        }
    }

    // ===========================================================================
    // JWT 1 – scopes: health.write  +  telemetry.write  +  log.write
    // Expected: all three endpoints are ALLOWED
    // ===========================================================================
    @Nested
    @DisplayName("JWT 1 (health.write + telemetry.write + log.write)")
    class Jwt1Tests {

        @Test
        @DisplayName("POST /audit/log/bulk   → ALLOWED (log.write scope present)")
        void jwt1_logEndpoint_isAllowed() {
            assertTrue(
                    authorize(jwt1, ACTION_POST, ID_LOG, PATH_LOG),
                    "JWT 1 must be authorized for /audit/log/bulk");
        }

        @Test
        @DisplayName("POST /audit/health/bulk → ALLOWED (health.write scope present)")
        void jwt1_healthEndpoint_isAllowed() {
            assertTrue(
                    authorize(jwt1, ACTION_POST, ID_HEALTH, PATH_HEALTH),
                    "JWT 1 must be authorized for /audit/health/bulk");
        }

        @Test
        @DisplayName("POST /audit/telemetry  → ALLOWED (telemetry.write scope present)")
        void jwt1_telemetryEndpoint_isAllowed() {
            assertTrue(
                    authorize(jwt1, ACTION_POST, ID_TELEMETRY, PATH_TELEMETRY),
                    "JWT 1 must be authorized for /audit/telemetry");
        }
    }

    // ===========================================================================
    // JWT 2 – scopes: health.write only
    // Expected: /health endpoint ALLOWED; /log and /telemetry endpoints DENIED
    // ===========================================================================
    @Nested
    @DisplayName("JWT 2 (health.write only)")
    class Jwt2Tests {

        @Test
        @DisplayName("POST /audit/health/bulk → ALLOWED (health.write scope present)")
        void jwt2_healthEndpoint_isAllowed() {
            assertTrue(
                    authorize(jwt2, ACTION_POST, ID_HEALTH, PATH_HEALTH),
                    "JWT 2 must be authorized for /audit/health/bulk");
        }

        @Test
        @DisplayName("POST /audit/log/bulk   → DENIED (log.write scope absent)")
        void jwt2_logEndpoint_isDenied() {
            assertFalse(
                    authorize(jwt2, ACTION_POST, ID_LOG, PATH_LOG),
                    "JWT 2 must NOT be authorized for /audit/log/bulk (missing log.write)");
        }

        @Test
        @DisplayName("POST /audit/telemetry  → DENIED (telemetry.write scope absent)")
        void jwt2_telemetryEndpoint_isDenied() {
            assertFalse(
                    authorize(jwt2, ACTION_POST, ID_TELEMETRY, PATH_TELEMETRY),
                    "JWT 2 must NOT be authorized for /audit/telemetry (missing telemetry.write)");
        }
    }

    // ===========================================================================
    // JWT 3 – scopes: log.write only
    // Expected: /log endpoint ALLOWED; /health and /telemetry endpoints DENIED
    // ===========================================================================
    @Nested
    @DisplayName("JWT 3 (log.write only)")
    class Jwt3Tests {

        @Test
        @DisplayName("POST /audit/log/bulk   → ALLOWED (log.write scope present)")
        void jwt3_logEndpoint_isAllowed() {
            assertTrue(
                    authorize(jwt3, ACTION_POST, ID_LOG, PATH_LOG),
                    "JWT 3 must be authorized for /audit/log/bulk");
        }

        @Test
        @DisplayName("POST /audit/health/bulk → DENIED (health.write scope absent)")
        void jwt3_healthEndpoint_isDenied() {
            assertFalse(
                    authorize(jwt3, ACTION_POST, ID_HEALTH, PATH_HEALTH),
                    "JWT 3 must NOT be authorized for /audit/health/bulk (missing health.write)");
        }

        @Test
        @DisplayName("POST /audit/telemetry  → DENIED (telemetry.write scope absent)")
        void jwt3_telemetryEndpoint_isDenied() {
            assertFalse(
                    authorize(jwt3, ACTION_POST, ID_TELEMETRY, PATH_TELEMETRY),
                    "JWT 3 must NOT be authorized for /audit/telemetry (missing telemetry.write)");
        }
    }

    // ===========================================================================
    // Helpers
    // ===========================================================================

    /**
     * Calls {@link CedarlingAuthorizationService#authorize} with a resource map
     * built the same way as {@code CedarlingProtectionService.getCedarlingResource()}.
     *
     * @param accessToken the JWT access token (after exp patching)
     * @param action      Cedar action string (e.g. {@code Jans::Action::"POST"})
     * @param permId      permission id from {@code @ProtectedCedarlingApi.id()}
     * @param path        endpoint path from {@code @ProtectedCedarlingApi.path()}
     * @return {@code true} when Cedarling grants the request
     */
	private boolean authorize(String accessToken, String action, String permId, String path) {
		Map<String, String> tokens = Map.of(ACCESS_TOKEN_KEY, accessToken);

		return authService.authorize(tokens, action, buildResource(permId, path), buildContext());
	}

    /**
     * Constructs the Cedar resource map that mirrors what
     * {@code CedarlingProtectionService.getCedarlingResource()} produces at runtime.
     *
     * <p>The numeric {@code id} inside {@code cedar_entity_mapping} is derived from the
     * {@link CedarlingPermission#hashCode()} of a permission built with the supplied
     * arguments, matching the production code exactly.
     *
     * @param permId the permission ID ({@code @ProtectedCedarlingApi.id()})
     * @param path   the endpoint path ({@code @ProtectedCedarlingApi.path()})
     * @return resource map ready for {@code CedarlingAuthorizationService.authorize()}
     */
    private static Map<String, Object> buildResource(String permId, String path) {
        Map<String, Object> resource = new HashMap<>();
        resource.put("cedar_entity_mapping",
                Map.of("entity_type", RESOURCE_TYPE, "id", permId));
        resource.put("url",
                Map.of("host", "", "path", path, "protocol", ""));
        resource.put("header", Collections.emptyMap());

        return resource;
    }

    /**
     * Returns an empty context map.  The production code also sends an empty map,
     * so we match that behaviour here.
     */
    private static Map<String, Object> buildContext() {
        return Collections.emptyMap();
    }
}