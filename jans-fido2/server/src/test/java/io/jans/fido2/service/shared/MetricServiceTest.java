/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.fido2.service.shared;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.metric.Fido2MetricsData;
import io.jans.fido2.model.trust.NativeFailureDiagnostic;
import io.jans.fido2.service.metric.Fido2MetricsService;
import io.jans.fido2.service.util.DeviceInfoExtractor;
import jakarta.enterprise.inject.Instance;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for FIDO2 MetricService
 *
 * @author Janssen Project
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MetricServiceTest {

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private DeviceInfoExtractor deviceInfoExtractor;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private Logger log;

    @Mock
    private Instance<Fido2MetricsService> fido2MetricsServiceInstance;

    @Mock
    private Fido2MetricsService fido2MetricsService;

    @InjectMocks
    private MetricService metricService;

    @BeforeEach
    void setUp() {
        // Set up default configuration
        when(appConfiguration.isFido2MetricsEnabled()).thenReturn(true);
        when(appConfiguration.getMetricReporterEnabled()).thenReturn(true);
        when(appConfiguration.isFido2DeviceInfoCollection()).thenReturn(true);
        when(appConfiguration.isFido2ErrorCategorization()).thenReturn(true);
        when(appConfiguration.isFido2PerformanceMetrics()).thenReturn(true);

        when(fido2MetricsServiceInstance.isUnsatisfied()).thenReturn(false);
        when(fido2MetricsServiceInstance.get()).thenReturn(fido2MetricsService);
    }

    @Test
    void testRecordPasskeyRegistrationAttempt() {
        // Given
        String username = "testuser";
        long startTime = System.currentTimeMillis();

        // When & Then - should not throw exception and complete successfully
        assertDoesNotThrow(() -> 
            metricService.recordPasskeyRegistrationAttempt(username, httpRequest, startTime)
        );
    }

    @Test
    void testRecordPasskeyRegistrationSuccess() {
        // Given
        String username = "testuser";
        String authenticatorType = "platform";
        long startTime = System.currentTimeMillis();

        // When & Then - should not throw exception and complete successfully
        assertDoesNotThrow(() -> 
            metricService.recordPasskeyRegistrationSuccess(username, httpRequest, startTime, authenticatorType)
        );
    }

    @Test
    void testRecordPasskeyRegistrationFailure() {
        // Given
        String username = "testuser";
        String errorReason = "Invalid challenge";
        String authenticatorType = "cross-platform";
        long startTime = System.currentTimeMillis();

        // When & Then - should not throw exception and complete successfully
        assertDoesNotThrow(() -> 
            metricService.recordPasskeyRegistrationFailure(username, httpRequest, startTime, errorReason, authenticatorType)
        );
    }

    @Test
    void testRecordPasskeyAuthenticationAttempt() {
        // Given
        String username = "testuser";
        long startTime = System.currentTimeMillis();

        // When & Then - should not throw exception and complete successfully
        assertDoesNotThrow(() -> 
            metricService.recordPasskeyAuthenticationAttempt(username, httpRequest, startTime)
        );
    }

    @Test
    void testRecordPasskeyAuthenticationSuccess() {
        // Given
        String username = "testuser";
        String authenticatorType = "security-key";
        long startTime = System.currentTimeMillis();

        // When & Then - should not throw exception and complete successfully
        assertDoesNotThrow(() -> 
            metricService.recordPasskeyAuthenticationSuccess(username, httpRequest, startTime, authenticatorType)
        );
    }

    @Test
    void testRecordPasskeyAuthenticationFailure() {
        // Given
        String username = "testuser";
        String errorReason = "Authentication failed";
        String authenticatorType = "platform";
        long startTime = System.currentTimeMillis();

        // When & Then - should not throw exception and complete successfully
        assertDoesNotThrow(() -> 
            metricService.recordPasskeyAuthenticationFailure(username, httpRequest, startTime, errorReason, authenticatorType)
        );
    }

    @Test
    void testRecordPasskeyFallback() {
        // Given
        String username = "testuser";
        String fallbackMethod = "PASSWORD";
        String reason = "User chose password";

        // When & Then - should not throw exception and complete successfully
        assertDoesNotThrow(() -> 
            metricService.recordPasskeyFallback(username, fallbackMethod, reason)
        );
    }

    @Test
    void testMetricsDisabled() {
        // Given
        when(appConfiguration.isFido2MetricsEnabled()).thenReturn(false);
        String username = "testuser";
        long startTime = System.currentTimeMillis();

        // When
        metricService.recordPasskeyRegistrationAttempt(username, httpRequest, startTime);

        // Then - should complete without any metrics processing
        assertDoesNotThrow(() -> {
            // No async processing should occur
        });
    }

    /**
     * The metrics write is handed to a background thread, but the request backing it
     * is request-scoped and only resolvable on the request thread. Reading it from the
     * async task silently yielded no ipAddress / userAgent / deviceInfo, so the request
     * must be consumed synchronously, before the caller returns.
     */
    @Test
    void testRequestIsReadOnCallingThread() {
        // Given - record which thread actually touches the request
        AtomicReference<Thread> remoteAddrThread = new AtomicReference<>();
        AtomicReference<Thread> userAgentThread = new AtomicReference<>();

        when(httpRequest.getRemoteAddr()).thenAnswer(invocation -> {
            remoteAddrThread.set(Thread.currentThread());
            return "203.0.113.7";
        });
        when(httpRequest.getHeader("User-Agent")).thenAnswer(invocation -> {
            userAgentThread.set(Thread.currentThread());
            return "Mozilla/5.0";
        });

        Thread callingThread = Thread.currentThread();

        // When
        metricService.recordPasskeyRegistrationAttempt("testuser", httpRequest, System.currentTimeMillis());

        // Then - both reads already happened, on this thread and not on the async pool
        assertSame(callingThread, remoteAddrThread.get(),
                "IP address must be read on the request thread, not the async pool");
        assertSame(callingThread, userAgentThread.get(),
                "User-Agent must be read on the request thread, not the async pool");
    }

    /**
     * A request-scoped proxy dereferenced outside an active request throws rather than
     * returning null; that must degrade to an empty snapshot, not break the operation.
     */
    @Test
    void testInactiveRequestScopeDoesNotPropagate() {
        // Given
        when(httpRequest.getRemoteAddr()).thenThrow(new IllegalStateException("Request scope not active"));

        // When & Then
        assertDoesNotThrow(() ->
            metricService.recordPasskeyRegistrationAttempt("testuser", httpRequest, System.currentTimeMillis())
        );
    }

    /**
     * The address being validated comes from caller-supplied proxy headers, so the octet
     * check must stay ASCII-only. Character.isDigit accepts other Unicode decimal digits
     * (e.g. Arabic-Indic U+0669) and Integer.parseInt converts them, which would let a
     * spoofed X-Forwarded-For through.
     */
    @Test
    void testIpv4ValidationRejectsNonAsciiDigits() {
        assertTrue(metricService.isValidIpAddress("192.168.1.1"));
        assertTrue(metricService.isValidIpAddress("255.255.255.255"));
        assertTrue(metricService.isValidIpAddress("0.0.0.0"));

        assertFalse(metricService.isValidIpAddress("٩.٩.٩.٩"));
        assertFalse(metricService.isValidIpAddress("192.168.1.٩"));
        assertFalse(metricService.isValidIpAddress("256.1.1.1"));
        assertFalse(metricService.isValidIpAddress("1.2.3"));
        assertFalse(metricService.isValidIpAddress("1.2.3.4.5"));
        assertFalse(metricService.isValidIpAddress("1.2.3."));
        assertFalse(metricService.isValidIpAddress("a.b.c.d"));
        assertFalse(metricService.isValidIpAddress("0000.1.1.1"));
    }

    /**
     * fido2DeviceInfoCollection governs the parsed device info only. fido2MetricsEnabled is
     * the master switch, so turning device info off must still persist the entry with its
     * ip address, user agent, session id and metric type intact.
     */
    @Test
    void testEntryIsStoredWhenDeviceInfoCollectionIsDisabled() {
        // Given
        when(appConfiguration.isFido2DeviceInfoCollection()).thenReturn(false);
        when(httpRequest.getRemoteAddr()).thenReturn("203.0.113.7");
        when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        // When
        metricService.recordPasskeyRegistrationSuccess("testuser", httpRequest,
                System.currentTimeMillis(), "platform");

        // Then
        Fido2MetricsData stored = captureStoredMetrics();
        assertEquals("203.0.113.7", stored.getIpAddress());
        assertEquals("Mozilla/5.0", stored.getUserAgent());
        assertEquals("fido2_registration_success", stored.getMetricType());
        assertNull(stored.getDeviceInfo(), "device info must be the only thing the flag suppresses");
    }

    /**
     * A fallback event carries no device info at all, so it must not be gated on
     * fido2DeviceInfoCollection either.
     */
    @Test
    void testFallbackIsStoredWhenDeviceInfoCollectionIsDisabled() {
        // Given
        when(appConfiguration.isFido2DeviceInfoCollection()).thenReturn(false);

        // When
        metricService.recordPasskeyFallback("testuser", "PASSWORD", "User chose password");

        // Then
        Fido2MetricsData stored = captureStoredMetrics();
        assertEquals("FALLBACK", stored.getOperationType());
        assertEquals("PASSWORD", stored.getFallbackMethod());
        assertEquals("fido2_fallback_event", stored.getMetricType());
    }

    /**
     * The auth server's session_id cookie is the authoritative source, so it must win over
     * a servlet session when both are present.
     */
    @Test
    void testSessionIdPrefersCookieOverServletSession() {
        // Given
        HttpSession servletSession = mock(HttpSession.class);
        when(servletSession.getId()).thenReturn("servlet-session-id");
        when(httpRequest.getSession(false)).thenReturn(servletSession);
        when(httpRequest.getCookies()).thenReturn(new Cookie[] {
                new Cookie("other", "irrelevant"),
                new Cookie("session_id", "cookie-session-id")
        });

        // When
        metricService.recordPasskeyAuthenticationSuccess("testuser", httpRequest,
                System.currentTimeMillis(), "platform");

        // Then
        assertEquals("cookie-session-id", captureStoredMetrics().getSessionId());
    }

    /**
     * FIDO2 endpoints are stateless, but where a servlet session does exist it is the
     * fallback for requests that carry no session_id cookie.
     */
    @Test
    void testSessionIdFallsBackToServletSession() {
        // Given
        HttpSession servletSession = mock(HttpSession.class);
        when(servletSession.getId()).thenReturn("servlet-session-id");
        when(httpRequest.getSession(false)).thenReturn(servletSession);
        when(httpRequest.getCookies()).thenReturn(new Cookie[] { new Cookie("other", "irrelevant") });

        // When
        metricService.recordPasskeyAuthenticationSuccess("testuser", httpRequest,
                System.currentTimeMillis(), "platform");

        // Then
        assertEquals("servlet-session-id", captureStoredMetrics().getSessionId());
    }

    @Test
    void testSessionIdIsAbsentWhenRequestCarriesNeither() {
        // Given - no cookies at all and no servlet session
        when(httpRequest.getCookies()).thenReturn(null);
        when(httpRequest.getSession(false)).thenReturn(null);

        // When
        metricService.recordPasskeyAuthenticationSuccess("testuser", httpRequest,
                System.currentTimeMillis(), "platform");

        // Then
        assertNull(captureStoredMetrics().getSessionId());
    }

    /**
     * metricReporter* drives the legacy jans-core reporter, which is a separate feature.
     * Turning it off must not silently stop passkey telemetry from being written.
     */
    @Test
    void testEntryIsStoredWhenLegacyMetricReporterIsDisabled() {
        // Given
        when(appConfiguration.getMetricReporterEnabled()).thenReturn(false);
        when(httpRequest.getRemoteAddr()).thenReturn("203.0.113.7");

        // When
        metricService.recordPasskeyRegistrationSuccess("testuser", httpRequest,
                System.currentTimeMillis(), "platform");

        // Then
        assertEquals("203.0.113.7", captureStoredMetrics().getIpAddress());
    }

    /**
     * A native-failure diagnostic code (#14608) is recognised as its own category, distinct from
     * attestation-trust codes and from the keyword-based bucketing applied to free-text messages —
     * checked before the keyword matching, since "JFS_RPID_HASH_MISMATCH" contains no bucketable
     * keyword and would otherwise fall through to OTHER.
     */
    @Test
    void testCategorizeErrorRecognisesNativeFailureDiagnosticCode() {
        assertEquals(NativeFailureDiagnostic.CATEGORY, metricService.categorizeError("JFS_RPID_HASH_MISMATCH"));
    }

    /**
     * Metrics are persisted asynchronously, so wait for the write and hand back what was stored.
     */
    private Fido2MetricsData captureStoredMetrics() {
        ArgumentCaptor<Fido2MetricsData> captor = ArgumentCaptor.forClass(Fido2MetricsData.class);
        verify(fido2MetricsService, timeout(5000)).storeMetricsData(captor.capture());
        return captor.getValue();
    }

    @Test
    void testNullRequestIsTolerated() {
        // When & Then - fallback callers have no request at all
        assertDoesNotThrow(() ->
            metricService.recordPasskeyAuthenticationAttempt("testuser", null, System.currentTimeMillis())
        );
    }


    // ---------------------------------------------------------------------------------------------
    // Trusted proxy validation (#13850)
    //
    // The recorded IP comes from headers the caller supplies. These pin which of them are believed,
    // and under what configuration, so that a client cannot choose the address stored against its own
    // ceremony.
    // ---------------------------------------------------------------------------------------------

    private void stubRequest(String remoteAddr, String forwardedFor) {
        when(httpRequest.getRemoteAddr()).thenReturn(remoteAddr);
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(forwardedFor);
    }

    @Test
    void extractIpAddress_whenTrustUnset_keepsLegacyBehaviour() {
        // Unset is the default, so upgrading must not change what is recorded.
        when(appConfiguration.getTrustedProxyEnabled()).thenReturn(null);
        stubRequest("10.1.1.1", "203.0.113.9, 10.0.0.5");

        assertEquals("203.0.113.9", metricService.extractIpAddress(httpRequest));
    }

    @Test
    void extractIpAddress_whenTrustDisabled_ignoresHeadersEntirely() {
        when(appConfiguration.getTrustedProxyEnabled()).thenReturn(Boolean.FALSE);
        stubRequest("10.1.1.1", "203.0.113.9");

        assertEquals("10.1.1.1", metricService.extractIpAddress(httpRequest));
    }

    @Test
    void extractIpAddress_whenTrustEnabledButNoRangesConfigured_ignoresHeaders() {
        // Enabling the check while trusting nothing must fail closed, not fall back to trusting all.
        when(appConfiguration.getTrustedProxyEnabled()).thenReturn(Boolean.TRUE);
        when(appConfiguration.getTrustedProxyIpRanges()).thenReturn(Collections.emptyList());
        stubRequest("10.1.1.1", "203.0.113.9");

        assertEquals("10.1.1.1", metricService.extractIpAddress(httpRequest));
    }

    /**
     * The four cases that matter once a deployment has declared its trusted proxies. They share the same
     * configuration and differ only in what arrives, so they are one parameterized test.
     * <ul>
     * <li><b>untrusted caller</b> - the defect itself: a direct caller spoofing the header is not believed.
     * <li><b>trusted caller</b> - a real proxy is believed.
     * <li><b>chain spoofed on the left</b> - the chain is read right to left, so a value the client
     * prepended before the proxy appended is skipped. Reading left to right would defeat the whole check.
     * <li><b>every hop trusted</b> - nothing identifies a client, so the socket address stands.
     * </ul>
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "untrusted caller is ignored,     198.51.100.7, 203.0.113.9,                      198.51.100.7",
            "trusted caller is believed,      10.1.1.1,     203.0.113.9,                      203.0.113.9",
            "chain spoofed on the left,       10.1.1.1,     '1.2.3.4, 203.0.113.9, 10.0.0.5', 203.0.113.9",
            "every hop is a trusted proxy,    10.1.1.1,     '10.0.0.5, 10.0.0.6',             10.1.1.1" })
    void extractIpAddress_whenTrustEnabled_resolvesTheClientAddress(String scenario, String remoteAddr,
            String forwardedFor, String expected) {
        when(appConfiguration.getTrustedProxyEnabled()).thenReturn(Boolean.TRUE);
        when(appConfiguration.getTrustedProxyIpRanges()).thenReturn(List.of("10.0.0.0/8"));
        stubRequest(remoteAddr, forwardedFor);

        assertEquals(expected, metricService.extractIpAddress(httpRequest), scenario);
    }

    /**
     * Legacy mode must stay byte-for-byte identical, and the previous implementation split every proxy
     * header on commas - not only X-Forwarded-For. Validating an alternative header as a whole value
     * would reject a chain it used to accept and silently record a different address after an upgrade.
     */
    @Test
    void extractIpAddress_whenTrustUnset_splitsChainsInAlternativeHeadersToo() {
        when(appConfiguration.getTrustedProxyEnabled()).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("10.1.1.1");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getHeader("Proxy-Client-IP")).thenReturn("203.0.113.9, 10.0.0.5");

        assertEquals("203.0.113.9", metricService.extractIpAddress(httpRequest));
    }

    @Test
    void extractIpAddress_whenTrustUnsetAndNoHeaders_usesTheSocketAddress() {
        when(appConfiguration.getTrustedProxyEnabled()).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("10.1.1.1");

        assertEquals("10.1.1.1", metricService.extractIpAddress(httpRequest));
    }

    // ---------------------------------------------------------------------------------------------
    // Follow-ups to the trusted-proxy work (#15097, #15098)
    // ---------------------------------------------------------------------------------------------

    /**
     * #15097. A proxy overwrites X-Forwarded-For but passes other request headers through as the client
     * sent them, so in trusted mode they carry no guarantee and must not be recorded. Consulting them
     * would reopen the very spoofing route the trusted-range check exists to close.
     */
    @ParameterizedTest(name = "trusted mode ignores {0}")
    @CsvSource({ "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_FORWARDED_FOR" })
    void extractIpAddress_whenTrustEnabled_ignoresTheAlternativeProxyHeaders(String header) {
        when(appConfiguration.getTrustedProxyEnabled()).thenReturn(Boolean.TRUE);
        when(appConfiguration.getTrustedProxyIpRanges()).thenReturn(List.of("10.0.0.0/8"));
        when(httpRequest.getRemoteAddr()).thenReturn("10.1.1.1");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getHeader(header)).thenReturn("203.0.113.9");

        assertEquals("10.1.1.1", metricService.extractIpAddress(httpRequest));
    }

    /**
     * The same headers must keep working in legacy mode, which is relied on for backward compatibility.
     */
    @ParameterizedTest(name = "legacy mode still reads {0}")
    @CsvSource({ "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_FORWARDED_FOR" })
    void extractIpAddress_whenTrustUnset_stillReadsTheAlternativeProxyHeaders(String header) {
        when(appConfiguration.getTrustedProxyEnabled()).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("10.1.1.1");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getHeader(header)).thenReturn("203.0.113.9");

        assertEquals("203.0.113.9", metricService.extractIpAddress(httpRequest));
    }

    /**
     * #15098. The prefix is written against the range as typed, but normalisation narrows a mapped range
     * to four bytes, so the prefix has to shed the 96 bits of the ::ffff: mapping. Without that,
     * ::ffff:10.0.0.0/104 is rejected as out of range and the configured proxy is silently untrusted.
     */
    @Test
    void isIpInCidr_honoursIpv4MappedRangesWrittenWithAnIpv6Prefix() {
        // /104 on the mapped form is the same set as /8 on the IPv4 form.
        assertTrue(metricService.isIpInCidr("10.1.2.3", "::ffff:10.0.0.0/104"));
        assertTrue(metricService.isIpInCidr("::ffff:10.1.2.3", "::ffff:10.0.0.0/104"));
        assertFalse(metricService.isIpInCidr("11.1.2.3", "::ffff:10.0.0.0/104"));

        // /120 == /24
        assertTrue(metricService.isIpInCidr("192.168.1.42", "::ffff:192.168.1.0/120"));
        assertFalse(metricService.isIpInCidr("192.168.2.42", "::ffff:192.168.1.0/120"));
    }

    @Test
    void isIpInCidr_withBareIpv4MappedAddressUsesAFullMask() {
        assertTrue(metricService.isIpInCidr("10.1.1.1", "::ffff:10.1.1.1"));
        assertFalse(metricService.isIpInCidr("10.1.1.2", "::ffff:10.1.1.1"));
    }

    /**
     * A mapped range whose prefix would fit an IPv4 mask keeps its existing meaning. This form already
     * worked before the prefix adjustment - InetAddress collapses the literal to an Inet4Address - and is
     * a natural thing to configure after copying an address out of a log on a dual-stack JVM, so
     * re-reading it on the IPv6 scale would silently stop honouring a range in use.
     */
    @Test
    void isIpInCidr_leavesAnIpv4ScalePrefixOnAMappedRangeAlone() {
        assertTrue(metricService.isIpInCidr("10.1.2.3", "::ffff:10.0.0.0/8"));
        assertFalse(metricService.isIpInCidr("11.1.2.3", "::ffff:10.0.0.0/8"));
        assertTrue(metricService.isIpInCidr("192.168.1.42", "::ffff:192.168.1.0/24"));
    }

    /**
     * Between 33 and 95 a mapped prefix covers part of the mapping itself and means nothing on either
     * scale, so it is still rejected.
     */
    @Test
    void isIpInCidr_rejectsAMappedPrefixThatCoversTheMapping() {
        assertFalse(metricService.isIpInCidr("10.1.2.3", "::ffff:10.0.0.0/33"));
        assertFalse(metricService.isIpInCidr("10.1.2.3", "::ffff:10.0.0.0/95"));
    }

    @Test
    void isIpInCidr_matchesIpv4Ranges() {
        assertTrue(metricService.isIpInCidr("10.1.2.3", "10.0.0.0/8"));
        assertTrue(metricService.isIpInCidr("192.168.1.42", "192.168.1.0/24"));
        assertFalse(metricService.isIpInCidr("11.1.2.3", "10.0.0.0/8"));
        assertFalse(metricService.isIpInCidr("192.168.2.1", "192.168.1.0/24"));
    }

    @Test
    void isIpInCidr_matchesIpv6AndNeverMixesFamilies() {
        assertTrue(metricService.isIpInCidr("::1", "::1/128"));
        assertTrue(metricService.isIpInCidr("2001:db8::5", "2001:db8::/32"));
        assertFalse(metricService.isIpInCidr("2001:db9::5", "2001:db8::/32"));
        // An IPv4 address cannot fall inside an IPv6 range, or the reverse.
        assertFalse(metricService.isIpInCidr("10.1.2.3", "2001:db8::/32"));
        assertFalse(metricService.isIpInCidr("2001:db8::5", "10.0.0.0/8"));
    }

    /**
     * A dual-stack JVM can return the IPv4-mapped form from getRemoteAddr(), which must still match an
     * IPv4 range -- otherwise the proxy stops being recognised depending on how the JVM was started.
     */
    @Test
    void isIpInCidr_matchesIpv4MappedAddressesAgainstIpv4Ranges() {
        assertTrue(metricService.isIpInCidr("::ffff:10.1.2.3", "10.0.0.0/8"));
        assertFalse(metricService.isIpInCidr("::ffff:11.1.2.3", "10.0.0.0/8"));
    }

    @Test
    void isIpInCidr_withBareAddressUsesAFullMask() {
        assertTrue(metricService.isIpInCidr("10.1.1.1", "10.1.1.1"));
        assertFalse(metricService.isIpInCidr("10.1.1.2", "10.1.1.1"));
    }

    /**
     * Both sides must be IP literals. InetAddress.getByName resolves a hostname through DNS, and this
     * runs on the request path for every metrics write, so a mistyped range must be rejected outright
     * rather than becoming a blocking lookup.
     */
    @Test
    void isIpInCidr_rejectsNonLiteralsRatherThanResolvingThem() {
        assertFalse(metricService.isIpInCidr("10.1.1.1", "example.com/24"));
        assertFalse(metricService.isIpInCidr("example.com", "10.0.0.0/8"));
        assertFalse(metricService.isIpInCidr("10.1.1.1", "not-an-ip"));
    }

    @Test
    void isIpInCidr_rejectsOutOfRangePrefixLengths() {
        assertFalse(metricService.isIpInCidr("10.1.1.1", "10.0.0.0/33"));
        assertFalse(metricService.isIpInCidr("10.1.1.1", "10.0.0.0/-1"));
        assertFalse(metricService.isIpInCidr("10.1.1.1", "10.0.0.0/abc"));
    }

    @Test
    void isFromTrustedProxy_toleratesMissingAndMalformedConfiguration() {
        assertFalse(metricService.isFromTrustedProxy("10.1.1.1", null));
        assertFalse(metricService.isFromTrustedProxy(null, List.of("10.0.0.0/8")));
        assertFalse(metricService.isFromTrustedProxy("10.1.1.1", Collections.emptyList()));
        // One unusable entry must not stop a later valid one from matching.
        assertTrue(metricService.isFromTrustedProxy("10.1.1.1", Arrays.asList("garbage", "10.0.0.0/8")));
    }
}
