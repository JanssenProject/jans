/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */
package io.jans.core.cedarling.telemetry;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.wiremock.grpc.GrpcExtensionFactory;

/**
 * Base class for gRPC integration tests that need a WireMock HTTPS server
 * with the gRPC extension enabled.
 *
 * <h3>What this class provides</h3>
 * <ul>
 *   <li>A {@link WireMockExtension} with both a dynamic HTTP port (for the admin
 *       API / stub registration) and a dynamic HTTPS port (for gRPC over H2).
 *       Using dynamic ports avoids collisions on shared CI runners.</li>
 *   <li>The server root directory is set to {@code src/test/resources/wiremock}.
 *       The gRPC extension scans the {@code grpc/} sub-directory for binary
 *       protobuf descriptor files ({@code *.dsc} / {@code *.desc}) that it
 *       uses to transcode proto&lt;-&gt;JSON.  The descriptor must be pre-generated
 *       by the {@code protoc-jar-maven-plugin} during {@code generate-test-resources}
 *       (see the project POM).</li>
 *   <li>A {@link ManagedChannel} connected to the WireMock HTTPS port, created
 *       once per test class in {@link #setUpGrpc()} and shut down in
 *       {@link #tearDownGrpc()}.  The channel uses an insecure SSL context so
 *       that WireMock's auto-generated self-signed certificate is accepted without
 *       importing it into the JVM trust store.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * @TestInstance(Lifecycle.PER_CLASS)
 * class MyGrpcTest extends BaseWireMockGrpcTest {
 *
 *     private WireMockGrpcService grpcService;
 *
 *     @BeforeEach
 *     void registerStubs() {
 *         grpcService = new WireMockGrpcService(
 *                 new WireMock(wireMockServer.getPort()),
 *                 "com.example.MyService");
 *         grpcService.stubFor(method("MyMethod").willReturn(json("{}")));
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Lifecycle note:</strong> {@link BeforeAll @BeforeAll} /
 * {@link AfterAll @AfterAll} methods here are <em>non-static</em>.  Concrete
 * subclasses must be annotated with
 * {@code @TestInstance(Lifecycle.PER_CLASS)} for JUnit 5 to call them.
 * Alternatively, override them as {@code @BeforeEach} / {@code @AfterEach}.
 */
public abstract class BaseWireMockGrpcTest {

    /**
     * WireMock server with:
     * <ul>
     *   <li>a dynamic HTTP port – used by {@code WireMockGrpcService} / admin API;</li>
     *   <li>a dynamic HTTPS port – used by gRPC clients over TLS/H2;</li>
     *   <li>{@code src/test/resources/wiremock} as root directory so the gRPC
     *       extension can locate {@code grpc/*.dsc} descriptor files;</li>
     *   <li>{@link GrpcExtensionFactory} registered to enable gRPC support.</li>
     * </ul>
     */
    @RegisterExtension
    protected static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig()
                    .dynamicPort()          // random HTTP port – no CI port collisions
                    .dynamicHttpsPort()     // random TLS port – auto-generates self-signed cert
                    .withRootDirectory("src/test/resources/wiremock")
                    .extensions(new GrpcExtensionFactory()))
            .build();

    /**
     * gRPC {@link ManagedChannel} connected to the WireMock HTTPS port.
     * Available to subclasses for creating blocking or async stubs when
     * direct channel access is needed.
     */
    protected ManagedChannel grpcChannel;

    /**
     * Builds the gRPC channel once before any test method runs.
     *
     * <p>The channel is created with {@link NettyChannelBuilder} (from
     * {@code grpc-netty-shaded}) and an {@link InsecureTrustManagerFactory}
     * so that WireMock's self-signed TLS certificate is accepted without any
     * additional trust-store configuration.
     *
     * @throws Exception if the SSL context cannot be built (should never happen
     *                   with InsecureTrustManagerFactory)
     */
    @BeforeAll
    public void setUpGrpc() throws Exception {
        // InsecureTrustManagerFactory accepts any server certificate.
        // Safe for test environments; never use in production.
        SslContext sslContext = GrpcSslContexts.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        grpcChannel = NettyChannelBuilder
                .forAddress("localhost", wireMockServer.getHttpsPort())
                .sslContext(sslContext)
                .build();
    }

    /**
     * Shuts down the gRPC channel after all tests in the class have finished
     * to prevent thread/socket leaks.
     */
    @AfterAll
    public void tearDownGrpc() {
        if (grpcChannel != null && !grpcChannel.isShutdown()) {
            grpcChannel.shutdownNow();
        }
    }

    /** Returns the shared gRPC channel for use in subclasses. */
    protected ManagedChannel getGrpcChannel() {
        return grpcChannel;
    }

    /**
     * Returns the dynamic HTTP port of the WireMock server.
     * Use this port when constructing a {@code WireMockGrpcService}:
     * <pre>{@code
     * new WireMockGrpcService(new WireMock(getPort()), "com.example.MyService")
     * }</pre>
     */
    protected int getPort() {
        return wireMockServer.getPort();
    }

    /**
     * Returns the dynamic HTTPS port of the WireMock server.
     * gRPC clients must connect to this port.
     */
    protected int getHttpsPort() {
        return wireMockServer.getHttpsPort();
    }

    /** Returns the WireMock server instance for stub registration and request verification. */
    protected WireMockExtension getWireMockServer() {
        return wireMockServer;
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