package io.jans.fido2.service.mds;

import com.sun.net.httpserver.HttpServer;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.exception.mds.MdsRateLimitedException;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.conf.MetadataServer;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.CertificateService;
import io.jans.service.document.store.model.Document;
import io.jans.service.document.store.service.DBDocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TocServiceTest {

    @InjectMocks
    private TocService tocService;

    @Mock
    private Logger log;
    @Mock
    private AppConfiguration appConfiguration;
    @Mock
    private CertificateService certificateService;
    @Mock
    private Base64Service base64Service;
    @Mock
    private DBDocumentService dbDocumentService;

    private void configureMetadataServers(List<MetadataServer> servers) {
        Fido2Configuration cfg = mock(Fido2Configuration.class);
        when(cfg.getMetadataServers()).thenReturn(servers);
        when(appConfiguration.getFido2Configuration()).thenReturn(cfg);
    }

    @Test
    void addConfiguredMetadataServerRootCerts_ifRootCertSet_addsTrustAnchor() {
        MetadataServer server = new MetadataServer();
        server.setRootCert("BASE64-DER-CERT");
        configureMetadataServers(Collections.singletonList(server));
        X509Certificate cert = mock(X509Certificate.class);
        when(certificateService.getCertificate("BASE64-DER-CERT")).thenReturn(cert);

        List<X509Certificate> trusted = new ArrayList<>();
        tocService.addConfiguredMetadataServerRootCerts(trusted);

        assertEquals(1, trusted.size());
        assertTrue(trusted.contains(cert));
    }

    @Test
    void addConfiguredMetadataServerRootCerts_ifRootCertEmpty_noChange() {
        MetadataServer server = new MetadataServer();
        server.setRootCert("");
        configureMetadataServers(Collections.singletonList(server));

        List<X509Certificate> trusted = new ArrayList<>();
        tocService.addConfiguredMetadataServerRootCerts(trusted);

        assertTrue(trusted.isEmpty());
    }

    @Test
    void addConfiguredMetadataServerRootCerts_ifNoMetadataServers_noChange() {
        configureMetadataServers(Collections.emptyList());

        List<X509Certificate> trusted = new ArrayList<>();
        tocService.addConfiguredMetadataServerRootCerts(trusted);

        assertTrue(trusted.isEmpty());
    }

    @Test
    void addConfiguredMetadataServerRootCerts_ifMetadataServersNull_noChange() {
        configureMetadataServers(null);

        List<X509Certificate> trusted = new ArrayList<>();
        tocService.addConfiguredMetadataServerRootCerts(trusted);

        assertTrue(trusted.isEmpty());
    }

    @Test
    void addConfiguredMetadataServerRootCerts_ifCertificateNull_noChange() {
        MetadataServer server = new MetadataServer();
        server.setRootCert("BASE64-DER-CERT");
        configureMetadataServers(Collections.singletonList(server));
        when(certificateService.getCertificate("BASE64-DER-CERT")).thenReturn(null);

        List<X509Certificate> trusted = new ArrayList<>();
        tocService.addConfiguredMetadataServerRootCerts(trusted);

        assertTrue(trusted.isEmpty());
    }

    @Test
    void addConfiguredMetadataServerRootCerts_ifRootCertMalformed_skipsWithoutThrowing() {
        MetadataServer server = new MetadataServer();
        server.setRootCert("BAD-CERT");
        configureMetadataServers(Collections.singletonList(server));
        when(certificateService.getCertificate(anyString())).thenThrow(new RuntimeException("bad cert"));

        List<X509Certificate> trusted = new ArrayList<>();
        // Must not propagate — a malformed rootCert falls back to the folder-based trust.
        tocService.addConfiguredMetadataServerRootCerts(trusted);

        assertTrue(trusted.isEmpty());
    }

    // --- Retry-After parsing -------------------------------------------------------------------

    @Test
    void parseRetryAfterSeconds_ifDeltaSeconds_returnsValue() {
        assertEquals(120, tocService.parseRetryAfterSeconds("120"));
        assertEquals(0, tocService.parseRetryAfterSeconds(" 0 "));
    }

    @Test
    void parseRetryAfterSeconds_ifHttpDate_returnsPositiveDelay() {
        String httpDate = ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(5)
                .format(DateTimeFormatter.RFC_1123_DATE_TIME);

        Integer seconds = tocService.parseRetryAfterSeconds(httpDate);

        assertNotNull(seconds);
        // Allow for clock granularity between formatting and parsing.
        assertTrue(seconds > 280 && seconds <= 300, "unexpected delay: " + seconds);
    }

    @Test
    void parseRetryAfterSeconds_ifHttpDateInThePast_returnsZero() {
        String httpDate = ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(5)
                .format(DateTimeFormatter.RFC_1123_DATE_TIME);

        assertEquals(0, tocService.parseRetryAfterSeconds(httpDate));
    }

    @Test
    void parseRetryAfterSeconds_ifAbsentOrUnparseable_returnsNull() {
        assertNull(tocService.parseRetryAfterSeconds(null));
        assertNull(tocService.parseRetryAfterSeconds(""));
        assertNull(tocService.parseRetryAfterSeconds("not-a-delay"));
    }

    @Test
    void parseRetryAfterSeconds_ifNegativeDeltaSeconds_clampsToZero() {
        assertEquals(0, tocService.parseRetryAfterSeconds("-30"));
    }

    // --- TOC download over HTTP ----------------------------------------------------------------

    /**
     * Starts a loopback HTTP server that replies with the given status, recording the User-Agent it
     * received. Uses the JDK's own HttpServer so no HTTP-mocking dependency is needed.
     */
    private HttpServer startStubMds(int status, String retryAfter, String body,
                                    AtomicReference<String> observedUserAgent) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            observedUserAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
            if (retryAfter != null) {
                exchange.getResponseHeaders().add("Retry-After", retryAfter);
            }
            byte[] payload = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, payload.length == 0 ? -1 : payload.length);
            if (payload.length > 0) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(payload);
                }
            }
            exchange.close();
        });
        server.start();
        return server;
    }

    private URL urlOf(HttpServer server) throws IOException {
        return URI.create(
                "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort() + "/").toURL();
    }

    @Test
    void downloadMdsFromServer_ifRateLimited_throwsMdsRateLimitedExceptionWithRetryAfter() throws IOException {
        AtomicReference<String> userAgent = new AtomicReference<>();
        HttpServer server = startStubMds(429, "300", null, userAgent);
        try {
            MdsRateLimitedException ex = assertThrows(MdsRateLimitedException.class,
                    () -> tocService.downloadMdsFromServer(urlOf(server)));

            assertEquals(300, ex.getRetryAfterSeconds());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void downloadMdsFromServer_ifRateLimitedWithoutRetryAfter_retryAfterIsNull() throws IOException {
        AtomicReference<String> userAgent = new AtomicReference<>();
        HttpServer server = startStubMds(429, null, null, userAgent);
        try {
            MdsRateLimitedException ex = assertThrows(MdsRateLimitedException.class,
                    () -> tocService.downloadMdsFromServer(urlOf(server)));

            assertNull(ex.getRetryAfterSeconds());
        } finally {
            server.stop(0);
        }
    }

    /**
     * A 429 must be distinguishable from other failures so a retry loop can back off instead of
     * hammering the endpoint; other error statuses stay generic.
     */
    @Test
    void downloadMdsFromServer_ifServerError_throwsGenericFailureNotRateLimited() throws IOException {
        AtomicReference<String> userAgent = new AtomicReference<>();
        HttpServer server = startStubMds(503, null, null, userAgent);
        try {
            Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class,
                    () -> tocService.downloadMdsFromServer(urlOf(server)));

            assertTrue(ex.getMessage().contains("503"), "expected the status in the message: " + ex.getMessage());
            assertFalse(ex instanceof MdsRateLimitedException, "503 must not be reported as rate limiting");
        } finally {
            server.stop(0);
        }
    }

    /**
     * The JDK default "Java/<version>" User-Agent is what the FIDO Alliance CDN throttles, so pin
     * that we identify ourselves instead.
     */
    @Test
    void downloadMdsFromServer_sendsIdentifyingUserAgent() throws IOException {
        AtomicReference<String> userAgent = new AtomicReference<>();
        HttpServer server = startStubMds(200, null, "toc-blob", userAgent);
        try {
            Fido2Configuration cfg = mock(Fido2Configuration.class);
            when(cfg.getMdsTocsFolder()).thenReturn("/etc/jans/conf/fido2/mds/toc");
            when(appConfiguration.getFido2Configuration()).thenReturn(cfg);
            when(base64Service.encodeToString(any())).thenReturn("ENCODED");
            when(dbDocumentService.getDocumentsByFilePath(anyString()))
                    .thenReturn(Collections.singletonList(new Document()));

            assertTrue(tocService.downloadMdsFromServer(urlOf(server)));

            assertEquals("Janssen-FIDO2", userAgent.get());
        } finally {
            server.stop(0);
        }
    }

    // --- Cached TOC fallback -------------------------------------------------------------------

    /**
     * Builds a TOC document whose stored nextUpdate marker is {@code daysFromNow} away, and points the
     * metadata server at a stub that fails the download.
     */
    private Fido2Configuration configureForFallback(String storedNextUpdate, String metadataUrl) {
        Fido2Configuration cfg = mock(Fido2Configuration.class);
        when(cfg.isDisableMetadataService()).thenReturn(false);
        when(cfg.getMdsTocsFolder()).thenReturn("/etc/jans/conf/fido2/mds/toc");
        when(cfg.getMdsCertsFolder()).thenReturn("/etc/jans/conf/fido2/mds/cert");
        MetadataServer metadataServer = new MetadataServer();
        metadataServer.setUrl(metadataUrl);
        when(cfg.getMetadataServers()).thenReturn(Collections.singletonList(metadataServer));
        when(appConfiguration.getFido2Configuration()).thenReturn(cfg);

        Document tocDocument = new Document();
        tocDocument.setDescription(storedNextUpdate);
        tocDocument.setDocument("CACHED-BLOB");
        when(dbDocumentService.getDocumentsByFilePath(anyString()))
                .thenReturn(Collections.singletonList(tocDocument));
        return cfg;
    }

    /**
     * The client-reported failure: MDS answers 429 at startup. The download must not propagate out of
     * the CDI observer, and the server must fall back to the TOC already cached in the DB instead of
     * coming up with no metadata at all.
     */
    @Test
    void fetchMetadata_ifDownloadRateLimited_doesNotPropagateAndFallsBackToCache() throws IOException {
        AtomicReference<String> userAgent = new AtomicReference<>();
        HttpServer server = startStubMds(429, "300", null, userAgent);
        try {
            String staleMarker = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
            configureForFallback(staleMarker, urlOf(server).toString());

            // A 429 at startup previously escaped the ApplicationInitialized observer.
            assertDoesNotThrow(() -> tocService.fetchMetadata());

            // The download was attempted (and throttled), then the cache path ran instead of the
            // exception propagating.
            assertEquals("Janssen-FIDO2", userAgent.get(), "the download should have been attempted");
        } finally {
            server.stop(0);
        }
    }

    /**
     * When the cached TOC is still current no download is attempted, and the cache path runs instead.
     * Previously refreshTOCEntries() only ran after a successful download, so a restart inside the
     * validity window left the entry map null. Asserting the entries are actually published needs a
     * real signed TOC blob, which belongs in an integration test rather than here.
     */
    @Test
    void fetchMetadata_ifCachedTocStillCurrent_doesNotDownload() throws IOException {
        AtomicReference<String> userAgent = new AtomicReference<>();
        HttpServer server = startStubMds(500, null, null, userAgent);
        try {
            String futureMarker = LocalDate.now().plusDays(20).format(DateTimeFormatter.ISO_LOCAL_DATE);
            configureForFallback(futureMarker, urlOf(server).toString());

            tocService.fetchMetadata();

            // No request may be issued while the cached TOC is still inside its validity window.
            assertNull(userAgent.get(), "a download was attempted despite a current cached TOC");
        } finally {
            server.stop(0);
        }
    }

    /**
     * A fresh install whose TOC document row is missing must still start: reading the update date
     * throws DocumentException, which previously escaped the ApplicationInitialized observer.
     */
    @Test
    void fetchMetadata_ifTocDocumentMissing_doesNotPropagate() throws IOException {
        AtomicReference<String> userAgent = new AtomicReference<>();
        HttpServer server = startStubMds(429, null, null, userAgent);
        try {
            configureForFallback(LocalDate.now().toString(), urlOf(server).toString());
            when(dbDocumentService.getDocumentsByFilePath(anyString())).thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> tocService.fetchMetadata());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchMetadata_ifMetadataServiceDisabled_doesNothing() throws IOException {
        AtomicReference<String> userAgent = new AtomicReference<>();
        HttpServer server = startStubMds(200, null, "toc-blob", userAgent);
        try {
            Fido2Configuration cfg = mock(Fido2Configuration.class);
            when(cfg.isDisableMetadataService()).thenReturn(true);
            when(appConfiguration.getFido2Configuration()).thenReturn(cfg);

            tocService.fetchMetadata();

            assertNull(userAgent.get(), "no download may happen when the metadata service is disabled");
        } finally {
            server.stop(0);
        }
    }
}
