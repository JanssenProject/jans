package io.jans.fido2.service.mds;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.service.CertificateService;
import io.jans.fido2.service.DataMapperService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import javax.security.auth.x500.X500Principal;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttestationCertificateServiceTest {

    @InjectMocks
    private AttestationCertificateService attestationCertificateService;

    @Mock
    private Logger log;
    @Mock
    private AppConfiguration appConfiguration;
    @Mock
    private CertificateService certificateService;
    @Mock
    private MdsService mdsService;
    @Mock
    private DataMapperService dataMapperService;

    private AuthData authDataWithAaguid() {
        AuthData authData = mock(AuthData.class);
        when(authData.getAaguid()).thenReturn(new byte[]{1, 2, 3, 4});
        return authData;
    }

    private void configureMode(String mode) {
        Fido2Configuration cfg = mock(Fido2Configuration.class);
        when(cfg.isEnterpriseAttestation()).thenReturn(false);
        when(cfg.isDisableMetadataService()).thenReturn(false);
        when(cfg.getAttestationMode()).thenReturn(mode);
        when(appConfiguration.getFido2Configuration()).thenReturn(cfg);
    }

    @Test
    void getAttestationRootCertificates_enforcedAndMetadataFetchFails_rejects() {
        configureMode("enforced");
        AuthData authData = authDataWithAaguid();
        List<X509Certificate> certs = Collections.singletonList(mock(X509Certificate.class));
        when(mdsService.fetchMetadata(any())).thenThrow(new Fido2RuntimeException("MDS unavailable"));

        // CONF-22: enforced mode must surface the metadata-fetch failure rather than swallow it.
        assertThrows(Fido2RuntimeException.class,
                () -> attestationCertificateService.getAttestationRootCertificates(authData, certs));
    }

    @Test
    void getAttestationRootCertificates_monitorAndMetadataFetchFails_fallsBack() {
        configureMode("monitor");
        AuthData authData = authDataWithAaguid();
        List<X509Certificate> certs = Collections.singletonList(mock(X509Certificate.class));
        when(mdsService.fetchMetadata(any())).thenThrow(new Fido2RuntimeException("MDS unavailable"));
        when(dataMapperService.createObjectNode()).thenReturn(new ObjectMapper().createObjectNode());
        when(certificateService.selectRootCertificates(any(), any())).thenReturn(certs);

        // Regression guard: monitor/disabled keep the previous lenient behavior — fall back, do not throw.
        assertDoesNotThrow(() -> attestationCertificateService.getAttestationRootCertificates(authData, certs));
    }

    // #14602 — Apple root CA presence surfaced by the trust/attestation/config endpoint.

    private void configureAuthenticatorCertsFolder() {
        Fido2Configuration cfg = mock(Fido2Configuration.class);
        when(cfg.getAuthenticatorCertsFolder()).thenReturn("/etc/jans/conf/fido2/authenticator_cert");
        when(appConfiguration.getFido2Configuration()).thenReturn(cfg);
    }

    @Test
    void isAppleRootCaPresent_ifCertificateLoaded_returnsTrue() {
        configureAuthenticatorCertsFolder();
        X509Certificate appleCert = mock(X509Certificate.class);
        when(appleCert.getSubjectX500Principal()).thenReturn(new X500Principal("CN=Apple WebAuthn Root CA"));
        when(certificateService.getCertificateByDisplayName("Apple_WebAuthn_Root_CA.pem")).thenReturn(appleCert);

        attestationCertificateService.init(new Object());

        assertTrue(attestationCertificateService.isAppleRootCaPresent());
    }

    @Test
    void isAppleRootCaPresent_ifCertificateMissing_returnsFalse() {
        configureAuthenticatorCertsFolder();
        when(certificateService.getCertificateByDisplayName("Apple_WebAuthn_Root_CA.pem")).thenReturn(null);

        attestationCertificateService.init(new Object());

        // Today this condition is only a startup log warning; the endpoint has to be able to report it.
        assertFalse(attestationCertificateService.isAppleRootCaPresent());
    }

    @Test
    void isAppleRootCaPresent_ifFido2ConfigurationMissing_returnsFalse() {
        when(appConfiguration.getFido2Configuration()).thenReturn(null);

        attestationCertificateService.init(new Object());

        assertFalse(attestationCertificateService.isAppleRootCaPresent());
    }
}
