/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Smoke test proving the {@code -Dcfg=<profile>} test-profile mechanism end-to-end (task 22
 * out-of-scope follow-up): a plain, unauthenticated GET of
 * {@code /.well-known/lock-server-configuration} against whatever host the selected profile names.
 * Deliberately the smallest possible real request — this endpoint carries no
 * {@code @ProtectedApi}/{@code @ProtectedCedarlingApi} annotation (see
 * {@code WellKnownConfiguration.java}), so no OAuth client is needed to prove the profile is wired
 * correctly end to end. Skips itself under the {@code default} profile (see
 * {@link BaseLockClientTest#hasServer()}).
 */
class WellKnownConfigurationSmokeTest extends BaseLockClientTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private HttpClient client;

	@BeforeEach
	void setUp() {
		assumeTrue(hasServer(), "No real profile selected (-Dcfg=<profile>); skipping, see profiles/default/README.md");
		client = HttpClient.newBuilder().sslContext(trustAllSslContext()).connectTimeout(Duration.ofSeconds(10)).build();
	}

	@Test
	void wellKnownConfiguration_returns200WithIssuerAndVersion() throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl() + "/.well-known/lock-server-configuration"))
				.timeout(Duration.ofSeconds(10)).GET().build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		assertEquals(200, response.statusCode());
		JsonNode body = MAPPER.readTree(response.body());
		assertEquals("1.0", body.get("version").asText());
		assertTrue(body.get("issuer").asText().contains(serverName()));
	}

	/**
	 * The dev stand's TLS certificate is self-signed (see {@code VM-DEV-PROMPT.md}); this trusts any
	 * certificate for this HTTP client instance only, exactly like {@code curl -k} in the project's
	 * other manual scripts. Never use this pattern outside a test.
	 */
	private static SSLContext trustAllSslContext() {
		TrustManager trustAll = new X509TrustManager() {

			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) {
				// test-only: accept any certificate chain
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) {
				// test-only: accept any certificate chain
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}
		};
		try {
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[] { trustAll }, new SecureRandom());
			return sslContext;
		} catch (NoSuchAlgorithmException | KeyManagementException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
