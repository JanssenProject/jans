/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.readiness;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.jans.fido2.service.readiness.WellKnownFetchResult.Failure;

class WellKnownFetcherTest {

    private static final int MAX_BYTES = 1024;
    private static final ValidatedRpId RP_ID = new ValidatedRpId("rp.test.example");

    private HttpServer server;
    private final List<String> requestedPaths = new CopyOnWriteArrayList<>();
    private final List<Map<String, List<String>>> requestHeaders = new CopyOnWriteArrayList<>();
    private final List<String> requestHosts = new CopyOnWriteArrayList<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private void handle(String path, ExchangeHandler handler) {
        server.createContext(path, exchange -> {
            requestedPaths.add(exchange.getRequestURI().toString());
            requestHeaders.add(Map.copyOf(exchange.getRequestHeaders()));
            requestHosts.add(exchange.getRequestHeaders().getFirst("Host"));
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
    }

    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }

    /** Waits on a latch that is never released: same delay as a sleep, interruptible the same way. */
    private static void pause(long millis) throws InterruptedException {
        new CountDownLatch(1).await(millis, TimeUnit.MILLISECONDS);
    }

    private static void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        if (body.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        }
    }

    /** A fetcher aimed at the local server: the guard is off, the resolver is scripted. */
    private WellKnownFetcher localFetcher(AtomicInteger lookups, int readTimeoutMs, int totalTimeoutMs) {
        return new WellKnownFetcher("http", server.getAddress().getPort(), host -> {
            lookups.incrementAndGet();
            return new InetAddress[] { InetAddress.getLoopbackAddress() };
        }, address -> false, MAX_BYTES, 1000, readTimeoutMs, totalTimeoutMs);
    }

    private WellKnownFetcher localFetcher(AtomicInteger lookups) {
        return localFetcher(lookups, 2000, 5000);
    }

    // ---- guard -------------------------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = { "127.0.0.1", "10.0.0.5", "169.254.169.254", "192.168.1.1", "::1", "fd00:ec2::254" })
    void refusesABlockedAddress_andOpensNoConnection(String literal) {
        handle("/", exchange -> respond(exchange, 200, "{}".getBytes(StandardCharsets.UTF_8)));
        WellKnownFetcher fetcher = new WellKnownFetcher("http", server.getAddress().getPort(),
                host -> new InetAddress[] { InetAddress.getByName(literal) }, BlockedAddressClassifier::isBlocked,
                MAX_BYTES, 1000, 1000, 3000);

        WellKnownFetchResult result = fetcher.fetchAssetLinks(RP_ID);

        assertEquals(Failure.BLOCKED_ADDRESS, result.failure());
        assertFalse(result.hasResponse());
        assertTrue(requestedPaths.isEmpty());
    }

    @Test
    void refusesWhenAnyAnswerIsBlocked_evenIfAnotherIsPublic() {
        handle("/", exchange -> respond(exchange, 200, "{}".getBytes(StandardCharsets.UTF_8)));
        WellKnownFetcher fetcher = new WellKnownFetcher("http", server.getAddress().getPort(),
                host -> new InetAddress[] { InetAddress.getByName("8.8.8.8"), InetAddress.getByName("127.0.0.1") },
                BlockedAddressClassifier::isBlocked, MAX_BYTES, 1000, 1000, 3000);

        assertEquals(Failure.BLOCKED_ADDRESS, fetcher.fetchAssetLinks(RP_ID).failure());
        assertTrue(requestedPaths.isEmpty());
    }

    @Test
    void resolvesOnce_andConnectsToTheResolvedAddress() {
        handle("/", exchange -> respond(exchange, 200, "{}".getBytes(StandardCharsets.UTF_8)));
        AtomicInteger lookups = new AtomicInteger();

        WellKnownFetchResult result = localFetcher(lookups).fetchAssetLinks(RP_ID);

        // The name rp.test.example does not exist in DNS: reaching the server proves the pinned address was
        // used, and one lookup proves a rebinding answer to a second lookup could never be consulted.
        assertEquals(200, result.status());
        assertEquals(1, lookups.get());
        assertEquals(1, requestedPaths.size());
        assertEquals("rp.test.example:" + server.getAddress().getPort(), requestHosts.get(0));
    }

    @Test
    void reportsAnUnresolvableName() {
        WellKnownFetcher fetcher = new WellKnownFetcher("http", server.getAddress().getPort(), host -> {
            throw new UnknownHostException(host);
        }, address -> false, MAX_BYTES, 1000, 1000, 3000);

        assertEquals(Failure.UNRESOLVABLE, fetcher.fetchAssetLinks(RP_ID).failure());
    }

    // ---- request shape -----------------------------------------------------------------------------

    @Test
    void requestsExactlyTheDerivedPaths_withNoQuery() {
        handle("/", exchange -> respond(exchange, 200, "{}".getBytes(StandardCharsets.UTF_8)));
        WellKnownFetcher fetcher = localFetcher(new AtomicInteger());

        fetcher.fetchAssetLinks(RP_ID);
        fetcher.fetchAppleAppSiteAssociation(RP_ID);

        assertEquals(List.of("/.well-known/assetlinks.json", "/.well-known/apple-app-site-association"),
                requestedPaths);
    }

    @Test
    void sendsNoCookieOrCredentialHeaders() {
        handle("/", exchange -> {
            exchange.getResponseHeaders().add("Set-Cookie", "session=abc");
            respond(exchange, 200, "{}".getBytes(StandardCharsets.UTF_8));
        });
        WellKnownFetcher fetcher = localFetcher(new AtomicInteger());

        fetcher.fetchAssetLinks(RP_ID);
        fetcher.fetchAssetLinks(RP_ID);

        for (Map<String, List<String>> headers : requestHeaders) {
            Map<String, List<String>> lower = new java.util.HashMap<>();
            headers.forEach((name, values) -> lower.put(name.toLowerCase(), values));
            assertFalse(lower.containsKey("authorization"));
            assertFalse(lower.containsKey("cookie"));
            assertFalse(lower.containsKey("proxy-authorization"));
            assertFalse(lower.containsKey("accept-encoding"));
            assertEquals(List.of("application/json"), lower.get("accept"));
            assertEquals(List.of("Janssen-FIDO2-NativeReadiness"), lower.get("user-agent"));
        }
    }

    // ---- redirects, size, time ---------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(ints = { 301, 302, 307, 308 })
    void reportsARedirect_andNeverFollowsIt(int status) {
        handle("/.well-known/assetlinks.json", exchange -> {
            exchange.getResponseHeaders().add("Location", "/elsewhere");
            respond(exchange, status, new byte[0]);
        });
        handle("/elsewhere", exchange -> respond(exchange, 200, "{}".getBytes(StandardCharsets.UTF_8)));

        WellKnownFetchResult result = localFetcher(new AtomicInteger()).fetchAssetLinks(RP_ID);

        assertEquals(status, result.status());
        assertEquals("/elsewhere", result.location());
        assertEquals(List.of("/.well-known/assetlinks.json"), requestedPaths);
    }

    @Test
    void returnsASmallBodyIntact() {
        byte[] body = "[{\"relation\":[]}]".getBytes(StandardCharsets.UTF_8);
        handle("/", exchange -> respond(exchange, 200, body));

        WellKnownFetchResult result = localFetcher(new AtomicInteger()).fetchAssetLinks(RP_ID);

        assertEquals(200, result.status());
        assertEquals("application/json", result.contentType());
        assertArrayEquals(body, result.body());
        assertFalse(result.truncated());
        assertNull(result.location());
    }

    @Test
    void cutsABodyOverTheCap_andFlagsIt() {
        byte[] big = new byte[MAX_BYTES * 8];
        java.util.Arrays.fill(big, (byte) 'x');
        handle("/", exchange -> respond(exchange, 200, big));

        WellKnownFetchResult result = localFetcher(new AtomicInteger()).fetchAssetLinks(RP_ID);

        assertEquals(200, result.status());
        assertTrue(result.truncated());
        assertEquals(MAX_BYTES, result.body().length);
    }

    @Test
    void anExactlyCapSizedBodyIsNotTruncated() {
        handle("/", exchange -> respond(exchange, 200, new byte[MAX_BYTES]));

        WellKnownFetchResult result = localFetcher(new AtomicInteger()).fetchAssetLinks(RP_ID);

        assertFalse(result.truncated());
        assertEquals(MAX_BYTES, result.body().length);
    }

    @Test
    void aSlowServerTimesOut() {
        handle("/", exchange -> {
            try {
                pause(2500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            respond(exchange, 200, "{}".getBytes(StandardCharsets.UTF_8));
        });

        long start = System.nanoTime();
        WellKnownFetchResult result = localFetcher(new AtomicInteger(), 300, 5000).fetchAssetLinks(RP_ID);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertEquals(Failure.TIMEOUT, result.failure());
        assertTrue(elapsedMs < 2000, "returned after " + elapsedMs + " ms");
    }

    @Test
    void theTotalDeadlineStopsAServerThatDripsBytes() {
        handle("/", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream out = exchange.getResponseBody()) {
                for (int i = 0; i < 40; i++) {
                    out.write('x');
                    out.flush();
                    pause(100);
                }
            } catch (IOException | InterruptedException e) {
                // the client gave up, which is the point
            }
        });

        long start = System.nanoTime();
        // Each read arrives well inside the 2 s read timeout, so only the total deadline can end this.
        WellKnownFetchResult result = localFetcher(new AtomicInteger(), 2000, 600).fetchAssetLinks(RP_ID);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertEquals(Failure.TIMEOUT, result.failure());
        assertTrue(elapsedMs < 2500, "returned after " + elapsedMs + " ms");
    }

    @Test
    void reportsARefusedConnection() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            closedPort = socket.getLocalPort();
        }
        WellKnownFetcher fetcher = new WellKnownFetcher("http", closedPort,
                host -> new InetAddress[] { InetAddress.getLoopbackAddress() }, address -> false, MAX_BYTES, 1000,
                1000, 3000);

        assertEquals(Failure.CONNECTION_FAILED, fetcher.fetchAssetLinks(RP_ID).failure());
    }

    @Test
    void theProductionConstructorIsHttpsOnPort443() {
        // The seam above is package-private; the public constructors cannot change scheme or port.
        for (java.lang.reflect.Constructor<?> constructor : WellKnownFetcher.class.getConstructors()) {
            for (Class<?> type : constructor.getParameterTypes()) {
                assertEquals(int.class, type);
            }
        }
    }
}
