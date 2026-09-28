/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.client;

import com.sun.net.httpserver.HttpServer;
import io.jans.fido2.model.attestation.AttestationOptions;
import jakarta.ws.rs.core.Response;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * Confirms {@link Fido2ClientFactory}'s optional forwarded-context overloads actually stamp
 * X-Forwarded-For/User-Agent on the wire, and that the plain overload does not, so a caller relaying a
 * browser request (Casa, a person-authentication script) can trust the behaviour before wiring into it.
 */
public class Fido2ClientFactoryTest {

    private HttpServer server;
    private AtomicReference<String> capturedForwardedFor;
    private AtomicReference<String> capturedUserAgent;
    private String metadata;

    @BeforeMethod
    public void startServer() throws IOException {
        capturedForwardedFor = new AtomicReference<>();
        capturedUserAgent = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/restv1/attestation/options", exchange -> {
            capturedForwardedFor.set(exchange.getRequestHeaders().getFirst("X-Forwarded-For"));
            capturedUserAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
            exchange.getRequestBody().readAllBytes();
            byte[] body = "{}".getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        String basePath = "http://localhost:" + server.getAddress().getPort() + "/restv1/attestation";
        metadata = "{\"attestation\":{\"base_path\":\"" + basePath + "\"}}";
    }

    @AfterMethod
    public void stopServer() {
        server.stop(0);
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
}
