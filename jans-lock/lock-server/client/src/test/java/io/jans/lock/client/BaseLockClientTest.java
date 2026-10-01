/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

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

}
