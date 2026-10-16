/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.trace.crypto.tracecore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.lock.service.trace.testkit.TraceTestCrypto;
import io.jans.lock.service.trace.testkit.TraceTestHarness;
import io.jans.lock.trace.crypto.TraceCryptoProvider;

/**
 * Builds the two providers and lists test resources of the shared test-jars.
 */
final class TestProviders {

	private TestProviders() {
	}

	static NativeTraceCryptoProvider nativeProvider() {
		return nativeProvider(new TraceConfiguration());
	}

	static NativeTraceCryptoProvider nativeProvider(TraceConfiguration traceConfiguration) {
		NativeTraceCryptoProvider provider = new NativeTraceCryptoProvider(traceConfiguration);
		provider.probe();
		return provider;
	}

	static TraceCryptoProvider javaProvider() {
		return TraceTestCrypto.javaProvider();
	}

	/**
	 * @return the file names directly under {@code resourceDir} (e.g. {@code "trace/assertions"})
	 *         of the test-jar or directory that holds {@code anchor}, sorted
	 */
	static List<String> list(Class<?> anchor, String resourceDir) {
		try {
			CodeSource source = anchor.getProtectionDomain().getCodeSource();
			Path location = Paths.get(source.getLocation().toURI());
			List<String> names = new ArrayList<>();
			if (Files.isDirectory(location)) {
				try (Stream<Path> files = Files.list(location.resolve(resourceDir))) {
					names = files.filter(Files::isRegularFile).map(p -> p.getFileName().toString())
							.collect(Collectors.toList());
				}
			} else {
				String prefix = resourceDir + "/";
				try (JarFile jar = new JarFile(location.toFile())) {
					for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
						String name = entries.nextElement().getName();
						if (name.startsWith(prefix) && !name.endsWith("/") && name.indexOf('/', prefix.length()) < 0) {
							names.add(name.substring(prefix.length()));
						}
					}
				}
			}
			Collections.sort(names);
			return names;
		} catch (IOException | URISyntaxException ex) {
			throw new IllegalStateException(ex);
		}
	}

	static String read(Class<?> anchor, String resource) {
		try (InputStream in = anchor.getClassLoader().getResourceAsStream(resource)) {
			if (in == null) {
				throw new IllegalArgumentException("Resource not found: " + resource);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	static Class<?> serviceAnchor() {
		return TraceTestHarness.class;
	}

}
