/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.core.cedarling.telemetry;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;

/**
 * Base test class that provides a pre-configured WireMock server running over
 * HTTPS for use by telemetry integration tests.
 *
 * <h3>What this class provides</h3>
 * <ul>
 *   <li>A {@link WireMockExtension} registered as a JUnit 5 extension.  The
 *       server starts on a <em>dynamic</em> (randomly assigned) HTTPS port, so
 *       tests never clash on CI runners that share a port pool.</li>
 *   <li>A {@link BeforeEach} hook that configures REST Assured to target the
 *       WireMock server and to bypass strict TLS validation (WireMock uses a
 *       self-signed certificate).</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * class MyTest extends BaseWireMockHttpTest {
 *
 *     @Test
 *     void example() {
 *         wireMockServer.stubFor(get("/ping").willReturn(ok()));
 *         RestAssured.get("/ping").then().statusCode(200);
 *     }
 * }
 * }</pre>
 *
 * <p>The {@link org.junit.jupiter.api.BeforeAll @BeforeAll}
 * lifecycle method in this class is non-static.  JUnit 5 requires the concrete
 * test class to be annotated with
 * {@code @TestInstance(Lifecycle.PER_CLASS)} for non-static {@code @BeforeAll}
 * methods to be recognised; subclasses that omit that annotation must override
 * {@code setUp()} as a {@code @BeforeEach} method instead.
 *
 * @author Yuriy Movchan Date: 12/05/2026
 *  */
public abstract class BaseWireMockHttpTest {

    /**
     * WireMock server started with a dynamically assigned HTTPS port and an
     * auto-generated self-signed TLS certificate.
     *
     * <p>Declared {@code protected static} so that subclasses annotated with
     * {@code @TestInstance(PER_CLASS)} as well as the default per-method
     * lifecycle can both access the single server instance.
     */
    @RegisterExtension
    protected static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig()
                    .dynamicHttpsPort()) // auto-generates self-signed TLS cert
            .build();

    /**
     * Configures REST Assured before every test:
     * <ol>
     *   <li>Sets the base URI to {@code https://localhost}.</li>
     *   <li>Sets the port to the dynamically assigned HTTPS port.</li>
     *   <li>Enables relaxed HTTPS validation so WireMock's self-signed
     *       certificate is accepted without importing it into a trust store.</li>
     * </ol>
     */
    @BeforeAll
    public void setUp() {
        RestAssured.baseURI = "https://localhost";
        RestAssured.port    = wireMockServer.getHttpsPort();

        RestAssured.config = RestAssured.config()
                .sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation());
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