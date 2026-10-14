/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.readiness;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import javax.net.ssl.SSLException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import io.jans.fido2.service.readiness.WellKnownFetchResult.Failure;

/**
 * Fetches the two native-app association files from a relying party's domain, and nothing else.
 * <p>
 * This is the SSRF boundary of the readiness checker. What it guarantees:
 * <ul>
 * <li>The URL is built here, from a {@link ValidatedRpId}: exactly
 * {@code https://<rp-id>/.well-known/assetlinks.json} or
 * {@code https://<rp-id>/.well-known/apple-app-site-association}, port 443. No caller supplies a scheme,
 * host, port, path or query.</li>
 * <li>The host name is resolved once. If any returned address is blocked by
 * {@link BlockedAddressClassifier}, nothing is connected. Otherwise the connection is made to those exact
 * addresses with no second lookup, so DNS cannot change its answer between the check and the connect. TLS
 * still uses the original host name for SNI and certificate verification.</li>
 * <li>Redirects are never followed; a 3xx response is returned as data.</li>
 * <li>The body is read up to a fixed cap and no further. Compression is off, so the cap applies to the
 * bytes on the wire.</li>
 * <li>Connect time, read time and total time are bounded.</li>
 * <li>No cookies, credentials or server tokens are sent, and no proxy is used. Only {@code Accept} and
 * {@code User-Agent} (plus the {@code Host} header) are set.</li>
 * <li>Failures come back as a coarse {@link Failure}, never as exception text.</li>
 * </ul>
 * <b>Not covered:</b> the DNS lookup itself is bounded by the JVM resolver, not by the timeouts here.
 */
public class WellKnownFetcher {

    public static final String ASSET_LINKS_PATH = "/.well-known/assetlinks.json";
    public static final String APPLE_APP_SITE_ASSOCIATION_PATH = "/.well-known/apple-app-site-association";

    static final int DEFAULT_MAX_RESPONSE_BYTES = 64 * 1024;
    static final int DEFAULT_TIMEOUT_MS = 3000;
    static final int DEFAULT_TOTAL_TIMEOUT_MS = 10000;

    private static final int HTTPS_PORT = 443;
    private static final String USER_AGENT = "Janssen-FIDO2-NativeReadiness";

    private static final ScheduledExecutorService DEADLINES = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "fido2-readiness-deadline");
        thread.setDaemon(true);
        return thread;
    });

    /** Resolves a host name to addresses. The default is the JVM resolver. */
    @FunctionalInterface
    interface Resolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    private final String scheme;
    private final int port;
    private final Resolver resolver;
    private final Predicate<InetAddress> blocked;
    private final int maxResponseBytes;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final int totalTimeoutMs;

    public WellKnownFetcher() {
        this(DEFAULT_MAX_RESPONSE_BYTES, DEFAULT_TIMEOUT_MS, DEFAULT_TIMEOUT_MS, DEFAULT_TOTAL_TIMEOUT_MS);
    }

    public WellKnownFetcher(int maxResponseBytes, int connectTimeoutMs, int readTimeoutMs, int totalTimeoutMs) {
        this("https", HTTPS_PORT, InetAddress::getAllByName, BlockedAddressClassifier::isBlocked,
                maxResponseBytes, connectTimeoutMs, readTimeoutMs, totalTimeoutMs);
    }

    /**
     * Test seam, visible to the package only: lets a test point the fetcher at a local server with a
     * scripted resolver. Production code cannot reach it, so there is no way to switch the guard off.
     */
    WellKnownFetcher(String scheme, int port, Resolver resolver, Predicate<InetAddress> blocked,
            int maxResponseBytes, int connectTimeoutMs, int readTimeoutMs, int totalTimeoutMs) {
        this.scheme = scheme;
        this.port = port;
        this.resolver = resolver;
        this.blocked = blocked;
        this.maxResponseBytes = maxResponseBytes;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.totalTimeoutMs = totalTimeoutMs;
    }

    public WellKnownFetchResult fetchAssetLinks(ValidatedRpId rpId) {
        return fetch(rpId, ASSET_LINKS_PATH);
    }

    public WellKnownFetchResult fetchAppleAppSiteAssociation(ValidatedRpId rpId) {
        return fetch(rpId, APPLE_APP_SITE_ASSOCIATION_PATH);
    }

    private WellKnownFetchResult fetch(ValidatedRpId rpId, String path) {
        InetAddress[] addresses;
        try {
            addresses = resolver.resolve(rpId.hostname());
        } catch (UnknownHostException e) {
            return WellKnownFetchResult.failed(Failure.UNRESOLVABLE);
        }
        if (addresses == null || addresses.length == 0) {
            return WellKnownFetchResult.failed(Failure.UNRESOLVABLE);
        }
        // Every address must pass: an attacker-controlled name can answer with a public and a private
        // address at once, and the client is free to try either.
        for (InetAddress address : addresses) {
            if (blocked.test(address)) {
                return WellKnownFetchResult.failed(Failure.BLOCKED_ADDRESS);
            }
        }
        InetAddress[] pinned = addresses.clone();

        RequestConfig requestConfig = RequestConfig.custom()
                .setRedirectsEnabled(false)
                .setConnectTimeout(connectTimeoutMs)
                .setConnectionRequestTimeout(connectTimeoutMs)
                .setSocketTimeout(readTimeoutMs)
                .build();

        HttpGet get = new HttpGet(url(rpId, path));
        get.setHeader(HttpHeaders.ACCEPT, "application/json");

        AtomicBoolean timedOut = new AtomicBoolean();
        // A new client per fetch, so the DNS answer it may use is exactly the one that was checked above.
        try (CloseableHttpClient client = HttpClientBuilder.create()
                .setDnsResolver(host -> pinned)
                .setDefaultRequestConfig(requestConfig)
                .setUserAgent(USER_AGENT)
                .disableCookieManagement()
                .disableAuthCaching()
                .disableAutomaticRetries()
                .disableRedirectHandling()
                .disableContentCompression()
                .build()) {
            ScheduledFuture<?> deadline = DEADLINES.schedule(() -> {
                timedOut.set(true);
                get.abort();
            }, totalTimeoutMs, TimeUnit.MILLISECONDS);
            try (CloseableHttpResponse response = client.execute(get)) {
                return toResult(response);
            } finally {
                deadline.cancel(false);
            }
        } catch (IOException e) {
            return WellKnownFetchResult.failed(classify(e, timedOut.get()));
        } catch (RuntimeException e) {
            return WellKnownFetchResult.failed(timedOut.get() ? Failure.TIMEOUT : Failure.ERROR);
        }
    }

    private String url(ValidatedRpId rpId, String path) {
        boolean defaultPort = "https".equals(scheme) && port == HTTPS_PORT;
        return scheme + "://" + rpId.hostname() + (defaultPort ? "" : ":" + port) + path;
    }

    private WellKnownFetchResult toResult(CloseableHttpResponse response) throws IOException {
        int status = response.getStatusLine().getStatusCode();
        String contentType = headerValue(response.getFirstHeader(HttpHeaders.CONTENT_TYPE));
        String location = (status >= 300 && status < 400)
                ? headerValue(response.getFirstHeader(HttpHeaders.LOCATION)) : null;

        byte[] body = new byte[0];
        boolean truncated = false;
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            // The stream is deliberately not closed here: closing it would read the rest of the body to
            // keep the connection reusable. Closing the response drops the connection instead.
            InputStream in = entity.getContent();
            body = in.readNBytes(maxResponseBytes + 1);
            if (body.length > maxResponseBytes) {
                truncated = true;
                body = Arrays.copyOf(body, maxResponseBytes);
            }
        }
        return WellKnownFetchResult.response(status, contentType, location, body, truncated);
    }

    private static String headerValue(Header header) {
        return header == null ? null : header.getValue();
    }

    private static Failure classify(IOException e, boolean timedOut) {
        if (timedOut || e instanceof SocketTimeoutException || e instanceof ConnectTimeoutException) {
            return Failure.TIMEOUT;
        }
        if (e instanceof SSLException) {
            return Failure.TLS_FAILED;
        }
        return Failure.CONNECTION_FAILED;
    }
}
