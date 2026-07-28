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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    private TocService tocService;
    @Mock
    private AttestationCertificateService attestationCertificateService;

    private Fido2Configuration configure(String attestationMode, boolean metadataServiceDisabled) {
        Fido2Configuration cfg = mock(Fido2Configuration.class);
        when(cfg.getAttestationMode()).thenReturn(attestationMode);
        when(cfg.isDisableMetadataService()).thenReturn(metadataServiceDisabled);
        when(appConfiguration.getFido2Configuration()).thenReturn(cfg);
        return cfg;
    }

    private void configureLoadedMetadata(LocalDate nextUpdate, int entryCount, String refreshError) {
        when(tocService.getLoadedTocNextUpdate()).thenReturn(nextUpdate);
        when(tocService.getTocEntryCount()).thenReturn(entryCount);
        when(tocService.getLastRefreshError()).thenReturn(refreshError);
        when(tocService.getLastSuccessfulRefresh()).thenReturn(LocalDateTime.of(2026, 7, 27, 4, 15, 22));
    }

    // ---------- attestation trust config ----------

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

    // ---------- MDS health ----------

    @Test
    void getMdsHealth_ifMetadataServiceDisabled_reportsDisabledNotDown() {
        configure("monitor", true);

        MdsHealth health = trustStatusService.getMdsHealth();

        assertEquals(MdsHealthStatus.DISABLED, health.getStatus());
        assertTrue(health.isMetadataServiceDisabled());
        assertNotNull(health.getTimestamp());
    }

    @Test
    void getMdsHealth_ifBlobLoadedAndValid_reportsUp() {
        configure("monitor", false);
        configureLoadedMetadata(LocalDate.now().plusDays(30), 1284, null);

        MdsHealth health = trustStatusService.getMdsHealth();

        assertEquals(MdsHealthStatus.UP, health.getStatus());
        assertEquals(1284, health.getTocEntryCount());
        assertFalse(health.isBlobExpired());
        assertNotNull(health.getLastSuccessfulRefresh());
        assertNull(health.getLastRefreshError());
    }

    @Test
    void getMdsHealth_ifLastRefreshFailed_reportsDown() {
        configure("monitor", false);
        configureLoadedMetadata(LocalDate.now().plusDays(30), 1284, "Can't parse MDS TOC document: boom");

        MdsHealth health = trustStatusService.getMdsHealth();

        assertEquals(MdsHealthStatus.DOWN, health.getStatus());
        assertNotNull(health.getLastRefreshError());
    }

    @Test
    void getMdsHealth_ifBlobExpired_reportsDown() {
        configure("monitor", false);
        configureLoadedMetadata(LocalDate.now().minusDays(1), 1284, null);

        MdsHealth health = trustStatusService.getMdsHealth();

        assertEquals(MdsHealthStatus.DOWN, health.getStatus());
        assertTrue(health.isBlobExpired());
    }

    @Test
    void getMdsHealth_ifNoBlobLoaded_reportsDownAndExpired() {
        configure("monitor", false);
        configureLoadedMetadata(null, 0, null);

        MdsHealth health = trustStatusService.getMdsHealth();

        assertEquals(MdsHealthStatus.DOWN, health.getStatus());
        assertTrue(health.isBlobExpired());
        assertNull(health.getNextUpdate());
    }

    @Test
    void getMdsHealth_metadataServers_reportRootCertPresenceOnly() {
        Fido2Configuration cfg = configure("monitor", false);
        MetadataServer server = new MetadataServer();
        server.setUrl("https://mds.fidoalliance.org/");
        server.setRootCert("BASE64-DER-CERT");
        when(cfg.getMetadataServers()).thenReturn(Collections.singletonList(server));
        configureLoadedMetadata(LocalDate.now().plusDays(30), 10, null);

        MdsHealth health = trustStatusService.getMdsHealth();

        assertEquals(1, health.getMetadataServers().size());
        assertEquals("https://mds.fidoalliance.org/", health.getMetadataServers().get(0).getUrl());
        // The certificate itself must never leave the server.
        assertTrue(health.getMetadataServers().get(0).isRootCertConfigured());
        assertFalse(health.toString().contains("BASE64-DER-CERT"));
    }
}
