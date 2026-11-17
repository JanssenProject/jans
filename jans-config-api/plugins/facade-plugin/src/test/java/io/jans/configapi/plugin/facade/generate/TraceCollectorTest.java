/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.facade.generate;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import io.jans.configapi.plugin.facade.lock.LockClient;
import io.jans.configapi.plugin.facade.lock.LockSettings;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/** Runs the Lock client and the collector against a small fake Auth Server and Lock API. */
public class TraceCollectorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Path root;
    private HttpServer server;
    private LockSettings settings;
    private final List<String> requests = new CopyOnWriteArrayList<>();

    @BeforeMethod
    public void setUp() throws IOException {
        root = Files.createTempDirectory("facade-collector-test");
        requests.clear();
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/jans-auth/restv1/token", exchange -> {
            requests.add("token " + exchange.getRequestHeaders().getFirst("Authorization").substring(0, 5) + " "
                    + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            reply(exchange, 200, "{\"access_token\":\"test-token\"}");
        });
        server.createContext("/jans-lock/api/v1/audit/trace/executions/", exchange -> {
            requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI() + " "
                    + exchange.getRequestHeaders().getFirst("Authorization"));
            String id = exchange.getRequestURI().getPath().substring(exchange.getRequestURI().getPath().lastIndexOf('/') + 1);
            int records = id.equals("exec-short") ? 1 : 2;
            List<String> list = new ArrayList<>();
            for (int i = 0; i < records; i++) {
                list.add("{\"record_id\":\"" + id + "-" + i + "\"}");
            }
            reply(exchange, 200, "{\"records\":[" + String.join(",", list) + "]}");
        });
        server.start();
        String base = "http://localhost:" + server.getAddress().getPort();
        settings = new LockSettings(base, base, "client-1", "secret-1");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        server.stop(0);
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted((a, b) -> b.compareTo(a)).forEach(p -> p.toFile().delete());
        }
    }

    @Test
    public void tokenRequestUsesClientCredentialsAndAllThreeScopes() throws Exception {
        assertEquals(new LockClient(settings).token(), "test-token");

        String request = requests.get(0);
        assertTrue(request.startsWith("token Basic grant_type=client_credentials&scope="), request);
        for (String scope : List.of("trace.admin", "trace.write", "trace.readonly")) {
            assertTrue(request.contains(scope), scope);
        }
    }

    @Test
    public void collectsEachExecutionAndCountsMissingRecords() throws Exception {
        TraceFiles.writeExecutions(root, "run1", "facade-plugin", List.of(
                new TraceFiles.Sent("exec-ok", "https://gateway.example.org", "train", 2, List.of("a")),
                new TraceFiles.Sent("exec-short", "https://gateway.example.org", "test", 2, List.of("b"))));

        int missing = TraceCollector.collect(new LockClient(settings), "test-token", root, Duration.ofMillis(100));

        assertEquals(missing, 1);
        assertEquals(MAPPER.readTree(root.resolve("traces/exec-ok.json").toFile()).path("records").size(), 2);
        assertEquals(MAPPER.readTree(root.resolve("traces/exec-short.json").toFile()).path("records").size(), 1);
        assertTrue(requests.stream().anyMatch(r -> r.contains("execution_authority=https%3A%2F%2Fgateway.example.org")
                && r.endsWith("Bearer test-token")), requests.toString());
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*HTTP 401.*")
    public void rejectedClientGivesAClearError() throws Exception {
        server.removeContext("/jans-auth/restv1/token");
        server.createContext("/jans-auth/restv1/token", exchange -> reply(exchange, 401, "{}"));
        new LockClient(settings).token();
    }

    private static void reply(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
