package io.jans.fido2.service.mds;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.conf.MetadataServer;
import io.jans.fido2.service.CertificateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
}
