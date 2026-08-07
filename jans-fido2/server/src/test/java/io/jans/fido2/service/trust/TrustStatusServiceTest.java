/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.trust;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.conf.MetadataServer;
import io.jans.fido2.model.trust.AttestationTrustConfig;
import io.jans.fido2.model.trust.MdsHealth;
import io.jans.fido2.model.trust.MdsHealthStatus;
import io.jans.fido2.model.trust.MetadataServerStatus;
import io.jans.fido2.service.mds.AttestationCertificateService;
import io.jans.fido2.service.mds.TocService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TrustStatusServiceTest {

    @InjectMocks
    private TrustStatusService trustStatusService;

    @Mock
    private Logger log;
    @Mock
    private AppConfiguration appConfiguration;
    @Mock
    private AttestationCertificateService attestationCertificateService;
    @Mock
    private TocService tocService;

    private Fido2Configuration configure(String attestationMode, boolean metadataServiceDisabled) {
        Fido2Configuration cfg = mock(Fido2Configuration.class);
        when(cfg.getAttestationMode()).thenReturn(attestationMode);
        when(cfg.isDisableMetadataService()).thenReturn(metadataServiceDisabled);
        when(appConfiguration.getFido2Configuration()).thenReturn(cfg);
        return cfg;
    }

    @Test
    void getAttestationTrustConfig_ifEnforced_disallowsUnattested() {
        configure("enforced", false);
        when(attestationCertificateService.isAppleRootCaPresent()).thenReturn(true);

        AttestationTrustConfig config = trustStatusService.getAttestationTrustConfig();

        assertEquals("enforced", config.getAttestationMode());
        assertTrue(config.isAttestationModeRecognized());
        assertFalse(config.isUnattestedAuthenticatorsAllowed());
        assertTrue(config.isAppleRootCaPresent());
    }

    @Test
    void getAttestationTrustConfig_ifMonitor_stillAllowsUnattested() {
        configure("monitor", false);

        AttestationTrustConfig config = trustStatusService.getAttestationTrustConfig();

        // Only "enforced" applies the stricter trust rules — monitor is lenient, which is exactly the
        // thing administrators get wrong about the default.
        assertTrue(config.isAttestationModeRecognized());
        assertTrue(config.isUnattestedAuthenticatorsAllowed());
    }

    @Test
    void getAttestationTrustConfig_ifDisabled_allowsUnattested() {
        configure("disabled", true);

        AttestationTrustConfig config = trustStatusService.getAttestationTrustConfig();

        assertTrue(config.isAttestationModeRecognized());
        assertTrue(config.isUnattestedAuthenticatorsAllowed());
        assertTrue(config.isMetadataServiceDisabled());
    }

    @Test
    void getAttestationTrustConfig_ifModeUnrecognized_reportsRawValueAndLenientBehaviour() {
        configure("Enforce", false);

        AttestationTrustConfig config = trustStatusService.getAttestationTrustConfig();

        // A typo silently makes the server lenient; report it rather than normalising it away.
        assertEquals("Enforce", config.getAttestationMode());
        assertFalse(config.isAttestationModeRecognized());
        assertTrue(config.isUnattestedAuthenticatorsAllowed());
    }

    @Test
    void getAttestationTrustConfig_ifAppleRootCaMissing_reportsAbsent() {
        configure("monitor", false);
        when(attestationCertificateService.isAppleRootCaPresent()).thenReturn(false);

        AttestationTrustConfig config = trustStatusService.getAttestationTrustConfig();

        // Today this condition is only a startup log warning.
        assertFalse(config.isAppleRootCaPresent());
    }

    @Test
    void getAttestationTrustConfig_ifNoFido2Configuration_returnsEmptyConfig() {
        when(appConfiguration.getFido2Configuration()).thenReturn(null);

        AttestationTrustConfig config = trustStatusService.getAttestationTrustConfig();

        assertNotNull(config);
        assertNull(config.getAttestationMode());
    }

    // --- MDS health (#14639) --------------------------------------------------------------------

    /** A healthy server: metadata enabled, entries loaded, blob still inside its validity window. */
    private Fido2Configuration configureHealthyMds() {
        Fido2Configuration cfg = configure("monitor", false);
        when(tocService.getTocEntryCount()).thenReturn(1284);
        when(tocService.getLoadedTocNextUpdate()).thenReturn(LocalDate.now().plusDays(20));
        return cfg;
    }

    @Test
    void getMdsHealth_ifLoadedAndCurrent_isUp() {
        configureHealthyMds();
        when(tocService.getLastSuccessfulRefresh()).thenReturn(LocalDateTime.of(2026, 8, 1, 4, 15, 22));

        MdsHealth health = trustStatusService.getMdsHealth();

        assertEquals(MdsHealthStatus.UP, health.getStatus());
        assertEquals(1284, health.getTocEntryCount());
        assertFalse(health.isBlobExpired());
        assertEquals("2026-08-01T04:15:22", health.getLastSuccessfulRefresh());
        assertNotNull(health.getTimestamp());
    }

    @Test
    void getMdsHealth_ifNoEntriesLoaded_isDown() {
        configure("monitor", false);
        when(tocService.getTocEntryCount()).thenReturn(0);
        when(tocService.getLoadedTocNextUpdate()).thenReturn(LocalDate.now().plusDays(20));

        MdsHealth health = trustStatusService.getMdsHealth();

        // Nothing to validate attestation against, whatever the blob says.
        assertEquals(MdsHealthStatus.DOWN, health.getStatus());
    }

    /**
     * The blob is due for re-download from the moment nextUpdate arrives, not the day after — the same
     * boundary the download path applies, so health and behaviour cannot disagree.
     */
    @Test
    void getMdsHealth_ifBlobNextUpdateIsToday_isExpiredAndDown() {
        configure("monitor", false);
        when(tocService.getTocEntryCount()).thenReturn(1284);
        when(tocService.getLoadedTocNextUpdate()).thenReturn(LocalDate.now());

        MdsHealth health = trustStatusService.getMdsHealth();

        assertTrue(health.isBlobExpired());
        assertEquals(MdsHealthStatus.DOWN, health.getStatus());
    }

    @Test
    void getMdsHealth_ifNoBlobLoaded_isExpiredAndDown() {
        configure("monitor", false);
        when(tocService.getTocEntryCount()).thenReturn(0);
        when(tocService.getLoadedTocNextUpdate()).thenReturn(null);

        MdsHealth health = trustStatusService.getMdsHealth();

        assertNull(health.getNextUpdate());
        assertTrue(health.isBlobExpired());
        assertEquals(MdsHealthStatus.DOWN, health.getStatus());
    }

    /**
     * A switched-off metadata service is a configuration choice, not an outage. It must not be DOWN,
     * because this endpoint is meant to be wired to a monitor and that would page someone for a
     * deliberate setting.
     */
    @Test
    void getMdsHealth_ifMetadataServiceDisabled_isDisabledNotDown() {
        configure("monitor", true);
        when(tocService.getTocEntryCount()).thenReturn(0);
        when(tocService.getLoadedTocNextUpdate()).thenReturn(null);

        MdsHealth health = trustStatusService.getMdsHealth();

        assertEquals(MdsHealthStatus.DISABLED, health.getStatus());
        assertTrue(health.isMetadataServiceDisabled());
    }

    /**
     * A failed refresh while the cached blob is still valid is reported, but is not an outage:
     * attestation validation is working normally off the cached metadata, so returning DOWN (and hence
     * 503) would be a false alarm. lastRefreshError is the early-warning signal instead.
     */
    @Test
    void getMdsHealth_ifRefreshFailedButBlobStillValid_reportsErrorAndStaysUp() {
        configureHealthyMds();
        when(tocService.getLastRefreshError()).thenReturn("MDS TOC download failed: Connection timed out");

        MdsHealth health = trustStatusService.getMdsHealth();

        assertEquals(MdsHealthStatus.UP, health.getStatus());
        assertEquals("MDS TOC download failed: Connection timed out", health.getLastRefreshError());
    }

    /** The configured trust anchor is reported as a presence flag; the certificate never leaves the server. */
    @Test
    void getMdsHealth_reportsRootCertPresenceOnly() {
        Fido2Configuration cfg = configureHealthyMds();
        MetadataServer withCert = new MetadataServer();
        withCert.setUrl("https://mds.example.org/");
        withCert.setRootCert("MIIB...base64-DER...");
        MetadataServer withoutCert = new MetadataServer();
        withoutCert.setUrl("https://mds.fidoalliance.org/");
        when(cfg.getMetadataServers()).thenReturn(Arrays.asList(withCert, withoutCert));

        MdsHealth health = trustStatusService.getMdsHealth();

        assertEquals(2, health.getMetadataServers().size());
        MetadataServerStatus first = health.getMetadataServers().get(0);
        assertEquals("https://mds.example.org/", first.getUrl());
        assertTrue(first.isRootCertConfigured());
        assertFalse(health.getMetadataServers().get(1).isRootCertConfigured());
        assertFalse(health.toString().contains("MIIB"), "the certificate must never be exposed");
    }

    @Test
    void getMdsHealth_ifNoMetadataServersConfigured_reportsEmptyList() {
        Fido2Configuration cfg = configureHealthyMds();
        when(cfg.getMetadataServers()).thenReturn(null);

        MdsHealth health = trustStatusService.getMdsHealth();

        assertNotNull(health.getMetadataServers());
        assertTrue(health.getMetadataServers().isEmpty());
    }

    /**
     * The endpoint must not trigger a download or hit the document store. getNextUpdateDate() reads
     * jansDocument and throws DocumentException on failure, so a health check that called it would
     * fail exactly when it is most needed.
     */
    @Test
    void getMdsHealth_doesNotTouchTheDocumentStore() {
        configureHealthyMds();

        trustStatusService.getMdsHealth();

        verify(tocService, never()).getNextUpdateDate();
        verify(tocService, never()).fetchMetadata();
        verify(tocService, never()).refreshTOCEntries();
    }

    @Test
    void getMdsHealth_ifNoFido2Configuration_isDown() {
        when(appConfiguration.getFido2Configuration()).thenReturn(null);

        MdsHealth health = trustStatusService.getMdsHealth();

        assertEquals(MdsHealthStatus.DOWN, health.getStatus());
        assertNotNull(health.getLastRefreshError());
    }

    @Test
    void getMdsHealth_ifNoRefreshHasSucceeded_omitsTheTimestamp() {
        configureHealthyMds();
        when(tocService.getLastSuccessfulRefresh()).thenReturn(null);

        MdsHealth health = trustStatusService.getMdsHealth();

        assertNull(health.getLastSuccessfulRefresh());
    }

    @Test
    void getMdsHealth_reportsConfiguredServerUrl() {
        Fido2Configuration cfg = configureHealthyMds();
        MetadataServer server = new MetadataServer();
        server.setUrl("https://mds.fidoalliance.org/");
        when(cfg.getMetadataServers()).thenReturn(Collections.singletonList(server));

        MdsHealth health = trustStatusService.getMdsHealth();

        assertEquals("https://mds.fidoalliance.org/", health.getMetadataServers().get(0).getUrl());
    }
}
