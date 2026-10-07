/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.metric;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.metric.Fido2MetricsConstants;
import io.jans.fido2.model.metric.Fido2MetricsEntry;
import io.jans.fido2.model.trust.AttestationTrustDiagnostic;
import io.jans.orm.PersistenceEntryManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

/**
 * Aggregation of attestation rejections by trust diagnostic code.
 *
 * @author Janssen Project
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Fido2MetricsAttestationRejectionTest {

    @Spy
    @InjectMocks
    private Fido2MetricsService metricsService;

    @Mock
    private Logger log;
    @Mock
    private AppConfiguration appConfiguration;
    @Mock
    private PersistenceEntryManager persistenceEntryManager;

    private static final LocalDateTime START = LocalDateTime.of(2026, 8, 1, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 8, 7, 0, 0);

    private Fido2MetricsEntry rejection(AttestationTrustDiagnostic diagnostic, String aaguid) {
        Fido2MetricsEntry entry = new Fido2MetricsEntry();
        entry.setOperationType(Fido2MetricsConstants.REGISTRATION);
        entry.setStatus(Fido2MetricsConstants.FAILURE);
        entry.setErrorReason(diagnostic.name());
        entry.setErrorCategory(AttestationTrustDiagnostic.CATEGORY);
        if (aaguid != null) {
            Map<String, Object> additionalData = new HashMap<>();
            additionalData.put(Fido2MetricsConstants.AAGUID, aaguid);
            entry.setAdditionalData(additionalData);
        }
        return entry;
    }

    private Fido2MetricsEntry attempt() {
        Fido2MetricsEntry entry = new Fido2MetricsEntry();
        entry.setOperationType(Fido2MetricsConstants.REGISTRATION);
        entry.setStatus(Fido2MetricsConstants.ATTEMPT);
        return entry;
    }

    private Fido2MetricsEntry unrelatedFailure() {
        Fido2MetricsEntry entry = new Fido2MetricsEntry();
        entry.setOperationType(Fido2MetricsConstants.REGISTRATION);
        entry.setStatus(Fido2MetricsConstants.FAILURE);
        entry.setErrorReason("Challenge mismatch");
        entry.setErrorCategory("INVALID_INPUT");
        return entry;
    }

    private void givenEntries(List<Fido2MetricsEntry> entries) {
        doReturn(entries).when(metricsService).getMetricsEntries(any(), any());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> counts(Map<String, Object> analysis, String key) {
        return (Map<String, Long>) analysis.get(key);
    }

    @Test
    void getAttestationRejectionAnalysis_countsPerReasonCode() {
        givenEntries(Arrays.asList(
                rejection(AttestationTrustDiagnostic.JFS_AAGUID_NOT_IN_MDS, "aaguid-a"),
                rejection(AttestationTrustDiagnostic.JFS_AAGUID_NOT_IN_MDS, "aaguid-a"),
                rejection(AttestationTrustDiagnostic.JFS_ROOT_CERT_NOT_TRUSTED, null),
                attempt(), attempt(), attempt(), attempt()));

        Map<String, Object> analysis = metricsService.getAttestationRejectionAnalysis(START, END);

        assertEquals(3L, analysis.get("totalRejections"));
        Map<String, Long> reasonCodes = counts(analysis, "reasonCodes");
        assertEquals(2L, reasonCodes.get("JFS_AAGUID_NOT_IN_MDS"));
        assertEquals(1L, reasonCodes.get("JFS_ROOT_CERT_NOT_TRUSTED"));
        assertEquals(0.75, (Double) analysis.get("rejectionRate"));
    }

    /**
     * A registration failure that is not a trust problem must not be counted as a rejection — telling
     * the two apart is the whole point of the category.
     */
    @Test
    void getAttestationRejectionAnalysis_ignoresNonTrustFailures() {
        givenEntries(Arrays.asList(
                rejection(AttestationTrustDiagnostic.JFS_AAGUID_NOT_IN_MDS, "aaguid-a"),
                unrelatedFailure(), unrelatedFailure(),
                attempt(), attempt()));

        Map<String, Object> analysis = metricsService.getAttestationRejectionAnalysis(START, END);

        assertEquals(1L, analysis.get("totalRejections"));
        assertEquals(1, counts(analysis, "reasonCodes").size());
    }

    @Test
    void getAttestationRejectionAnalysis_ranksRejectedAaguids() {
        givenEntries(Arrays.asList(
                rejection(AttestationTrustDiagnostic.JFS_AAGUID_NOT_IN_MDS, "aaguid-a"),
                rejection(AttestationTrustDiagnostic.JFS_AAGUID_NOT_IN_MDS, "aaguid-a"),
                rejection(AttestationTrustDiagnostic.JFS_AUTHENTICATOR_STATUS_UNACCEPTABLE, "aaguid-b"),
                // Not tied to an authenticator model: counted as a rejection, but not against an AAGUID.
                rejection(AttestationTrustDiagnostic.JFS_ATTESTATION_FORMAT_NOT_PERMITTED, null),
                attempt(), attempt(), attempt(), attempt()));

        Map<String, Object> analysis = metricsService.getAttestationRejectionAnalysis(START, END);

        Map<String, Long> aaguids = counts(analysis, "topRejectedAaguids");
        assertEquals(2, aaguids.size());
        assertEquals(2L, aaguids.get("aaguid-a"));
        assertEquals(1L, aaguids.get("aaguid-b"));
        assertEquals(4L, analysis.get("totalRejections"));
    }

    /**
     * A rejection and the attempt it belongs to are separate records, so a range can hold one without
     * the other. Publishing 0.0 against real rejections would read as "nothing is being rejected",
     * which is the opposite of the truth.
     */
    @Test
    void getAttestationRejectionAnalysis_ifNoAttemptsInRange_reportsNoRateAndSaysWhy() {
        givenEntries(Collections.singletonList(
                rejection(AttestationTrustDiagnostic.JFS_AAGUID_NOT_IN_MDS, "aaguid-a")));

        Map<String, Object> analysis = metricsService.getAttestationRejectionAnalysis(START, END);

        assertTrue(analysis.containsKey("rejectionRate"));
        assertNull(analysis.get("rejectionRate"));
        assertNotNull(analysis.get("rejectionRateNote"));
        assertEquals(1L, analysis.get("totalRejections"));
    }

    /** More rejections than attempts means attempts fell outside the range; cap rather than exceed 1.0. */
    @Test
    void getAttestationRejectionAnalysis_ifMoreRejectionsThanAttempts_capsTheRate() {
        givenEntries(Arrays.asList(
                rejection(AttestationTrustDiagnostic.JFS_AAGUID_NOT_IN_MDS, "aaguid-a"),
                rejection(AttestationTrustDiagnostic.JFS_AAGUID_NOT_IN_MDS, "aaguid-a"),
                rejection(AttestationTrustDiagnostic.JFS_AAGUID_NOT_IN_MDS, "aaguid-a"),
                attempt()));

        Map<String, Object> analysis = metricsService.getAttestationRejectionAnalysis(START, END);

        assertEquals(1.0, (Double) analysis.get("rejectionRate"));
        assertNotNull(analysis.get("rejectionRateNote"));
    }

    @Test
    void getAttestationRejectionAnalysis_ifNoRejections_reportsZeroWithoutFailing() {
        givenEntries(Arrays.asList(attempt(), attempt()));

        Map<String, Object> analysis = metricsService.getAttestationRejectionAnalysis(START, END);

        assertEquals(0L, analysis.get("totalRejections"));
        assertTrue(counts(analysis, "reasonCodes").isEmpty());
        assertTrue(counts(analysis, "topRejectedAaguids").isEmpty());
        assertEquals(0.0, (Double) analysis.get("rejectionRate"));
    }

    @Test
    void getAttestationRejectionAnalysis_ifNoEntriesAtAll_returnsEmptyAnalysis() {
        givenEntries(Collections.emptyList());

        Map<String, Object> analysis = metricsService.getAttestationRejectionAnalysis(START, END);

        assertEquals(0L, analysis.get("totalRejections"));
        assertEquals(0L, analysis.get("registrationAttempts"));
        assertNull(analysis.get("rejectionRate"));
    }
}
