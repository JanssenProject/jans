/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.fido2.service.metric;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.metric.Fido2MetricsConstants;
import io.jans.fido2.model.metric.Fido2MetricsData;
import io.jans.fido2.model.metric.Fido2MetricsEntry;
import io.jans.orm.PersistenceEntryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for Fido2MetricsService field-length handling.
 *
 * <p>A metrics entry is persisted as one row, so a single oversized value makes
 * the whole INSERT fail and the entry is lost silently -- the write is
 * fire-and-forget. These tests pin the guard that prevents that.
 *
 * <p>Coverage is targeted at the fields that can actually overflow: those carrying
 * free-form input (user agent, error reason, fallback reason, session id, username,
 * user id) and the device-info members. The remaining fields -- operation type,
 * status, metric type, authenticator type, error category, fallback method, node id
 * and IP address -- are bounded by construction, drawn from fixed vocabularies or a
 * MAC address or an IPv6 literal, and all sit at the schema default. They share the
 * same code path as the fields exercised here, so a per-field test would add no
 * signal.
 *
 * @author Janssen Project
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Fido2MetricsServiceTest {

    private static final String REAL_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private PersistenceEntryManager persistenceEntryManager;

    @InjectMocks
    private Fido2MetricsService fido2MetricsService;

    @BeforeEach
    void setUp() {
        when(appConfiguration.isFido2MetricsEnabled()).thenReturn(true);
    }

    /**
     * Abandonment is reported alongside the inferred dropOffRate, not instead of it. dropOffRate is
     * the residual of attempts minus completions and so also absorbs ceremonies still in flight,
     * whereas abandonedOperations counts ceremonies observed to have lapsed.
     */
    @Test
    void getErrorAnalysis_reportsObservedAbandonmentAlongsideInferredDropOff() {
        stubMetricsEntries(
                statusEntry(Fido2MetricsConstants.ATTEMPT),
                statusEntry(Fido2MetricsConstants.ATTEMPT),
                statusEntry(Fido2MetricsConstants.ATTEMPT),
                statusEntry(Fido2MetricsConstants.ATTEMPT),
                statusEntry(Fido2MetricsConstants.SUCCESS),
                statusEntry(Fido2MetricsConstants.ABANDONED));

        Map<String, Object> analysis = fido2MetricsService.getErrorAnalysis(LocalDateTime.now().minusDays(1),
                LocalDateTime.now());

        assertEquals(1L, analysis.get(Fido2MetricsConstants.ABANDONED_OPERATIONS));
        assertEquals(0.25, (Double) analysis.get(Fido2MetricsConstants.ABANDONMENT_RATE), 0.0001);
        // Still inferred independently, and still larger, because it also covers the three attempts
        // that produced no terminal entry at all.
        assertEquals(0.75, (Double) analysis.get(Fido2MetricsConstants.DROP_OFF_RATE), 0.0001);
    }

    /**
     * An ABANDONED entry must not be counted as a completion — it is neither a verified success nor a
     * server-rejected failure, and folding it into either would misstate both rates.
     */
    @Test
    void getErrorAnalysis_doesNotCountAbandonmentAsCompletion() {
        stubMetricsEntries(
                statusEntry(Fido2MetricsConstants.ATTEMPT),
                statusEntry(Fido2MetricsConstants.ATTEMPT),
                statusEntry(Fido2MetricsConstants.SUCCESS),
                statusEntry(Fido2MetricsConstants.ABANDONED));

        Map<String, Object> analysis = fido2MetricsService.getErrorAnalysis(LocalDateTime.now().minusDays(1),
                LocalDateTime.now());

        assertEquals(0.5, (Double) analysis.get(Fido2MetricsConstants.SUCCESS_RATE), 0.0001);
        assertEquals(0.0, (Double) analysis.get(Fido2MetricsConstants.FAILURE_RATE), 0.0001);
        assertEquals(0.5, (Double) analysis.get(Fido2MetricsConstants.COMPLETION_RATE), 0.0001);
    }

    /**
     * The response shape stays stable when there is nothing to report, so clients do not have to
     * distinguish "absent key" from "zero".
     */
    @Test
    void getErrorAnalysis_ifNoEntries_stillEmitsAbandonmentKeys() {
        stubMetricsEntries();

        Map<String, Object> analysis = fido2MetricsService.getErrorAnalysis(LocalDateTime.now().minusDays(1),
                LocalDateTime.now());

        assertEquals(0L, analysis.get(Fido2MetricsConstants.ABANDONED_OPERATIONS));
        assertEquals(0.0, (Double) analysis.get(Fido2MetricsConstants.ABANDONMENT_RATE), 0.0001);
    }

    private void stubMetricsEntries(Fido2MetricsEntry... entries) {
        when(persistenceEntryManager.findEntries(any(String.class), eq(Fido2MetricsEntry.class), any()))
                .thenReturn(List.of(entries));
    }

    private Fido2MetricsEntry statusEntry(String status) {
        Fido2MetricsEntry entry = new Fido2MetricsEntry();
        entry.setStatus(status);
        entry.setOperationType(Fido2MetricsConstants.AUTHENTICATION);
        return entry;
    }

    @Test
    void testOversizedUserAgentIsTruncatedToColumnWidth() {
        // Given
        Fido2MetricsData metricsData = baseMetricsData();
        metricsData.setUserAgent("u".repeat(1000));

        // When
        fido2MetricsService.storeMetricsData(metricsData);

        // Then
        Fido2MetricsEntry entry = capturePersistedEntry();
        assertEquals(Fido2MetricsConstants.MAX_LENGTH_USER_AGENT, entry.getUserAgent().length(),
                "user agent must be cut to the column width instead of failing the INSERT");
    }

    /**
     * The regression guard in the other direction: a real browser user agent is well
     * under the widened column, so it must reach the database byte for byte. If this
     * fails the schema widening is doing nothing and truncation is destroying data.
     */
    @Test
    void testTypicalUserAgentIsStoredIntact() {
        // Given
        Fido2MetricsData metricsData = baseMetricsData();
        metricsData.setUserAgent(REAL_USER_AGENT);

        // When
        fido2MetricsService.storeMetricsData(metricsData);

        // Then
        Fido2MetricsEntry entry = capturePersistedEntry();
        assertEquals(REAL_USER_AGENT, entry.getUserAgent(),
                "a real user agent fits the widened column and must not be shortened");
    }

    @Test
    void testOversizedErrorReasonIsTruncated() {
        // Given: raw exception text, which is unbounded
        Fido2MetricsData metricsData = baseMetricsData();
        metricsData.setErrorReason("e".repeat(4000));

        // When
        fido2MetricsService.storeMetricsData(metricsData);

        // Then
        Fido2MetricsEntry entry = capturePersistedEntry();
        assertEquals(Fido2MetricsConstants.MAX_LENGTH_ERROR_REASON, entry.getErrorReason().length());
    }

    @Test
    void testOversizedFallbackReasonIsTruncated() {
        // Given: free text supplied by the caller of recordPasskeyFallback
        Fido2MetricsData metricsData = baseMetricsData();
        metricsData.setFallbackReason("f".repeat(2000));

        // When
        fido2MetricsService.storeMetricsData(metricsData);

        // Then
        Fido2MetricsEntry entry = capturePersistedEntry();
        assertEquals(Fido2MetricsConstants.MAX_LENGTH_FALLBACK_REASON, entry.getFallbackReason().length());
    }

    @Test
    void testOversizedSessionIdIsTruncated() {
        // Given: the session id comes from a client-controlled cookie
        Fido2MetricsData metricsData = baseMetricsData();
        metricsData.setSessionId("s".repeat(500));

        // When
        fido2MetricsService.storeMetricsData(metricsData);

        // Then
        Fido2MetricsEntry entry = capturePersistedEntry();
        assertEquals(Fido2MetricsConstants.MAX_LENGTH_SESSION_ID, entry.getSessionId().length());
    }

    /**
     * Essential fields were set with raw setters that bypassed every guard, so a
     * client submitting a long username on a failed attempt could drop the row.
     */
    @Test
    void testOversizedEssentialFieldsAreTruncated() {
        // Given
        Fido2MetricsData metricsData = baseMetricsData();
        metricsData.setUsername("n".repeat(900));
        metricsData.setUserId("i".repeat(900));

        // When
        fido2MetricsService.storeMetricsData(metricsData);

        // Then
        Fido2MetricsEntry entry = capturePersistedEntry();
        assertEquals(Fido2MetricsConstants.MAX_LENGTH_USERNAME, entry.getUsername().length());
        assertEquals(Fido2MetricsConstants.MAX_LENGTH_USER_ID, entry.getUserId().length());
    }

    /**
     * The device info blob embeds its own copy of the raw user agent, so it overflows
     * independently of the top-level column.
     */
    @Test
    void testOversizedNestedDeviceInfoUserAgentIsTruncated() {
        // Given
        Fido2MetricsData metricsData = baseMetricsData();
        Fido2MetricsData.DeviceInfo deviceInfo = new Fido2MetricsData.DeviceInfo();
        deviceInfo.setBrowser("Chrome");
        deviceInfo.setUserAgent("d".repeat(2000));
        metricsData.setDeviceInfo(deviceInfo);

        // When
        fido2MetricsService.storeMetricsData(metricsData);

        // Then
        Fido2MetricsEntry entry = capturePersistedEntry();
        assertNotNull(entry.getDeviceInfo());
        assertEquals(Fido2MetricsConstants.MAX_LENGTH_USER_AGENT, entry.getDeviceInfo().getUserAgent().length());
        assertEquals("Chrome", entry.getDeviceInfo().getBrowser(), "short members must be untouched");
    }

    /**
     * The parsed device-info members are capped separately from the nested user
     * agent, at a tighter limit. A malformed user agent can push a long value into
     * any of them, so the cap has to hold for every member, not just the one.
     */
    @Test
    void testOversizedDeviceInfoMembersAreTruncatedToTheFieldCap() {
        // Given
        Fido2MetricsData metricsData = baseMetricsData();
        Fido2MetricsData.DeviceInfo deviceInfo = new Fido2MetricsData.DeviceInfo();
        deviceInfo.setBrowser("b".repeat(300));
        deviceInfo.setBrowserVersion("v".repeat(300));
        deviceInfo.setOperatingSystem("o".repeat(300));
        deviceInfo.setOsVersion("V".repeat(300));
        deviceInfo.setDeviceType("t".repeat(300));
        metricsData.setDeviceInfo(deviceInfo);

        // When
        fido2MetricsService.storeMetricsData(metricsData);

        // Then
        Fido2MetricsEntry.DeviceInfo stored = capturePersistedEntry().getDeviceInfo();
        assertNotNull(stored);
        int cap = Fido2MetricsConstants.MAX_LENGTH_DEVICE_INFO_FIELD;
        assertEquals(cap, stored.getBrowser().length());
        assertEquals(cap, stored.getBrowserVersion().length());
        assertEquals(cap, stored.getOs().length());
        assertEquals(cap, stored.getOsVersion().length());
        assertEquals(cap, stored.getDeviceType().length());
    }

    @Test
    void testNullAndBlankFieldsKeepTheirExistingSemantics() {
        // Given
        Fido2MetricsData metricsData = baseMetricsData();
        metricsData.setUsername(null);
        metricsData.setUserAgent("   ");

        // When
        fido2MetricsService.storeMetricsData(metricsData);

        // Then
        Fido2MetricsEntry entry = capturePersistedEntry();
        assertNull(entry.getUsername(), "a null essential field must still be set to null");
        assertNull(entry.getUserAgent(), "a blank optional field must still be skipped");
    }

    /**
     * A client sending oversized headers on every request must not be able to flood
     * the log, so the warning is emitted once per entry rather than once per field.
     */
    @Test
    void testWarningIsLoggedOncePerEntryRegardlessOfFieldCount() {
        // Given
        Fido2MetricsData metricsData = baseMetricsData();
        metricsData.setUserAgent("u".repeat(1000));
        metricsData.setErrorReason("e".repeat(4000));
        metricsData.setSessionId("s".repeat(500));

        // When
        fido2MetricsService.storeMetricsData(metricsData);
        capturePersistedEntry();

        // Then
        verify(log).warn(anyString(), any(), any());
    }

    @Test
    void testNoWarningWhenNothingIsTruncated() {
        // Given
        Fido2MetricsData metricsData = baseMetricsData();
        metricsData.setUserAgent(REAL_USER_AGENT);

        // When
        fido2MetricsService.storeMetricsData(metricsData);
        capturePersistedEntry();

        // Then
        verify(log, never()).warn(anyString(), any(), any());
    }

    /**
     * The warning names fields and lengths. The values are PII and must never reach
     * the log.
     */
    @Test
    void testWarningDoesNotContainTheTruncatedValue() {
        // Given
        Fido2MetricsData metricsData = baseMetricsData();
        String sensitive = "secret-token-" + "z".repeat(600);
        metricsData.setSessionId(sensitive);

        // When
        fido2MetricsService.storeMetricsData(metricsData);
        capturePersistedEntry();

        // Then
        ArgumentCaptor<Object> detail = ArgumentCaptor.forClass(Object.class);
        verify(log).warn(anyString(), any(), detail.capture());
        assertFalse(String.valueOf(detail.getValue()).contains("secret-token"),
                "the truncated value is PII and must stay out of the log");
    }

    /**
     * Entries are persisted asynchronously, so wait for the write and hand back what
     * was stored.
     */
    private Fido2MetricsEntry capturePersistedEntry() {
        ArgumentCaptor<Fido2MetricsEntry> captor = ArgumentCaptor.forClass(Fido2MetricsEntry.class);
        verify(persistenceEntryManager, timeout(5000)).persist(captor.capture());
        return captor.getValue();
    }

    private Fido2MetricsData baseMetricsData() {
        Fido2MetricsData metricsData = new Fido2MetricsData();
        metricsData.setMetricType("fido2_registration_success");
        metricsData.setOperationType("REGISTRATION");
        metricsData.setOperationStatus("SUCCESS");
        metricsData.setUsername("testuser");
        metricsData.setUserId("inum-1234");
        return metricsData;
    }
}
