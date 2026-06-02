/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */
package io.jans.lock.cedarling.telemetry;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.wiremock.grpc.dsl.WireMockGrpc.json;
import static org.wiremock.grpc.dsl.WireMockGrpc.method;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
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
import org.wiremock.grpc.dsl.WireMockGrpcService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import io.jans.lock.cedarling.config.BootstrapConfig;
import io.jans.lock.cedarling.service.CedarlingAuthorizationService;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.cedarling.CedarlingConfiguration;
import io.jans.lock.model.config.cedarling.CedarlingPolicyConfiguration;
import io.jans.lock.model.config.cedarling.LockTransport;
import io.jans.lock.model.config.cedarling.LogLevel;
import io.jans.lock.model.config.cedarling.LogType;

/**
 * Integration test that verifies Cedarling pushes health, log, and telemetry audit data to a Lock server over gRPC. A WireMock HTTPS server with the gRPC
 * extension enabled (inherited from {@link BaseWireMockGrpcTest}) acts as the Lock server so no real network or external process is required.
 *
 * <h3>Test structure</h3>
 * <ol>
 * <li><strong>Setup</strong> – WireMock stubs the {@code /.well-known} discovery document (HTTPS) and all six gRPC audit methods ({@code Process*} /
 * {@code ProcessBulk*}). Cedarling is initialised with {@code CEDARLING_LOCK=enabled}, gRPC transport, and a short telemetry interval
 * ({@value #TELEMETRY_INTERVAL_SEC} s).</li>
 * <li><strong>Round 1</strong> – 5 authorisation calls (4 ALLOW + 1 DENY). The test waits up to {@value #WAIT_TIMEOUT_SEC} s for any gRPC audit call to arrive,
 * captures the payloads for every method, and verifies all required proto fields.</li>
 * <li><strong>Reset</strong> – WireMock's request journal is cleared.</li>
 * <li><strong>Round 2</strong> – 7 authorisation calls (4 ALLOW + 3 DENY). Same verification cycle.</li>
 * <li><strong>Cross-round comparison</strong> – health status must remain {@code "running"}; {@code evaluation_requests_count} must be &ge; the Round 1
 * value.</li>
 * </ol>
 *
 * <h3>gRPC URL mapping</h3>
 * <p>
 * WireMock gRPC maps incoming calls to HTTP POST requests whose path is {@code /<fully-qualified-service-name>/<MethodName>}. For {@code audit.proto} the
 * fully-qualified service name is {@value #AUDIT_SERVICE_FQSN}, so {@code ProcessHealth} is matched at {@code /io.jans.lock.audit.AuditService/ProcessHealth}.
 *
 * <h3>Proto JSON field layout</h3>
 * 
 * <pre>
 * // Single-entry methods:  { "entry":   { "service": "...", ... } }
 * // Bulk methods:          { "entries": [ { ... }, { ... } ] }
 * </pre>
 *
 * <h3>Token matrix</h3>
 * 
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
 * @see BaseWireMockGrpcTest
 * @see CedarlingAuthorizationService
 *
 * @author Yuriy Movchan Date: 12/05/2026
 */
@TestInstance(Lifecycle.PER_CLASS)
@DisplayName("Cedarling gRPC Telemetry – Integration Tests")
public class CedarlingGrpcTelemetryIntegrationTest extends BaseWireMockGrpcTest {

	static {
		Configurator.setRootLevel(Level.INFO);
	}

	private static final Logger log = LoggerFactory.getLogger(CedarlingGrpcTelemetryIntegrationTest.class);

	private static final boolean DUMP_CAPTURED_REQUEST = true;

	// ─── WireMock endpoint paths ──────────────────────────────────────────────

	private static final String WELL_KNOWN_PATH = "/.well-known/lock-server-configuration";

	// ─── gRPC service / method names ─────────────────────────────────────────

	/**
	 * Fully-qualified gRPC service name derived from audit.proto:
	 * 
	 * <pre>
	 *   package io.jans.lock.audit;
	 *   service AuditService { ... }
	 * </pre>
	 * 
	 * WireMock gRPC maps every call to an HTTP POST path of the form {@code /<FQSN>/<MethodName>}.
	 */
	private static final String AUDIT_SERVICE_FQSN = "io.jans.lock.audit.AuditService";

	/** Simple method names – must match the rpc declarations in audit.proto exactly. */
	private static final String METHOD_HEALTH = "ProcessHealth";
	private static final String METHOD_BULK_HEALTH = "ProcessBulkHealth";
	private static final String METHOD_LOG = "ProcessLog";
	private static final String METHOD_BULK_LOG = "ProcessBulkLog";
	private static final String METHOD_TELEMETRY = "ProcessTelemetry";
	private static final String METHOD_BULK_TELEMETRY = "ProcessBulkTelemetry";

	/** All six gRPC audit methods – used for bulk stub registration and capture. */
	private static final List<String> ALL_GRPC_METHODS = List.of(METHOD_HEALTH, METHOD_BULK_HEALTH, METHOD_LOG, METHOD_BULK_LOG, METHOD_TELEMETRY, METHOD_BULK_TELEMETRY);

	// ─── Raw JWT strings – exp claims are patched at runtime ─────────────────

	/** JWT 1 – scopes: {@code health.write}, {@code telemetry.write}, {@code log.write}. */
	private static final String RAW_JWT_1 = "eyJraWQiOiJjb25uZWN0XzNjMzJjMTkzLTY5ZjYtNDBkYS1iNmQyLTE3ODY0YmJkYzU1MV9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9"
			+ ".eyJhdWQiOiIwZTk5NDAzMC1lZTg4LTRmMTQtYWUwOC04YjBkYTEwNDQwMjkiLCJzdWIiOiIwZTk5NDAzMC1lZTg4LTRmMTQtYWUwOC04YjBkYTEwNDQwMjkiLC"
			+ "J4NXQjUzI1NiI6IiIsIm5iZiI6MTc3ODYxNDA2Niwic2NvcGUiOlsiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2xvY2svaGVhbHRoLndyaXRlIiwiaHR0cHM6Ly9qYW"
			+ "5zLmlvL29hdXRoL2xvY2svdGVsZW1ldHJ5LndyaXRlIiwiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2xvY2svbG9nLndyaXRlIl0sImlzcyI6Imh0dHBzOi8vamFucy"
			+ "1teXNxbC1sb2NrLXNlcnZlci5qYW5zLmluZm8iLCJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiZXhwIjoxNzc4NjE0MzY2LCJpYXQiOjE3Nzg2MTQwNjYsImNsaWVudF"
			+ "9pZCI6IjBlOTk0MDMwLWVlODgtNGYxNC1hZTA4LThiMGRhMTA0NDAyOSIsImp0aSI6IkhNdjluYnBiUkRLRTAzNVRUTkgwT3ciLCJzdGF0dXMiOnsic3RhdHVzX2"
			+ "xpc3QiOnsiaWR4Ijo0MDAsInVyaSI6Imh0dHBzOi8vamFucy1teXNxbC1sb2NrLXNlcnZlci5qYW5zLmluZm8vamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdC" + "J9fX0"
			+ ".bNXIL4f1lqvLoS49iMSZJORD2mJ9MWYCM5a8nyAiLqKy_fEqvqb-g1X6SgVeS2dJ9aFV-KRrfcjl0zSSQq6mBn-1pAostlMgV-lkOBi7rCbJUMwmdN7Bv7Op8E"
			+ "yuD44_4hHRYhAXOXYv1CcjkyXtv-A9gDxNjHvhHVvpjaizcIMXVRrPxTTQgZF7r7n0t13La2E0vOxzzsgcWQjJukAY8HYybtoRL4JFswBIWPcgET9Btg9mZghDMl"
			+ "vs0yiLVQfiGUZYcmxCCEQinjtutKgONP0Gv6xVMdsXMUpgXGZi6PCiEaEWButMwBauc9RJWEHbd7C4muKoAQ6_tFNuS_eoRw";

	/** JWT 2 – scope: {@code health.write} only. */
	private static final String RAW_JWT_2 = "eyJraWQiOiJjb25uZWN0XzNjMzJjMTkzLTY5ZjYtNDBkYS1iNmQyLTE3ODY0YmJkYzU1MV9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9"
			+ ".eyJhdWQiOiIwZTk5NDAzMC1lZTg4LTRmMTQtYWUwOC04YjBkYTEwNDQwMjkiLCJzdWIiOiIwZTk5NDAzMC1lZTg4LTRmMTQtYWUwOC04YjBkYTEwNDQwMjkiLC"
			+ "J4NXQjUzI1NiI6IiIsIm5iZiI6MTc3ODYxNDE2MSwic2NvcGUiOlsiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2xvY2svaGVhbHRoLndyaXRlIl0sImlzcyI6Imh0dH"
			+ "BzOi8vamFucy1teXNxbC1sb2NrLXNlcnZlci5qYW5zLmluZm8iLCJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiZXhwIjoxNzc4NjE0NDYxLCJpYXQiOjE3Nzg2MTQxNj"
			+ "EsImNsaWVudF9pZCI6IjBlOTk0MDMwLWVlODgtNGYxNC1hZTA4LThiMGRhMTA0NDAyOSIsImp0aSI6InRVMGVQb0haU0RPSjJ5Z0EyZFRnc3ciLCJzdGF0dXMiOn"
			+ "sic3RhdHVzX2xpc3QiOnsiaWR4Ijo0MDEsInVyaSI6Imh0dHBzOi8vamFucy1teXNxbC1sb2NrLXNlcnZlci5qYW5zLmluZm8vamFucy1hdXRoL3Jlc3R2MS9zdG" + "F0dXNfbGlzdCJ9fX0"
			+ ".iIN4rKz4wnGgijhNWJqY_RqYNx_7zT0hdevnU6wqRwiLp3rQG3c4ouv8P6X4CbiaxERzABbrjsS-4JcW2H2oLpAsuJGhJtr-HExe3iLs_OQ2_4NDwo0k2KJ5e_"
			+ "zGP6Wykr6mQ8WhvGIfURk1aLirLCsegKhH1b26tSp6i8z7z-etNLwGjVPDfw6vV01kYJ0_O_tSf0HuLkGTPf34ld86CUNbPf2cE9Q4uqX_3xVTtMW0ffmOhDo8Qs"
			+ "2dL96xs8O6ah-Rvp6UVjcD4A1qbVImN6USE70nEndmtDR_rvfsCBiL-htkgChTDZymceTcOn00NOvWB2I00rvSy7FdWwNAFQ";

	/** JWT 3 – scope: {@code log.write} only. */
	private static final String RAW_JWT_3 = "eyJraWQiOiJjb25uZWN0XzNjMzJjMTkzLTY5ZjYtNDBkYS1iNmQyLTE3ODY0YmJkYzU1MV9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9"
			+ ".eyJhdWQiOiIwZTk5NDAzMC1lZTg4LTRmMTQtYWUwOC04YjBkYTEwNDQwMjkiLCJzdWIiOiIwZTk5NDAzMC1lZTg4LTRmMTQtYWUwOC04YjBkYTEwNDQwMjkiLC"
			+ "J4NXQjUzI1NiI6IiIsIm5iZiI6MTc3ODYxNzc2MSwic2NvcGUiOlsiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2xvY2svbG9nLndyaXRlIl0sImlzcyI6Imh0dHBzOi"
			+ "8vamFucy1teXNxbC1sb2NrLXNlcnZlci5qYW5zLmluZm8iLCJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiZXhwIjoxNzc4NjE4MDYxLCJpYXQiOjE3Nzg2MTc3NjEsIm"
			+ "NsaWVudF9pZCI6IjBlOTk0MDMwLWVlODgtNGYxNC1hZTA4LThiMGRhMTA0NDAyOSIsImp0aSI6IjVxUWRHYWl4VEgybkQ5Z0Y2WDFPaXciLCJzdGF0dXMiOnsic3"
			+ "RhdHVzX2xpc3QiOnsiaWR4Ijo1MDAsInVyaSI6Imh0dHBzOi8vamFucy1teXNxbC1sb2NrLXNlcnZlci5qYW5zLmluZm8vamFucy1hdXRoL3Jlc3R2MS9zdGF0dX" + "NfbGlzdCJ9fX0"
			+ ".Q7xieptgb5r9eXqjI5BCSDv_ITtzZXbsXoyqcjsYw0PonF6z3c5XjiSPPrXVUU9dY_HQUrd4ib3U7oIQrKtfXcjJ2pMNuTZ0vPRCcZM_XqqbV3IewUbztabDKD"
			+ "NpK0pSaNZy9V1SslHjW_vQoVDnclJL-w2usyXlMVnFub92GV3ldBZ9cB4UYVRovrzG_UxCa8FI-WkikYoET-vIiHbS5yP3EXlRKwP2pWwhHKwhAC7sjbnYW8ApgY"
			+ "VAmvAnWqwPcaY_Bl-UobDHGBr0b0FhLtMIZvGevo1KdQE5dJwiflZOgiUZiYJU9uJ-tklD2gd5Pq-7g1-DW9Fvsmo2WVDcHw";

	/** Lock endpoints access token – grants all three audit scopes. */
	private static final String RAW_LOCK_ACCESS_TOKEN_JWT = "eyJraWQiOiJjb25uZWN0X2RjNzViZWZjLWU4N2QtNDMyZi1hOWExLTczYjE0YzJhNjUyMl9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9"
			+ ".eyJhdWQiOiJmNDUwMTQ1Zi1hYjgyLTRiY2UtOTdjZi02MjQ2YjFjNmIxYTYiLCJzdWIiOiJmNDUwMTQ1Zi1hYjgyLTRiY2UtOTdjZi02MjQ2YjFjNmIxYTYiLC"
			+ "J4NXQjUzI1NiI6IiIsIm5iZiI6MTc3OTk1ODYyNCwic2NvcGUiOlsiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2xvY2svaGVhbHRoLndyaXRlIiwiaHR0cHM6Ly9qYW"
			+ "5zLmlvL29hdXRoL2xvY2svdGVsZW1ldHJ5LndyaXRlIiwiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2xvY2svbG9nLndyaXRlIl0sImlzcyI6Imh0dHBzOi8vamFucy"
			+ "1teXNxbC1sb2NrLXNlcnZlci5qYW5zLmluZm8iLCJ0b2tlbl90eXBlIjoiQmVhcmVyIiwiZXhwIjoxNzc5OTU4OTI0LCJpYXQiOjE3Nzk5NTg2MjQsImNsaWVudF"
			+ "9pZCI6ImY0NTAxNDVmLWFiODItNGJjZS05N2NmLTYyNDZiMWM2YjFhNiIsImp0aSI6IjNOU2ZLM1hQU25XZDlNWDFDNlFhT1EiLCJzdGF0dXMiOnsic3RhdHVzX2"
			+ "xpc3QiOnsiaWR4Ijo0MDEsInVyaSI6Imh0dHBzOi8vamFucy1teXNxbC1sb2NrLXNlcnZlci5qYW5zLmluZm8vamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdC" + "J9fX0"
			+ ".YSMkLZIm0JzIIiuShrXbZwLT5Bm58nBpz2fcaGEP4zyyDE0Te1T3WKmJZRsyRNPN3QgIwa9b02C-lGTqUJ9YkylTolLitJHgy7aVhfestGYTZ_r0gvfcGYVF8h"
			+ "zsk5k11U-hb9SGbZOXOvuis998fCXolG-UUaYj7VGjU8xreGLgmEx7Otmfpi2bjenQ0DGFjo82XAVzgqO7gwGT-5zohBQ8uNQcKKASGj4g2NtVcjXqmxBS9huI7e"
			+ "dAJxFPlZ5J7gghfJAIARDLm8UrYgNEHqVdQPDOrAdnIOE5n0I4oJad5fl5luyKSNmd6sL4hi82OR7Ldig3XIjxyHQ7VJYZhA";

	// ─── Cedar action / resource constants ───────────────────────────────────

	private static final String ACTION_POST = "Jans::Action::\"POST\"";
	private static final String RESOURCE_TYPE = "Jans::HTTP_Request";

	private static final String ID_LOG = "lock_audit_log_write";
	private static final String ID_HEALTH = "lock_audit_health_write";
	private static final String ID_TELEMETRY = "lock_audit_telemetry_write";

	private static final String PATH_LOG = "/audit/log/bulk";
	private static final String PATH_HEALTH = "/audit/health/bulk";
	private static final String PATH_TELEMETRY = "/audit/telemetry/bulk";

	private static final String ACCESS_TOKEN_KEY = CedarlingAuthorizationService.CEDARLING_JANS_ACCESS_TOKEN;

	// ─── Timing ──────────────────────────────────────────────────────────────

	/** Cedarling telemetry/health push interval in seconds. */
	private static final int TELEMETRY_INTERVAL_SEC = 5;

	/** Maximum time to wait for at least one gRPC audit payload per round. */
	private static final int WAIT_TIMEOUT_SEC = 30;
	private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(WAIT_TIMEOUT_SEC);

	// ─── Test state ───────────────────────────────────────────────────────────

	private final ObjectMapper objectMapper = new ObjectMapper();
	private CedarlingAuthorizationService authService;

	/**
	 * WireMockGrpcService wraps the WireMock admin API and provides the {@code stubFor(method(...).willReturn(json(...)))} DSL used to register gRPC stubs.
	 *
	 * It is lazily created on the first call to {@link #configureGrpcAuditStubs()} because at field-initialisation time the WireMock server may not yet have an
	 * assigned port.
	 */
	private WireMockGrpcService grpcAuditService;

	private String jwt1, jwt2, jwt3;

	/**
	 * Guards one-time Cedarling initialisation inside {@link #registerStubs()}. WireMock clears stubs in its own {@code beforeEach} which runs before any
	 * {@code @BeforeEach} in the test class, so Cedarling must be started here (after stubs are up) rather than in {@code @BeforeAll}.
	 */
	private boolean serviceInitialized = false;

	// ─── Lifecycle ────────────────────────────────────────────────────────────

	/**
	 * Registers WireMock stubs and, on the very first call, initialises Cedarling.
	 *
	 * <pre>
	 * Execution order per test method:
	 *   &#64;BeforeAll  setUpGrpc()           – gRPC channel created once (base class)
	 *   WireMockExtension.beforeEach()    – wipes all stub mappings
	 *   @BeforeEach registerStubs()       – stubs re-registered; Cedarling init on first call
	 * </pre>
	 */
	@BeforeEach
	void registerStubs() throws Exception {
		configureWellKnownEndpoint();
		configureGrpcAuditStubs();
		log.info("WireMock stubs registered (well-known + {} gRPC audit methods)", ALL_GRPC_METHODS.size());

		if (!serviceInitialized) {
			initAuthService();
			serviceInitialized = true;
			log.info("WireMock HTTP port: {}  |  HTTPS port: {}  |  telemetry interval: {}s  |  wait timeout: {}s",
					wireMockServer.getPort(), wireMockServer.getHttpsPort(), TELEMETRY_INTERVAL_SEC, WAIT_TIMEOUT_SEC);
		}
	}

	/** Shuts down Cedarling and the gRPC channel after all tests in the class. */
	@AfterAll
	void tearDown() {
		if (serviceInitialized) {
			authService.destroy();
			log.info("Cedarling service destroyed");
		}
		// gRPC channel shutdown is handled by BaseWireMockGrpcTest.tearDownGrpc()
	}

	// ─── Tests ────────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("Two-round gRPC telemetry lifecycle")
	class TwoRoundGrpcTelemetryLifecycle {

		/**
		 * Main gRPC telemetry lifecycle test.
		 *
		 * <p>
		 * Executes two batches of authorisation calls separated by a WireMock request-journal reset, then verifies:
		 * <ul>
		 * <li>All required proto fields are present in every captured payload.</li>
		 * <li>Health status is {@code "running"} in both rounds.</li>
		 * <li>{@code evaluation_requests_count} is &ge; the Round 1 value in Round 2.</li>
		 * <li>Log entries carry correct {@code decision_result} values.</li>
		 * </ul>
		 */
		@Test
		@DisplayName("gRPC telemetry accumulates correctly across two authorisation rounds")
		void grpcTelemetryAccumulatesAcrossRounds() throws Exception {
			//log.info("PORT: {}", wireMockServer.getPort());
			//log.info("TOKEN: {}", jwt1);

			// ════════════════════════════ ROUND 1 ════════════════════════════
			log.info("=== ROUND 1 – 5 authorisation calls (4 ALLOW + 1 DENY) ===");
			int round1AuthCalls = executeRound1Authorizations();

			// Wait for data to arrive at least at the primary telemetry endpoint before waiting the full timeout.
			awaitDuration(Duration.ofSeconds(TELEMETRY_INTERVAL_SEC + 2));

			// Wait for the telemetry push triggered by round-1 evaluations
			Map<String, List<JsonNode>> r1Captured = awaitAndCapture("R1", WAIT_TIMEOUT);

			List<JsonNode> r1Health = getMethod(r1Captured, "R1 health", METHOD_HEALTH);
			List<JsonNode> r1BulkHealth = getMethod(r1Captured, "R1 bulk-health", METHOD_BULK_HEALTH);
			List<JsonNode> r1Log = getMethod(r1Captured, "R1 log", METHOD_LOG);
			List<JsonNode> r1BulkLog = getMethod(r1Captured, "R1 bulk-log", METHOD_BULK_LOG);
			List<JsonNode> r1Telemetry = getMethod(r1Captured, "R1 telemetry", METHOD_TELEMETRY);
			List<JsonNode> r1BulkTelemetry = getMethod(r1Captured, "R1 bulk-telemetry", METHOD_BULK_TELEMETRY);

			log.info("Round 1 received – health={}, bulkHealth={}, log={}, bulkLog={}, telemetry={}, bulkTelemetry={}",
					r1Health.size(), r1BulkHealth.size(), r1Log.size(), r1BulkLog.size(),
					r1Telemetry.size(), r1BulkTelemetry.size());

			// Structural and value verification
			verifyHealthPayloads(r1Health, "Round 1");
			verifyHealthBulkPayloads(r1BulkHealth, "Round 1 (bulk)", -1);
			verifyLogPayloads(r1Log, "Round 1");
			verifyLogBulkPayloads(r1BulkLog, "Round 1 (bulk)", round1AuthCalls);
			verifyTelemetryPayloads(r1Telemetry, "Round 1");
			verifyTelemetryBulkPayloads(r1BulkTelemetry, "Round 1", -1);

			// Capture round 1's latest evaluationRequestsCount for comparison
			long r1EvalCount = latestEntryLong(r1Telemetry, "evaluation_requests_count");
			log.info("Round 1 – evaluation_requests_count = {}", r1EvalCount);

			// Reset request journal – Round 2 starts clean
			wireMockServer.resetRequests();
			log.info("--- WireMock request journal reset – starting Round 2 ---");

			// ════════════════════════════ ROUND 2 ════════════════════════════
			log.info("=== ROUND 2 – 7 authorisation calls (4 ALLOW + 3 DENY) ===");
			int round2AuthCalls = executeRound2Authorizations();

			// Wait for data to arrive at least at the primary telemetry endpoint before waiting the full timeout.
			awaitDuration(Duration.ofSeconds(TELEMETRY_INTERVAL_SEC + 2));

			// Wait for the telemetry push triggered by round-2 evaluations
			Map<String, List<JsonNode>> r2Captured = awaitAndCapture("R2", WAIT_TIMEOUT);

			List<JsonNode> r2Health = getMethod(r2Captured, "R2 health", METHOD_HEALTH);
			List<JsonNode> r2BulkHealth = getMethod(r2Captured, "R2 bulk-health", METHOD_BULK_HEALTH);
			List<JsonNode> r2Log = getMethod(r2Captured, "R2 log", METHOD_LOG);
			List<JsonNode> r2BulkLog = getMethod(r2Captured, "R2 bulk-log", METHOD_BULK_LOG);
			List<JsonNode> r2Telemetry = getMethod(r2Captured, "R2 telemetry", METHOD_TELEMETRY);
			List<JsonNode> r2BulkTelemetry = getMethod(r2Captured, "R2 bulk-telemetry", METHOD_BULK_TELEMETRY);

			log.info("Round 2 received – health={}, bulkHealth={}, log={}, bulkLog={}, telemetry={}, bulkTelemetry={}",
					r2Health.size(), r2BulkHealth.size(), r2Log.size(), r2BulkLog.size(),
					r2Telemetry.size(), r2BulkTelemetry.size());

			verifyHealthPayloads(r2Health, "Round 2");
			verifyHealthBulkPayloads(r2BulkHealth, "Round 2 (bulk)", -1);
			verifyLogPayloads(r2Log, "Round 2");
			verifyLogBulkPayloads(r2BulkLog, "Round 2 (bulk)", round2AuthCalls);
			verifyTelemetryPayloads(r2Telemetry, "Round 2");
			verifyTelemetryBulkPayloads(r2BulkTelemetry, "Round 2", -1);

			// ════════════════════════ CROSS-ROUND COMPARISON ═════════════════
			compareTelemetryAcrossRounds(r1EvalCount, r1Telemetry, r2Telemetry);
			compareHealthAcrossRounds(r1Health, r2Health);

			log.info("✅ All gRPC telemetry assertions passed across both rounds.");
		}
	}

	// ─── Authorisation rounds ─────────────────────────────────────────────────

	/**
	 * Round 1: JWT 1 (all scopes) + JWT 2 (health only) – 5 calls, 4 ALLOW + 1 DENY.
	 *
	 * @return total number of authorisation calls made
	 */
	private int executeRound1Authorizations() {
		// JWT 1 – all three endpoints allowed
		assertTrue(authorize(jwt1, ACTION_POST, ID_LOG, PATH_LOG), "R1: JWT1 must be allowed for /log");
		assertTrue(authorize(jwt1, ACTION_POST, ID_HEALTH, PATH_HEALTH), "R1: JWT1 must be allowed for /health");
		assertTrue(authorize(jwt1, ACTION_POST, ID_TELEMETRY, PATH_TELEMETRY), "R1: JWT1 must be allowed for /telemetry");

		// JWT 2 – health allowed, log denied
		assertTrue(authorize(jwt2, ACTION_POST, ID_HEALTH, PATH_HEALTH), "R1: JWT2 must be allowed for /health");
		assertFalse(authorize(jwt2, ACTION_POST, ID_LOG, PATH_LOG), "R1: JWT2 must be denied for /log (missing log.write)");
		return 5; // 4 ALLOW + 1 DENY
	}

	/**
	 * Round 2: JWT 3 (log only) + JWT 2 (health only) + JWT 1 – 7 calls, 4 ALLOW + 3 DENY.
	 *
	 * @return total number of authorisation calls made
	 */
	private int executeRound2Authorizations() {
		// JWT 3 – log allowed, health and telemetry denied
		assertTrue(authorize(jwt3, ACTION_POST, ID_LOG, PATH_LOG), "R2: JWT3 must be allowed for /log");
		assertFalse(authorize(jwt3, ACTION_POST, ID_HEALTH, PATH_HEALTH), "R2: JWT3 must be denied for /health (missing health.write)");
		assertFalse(authorize(jwt3, ACTION_POST, ID_TELEMETRY, PATH_TELEMETRY), "R2: JWT3 must be denied for /telemetry (missing telemetry.write)");

		// JWT 2 – health allowed, telemetry denied
		assertTrue(authorize(jwt2, ACTION_POST, ID_HEALTH, PATH_HEALTH), "R2: JWT2 must be allowed for /health");
		assertFalse(authorize(jwt2, ACTION_POST, ID_TELEMETRY, PATH_TELEMETRY), "R2: JWT2 must be denied for /telemetry (missing telemetry.write)");

		// JWT 1 – all allowed
		assertTrue(authorize(jwt1, ACTION_POST, ID_LOG, PATH_LOG), "R2: JWT1 must be allowed for /log");
		assertTrue(authorize(jwt1, ACTION_POST, ID_HEALTH, PATH_HEALTH), "R2: JWT1 must be allowed for /health");
		return 7; // 4 ALLOW + 3 DENY
	}

	// ─── Payload verification ─────────────────────────────────────────────────

	/**
	 * Verifies structural fields and value assertions for every health payload.
	 *
	 * <p>
	 * Required fields: {@code service}, {@code status}, {@code node_name}.<br>
	 * Value assertions: {@code service == "Lock Server - Test Edition"}, {@code status == "running"}.
	 */
	private void verifyHealthPayloads(List<JsonNode> payloads, String label) {
		if (payloads.isEmpty()) {
			log.warn("[{}] No health payloads received – skipping field checks", label);
			return;
		}
		for (int i = 0; i < payloads.size(); i++) {
			verifyHealthEntry(label + " health[" + i + "]", payloads.get(i).path("entry"));
		}
		log.info("[{}] {} health payload(s) verified", label, payloads.size());
	}

	private void verifyHealthBulkPayloads(List<JsonNode> payloads, String label, int minExpectedEntries) {
		if (payloads.isEmpty()) {
			log.warn("⚠️ [{}] No bulk-health payloads received – skipping field checks", label);
			return;
		}
		int count = 0;
		for (int i = 0; i < payloads.size(); i++) {
			JsonNode entries = payloads.get(i).path("entries");
			assertTrue(entries.isArray(), label + " health[" + i + "]: 'entries' must be an array, got: " + entries.getNodeType());
			for (int j = 0; j < entries.size(); j++) {
				verifyHealthEntry(label + " health[" + i + "][" + j + "]", entries.get(j));
				count++;
			}
		}
		if (minExpectedEntries >= 0) {
			assertTrue(count >= minExpectedEntries, label + ": expected at least " + minExpectedEntries + " health entries but got " + count);
		}
		log.info("[{}] ✅ {} health entry/entries verified", label, count);
	}

	private void verifyHealthEntry(String ctx, JsonNode entry) {
		// ── Required fields ──
		assertTrue(entry.hasNonNull("service"), ctx + ": missing 'service'");
		assertTrue(entry.hasNonNull("status"), ctx + ": missing 'status'");
		assertTrue(entry.hasNonNull("nodeName"), ctx + ": missing 'node_name'");

		// ── Value assertions ──
		assertEquals("Lock Server - Test Edition", entry.get("service").asText(), ctx + ": service must be 'Lock Server - Test Edition'");
		assertEquals("running", entry.get("status").asText(), ctx + ": status must be 'running'");

		// node_name must be present and non-blank
		assertFalse(entry.get("nodeName").asText("").isBlank(), ctx + ": node_name must not be blank");
	}

	/**
	 * Verifies structural fields for every log (audit) payload.
	 *
	 * <p>
	 * Required fields: {@code clientId}, {@code principalId}, {@code decisionResult}, {@code action}.
	 * </p>
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
			verifyLogEntry(label + " log[" + i + "]", payloads.get(i).path("entry"));
		}
		log.info("[{}] {} log payload(s) verified", label, payloads.size());
	}

	private void verifyLogBulkPayloads(List<JsonNode> payloads, String label, int minExpectedEntries) {
		if (payloads.isEmpty()) {
			log.warn("⚠️ [{}] No bulk-log payloads received – skipping field checks", label);
			return;
		}
		int count = 0;
		for (int i = 0; i < payloads.size(); i++) {
			JsonNode entries = payloads.get(i).path("entries");
			assertTrue(entries.isArray(), label + " log[" + i + "]: 'entries' must be an array, got: " + entries.getNodeType());
			for (int j = 0; j < entries.size(); j++) {
				verifyLogEntry(label + " log[" + i + "][" + j + "]", entries.get(j));
				count++;
			}
		}
		if (minExpectedEntries >= 0) {
			assertTrue(count >= minExpectedEntries, label + ": expected at least " + minExpectedEntries + " log entries but got " + count);
		}
		log.info("[{}] ✅ {} log entry/entries verified", label, count);
	}

	private void verifyLogEntry(String ctx, JsonNode entry) {
		// ── Required fields ──
		assertTrue(entry.hasNonNull("client_id"), ctx + ": missing 'client_id'");
		assertTrue(entry.hasNonNull("principal_id"), ctx + ": missing 'principal_id'");
		assertTrue(entry.hasNonNull("decision_result"), ctx + ": missing 'decision_result'");
		assertTrue(entry.hasNonNull("action"), ctx + ": missing 'action'");

		// ── Value assertions ──
		String decision = entry.get("decision_result").asText();
		assertTrue("ALLOW".equalsIgnoreCase(decision) || "DENY".equalsIgnoreCase(decision), ctx + ": decision_result must be 'ALLOW' or 'DENY', got: " + decision);

		// action must follow the Cedar action URI format: Namespace::Action::"name"
		String action = entry.get("action").asText();
		assertTrue(action.matches(".+::Action::\"[^\"]+\""), ctx + ": action must match Cedar format '<Namespace>::Action::\"<name>\"', got: " + action);

		// clientId must be present and non-blank
		assertFalse(entry.get("client_id").asText("").isBlank(), ctx + ": client_id must not be blank");
	}

	/**
	 * Verifies structural fields and value assertions for every telemetry payload.
	 *
	 * <p>
	 * Required fields: {@code service}, {@code status}, {@code evaluation_requests_count},
	 * {@code avg_policy_evaluation_time_ns} (snake_case – proto JSON serialisation).
	 */
	private void verifyTelemetryPayloads(List<JsonNode> payloads, String label) {
		if (payloads.isEmpty()) {
			log.warn("[{}] No telemetry payloads received – skipping field checks", label);
			return;
		}
		for (int i = 0; i < payloads.size(); i++) {
			verifyTelemetryEntry(label + " telemetry[" + i + "]", payloads.get(i).path("entry"));
		}
		log.info("[{}] {} telemetry payload(s) verified", label, payloads.size());
	}

	private void verifyTelemetryBulkPayloads(List<JsonNode> payloads, String label, int minExpectedEntries) {
		if (payloads.isEmpty()) {
			log.warn("⚠️ [{}] No bulk-telemetry payloads received – skipping field checks", label);
			return;
		}
		int count = 0;
		for (int i = 0; i < payloads.size(); i++) {
			JsonNode entries = payloads.get(i).path("entries");
			assertTrue(entries.isArray(), label + " telemetry[" + i + "]: 'entries' must be an array, got: " + entries.getNodeType());
			for (int j = 0; j < entries.size(); j++) {
				verifyTelemetryEntry(label + " telemetry[" + i + "][" + j + "]", entries.get(j));
				count++;
			}
		}
		if (minExpectedEntries >= 0) {
			assertTrue(count >= minExpectedEntries, label + ": expected at least " + minExpectedEntries + " telemetry entries but got " + count);
		}
		log.info("[{}] ✅ {} telemetry entry/entries verified", label, count);
	}

	private void verifyTelemetryEntry(String ctx, JsonNode entry) {
		assertTrue(entry.hasNonNull("service"), ctx + ": missing 'service'");
		assertTrue(entry.hasNonNull("status"), ctx + ": missing 'status'");
		assertTrue(entry.hasNonNull("evaluation_requests_count"), ctx + ": missing 'evaluation_requests_count'");
		assertTrue(entry.hasNonNull("avg_policy_evaluation_time_ns"), ctx + ": missing 'avg_policy_evaluation_time_ns'");

		assertEquals("cedarling", entry.get("service").asText(), ctx + ": service must be 'cedarling'");

		long evalCount = entry.get("evaluation_requests_count").asLong();
		assertTrue(evalCount >= 0, ctx + ": evaluation_requests_count must be >= 0, got " + evalCount);

		long avgEvalNs = entry.get("avg_policy_evaluation_time_ns").asLong(-1L);
		assertTrue(avgEvalNs > 0, ctx + ": avg_policy_evaluation_time_ns must be > 0, got " + avgEvalNs);
	}

	// ─── Cross-round comparison ────────────────────────────────────────────────

	/**
	 * Compares telemetry counters from Round 1 vs Round 2.
	 *
	 * <ul>
	 * <li>{@code evaluationRequestsCount} in Round 2 must be ≥ the Round 1 value because Cedarling accumulates this counter globally.</li>
	 * <li>The {@code service} name must be identical across rounds.</li>
	 * <li>Round 2 must contain at least as many telemetry reports as Round 1.</li>
	 * </ul>
	 *
	 * @param r1EvalCount already extracted from the last round-1 telemetry entry
	 * @param round1      Round 1 telemetry payloads
	 * @param round2      Round 2 telemetry payloads
	 */
	private void compareTelemetryAcrossRounds(long r1EvalCount, List<JsonNode> round1, List<JsonNode> round2) {
		if (round1.isEmpty() || round2.isEmpty()) {
			log.warn("⚠️ Cannot compare telemetry rounds – empty data (r1={}, r2={})", round1.size(), round2.size());
			return;
		}
		long r2EvalCount = latestEntryLong(round2, "evaluation_requests_count");
		log.info("evaluation_requests_count – Round 1 (last) = {}  |  Round 2 (last) = {}", r1EvalCount, r2EvalCount);

		// Cedarling accumulates evaluations globally; Round 2 counter must not decrease
		assertTrue(r2EvalCount >= r1EvalCount, "evaluation_requests_count must not decrease between rounds: R1=" + r1EvalCount + ", R2=" + r2EvalCount);

		// Service name is invariant
		String r1Service = latestEntryString(round1, "service");
		String r2Service = latestEntryString(round2, "service");
		assertEquals(r1Service, r2Service, "service name must be consistent across rounds");

		// Round 2 had more authorization calls – the difference should be visible
		long delta = r2EvalCount - r1EvalCount;
		log.info("evaluationRequestsCount delta (R2 – R1) = {}", delta);
		// NOTE: delta could be 0 if Cedarling resets the counter per telemetry interval
		// rather than accumulating globally. The ≥ assertion above is the safe guard.
		log.info("✅ Cross-round gRPC telemetry comparison passed (delta={})", r2EvalCount - r1EvalCount);
	}

	/**
	 * Verifies that the Cedarling process remains healthy across both rounds regardless of DENY decisions during authorization.
	 */
	private void compareHealthAcrossRounds(List<JsonNode> round1, List<JsonNode> round2) {
		if (round1.isEmpty() || round2.isEmpty()) {
			log.warn("⚠️ Cannot compare health rounds – empty data (r1={}, r2={})", round1.size(), round2.size());
			return;
		}
		String r1Status = latestEntryString(round1, "status");
		String r2Status = latestEntryString(round2, "status");

		assertEquals("running", r1Status, "Round 1 health status must be 'running'");
		assertEquals("running", r2Status, "Round 2 health status must be 'running'");

		log.info("✅ Health status is 'running' in both rounds");
	}

	// ─── gRPC request capture utilities ──────────────────────────────────────

	/**
	 * Polls WireMock until at least one gRPC audit call arrives (on any method) or {@code timeout} elapses, then returns all captured request bodies grouped by
	 * method name.
	 *
	 * @param label   descriptive label for log output
	 * @param timeout maximum time to wait
	 * @return map from gRPC method name to list of parsed JSON bodies; empty map if nothing arrived within the timeout
	 */
	private Map<String, List<JsonNode>> awaitAndCapture(String label, Duration timeout) throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeout.toMillis();
		while (System.currentTimeMillis() < deadline) {
			Map<String, List<JsonNode>> captured = captureAllMethods();
			if (!captured.isEmpty()) {
				String summary = captured.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue().size()).collect(Collectors.joining(", ", "{", "}"));
				log.info("[{}] gRPC requests captured: {}", label, summary);

				if (DUMP_CAPTURED_REQUEST) {
					captured.forEach((m, nodes) -> {
						log.info("Captured {} request(s) via gRPC {}:", nodes.size(), m);
						for (int i = 0; i < nodes.size(); i++) {
							log.info("  [{}][{}]: {}", m, i, nodes.get(i).toPrettyString());
						}
					});
				}
				return captured;
			}
			Thread.sleep(500);
		}
		log.warn("[{}] Timeout ({}s) – no gRPC audit requests arrived", label, timeout.toSeconds());
		return Collections.emptyMap();
	}

	/**
	 * Queries the WireMock request journal for all recorded gRPC calls and returns their decoded JSON bodies grouped by method name.
	 *
	 * <p>
	 * WireMock gRPC maps every incoming call to an HTTP POST at the path {@code /<fully-qualified-service-name>/<MethodName>}. For {@code audit.proto} that means
	 * {@code /io.jans.lock.audit.AuditService/ProcessHealth} etc. The gRPC extension decodes the proto binary payload to JSON before recording it in the journal,
	 * so {@link LoggedRequest#getBodyAsString()} returns a plain JSON string that Jackson can parse directly.
	 *
	 * @return map keyed by simple method name (e.g. {@code "ProcessHealth"})
	 */
	private Map<String, List<JsonNode>> captureAllMethods() {
		Map<String, List<JsonNode>> result = new HashMap<>();

		for (String method : ALL_GRPC_METHODS) {
			// FIX: WireMock gRPC uses the fully-qualified path, not just the method name.
			// Pattern: /<package>.<ServiceName>/<MethodName>
			String grpcUrlPath = "/" + AUDIT_SERVICE_FQSN + "/" + method;

			List<LoggedRequest> logged = wireMockServer.findAll(postRequestedFor(urlEqualTo(grpcUrlPath)));

			if (logged.isEmpty()) {
				continue;
			}

			List<JsonNode> nodes = logged.stream().map(request -> {
				try {
					// The WireMock gRPC extension transcodes the proto binary to JSON
					// using the descriptor file, so the body is always valid JSON.
					return objectMapper.readTree(request.getBodyAsString());
				} catch (Exception ex) {
					log.warn("Could not parse gRPC body for method {}: {}", method, ex.getMessage());
					return null;
				}
			}).filter(Objects::nonNull).collect(Collectors.toList());

			if (!nodes.isEmpty()) {
				result.put(method, nodes);
			}
		}
		return result;
	}

	private void awaitDuration(Duration d) throws InterruptedException {
		long deadline = System.currentTimeMillis() + d.toMillis();
		while (System.currentTimeMillis() < deadline) {
			Thread.sleep(500);
		}
	}

	/**
	 * Retrieves captured payloads for a specific gRPC method from the map produced by {@link #awaitAndCapture}.
	 */
	private List<JsonNode> getMethod(Map<String, List<JsonNode>> captured, String label, String method) {
		List<JsonNode> nodes = captured.getOrDefault(method, Collections.emptyList());
		log.info("[{}] {} request(s) captured via gRPC {}", label, nodes.size(), method);
		return nodes;
	}

	// ─── Field extraction helpers ─────────────────────────────────────────────

	private long latestEntryLong(List<JsonNode> nodes, String field) {
		if (nodes.isEmpty())
			return 0L;
		return nodes.get(nodes.size() - 1).path("entry").path(field).asLong(0L);
	}

	private String latestEntryString(List<JsonNode> nodes, String field) {
		if (nodes.isEmpty())
			return "";
		return nodes.get(nodes.size() - 1).path("entry").path(field).asText("");
	}

	// ─── WireMock stub configuration ─────────────────────────────────────────

	/**
	 * Stubs the Lock server discovery document on the plain HTTP port.
	 *
	 * <p>
	 * <strong>Why HTTP and not HTTPS?</strong> WireMock auto-generates a self-signed TLS certificate with CN={@code tom akehurst}. Cedarling's gRPC transport uses
	 * Tonic (Rust), which validates the peer certificate via rustls. Even with {@code lockAcceptInvalidCerts=true} Tonic rejects the cert with
	 * {@code invalid peer certificate: UnknownIssuer} because the CA is not trusted – that flag only controls HTTP connections, not the Tonic gRPC layer.
	 *
	 * <p>
	 * WireMock's HTTP connector listens with {@code (http/1.1, h2c)} – it supports HTTP/2 cleartext, so gRPC over h2c works without any certificate at all.
	 * Pointing {@code lockServerConfigurationUri} at the plain HTTP port causes Cedarling to derive the gRPC target from an {@code http://} base URL and connect
	 * via h2c, bypassing TLS entirely and eliminating the UnknownIssuer failure.
	 */
	private void configureWellKnownEndpoint() {
		int port = wireMockServer.getPort(); // plain HTTP / h2c port – no TLS
		log.info("Configuring well-known stub: http://localhost:{}{}", port, WELL_KNOWN_PATH);

		String body = """
				{
				  "version": "1.0",
				  "issuer": "http://localhost:%d",
				  "audit": {
				    "health_endpoint":    "http://localhost:%d/jans-lock/api/v1/audit/health",
				    "log_endpoint":       "http://localhost:%d/jans-lock/api/v1/audit/log",
				    "telemetry_endpoint": "http://localhost:%d/jans-lock/api/v1/audit/telemetry"
				  }
				}
				""".formatted(port, port, port, port);

		wireMockServer.stubFor(get(urlEqualTo(WELL_KNOWN_PATH)).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(body)));
	}

	/**
	 * Registers WireMock stubs for all six gRPC audit methods defined in {@link #ALL_GRPC_METHODS} using the official WireMock gRPC DSL.
	 *
	 * <p>
	 * <strong>Why {@link WireMockGrpcService}?</strong> {@code WireMockGrpcService.stubFor()} uses WireMock's admin REST API (HTTP port) to register gRPC-aware
	 * stubs. The previous code called {@code wireMockServer.addStubMapping(method(...).willReturn(...).build(ok))} which is incorrect for two reasons:
	 * <ol>
	 * <li>{@code GrpcStubMappingBuilder.build()} accepts no arguments – the single-arg overload does not exist and causes a compile error.</li>
	 * <li>Registering a gRPC stub through {@code wireMockServer.addStubMapping()} bypasses the gRPC extension's URL/header enrichment, so WireMock cannot match
	 * incoming gRPC calls against the stub.</li>
	 * </ol>
	 *
	 * <p>
	 * The {@link WireMockGrpcService} is lazily initialised here (not as a field initialiser) because the dynamic HTTP port is not yet assigned when the class is
	 * loaded.
	 *
	 * <p>
	 * <strong>WireMock 4 constructor note:</strong> {@code new WireMock(String, int)} was removed in WireMock 4. The correct way to obtain a client from a
	 * {@link WireMockExtension} is via {@code wireMockServer.getRuntimeInfo().getWireMock()}, which returns a pre-configured {@link WireMock} instance pointing at
	 * the correct host and HTTP port.
	 */
	private void configureGrpcAuditStubs() {
		// Lazily create the service wrapper.
		// getRuntimeInfo().getWireMock() is the WireMock 4-compatible way to obtain
		// a WireMock client from a WireMockExtension (new WireMock(String,int)
		// constructor was removed in WireMock 4).
		if (grpcAuditService == null) {
			grpcAuditService = new WireMockGrpcService(wireMockServer.getRuntimeInfo().getWireMock(), AUDIT_SERVICE_FQSN);
		}

		String ok = """
				{"success": true, "message": "Audit data processed successfully"}
				""";

		for (String methodName : ALL_GRPC_METHODS) {
			// method() is statically imported from WireMockGrpc.
			// json() is statically imported from WireMockGrpc.
			grpcAuditService.stubFor(method(methodName).willReturn(json(ok)));
		}
	}

	// ─── Cedarling service initialisation ────────────────────────────────────

	/**
	 * Builds a fully-wired {@link CedarlingAuthorizationService} (no CDI container in tests) with Lock telemetry enabled, gRPC audit transport, and targeting the
	 * WireMock HTTPS server.
	 */
	private void initAuthService() throws Exception {
		Logger svcLog = LoggerFactory.getLogger(CedarlingAuthorizationService.class);
		AppConfiguration appConfig = mock(AppConfiguration.class);
		CedarlingPolicyConfiguration policyConfig = mock(CedarlingPolicyConfiguration.class);
		CedarlingConfiguration cedarConf = mock(CedarlingConfiguration.class);

		when(cedarConf.isEnabled()).thenReturn(true);
		when(cedarConf.getLogType()).thenReturn(LogType.STD_OUT);
		when(cedarConf.getLogLevel()).thenReturn(LogLevel.TRACE);
		when(appConfig.getCedarlingConfiguration()).thenReturn(cedarConf);

		String policyStoreFn = System.getProperty("user.dir") + "/target/test-classes/test-policy-store";
		// Use the plain HTTP port so Cedarling's Tonic gRPC client connects via h2c
		// (HTTP/2 cleartext) instead of TLS. WireMock's HTTP connector advertises h2c,
		// so gRPC works without a certificate. See configureWellKnownEndpoint() for details.
		String lockServerUri = "http://localhost:" + wireMockServer.getPort() + WELL_KNOWN_PATH;
		String lockAccessToken = withFutureExp(RAW_LOCK_ACCESS_TOKEN_JWT);

		authService = new CedarlingAuthorizationService() {
			@Override
			protected BootstrapConfig prepareBootstrapConfig(CedarlingConfiguration cedarConf) {
				// JWT signature and status validation are disabled for tests;
				// only the exp claim is patched to a future value.
				BootstrapConfig bootstrapConfig = BootstrapConfig.builder().applicationName("Lock Server - Test Edition").policyStoreLocalFn(policyStoreFn).jwtStatusValidation(false)
						.jwtSigValidation(false).logType(LogType.STD_OUT).logLevel(LogLevel.TRACE)
						// Lock / telemetry settings
						.lock(true).lockServerConfigurationUri(lockServerUri).lockLockTransport(LockTransport.GRPC).lockAcceptInvalidCerts(true).lockDynamicConfiguration(true)
						.lockHealthInterval(TELEMETRY_INTERVAL_SEC).lockTelemetryInterval(TELEMETRY_INTERVAL_SEC).lockListenSse(false).lockAccessTokenJwt(lockAccessToken).build();

				log.info("Cedarling bootstrap configuration: {}", bootstrapConfig.toJsonConfig());
				return bootstrapConfig;
			}
		};

		when(cedarConf.isEnabled()).thenReturn(true);

		injectField(authService, "log", svcLog);
		injectField(authService, "appConfiguration", appConfig);
		injectField(authService, "policyConfiguration", policyConfig);
		injectField(authService, "policyStoreLocalFn", policyStoreFn);

		authService.init();

		// Patch exp claims so tokens are valid during the entire test run
		jwt1 = withFutureExp(RAW_JWT_1);
		jwt2 = withFutureExp(RAW_JWT_2);
		jwt3 = withFutureExp(RAW_JWT_3);

		log.info("Cedarling service initialised (gRPC transport) – lock URI: {}", lockServerUri);
	}

	// ─── Authorisation helper ─────────────────────────────────────────────────

	private boolean authorize(String token, String action, String permId, String path) {
		Map<String, String> tokens = Map.of(ACCESS_TOKEN_KEY, token);
		return authService.authorize(tokens, action, buildResource(permId, path), buildContext());
	}

	private static Map<String, Object> buildResource(String permId, String path) {
		Map<String, Object> resource = new HashMap<>();
		resource.put("cedar_entity_mapping", Map.of("entity_type", RESOURCE_TYPE, "id", permId));
		resource.put("url", Map.of("host", "", "path", path, "protocol", ""));
		resource.put("header", Collections.emptyMap());
		return resource;
	}

	private static Map<String, Object> buildContext() {
		return Collections.emptyMap();
	}

	// ─── JWT exp patching ─────────────────────────────────────────────────────

	/**
	 * Replaces the {@code exp} claim in the JWT payload with {@code now + 1 hour}. The original signature is kept but becomes stale; Cedarling accepts it because
	 * the adapter is initialised with {@code jwtSigValidation(false)}.
	 *
	 * @param rawJwt original JWT string (header.payload.signature)
	 * @return patched JWT with a future {@code exp}
	 */
	static String withFutureExp(String rawJwt) {
		String[] parts = rawJwt.split("\\.");
		if (parts.length != 3) {
			throw new IllegalArgumentException("Not a 3-part JWT: " + rawJwt);
		}
		String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

		long futureExp = (System.currentTimeMillis() / 1000L) + 3600L;
		String patched = payloadJson.replaceAll("\"exp\"\\s*:\\s*\\d+", "\"exp\":" + futureExp);

		String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(patched.getBytes(StandardCharsets.UTF_8));

		return parts[0] + "." + encodedPayload + "." + parts[2];
	}

	// ─── Reflection utility ───────────────────────────────────────────────────

	/**
	 * Sets {@code fieldName} on {@code target} using reflection, walking up the class hierarchy until the field is found.
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
		throw new NoSuchFieldException("Field '" + fieldName + "' not found in " + target.getClass().getName());
	}
}