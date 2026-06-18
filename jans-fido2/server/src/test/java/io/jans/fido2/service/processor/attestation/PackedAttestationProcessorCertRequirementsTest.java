package io.jans.fido2.service.processor.attestation;

import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.util.security.SecurityProviderUtility;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the packed attestation-certificate requirement checks in
 * {@link PackedAttestationProcessor#verifyPackedAttestationCertRequirements}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PackedAttestationProcessorCertRequirementsTest {

    private static final String VALID_SUBJECT =
            "C=US,O=Acme,OU=Authenticator Attestation,CN=Acme Packed Attestation";

    @InjectMocks
    private PackedAttestationProcessor packedAttestationProcessor;

    @Mock
    private Logger log;
    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @BeforeAll
    static void beforeAll() {
        SecurityProviderUtility.installBCProvider();
    }

    private void stubError() {
        when(errorResponseFactory.badRequestException(any(), anyString()))
                .thenReturn(new WebApplicationException(Response.status(400).entity("packed_error").build()));
    }

    private X509Certificate buildCertificate(String subjectDn, boolean isCa) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = kpg.generateKeyPair();
        X500Name subject = new X500Name(subjectDn);
        Date notBefore = new Date(System.currentTimeMillis() - 3600_000L);
        Date notAfter = new Date(System.currentTimeMillis() + 3600_000L);
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                subject, BigInteger.ONE, notBefore, notAfter, subject, keyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(isCa));
        String bc = SecurityProviderUtility.getBCProviderName();
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").setProvider(bc).build(keyPair.getPrivate());
        return new JcaX509CertificateConverter().setProvider(bc).getCertificate(builder.build(signer));
    }

    @Test
    void verifyPackedAttestationCertRequirements_validCertificate_valid() throws Exception {
        X509Certificate cert = buildCertificate(VALID_SUBJECT, false);
        assertDoesNotThrow(() -> packedAttestationProcessor.verifyPackedAttestationCertRequirements(cert));
    }

    @Test
    void verifyPackedAttestationCertRequirements_ifNotVersion3_rejected() {
        X509Certificate cert = mock(X509Certificate.class);
        when(cert.getVersion()).thenReturn(1);
        stubError();

        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> packedAttestationProcessor.verifyPackedAttestationCertRequirements(cert));
        assertEquals(400, ex.getResponse().getStatus());
    }

    @Test
    void verifyPackedAttestationCertRequirements_ifCertificateIsCa_rejected() throws Exception {
        X509Certificate cert = buildCertificate(VALID_SUBJECT, true);
        stubError();

        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> packedAttestationProcessor.verifyPackedAttestationCertRequirements(cert));
        assertEquals(400, ex.getResponse().getStatus());
    }

    @Test
    void verifyPackedAttestationCertRequirements_ifWrongOrganisationUnit_rejected() throws Exception {
        X509Certificate cert = buildCertificate("C=US,O=Acme,OU=Wrong,CN=Acme", false);
        stubError();

        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> packedAttestationProcessor.verifyPackedAttestationCertRequirements(cert));
        assertEquals(400, ex.getResponse().getStatus());
    }

    @Test
    void verifyPackedAttestationCertRequirements_ifInvalidCountry_rejected() throws Exception {
        X509Certificate cert = buildCertificate("C=ZZ,O=Acme,OU=Authenticator Attestation,CN=Acme", false);
        stubError();

        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> packedAttestationProcessor.verifyPackedAttestationCertRequirements(cert));
        assertEquals(400, ex.getResponse().getStatus());
    }
}
