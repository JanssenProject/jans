/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.trust;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.trust.AttestationTrustConfig;
import io.jans.fido2.service.mds.AttestationCertificateService;
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
    private AttestationCertificateService attestationCertificateService;

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
}
