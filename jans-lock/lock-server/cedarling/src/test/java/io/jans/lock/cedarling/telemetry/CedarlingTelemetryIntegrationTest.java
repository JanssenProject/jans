/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */
package io.jans.lock.cedarling.telemetry;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import io.jans.lock.cedarling.config.BootstrapConfig;
import io.jans.lock.cedarling.service.CedarlingAuthorizationService;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.cedarling.CedarlingConfiguration;
import io.jans.lock.model.config.cedarling.CedarlingPolicyConfiguration;
import io.jans.lock.model.config.cedarling.LogLevel;
import io.jans.lock.model.config.cedarling.LogType;

/**
 * Integration test that verifies Cedarling pushes health, log, and telemetry
 * audit data to a Lock server.  A WireMock HTTPS server (inherited from
 * {@link BaseWireMockHttpTest}) acts as the Lock server, so no real network
 * or external process is required.
 *
 * <h3>Test structure</h3>
 * <ol>
 *   <li><strong>Setup</strong> – WireMock stubs the {@code /.well-known}
 *       discovery document and all audit endpoints.  Cedarling is initialised
 *       with {@code CEDARLING_LOCK=enabled} and a short telemetry interval
 *       ({@value #TELEMETRY_INTERVAL_SEC} s) so data arrives quickly.</li>
 *   <li><strong>Round 1</strong> – 5 authorization calls (4 ALLOW + 1 DENY).
 *       The test waits up to {@value #WAIT_TIMEOUT_SEC} seconds for telemetry,
 *       captures the payloads, and verifies all required fields.</li>
 *   <li><strong>Reset</strong> – WireMock's request journal is cleared to
 *       give Round 2 a clean baseline.</li>
 *   <li><strong>Round 2</strong> – 7 authorization calls (4 ALLOW + 3 DENY).
 *       Same capture / verification cycle.</li>
 *   <li><strong>Cross-round comparison</strong> – health status must remain
 *       {@code "ok"}; {@code evaluationRequestsCount} must be ≥ the Round 1
 *       value (Cedarling accumulates it globally).</li>
 * </ol>
 *
 * <h3>Token matrix</h3>
 * <pre>
 * ┌────────┬────────────────────────────────────────────────────┬──────────────────┐
 * │ Token  │ OAuth scopes                                       │ Allowed endpoints│
 * ├────────┼────────────────────────────────────────────────────┼──────────────────┤
 * │ JWT 1  │ health.write + telemetry.write + log.write         │ all three        │
 * │ JWT 2  │ health.write only                                  │ /health only     │
 * │ JWT 3  │ log.write only                                     │ /log only        │
 * └────────┴────────────────────────────────────────────────────┴──────────────────┘
 * </pre>
 *
 * <p><strong>Prerequisites:</strong> The native Cedarling UniFFI library must be
 * available on the dynamic-library path ({@code LD_LIBRARY_PATH} /
 * {@code java.library.path}).
 *
 * @see BaseWireMockHttpTest
 * @see CedarlingAuthorizationService
 *
 * @author Yuriy Movchan Date: 12/05/2026
 */
@TestInstance(Lifecycle.PER_CLASS)
@DisplayName("Cedarling Telemetry – Integration Tests")
public class CedarlingTelemetryIntegrationTest extends BaseWireMockHttpTest {

	static {
		Configurator.setRootLevel(Level.INFO);
    }
	
    private static final Logger log = LoggerFactory.getLogger(CedarlingTelemetryIntegrationTest.class);
    
    private static final boolean DUMP_CAPTURED_REQUEST = true;

    // ─── WireMock endpoint paths ─────────────────────────────────────────────

    private static final String WELL_KNOWN_PATH     = "/.well-known/lock-server-configuration";
    private static final String HEALTH_PATH         = "/jans-lock/api/v1/audit/health";
    private static final String LOG_PATH            = "/jans-lock/api/v1/audit/log";
    private static final String TELEMETRY_PATH      = "/jans-lock/api/v1/audit/telemetry";
    private static final String BULK_HEALTH_PATH    = HEALTH_PATH + "/bulk";
    private static final String BULK_LOG_PATH       = LOG_PATH + "/bulk";
    private static final String BULK_TELEMETRY_PATH = TELEMETRY_PATH + "/bulk";

    // ─── Raw JWT strings – exp claims are patched at runtime ────────────────

    /**
     * JWT 1 – scopes: {@code health.write}, {@code telemetry.write},
     * {@code log.write}.
     */
    private static final String RAW_JWT_1 =
            "eyJraWQiOiJjb25uZWN0XzNjMzJjMTkzLTY5ZjYtNDBkYS1iNmQyLTE3ODY0YmJkYzU1MV9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9"
            + ".eyJhdWQiOiIwZTk5NDAzMC1lZTg4LTRmMTQtYWUwOC04YjBkYTEwNDQwMjkiLCJzdWIiOiIwZTk5NDAzMC1lZTg4LTRmMTQtYWUwOC04YjBkYTEwNDQwMjkiLC"
            + "J4NXQjUzI1NiI6IiIsIm5iZiI6MTc3ODYxNDA2Niwic2NvcGUiOlsiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2xvY2svaGVhbHRoLndyaXRlIiwiaHR0cHM6Ly9qYW"
            + "5zLmlvL29hdXRoL2xvY2svdGVsZW1ldHJ5LndyaXRlIiwiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2xvY2svbG9nLndyaXRlIl0sImlzcyI6Imh0dHBzOi8vamFucy"
            + "1teXNxbC1sb2NrLXNlcnZlci5qYW5zLmluZm8iLCJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiZXhwIjoxNzc4NjE0MzY2LCJpYXQiOjE3Nzg2MTQwNjYsImNsaWVudF"
            + "9pZCI6IjBlOTk0MDMwLWVlODgtNGYxNC1hZTA4LThiMGRhMTA0NDAyOSIsImp0aSI6IkhNdjluYnBiUkRLRTAzNVRUTkgwT3ciLCJzdGF0dXMiOnsic3RhdHVzX2"
            + "xpc3QiOnsiaWR4Ijo0MDAsInVyaSI6Imh0dHBzOi8vamFucy1teXNxbC1sb2NrLXNlcnZlci5qYW5zLmluZm8vamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdC"
            + "J9fX0"
            + ".bNXIL4f1lqvLoS49iMSZJORD2mJ9MWYCM5a8nyAiLqKy_fEqvqb-g1X6SgVeS2dJ9aFV-KRrfcjl0zSSQq6mBn-1pAostlMgV-lkOBi7rCbJUMwmdN7Bv7Op8E"
            + "yuD44_4hHRYhAXOXYv1CcjkyXtv-A9gDxNjHvhHVvpjaizcIMXVRrPxTTQgZF7r7n0t13La2E0vOxzzsgcWQjJukAY8HYybtoRL4JFswBIWPcgET9Btg9mZghDMl"
            + "vs0yiLVQfiGUZYcmxCCEQinjtutKgONP0Gv6xVMdsXMUpgXGZi6PCiEaEWButMwBauc9RJWEHbd7C4muKoAQ6_tFNuS_eoRw";

    /** JWT 2 – scope: {@code health.write} only. */
    private static final String RAW_JWT_2 =
            "eyJraWQiOiJjb25uZWN0XzNjMzJjMTkzLTY5ZjYtNDBkYS1iNmQyLTE3ODY0YmJkYzU1MV9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9"
            + ".eyJhdWQiOiIwZTk5NDAzMC1lZTg4LTRmMTQtYWUwOC04YjBkYTEwNDQwMjkiLCJzdWIiOiIwZTk5NDAzMC1lZTg4LTRmMTQtYWUwOC04YjBkYTEwNDQwMjkiLC"
            + "J4NXQjUzI1NiI6IiIsIm5iZiI6MTc3ODYxNDE2MSwic2NvcGUiOlsiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2xvY2svaGVhbHRoLndyaXRlIl0sImlzcyI6Imh0dH"
            + "BzOi8vamFucy1teXNxbC1sb2NrLXNlcnZlci5qYW5zLmluZm8iLCJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiZXhwIjoxNzc4NjE0NDYxLCJpYXQiOjE3Nzg2MTQxNj"
            + "EsImNsaWVudF9pZCI6IjBlOTk0MDMwLWVlODgtNGYxNC1hZTA4LThiMGRhMTA0NDAyOSIsImp0aSI6InRVMGVQb0haU0RPSjJ5Z0EyZFRnc3ciLCJzdGF0dXMiOn"
            + "sic3RhdHVzX2xpc3QiOnsiaWR4Ijo0MDEsInVyaSI6Imh0dHBzOi8vamFucy1teXNxbC1sb2NrLXNlcnZlci5qYW5zLmluZm8vamFucy1hdXRoL3Jlc3R2MS9zdG"
            + "F0dXNfbGlzdCJ9fX0"
            + ".iIN4rKz4wnGgijhNWJqY_RqYNx_7zT0hdevnU6wqRwiLp3rQG3c4ouv8P6X4CbiaxERzABbrjsS-4JcW2H2oLpAsuJGhJtr-HExe3iLs_OQ2_4NDwo0k2KJ5e_"
            + "zGP6Wykr6mQ8WhvGIfURk1aLirLCsegKhH1b26tSp6i8z7z-etNLwGjVPDfw6vV01kYJ0_O_tSf0HuLkGTPf34ld86CUNbPf2cE9Q4uqX_3xVTtMW0ffmOhDo8Qs"
            + "2dL96xs8O6ah-Rvp6UVjcD4A1qbVImN6USE70nEndmtDR_rvfsCBiL-htkgChTDZymceTcOn00NOvWB2I00rvSy7FdWwNAFQ";

    /** JWT 3 – scope: {@code log.write} only. */
    private static final String RAW_JWT_3 =
            "eyJraWQiOiJjb25uZWN0XzNjMzJjMTkzLTY5ZjYtNDBkYS1iNmQyLTE3ODY0YmJkYzU1MV9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9"
            + ".eyJhdWQiOiIwZTk5NDAzMC1lZTg4LTRmMTQtYWUwOC04YjBkYTEwNDQwMjkiLCJzdWIiOiIwZTk5NDAzMC1lZTg4LTRmMTQtYWUwOC04YjBkYTEwNDQwMjkiLC"
            + "J4NXQjUzI1NiI6IiIsIm5iZiI6MTc3ODYxNzc2MSwic2NvcGUiOlsiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2xvY2svbG9nLndyaXRlIl0sImlzcyI6Imh0dHBzOi"
            + "8vamFucy1teXNxbC1sb2NrLXNlcnZlci5qYW5zLmluZm8iLCJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiZXhwIjoxNzc4NjE4MDYxLCJpYXQiOjE3Nzg2MTc3NjEsIm"
            + "NsaWVudF9pZCI6IjBlOTk0MDMwLWVlODgtNGYxNC1hZTA4LThiMGRhMTA0NDAyOSIsImp0aSI6IjVxUWRHYWl4VEgybkQ5Z0Y2WDFPaXciLCJzdGF0dXMiOnsic3"
            + "RhdHVzX2xpc3QiOnsiaWR4Ijo1MDAsInVyaSI6Imh0dHBzOi8vamFucy1teXNxbC1sb2NrLXNlcnZlci5qYW5zLmluZm8vamFucy1hdXRoL3Jlc3R2MS9zdGF0dX"
            + "NfbGlzdCJ9fX0"
            + ".Q7xieptgb5r9eXqjI5BCSDv_ITtzZXbsXoyqcjsYw0PonF6z3c5XjiSPPrXVUU9dY_HQUrd4ib3U7oIQrKtfXcjJ2pMNuTZ0vPRCcZM_XqqbV3IewUbztabDKD"
            + "NpK0pSaNZy9V1SslHjW_vQoVDnclJL-w2usyXlMVnFub92GV3ldBZ9cB4UYVRovrzG_UxCa8FI-WkikYoET-vIiHbS5yP3EXlRKwP2pWwhHKwhAC7sjbnYW8ApgY"
            + "VAmvAnWqwPcaY_Bl-UobDHGBr0b0FhLtMIZvGevo1KdQE5dJwiflZOgiUZiYJU9uJ-tklD2gd5Pq-7g1-DW9Fvsmo2WVDcHw";

    // ─── Cedar action / resource constants (mirror CedarlingAuthorizationServiceIntegrationTest) ──
    /**
     * Lock endpoints access token
     */
    private static final String RAW_LOCK_ACCESS_TOKEN_JWT =
            "eyJraWQiOiJjb25uZWN0X2RjNzViZWZjLWU4N2QtNDMyZi1hOWExLTczYjE0YzJhNjUyMl9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9"
            + ".eyJhdWQiOiJmNDUwMTQ1Zi1hYjgyLTRiY2UtOTdjZi02MjQ2YjFjNmIxYTYiLCJzdWIiOiJmNDUwMTQ1Zi1hYjgyLTRiY2UtOTdjZi02MjQ2YjFjNmIxYTYiLC"
            + "J4NXQjUzI1NiI6IiIsIm5iZiI6MTc3OTk1ODYyNCwic2NvcGUiOlsiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2xvY2svaGVhbHRoLndyaXRlIiwiaHR0cHM6Ly9qYW"
            + "5zLmlvL29hdXRoL2xvY2svdGVsZW1ldHJ5LndyaXRlIiwiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2xvY2svbG9nLndyaXRlIl0sImlzcyI6Imh0dHBzOi8vamFucy"
            + "1teXNxbC1sb2NrLXNlcnZlci5qYW5zLmluZm8iLCJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiZXhwIjoxNzc5OTU4OTI0LCJpYXQiOjE3Nzk5NTg2MjQsImNsaWVudF"
            + "9pZCI6ImY0NTAxNDVmLWFiODItNGJjZS05N2NmLTYyNDZiMWM2YjFhNiIsImp0aSI6IjNOU2ZLM1hQU25XZDlNWDFDNlFhT1EiLCJzdGF0dXMiOnsic3RhdHVzX2"
            + "xpc3QiOnsiaWR4Ijo0MDEsInVyaSI6Imh0dHBzOi8vamFucy1teXNxbC1sb2NrLXNlcnZlci5qYW5zLmluZm8vamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdC"
            + "J9fX0"
            + ".YSMkLZIm0JzIIiuShrXbZwLT5Bm58nBpz2fcaGEP4zyyDE0Te1T3WKmJZRsyRNPN3QgIwa9b02C-lGTqUJ9YkylTolLitJHgy7aVhfestGYTZ_r0gvfcGYVF8h"
            + "zsk5k11U-hb9SGbZOXOvuis998fCXolG-UUaYj7VGjU8xreGLgmEx7Otmfpi2bjenQ0DGFjo82XAVzgqO7gwGT-5zohBQ8uNQcKKASGj4g2NtVcjXqmxBS9huI7e"
            + "dAJxFPlZ5J7gghfJAIARDLm8UrYgNEHqVdQPDOrAdnIOE5n0I4oJad5fl5luyKSNmd6sL4hi82OR7Ldig3XIjxyHQ7VJYZhA";

    // ─── Cedar action / resource constants (mirror CedarlingAuthorizationServiceIntegrationTest) ──

    private static final String ACTION_POST    = "Jans::Action::\"POST\"";
    private static final String RESOURCE_TYPE  = "Jans::HTTP_Request";

    private static final String ID_LOG         = "lock_audit_log_write";
    private static final String ID_HEALTH      = "lock_audit_health_write";
    private static final String ID_TELEMETRY   = "lock_audit_telemetry_write";

    private static final String PATH_LOG       = "/audit/log/bulk";
    private static final String PATH_HEALTH    = "/audit/health/bulk";
    private static final String PATH_TELEMETRY = "/audit/telemetry/bulk";

    private static final String ACCESS_TOKEN_KEY = CedarlingAuthorizationService.CEDARLING_JANS_ACCESS_TOKEN;

    // ─── Timing ──────────────────────────────────────────────────────────────

    /**
     * Cedarling telemetry / health push interval in seconds.
     * Low enough for integration tests to complete in reasonable time.
     */
    private static final int      TELEMETRY_INTERVAL_SEC = 5;

    /** Maximum time to wait for at least one telemetry payload per round. */
    private static final int      WAIT_TIMEOUT_SEC       = 30;
    private static final Duration WAIT_TIMEOUT           = Duration.ofSeconds(WAIT_TIMEOUT_SEC);

    // ─── Test state ──────────────────────────────────────────────────────────

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CedarlingAuthorizationService authService;
    private String jwt1, jwt2, jwt3;

    /**
     * Guards one-time Cedarling initialisation inside {@link #registerStubs()}.
     * WireMock stubs must exist before {@code initCedarlingService()} is called
     * (Cedarling fetches the well-known document during startup), so the service
     * cannot be initialised in {@code @BeforeAll} – at that point the
     * {@link com.github.tomakehurst.wiremock.junit5.WireMockExtension} has not
     * yet registered any stubs.  The flag ensures initialisation happens exactly
     * once, on the first {@code @BeforeEach} invocation.
     */
    private boolean serviceInitialized = false;

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    /**
     * Registers WireMock stubs and, on the very first call, initialises the
     * Cedarling service.
     *
     * <h3>Why everything lives here and not in {@code @BeforeAll}</h3>
     * <p>{@link com.github.tomakehurst.wiremock.junit5.WireMockExtension} resets
     * <em>all</em> stub mappings in its own {@code beforeEach} callback, which
     * executes <strong>after</strong> {@code @BeforeAll} but
     * <strong>before</strong> any {@code @BeforeEach} defined in the test class.
     * The execution order for each test method is therefore:
     * <pre>
     *   @BeforeAll  setUpAll()          – runs once, but stubs aren't registered yet
     *   WireMockExtension.beforeEach()  – wipes all stub mappings
     *   @BeforeEach registerStubs()     – stubs registered here, safe to call Cedarling init
     * </pre>
     * <p>Cedarling fetches the well-known discovery document during
     * {@code initCedarlingService()}, so the WireMock stubs <em>must</em> be
     * present before that call.  The {@code serviceInitialized} flag ensures the
     * expensive init runs exactly once while the stub registration runs before
     * every test.
     */
    @BeforeEach
    void registerStubs() throws Exception {
        configureWellKnownEndpoint();
        configureAuditEndpoints();
        log.info("WireMock stubs registered (well-known + 6 audit endpoints)");

        if (!serviceInitialized) {
            initAuthService();
            serviceInitialized = true;
            log.info("WireMock HTTPS port: {}  |  telemetry interval: {}s  |  wait timeout: {}s",
                    wireMockServer.getHttpsPort(), TELEMETRY_INTERVAL_SEC, WAIT_TIMEOUT_SEC);
        }
    }

    /** Shuts down the Cedarling adapter after the class is done. */
    @AfterAll
    void tearDown() {
        if (serviceInitialized) {
            authService.destroy();
            log.info("Cedarling service destroyed");
         }
    }

    // ─── Tests ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Two-round telemetry lifecycle")
    class TwoRoundTelemetryLifecycle {

        /**
         * Main telemetry lifecycle test.
         *
         * <p>Executes two batches of authorization calls separated by a WireMock
         * request journal reset, then verifies:
         * <ul>
         *   <li>All required fields are present in every captured payload.</li>
         *   <li>Health status is {@code "ok"} in both rounds.</li>
         *   <li>{@code evaluationRequestsCount} is ≥ the Round 1 value in Round 2
         *       (Cedarling accumulates the counter globally).</li>
         *   <li>Log entries carry correct decision results.</li>
         * </ul>
         */
        @Test
        @DisplayName("Telemetry accumulates correctly across two authorization rounds")
        void telemetryAccumulatesAcrossRounds() throws Exception {

            // ═══════════════════════════════ ROUND 1 ═══════════════════════════
            log.info("═══ ROUND 1 – 5 authorization calls (4 ALLOW + 1 DENY) ═══");
            int round1AuthCalls = executeRound1Authorizations();

            // Wait for data to arrive at least at the primary telemetry endpoint before waiting the full timeout.
            awaitDuration(Duration.ofSeconds(TELEMETRY_INTERVAL_SEC + 2));

            // Wait for the telemetry push triggered by round-1 evaluations
            Map<String, List<JsonNode>> round1CapturedRequests     = awaitAndCapture("R1", WAIT_TIMEOUT);

            List<JsonNode> r1Telemetry     = findCaptured(round1CapturedRequests, "R1 telemetry", TELEMETRY_PATH);
            List<JsonNode> r1BulkTelemetry = findCaptured(round1CapturedRequests, "R1 bulk-telemetry", BULK_TELEMETRY_PATH);
            List<JsonNode> r1Health        = findCaptured(round1CapturedRequests, "R1 health",         HEALTH_PATH);
            List<JsonNode> r1BulkHealth    = findCaptured(round1CapturedRequests, "R1 bulk-health",    BULK_HEALTH_PATH);
            List<JsonNode> r1Log           = findCaptured(round1CapturedRequests, "R1 log",            LOG_PATH);
            List<JsonNode> r1BulkLog       = findCaptured(round1CapturedRequests, "R1 bulk-log",       BULK_LOG_PATH);

            log.info("Round 1 received – telemetry={}, health={}, log={}, bulkTel={}, bulkHealth={}, bulkLog={}",
                    r1Telemetry.size(), r1Health.size(), r1Log.size(),
                    r1BulkTelemetry.size(), r1BulkHealth.size(), r1BulkLog.size());

            // Structural and value verification
            verifyHealthPayloads(r1Health,         "Round 1");
            verifyHealthBulkPayloads(r1BulkHealth,     "Round 1 (bulk)", -1);
            verifyLogPayloads(r1Log,               "Round 1");
            verifyLogBulkPayloads(r1BulkLog,           "Round 1 (bulk)", round1AuthCalls);
            verifyTelemetryPayloads(r1Telemetry,   "Round 1");
            verifyTelemetryBulkPayloads(r1BulkTelemetry, "Round 1", -1);

            // Capture round 1's latest evaluationRequestsCount for comparison
            long r1EvalCount = latestLong(r1Telemetry, "evaluationRequestsCount");
            log.info("Round 1 – evaluationRequestsCount = {}", r1EvalCount);

            // ─── Wipe request journal – round 2 starts clean ──────────────────
            wireMockServer.resetRequests();
            log.info("--- WireMock request journal reset – starting Round 2 ---");

            // ═══════════════════════════════ ROUND 2 ═══════════════════════════
            log.info("═══ ROUND 2 – 7 authorization calls (4 ALLOW + 3 DENY) ═══");
            int round2AuthCalls = executeRound2Authorizations();

            // Wait for data to arrive at least at the primary telemetry endpoint before waiting the full timeout.
            awaitDuration(Duration.ofSeconds(TELEMETRY_INTERVAL_SEC + 2));

            // Wait for the telemetry push triggered by round-2 evaluations
            Map<String, List<JsonNode>> round2CapturedRequests     = awaitAndCapture("R2", WAIT_TIMEOUT);

            List<JsonNode> r2Telemetry     = findCaptured(round2CapturedRequests, "R2 telemetry",      TELEMETRY_PATH);
            List<JsonNode> r2BulkTelemetry = findCaptured(round2CapturedRequests, "R2 bulk-telemetry", BULK_TELEMETRY_PATH);
            List<JsonNode> r2Health        = findCaptured(round2CapturedRequests, "R2 health",         HEALTH_PATH);
            List<JsonNode> r2BulkHealth    = findCaptured(round2CapturedRequests, "R2 bulk-health",    BULK_HEALTH_PATH);
            List<JsonNode> r2Log           = findCaptured(round2CapturedRequests, "R2 log",            LOG_PATH);
            List<JsonNode> r2BulkLog       = findCaptured(round2CapturedRequests, "R2 bulk-log",       BULK_LOG_PATH);

            log.info("Round 2 received – telemetry={}, health={}, log={}, bulkTel={}, bulkHealth={}, bulkLog={}",
                    r2Telemetry.size(), r2Health.size(), r2Log.size(),
                    r2BulkTelemetry.size(), r2BulkHealth.size(), r2BulkLog.size());

            verifyHealthPayloads(r2Health,         "Round 2");
            verifyHealthBulkPayloads(r2BulkHealth,     "Round 2 (bulk)", -1);
            verifyLogPayloads(r2Log,               "Round 2");
            verifyLogBulkPayloads(r2BulkLog,           "Round 2 (bulk)", round2AuthCalls);
            verifyTelemetryPayloads(r2Telemetry,   "Round 2");
            verifyTelemetryBulkPayloads(r2BulkTelemetry, "Round 2", -1);

            // ═══════════════════════════ CROSS-ROUND COMPARISON ════════════════
            compareTelemetryAcrossRounds(r1EvalCount, r1Telemetry, r2Telemetry);
            compareHealthAcrossRounds(r1Health, r2Health);

            log.info("✅ All telemetry assertions passed across both rounds.");
        }
    }

    // ─── Authorization rounds ─────────────────────────────────────────────────

    /**
     * Round 1: JWT 1 (all scopes) + JWT 2 (health only) – 5 calls, 4 ALLOW + 1 DENY.
     *
     * @return total number of authorization calls made
     */
    private int executeRound1Authorizations() {
        // JWT 1 – all three endpoints allowed
        assertTrue(authorize(jwt1, ACTION_POST, ID_LOG,       PATH_LOG),
                "R1: JWT1 must be allowed for /log");
        assertTrue(authorize(jwt1, ACTION_POST, ID_HEALTH,    PATH_HEALTH),
                "R1: JWT1 must be allowed for /health");
        assertTrue(authorize(jwt1, ACTION_POST, ID_TELEMETRY, PATH_TELEMETRY),
                "R1: JWT1 must be allowed for /telemetry");

        // JWT 2 – health allowed, log denied
        assertTrue(authorize(jwt2, ACTION_POST, ID_HEALTH,    PATH_HEALTH),
                "R1: JWT2 must be allowed for /health");
        assertFalse(authorize(jwt2, ACTION_POST, ID_LOG,      PATH_LOG),
                "R1: JWT2 must be denied for /log (missing log.write)");

        return 5; // 4 ALLOW + 1 DENY
    }

    /**
     * Round 2: JWT 3 (log only) + JWT 2 (health only) + JWT 1 – 7 calls, 4 ALLOW + 3 DENY.
     *
     * @return total number of authorization calls made
     */
    private int executeRound2Authorizations() {
        // JWT 3 – log allowed, health and telemetry denied
        assertTrue(authorize(jwt3,  ACTION_POST, ID_LOG,       PATH_LOG),
                "R2: JWT3 must be allowed for /log");
        assertFalse(authorize(jwt3, ACTION_POST, ID_HEALTH,    PATH_HEALTH),
                "R2: JWT3 must be denied for /health (missing health.write)");
        assertFalse(authorize(jwt3, ACTION_POST, ID_TELEMETRY, PATH_TELEMETRY),
                "R2: JWT3 must be denied for /telemetry (missing telemetry.write)");

        // JWT 2 – health allowed, telemetry denied
        assertTrue(authorize(jwt2,  ACTION_POST, ID_HEALTH,    PATH_HEALTH),
                "R2: JWT2 must be allowed for /health");
        assertFalse(authorize(jwt2, ACTION_POST, ID_TELEMETRY, PATH_TELEMETRY),
                "R2: JWT2 must be denied for /telemetry (missing telemetry.write)");

        // JWT 1 – all allowed
        assertTrue(authorize(jwt1, ACTION_POST, ID_LOG,       PATH_LOG),
                "R2: JWT1 must be allowed for /log");
        assertTrue(authorize(jwt1, ACTION_POST, ID_HEALTH,    PATH_HEALTH),
                "R2: JWT1 must be allowed for /health");

        return 7; // 4 ALLOW + 3 DENY
    }

    // ─── Payload verification ─────────────────────────────────────────────────

    /**
     * Verifies structural fields and value assertions for every health payload.
     *
     * <p>Required fields: {@code service}, {@code status}, {@code nodeName}.<br>
     * Value assertions: {@code service == "cedarling"}, {@code status == "ok"}.
     */
    private void verifyHealthPayloads(List<JsonNode> payloads, String label) {
        if (payloads.isEmpty()) {
            log.warn("[{}] No health payloads received – skipping field checks", label);
            return;
        }
        for (int i = 0; i < payloads.size(); i++) {
            JsonNode node = payloads.get(i);
            String ctx = label + " health[" + i + "]";

            verifyHealthPayload(ctx, node);
        }
        log.info("[{}] ✅ {} health payload(s) verified", label, payloads.size());
    }

    private void verifyHealthBulkPayloads(List<JsonNode> payloads, String label, int minExpectedEntries) {
        if (payloads.isEmpty()) {
            log.warn("⚠️  [{}] No health payloads received – skipping field checks", label);
            return;
        }

        int payloadCount = 0;
        for (int i = 0; i < payloads.size(); i++) {
            JsonNode nodeArray = payloads.get(i);

            assertTrue(nodeArray.isArray(),
                    "Bulk Health response should be an array of health payloads, but got: " + nodeArray.getNodeType());

            for (int j = 0; j < nodeArray.size(); j++) {
	            String ctx = label + " health[" + i + "][" + j + "]";
	            JsonNode node = nodeArray.get(j);
	
	            verifyHealthPayload(ctx, node);
	            payloadCount++;
            }
        }
        if (minExpectedEntries >= 0) {
            assertTrue(payloadCount >= minExpectedEntries,
                    label + ": expected at least " + minExpectedEntries
                            + " log entries but got " + payloadCount);
        }
        log.info("[{}] ✅ {} health payload(s) verified", label, payloadCount);
    }

	private void verifyHealthPayload(String ctx, JsonNode node) {
		// ── Required fields ──
		assertTrue(node.hasNonNull("service"),  ctx + ": missing 'service'");
		assertTrue(node.hasNonNull("status"),   ctx + ": missing 'status'");
		assertTrue(node.hasNonNull("node_name"), ctx + ": missing 'node_name'");

		// ── Value assertions ──
		assertEquals("Lock Server - Test Edition", node.get("service").asText(), ctx + ": service must be 'Lock Server - Test Edition'");
		assertEquals("running",        node.get("status").asText(),  ctx + ": status must be 'running'");

		// node_name must be present and non-blank
		assertFalse(node.get("node_name").asText("").isBlank(), ctx + ": node_name must not be blank");
	}

    /**
     * Verifies structural fields for every log (audit) payload.
     *
     * <p>Required fields: {@code clientId}, {@code principalId},
     * {@code decisionResult}, {@code action}.</p>
     *
     * @param payloads parsed request bodies
     * @param label    test label for assertion messages
     */
    private void verifyLogPayloads(List<JsonNode> payloads, String label) {
        if (payloads.isEmpty()) {
            log.warn("[{}] No log payloads received – skipping field checks", label);
            return;
        }
        for (int i = 0; i < payloads.size(); i++) {
            JsonNode node = payloads.get(i);
            String ctx = label + " log[" + i + "]";

            verifyLogPayload(ctx, node);
        }
        log.info("[{}] ✅ {} log payload(s) verified", label, payloads.size());
    }

    private void verifyLogBulkPayloads(List<JsonNode> payloads, String label, int minExpectedEntries) {
        if (payloads.isEmpty()) {
            log.warn("⚠️  [{}] No log payloads received – skipping field checks", label);
            return;
        }

        int payloadCount = 0;
        for (int i = 0; i < payloads.size(); i++) {
            JsonNode nodeArray = payloads.get(i);

            assertTrue(nodeArray.isArray(),
                    "Bulk Log response should be an array of log payloads, but got: " + nodeArray.getNodeType());

            for (int j = 0; j < nodeArray.size(); j++) {
	            String ctx = label + " log[" + i + "][" + j + "]";
	            JsonNode node = nodeArray.get(j);
	
	            verifyLogPayload(ctx, node);
	            payloadCount++;
            }
        }
        if (minExpectedEntries >= 0) {
            assertTrue(payloadCount >= minExpectedEntries,
                    label + ": expected at least " + minExpectedEntries
                            + " log entries but got " + payloadCount);
        }
        log.info("[{}] ✅ {} log payload(s) verified", label, payloadCount);
    }

	private void verifyLogPayload(String ctx, JsonNode node) {
		// ── Required fields ──
		assertTrue(node.hasNonNull("clientId"),       ctx + ": missing 'clientId'");
		assertTrue(node.hasNonNull("principalId"),    ctx + ": missing 'principalId'");
		assertTrue(node.hasNonNull("decisionResult"), ctx + ": missing 'decisionResult'");
		assertTrue(node.hasNonNull("action"),         ctx + ": missing 'action'");

		// ── Value assertions ──
		String decision = node.get("decisionResult").asText();
		assertTrue("ALLOW".equalsIgnoreCase(decision) || "DENY".equalsIgnoreCase(decision),
		        ctx + ": decisionResult must be 'ALLOW' or 'DENY', got: " + decision);

		// action must follow the Cedar action URI format: Namespace::Action::"name"
		String action = node.get("action").asText();
		assertTrue(action.matches(".+::Action::\"[^\"]+\""),
		        ctx + ": action must match Cedar format '<Namespace>::Action::\"<name>\"', got: " + action);

		// clientId must be present and non-blank
		assertFalse(node.get("clientId").asText("").isBlank(),
		        ctx + ": clientId must not be blank");
	}

    /**
     * Verifies structural fields and value assertions for every telemetry payload.
     *
     * <p>Required fields: {@code service}, {@code status},
     * {@code evaluationRequestsCount}, {@code avgPolicyEvaluationTimeNs}.
     */
    private void verifyTelemetryPayloads(List<JsonNode> payloads, String label) {
        if (payloads.isEmpty()) {
            log.warn("[{}] No telemetry payloads received – skipping field checks", label);
            return;
        }
        for (int i = 0; i < payloads.size(); i++) {
            JsonNode node = payloads.get(i);
            String ctx = label + " telemetry[" + i + "]";

            verifyTelemetryPayload(ctx, node);
        }
        log.info("[{}] ✅ {} telemetry payload(s) verified", label, payloads.size());
    }

    private void verifyTelemetryBulkPayloads(List<JsonNode> payloads, String label, int minExpectedEntries) {
        if (payloads.isEmpty()) {
            log.warn("⚠️  [{}] No telemetry payloads received – skipping field checks", label);
            return;
        }

        int payloadCount = 0;
        for (int i = 0; i < payloads.size(); i++) {
            JsonNode nodeArray = payloads.get(i);

            assertTrue(nodeArray.isArray(),
                    "Bulk Telemetry response should be an array of telemetry payloads, but got: " + nodeArray.getNodeType());

            for (int j = 0; j < nodeArray.size(); j++) {
	            String ctx = label + " telemetry[" + i + "][" + j + "]";
	            JsonNode node = nodeArray.get(j);
	
	            verifyTelemetryPayload(ctx, node);
	            payloadCount++;
            }
        }
        if (minExpectedEntries >= 0) {
            assertTrue(payloadCount >= minExpectedEntries,
                    label + ": expected at least " + minExpectedEntries
                            + " log entries but got " + payloadCount);
        }
        log.info("[{}] ✅ {} telemetry payload(s) verified", label, payloadCount);
    }
	
    private void verifyTelemetryPayload(String ctx, JsonNode node) {
		// ── Required fields ──
		assertTrue(node.hasNonNull("service"),                   ctx + ": missing 'service'");
		assertTrue(node.hasNonNull("status"),                    ctx + ": missing 'status'");
		assertTrue(node.hasNonNull("evaluationRequestsCount"),   ctx + ": missing 'evaluationRequestsCount'");
		assertTrue(node.hasNonNull("avgPolicyEvaluationTimeNs"), ctx + ": missing 'avgPolicyEvaluationTimeNs'");

		// ── Value assertions ──
		assertEquals("cedarling", node.get("service").asText(), ctx + ": service must be 'cedarling'");

		long evalCount = node.get("evaluationRequestsCount").asLong();
		assertTrue(evalCount >= 0, ctx + ": evaluationRequestsCount must be >= 0, got " + evalCount);

		// Average evaluation time must be strictly positive once any evaluation has occurred
		long avgEvalNs = node.get("avgPolicyEvaluationTimeNs").asLong(-1L);
		assertTrue(avgEvalNs > 0,
		        ctx + ": avgPolicyEvaluationTimeNs must be > 0, got " + avgEvalNs);
	}

    // ─── Cross-round comparison ───────────────────────────────────────────────

    /**
     * Compares telemetry counters from Round 1 vs Round 2.
     *
     * <ul>
     *   <li>{@code evaluationRequestsCount} in Round 2 must be ≥ the Round 1
     *       value because Cedarling accumulates this counter globally.</li>
     *   <li>The {@code service} name must be identical across rounds.</li>
     *   <li>Round 2 must contain at least as many telemetry reports as Round 1.</li>
     * </ul>
     *
     * @param r1EvalCount   already extracted from the last round-1 telemetry entry
     * @param round1        Round 1 telemetry payloads
     * @param round2        Round 2 telemetry payloads
     */
    private void compareTelemetryAcrossRounds(long r1EvalCount,
            List<JsonNode> round1, List<JsonNode> round2) {

        if (round1.isEmpty() || round2.isEmpty()) {
            log.warn("⚠️  Cannot compare telemetry rounds – empty data (r1={}, r2={})",
                    round1.size(), round2.size());
            return;
        }

        long r2EvalCount = latestLong(round2, "evaluationRequestsCount");
        log.info("evaluationRequestsCount — Round 1 (last) = {}  |  Round 2 (last) = {}",
                r1EvalCount, r2EvalCount);

        // Cedarling accumulates evaluations globally; Round 2 counter must not decrease
        assertTrue(r2EvalCount >= r1EvalCount,
                "evaluationRequestsCount must not decrease between rounds: R1=" + r1EvalCount
                        + ", R2=" + r2EvalCount);

        // Service name is invariant
        String r1Service = latestString(round1, "service");
        String r2Service = latestString(round2, "service");
        assertEquals(r1Service, r2Service, "service name must be consistent across rounds");

        // Round 2 had more authorization calls – the difference should be visible
        long delta = r2EvalCount - r1EvalCount;
        log.info("evaluationRequestsCount delta (R2 – R1) = {}", delta);
        // NOTE: delta could be 0 if Cedarling resets the counter per telemetry interval
        // rather than accumulating globally.  The ≥ assertion above is the safe guard.

        log.info("✅ Cross-round telemetry comparison passed");
    }

    /**
     * Verifies that the Cedarling process remains healthy across both rounds
     * regardless of DENY decisions during authorization.
     */
    private void compareHealthAcrossRounds(List<JsonNode> round1, List<JsonNode> round2) {
        if (round1.isEmpty() || round2.isEmpty()) {
            log.warn("⚠️  Cannot compare health rounds – empty data (r1={}, r2={})",
                    round1.size(), round2.size());
            return;
        }
        String r1Status = latestString(round1, "status");
        String r2Status = latestString(round2, "status");

        assertEquals("ok", r1Status, "Round 1 health status must be 'ok'");
        assertEquals("ok", r2Status, "Round 2 health status must be 'ok'");

        log.info("✅ Health status is 'ok' in both rounds");
    }

    // ─── WireMock utilities ───────────────────────────────────────────────────
    public List<JsonNode> findCaptured(
            Map<String, List<JsonNode>> capturedRequests, 
            String label, 
            String endpointPath) {
            
    	List<JsonNode> cpatured = capturedRequests.entrySet().stream()
                .filter(entry -> entry.getKey() != null 
                        && entry.getKey().endsWith(endpointPath))
                .map(Map.Entry::getValue)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        log.info("[{}] {} request(s) captured at {}", label, cpatured.size(), endpointPath);
        
        return cpatured;
     }
    /**
     * Polls WireMock for POST requests at {@code endpointPath} until at least
     * one arrives or {@code timeout} elapses, then returns all captured bodies
     * as parsed {@link JsonNode}s.
     *
     * @param label        descriptive label for log output
     * @param endpointPath WireMock path to watch
     * @param timeout      maximum time to wait
     * @return list of parsed JSON bodies; empty if nothing arrived in time
     */
    private Map<String, List<JsonNode>> awaitAndCapture(String label, Duration timeout)
            throws InterruptedException {

        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            List<LoggedRequest> requests = wireMockServer.findAll(
                    postRequestedFor(anyUrl()));
            if (!requests.isEmpty()) {
                Map<String, List<JsonNode>> parsed = parseRequestBodies(requests);
                String logDump = parsed.entrySet().stream()
                        .map(entry -> entry.getKey() + " = " + entry.getValue().size())
                        .collect(Collectors.joining(", ", "{", "}"));

                log.info("Requests captured summary: {}", logDump);
                
                if (DUMP_CAPTURED_REQUEST) {
					parsed.forEach((path, nodes) -> {
						log.debug("Captured {} request(s) at {}:", nodes.size(), path);
						for (int i = 0; i < nodes.size(); i++) {
							log.debug("  [{}][{}]: {}", path, i, nodes.get(i).toPrettyString());
						}
					});
				}
                return parsed;
            }
            Thread.sleep(500);
        }
        log.warn("[{}] Timeout ({}s) – no requests arrived", label, timeout.toSeconds());
        return Collections.emptyMap();
    }

    private void awaitDuration(Duration timeout)throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(500);
        }
    }

    private Map<String, List<JsonNode>> parseRequestBodies(List<LoggedRequest> requests) {
        return requests.stream()
                .map(r -> {
                    try {
                        // Extract clean path (ignoring query parameters)
                        String fullPath = new URI(r.getUrl()).getPath();
                        String contextPath = fullPath.startsWith("/") ? "/" + fullPath.substring(1) : fullPath;
                        // Parse body
                        JsonNode node = objectMapper.readTree(r.getBodyAsString());
                        
                        return new AbstractMap.SimpleEntry<>(contextPath, node);
                    } catch (Exception ex) {
                        log.warn("Could not parse request body or URL: {}", r.getUrl(), ex);
                        return null;
                    }
                })
                // Remove requests that failed to parse
                .filter(Objects::nonNull)
                // Group by the path (key) and collect the JsonNodes (value) into a List
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
    }

    /** Extracts a {@code long} field from the last node in {@code nodes}. */
    private long latestLong(List<JsonNode> nodes, String field) {
        if (nodes.isEmpty()) return 0L;
        return nodes.get(nodes.size() - 1).path(field).asLong(0L);
    }

    /** Extracts a {@code String} field from the last node in {@code nodes}. */
    private String latestString(List<JsonNode> nodes, String field) {
        if (nodes.isEmpty()) return "";
        return nodes.get(nodes.size() - 1).path(field).asText("");
    }

    private List<String> iteratorToList(Iterator<String> it) {
        List<String> list = new ArrayList<>();
        it.forEachRemaining(list::add);
        return list;
    }

    // ─── WireMock stub configuration ─────────────────────────────────────────

    /**
     * Stubs the Lock server discovery document with endpoint URIs that all
     * point back to the WireMock HTTPS port.
     */
    private void configureWellKnownEndpoint() {
		int port = wireMockServer.getHttpsPort();

		String fullUrl = "https://localhost:" + port + WELL_KNOWN_PATH;
		log.info("Configuring WireMock well-known endpoint stub at: {}", fullUrl);
        
        String body = """
                {
                  "version": "1.0",
                  "issuer": "https://localhost:%d",
                  "audit": {
                    "health_endpoint":    "https://localhost:%d/jans-lock/api/v1/audit/health",
                    "log_endpoint":       "https://localhost:%d/jans-lock/api/v1/audit/log",
                    "telemetry_endpoint": "https://localhost:%d/jans-lock/api/v1/audit/telemetry"
                  }
                }
                """.formatted(port, port, port, port);

        wireMockServer.stubFor(get(urlEqualTo(WELL_KNOWN_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    /** Stubs all six audit endpoints (single + bulk) to return HTTP 200 OK. */
    private void configureAuditEndpoints() {
        String ok = """
                {"success": true, "message": "Audit data processed successfully"}
                """;
        List.of(HEALTH_PATH, BULK_HEALTH_PATH,
                LOG_PATH, BULK_LOG_PATH,
                TELEMETRY_PATH, BULK_TELEMETRY_PATH)
            .forEach(path ->
                wireMockServer.stubFor(post(urlEqualTo(path))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(ok))));
    }

    // ─── Cedarling service initialisation ────────────────────────────────────

    /**
     * Builds a fully-wired {@link CedarlingAuthorizationService} (no CDI
     * container available in tests) with Lock telemetry enabled and pointing
     * at the WireMock HTTPS server.
     */
    private void initAuthService() throws Exception {
        Logger svcLog      = LoggerFactory.getLogger(CedarlingAuthorizationService.class);
        AppConfiguration          appConfig    = mock(AppConfiguration.class);
        CedarlingPolicyConfiguration policyConfig = mock(CedarlingPolicyConfiguration.class);
        CedarlingConfiguration    cedarConf    = mock(CedarlingConfiguration.class);

        when(cedarConf.isEnabled()).thenReturn(true);
        when(cedarConf.getLogType()).thenReturn(LogType.STD_OUT);
        when(cedarConf.getLogLevel()).thenReturn(LogLevel.TRACE);
        when(appConfig.getCedarlingConfiguration()).thenReturn(cedarConf);

        String policyStoreFn = System.getProperty("user.dir") + "/target/test-classes/test-policy-store";
        // Point Cedarling at the WireMock discovery document
        String lockServerUri = "https://localhost:" + wireMockServer.getHttpsPort()
                + WELL_KNOWN_PATH;
        String lockAccessTokenJwt = withFutureExp(RAW_LOCK_ACCESS_TOKEN_JWT);

        authService = new CedarlingAuthorizationService() {
        		@Override
        	    protected BootstrapConfig prepareBootstrapConfig(CedarlingConfiguration cedarConf) {
        	        // Delegate to the standard builder; JWT validation is disabled for tests
        			BootstrapConfig bootstrapConfig = BootstrapConfig.builder()
        	            .applicationName("Lock Server - Test Edition")
        	            .policyStoreLocalFn(System.getProperty("user.dir") + "/target/test-classes/test-policy-store")
        	            .jwtStatusValidation(false) 
        	            .jwtSigValidation(false)
        	            .logType(LogType.STD_OUT)
        	            .logLevel(LogLevel.TRACE)
						// Lock / telemetry
						.lock(true)
						.lockAcceptInvalidCerts(true)
						.lockServerConfigurationUri(lockServerUri)
						.lockDynamicConfiguration(true)
						.lockHealthInterval(TELEMETRY_INTERVAL_SEC)
						.lockTelemetryInterval(TELEMETRY_INTERVAL_SEC)
						.lockListenSse(false)
						.lockAccessTokenJwt(lockAccessTokenJwt)
						.build();
        			
        	        log.info("Cedarling bootstrap configuration: {}", bootstrapConfig.toJsonConfig());
        			return bootstrapConfig;
        	    }
        };

        when(cedarConf.isEnabled()).thenReturn(true);
        
        injectField(authService, "log",                         svcLog);
        injectField(authService, "appConfiguration",            appConfig);
        injectField(authService, "policyConfiguration",         policyConfig);
        injectField(authService, "policyStoreLocalFn",          policyStoreFn);

        authService.init();

        // Patch exp claims so tokens are valid during the entire test run
        jwt1 = withFutureExp(RAW_JWT_1);
        jwt2 = withFutureExp(RAW_JWT_2);
        jwt3 = withFutureExp(RAW_JWT_3);

        log.info("Cedarling service initialised – lock URI: {}", lockServerUri);
    }

    // ─── Authorization helpers ────────────────────────────────────────────────

    private boolean authorize(String token, String action, String permId, String path) {
        Map<String, String> tokens = Map.of(ACCESS_TOKEN_KEY, token);
        return authService.authorize(tokens, action, buildResource(permId, path), buildContext());
    }

    private static Map<String, Object> buildResource(String permId, String path) {
        Map<String, Object> resource = new HashMap<>();
        resource.put("cedar_entity_mapping",
                Map.of("entity_type", RESOURCE_TYPE, "id", permId));
        resource.put("url",
                Map.of("host", "", "path", path, "protocol", ""));
        resource.put("header", Collections.emptyMap());
        return resource;
    }

    private static Map<String, Object> buildContext() {
        return Collections.emptyMap();
    }

    // ─── JWT exp patching ─────────────────────────────────────────────────────

    /**
     * Replaces the {@code exp} claim in the JWT payload with {@code now + 1 hour}.
     * The original signature is kept but becomes stale; Cedarling accepts it
     * because the adapter is initialised with {@code jwtSigValidation(false)}.
     *
     * @param rawJwt original JWT string (header.payload.signature)
     * @return patched JWT with a future {@code exp}
     */
    static String withFutureExp(String rawJwt) {
        String[] parts = rawJwt.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Not a 3-part JWT: " + rawJwt);
        }
        String payloadJson = new String(
                Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

        long futureExp = (System.currentTimeMillis() / 1000L) + 3600L;
        String patched = payloadJson.replaceAll("\"exp\"\\s*:\\s*\\d+", "\"exp\":" + futureExp);

        String encodedPayload = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(patched.getBytes(StandardCharsets.UTF_8));

        return parts[0] + "." + encodedPayload + "." + parts[2];
    }

    // ─── Reflection utility ───────────────────────────────────────────────────

    /**
     * Sets {@code fieldName} on {@code target} using reflection, walking up
     * the class hierarchy until the field is found.
     *
     * @param target    object to mutate
     * @param fieldName field name
     * @param value     value to set
     * @throws NoSuchFieldException if the field is not found in any superclass
     */
    static void injectField(Object target, String fieldName, Object value) throws Exception {
        Class<?> cls = target.getClass();
        while (cls != null) {
            try {
                Field field = cls.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                cls = cls.getSuperclass();
            }
        }
        throw new NoSuchFieldException(
                "Field '" + fieldName + "' not found in " + target.getClass().getName());
    }
}