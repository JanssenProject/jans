/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.client;

import com.sun.net.httpserver.HttpServer;
import io.jans.fido2.model.assertion.AssertionOptions;
import io.jans.fido2.model.attestation.AttestationOptions;
import jakarta.ws.rs.core.Response;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;

/**
 * Confirms {@link Fido2ClientFactory}'s optional forwarded-context overloads actually stamp
 * X-Forwarded-For/User-Agent on the wire, and that the plain overload does not, so a caller relaying a
 * browser request (Casa, a person-authentication script) can trust the behaviour before wiring into it.
 */
public class Fido2ClientFactoryTest {

    private HttpServer server;
    private HttpServer redirectTargetServer;
    private AtomicReference<String> capturedForwardedFor;
    private AtomicReference<String> capturedUserAgent;
    private AtomicBoolean redirectTargetReached;
    private String metadata;
    private String redirectSourceMetadata;

    @BeforeMethod
    public void startServer() throws IOException {
        capturedForwardedFor = new AtomicReference<>();
        capturedUserAgent = new AtomicReference<>();
        redirectTargetReached = new AtomicBoolean(false);

        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        com.sun.net.httpserver.HttpHandler capture = exchange -> {
            capturedForwardedFor.set(exchange.getRequestHeaders().getFirst("X-Forwarded-For"));
            capturedUserAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
            exchange.getRequestBody().readAllBytes();
            byte[] body = "{}".getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        };
        server.createContext("/restv1/attestation/options", capture);
        server.createContext("/restv1/assertion/options", capture);
        server.start();

        String base = "http://localhost:" + server.getAddress().getPort();
        metadata = "{\"attestation\":{\"base_path\":\"" + base + "/restv1/attestation\"},"
                + "\"assertion\":{\"base_path\":\"" + base + "/restv1/assertion\"}}";

        // A second host+context pair, so the redirect Location genuinely leaves the requested target
        // rather than just changing path on the same host — the scenario the CWE-200 fix guards against.
        redirectTargetServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        redirectTargetServer.createContext("/reached", exchange -> {
            redirectTargetReached.set(true);
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });
        redirectTargetServer.start();
        String redirectTargetUrl = "http://localhost:" + redirectTargetServer.getAddress().getPort() + "/reached";

        server.createContext("/restv1/redirect-source/options", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().add("Location", redirectTargetUrl);
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        redirectSourceMetadata = "{\"attestation\":{\"base_path\":\"" + base + "/restv1/redirect-source\"}}";
    }

    @AfterMethod
    public void stopServer() {
        server.stop(0);
        redirectTargetServer.stop(0);
    }

    @Test
    public void plainOverloadDoesNotForwardClientContext() throws IOException {
        AttestationService service = Fido2ClientFactory.instance().createAttestationService(metadata);

        try (Response response = service.register(new AttestationOptions())) {
            assertEquals(response.getStatus(), 200);
        }

        assertNull(capturedForwardedFor.get(), "plain overload must not invent an X-Forwarded-For header");
    }

    @Test
    public void forwardedOverloadStampsClientContextOnEveryRequest() throws IOException {
        AttestationService service = Fido2ClientFactory.instance()
                .createAttestationService(metadata, "203.0.113.7", "TestAgent/1.0");

        try (Response response = service.register(new AttestationOptions())) {
            assertEquals(response.getStatus(), 200);
        }

        assertEquals(capturedForwardedFor.get(), "203.0.113.7");
        assertEquals(capturedUserAgent.get(), "TestAgent/1.0");
    }

    @Test
    public void forwardedOverloadStampsClientContextOnEveryRequest_forAssertionService() throws IOException {
        AssertionService service = Fido2ClientFactory.instance()
                .createAssertionService(metadata, "203.0.113.7", "TestAgent/1.0");

        try (Response response = service.authenticate(new AssertionOptions())) {
            assertEquals(response.getStatus(), 200);
        }

        assertEquals(capturedForwardedFor.get(), "203.0.113.7");
        assertEquals(capturedUserAgent.get(), "TestAgent/1.0");
    }

    @Test
    public void forwardedOverloadWithOnlyOneValue_sendsOnlyThatHeader() throws IOException {
        AssertionService plain = Fido2ClientFactory.instance().createAssertionService(metadata);
        try (Response response = plain.authenticate(new AssertionOptions())) {
            assertEquals(response.getStatus(), 200);
        }
        String baselineUserAgent = capturedUserAgent.get();

        AssertionService service = Fido2ClientFactory.instance()
                .createAssertionService(metadata, "203.0.113.9", null);

        try (Response response = service.authenticate(new AssertionOptions())) {
            assertEquals(response.getStatus(), 200);
        }

        assertEquals(capturedForwardedFor.get(), "203.0.113.9");
        assertEquals(capturedUserAgent.get(), baselineUserAgent,
                "a null userAgent argument must fall back to the engine's own default, not any custom value");
    }

    @Test
    public void forwardedOverloadDoesNotFollowARedirectToAnotherHost() throws IOException {
        AttestationService service = Fido2ClientFactory.instance()
                .createAttestationService(redirectSourceMetadata, "203.0.113.7", "TestAgent/1.0");

        try (Response response = service.register(new AttestationOptions())) {
            assertEquals(response.getStatus(), 302, "the raw redirect must be returned, not followed");
        }

        assertFalse(redirectTargetReached.get(),
                "a forwarded-context request must never follow a redirect off the requested host");
    }

    @Test
    public void forwardedOverloadOverPlainHttpNonLoopback_isRejected() {
        String insecureMetadata = "{\"attestation\":{\"base_path\":\"http://example.com/restv1/attestation\"}}";

        assertThrows(IllegalArgumentException.class, () -> Fido2ClientFactory.instance()
                .createAttestationService(insecureMetadata, "203.0.113.7", null));
    }
}
