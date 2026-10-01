/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Reads the test profile ({@code profiles/${cfg}/config-jans-lock-test-data.properties}, filtered
 * at build time into {@code lock-client-test.properties}) and exposes it to every client test.
 * {@code test.server.name} stays the sentinel {@code CHANGE_ME} under the {@code default} profile,
 * so {@link #hasServer()} lets a server-dependent test skip itself instead of failing the build
 * when no real profile was selected (see {@code profiles/default/README.md}).
 */
public class BaseLockClientTest {

	private static final String SENTINEL = "CHANGE_ME";

	private static final Properties PROPERTIES = load();

	private static Properties load() {
		Properties properties = new Properties();
		try (InputStream is = BaseLockClientTest.class.getResourceAsStream("/lock-client-test.properties")) {
			properties.load(is);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		return properties;
	}

	protected static String serverName() {
		return PROPERTIES.getProperty("testServerName", SENTINEL).trim();
	}

	protected static boolean hasServer() {
		String serverName = serverName();
		return !serverName.isEmpty() && !SENTINEL.equals(serverName);
	}

	protected static String baseUrl() {
		return "https://" + serverName();
	}

	protected static String traceClientId() {
		return PROPERTIES.getProperty("traceClientId", "").trim();
	}

	protected static String traceClientSecret() {
		return PROPERTIES.getProperty("traceClientSecret", "").trim();
	}

	/**
	 * @return true when both a real server and a TRACE-scoped client id/secret are configured
	 *         (see {@code profiles/default/README.md}); a test needing the TRACE client skips
	 *         itself via {@code Assumptions.assumeTrue(hasTraceClient(), ...)} otherwise.
	 */
	protected static boolean hasTraceClient() {
		return hasServer() && !traceClientId().isEmpty() && !traceClientSecret().isEmpty();
	}

	/**
	 * The dev stand's TLS certificate is self-signed (see {@code VM-DEV-PROMPT.md}); this trusts
	 * any certificate for the returned client instance only, exactly like {@code curl -k} in the
	 * project's other manual scripts. Never use this pattern outside a test.
	 */
	protected static HttpClient newInsecureHttpClient() {
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
			return HttpClient.newBuilder().sslContext(sslContext).connectTimeout(Duration.ofSeconds(10)).build();
		} catch (NoSuchAlgorithmException | KeyManagementException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
