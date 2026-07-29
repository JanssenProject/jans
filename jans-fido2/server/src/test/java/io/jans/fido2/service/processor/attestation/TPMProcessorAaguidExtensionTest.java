package io.jans.fido2.service.processor.attestation;

import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.error.ErrorResponseFactory;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.bouncycastle.asn1.DEROctetString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import java.io.IOException;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the id-fido-gen-ce-aaguid extension check in {@link TPMProcessor#verifyTPMCertificateExtenstion}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TPMProcessorAaguidExtensionTest {

    private static final String FIDO_AAGUID_OID = "1.3.6.1.4.1.45724.1.1.4";

    @InjectMocks
    private TPMProcessor tpmProcessor;

    @Mock
    private Logger log;
    @Mock
    private ErrorResponseFactory errorResponseFactory;

    private byte[] aaguidExtension(byte[] aaguid) throws IOException {
        // An X.509 extension value is a DER OCTET STRING wrapping the extnValue, which for
        // id-fido-gen-ce-aaguid is itself an OCTET STRING containing the raw AAGUID bytes.
        return new DEROctetString(new DEROctetString(aaguid).getEncoded()).getEncoded();
    }

    @Test
    void verifyTPMCertificateExtenstion_ifAaguidMatches_valid() throws IOException {
        X509Certificate aikCertificate = mock(X509Certificate.class);
        AuthData authData = mock(AuthData.class);
        byte[] aaguid = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        when(aikCertificate.getExtensionValue(FIDO_AAGUID_OID)).thenReturn(aaguidExtension(aaguid));
        when(authData.getAaguid()).thenReturn(aaguid);

        assertDoesNotThrow(() -> tpmProcessor.verifyTPMCertificateExtenstion(aikCertificate, authData));
    }

    @Test
    void verifyTPMCertificateExtenstion_ifAaguidMismatch_rejected() throws IOException {
        X509Certificate aikCertificate = mock(X509Certificate.class);
        AuthData authData = mock(AuthData.class);
        byte[] certAaguid = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        byte[] authAaguid = {16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
        when(aikCertificate.getExtensionValue(FIDO_AAGUID_OID)).thenReturn(aaguidExtension(certAaguid));
        when(authData.getAaguid()).thenReturn(authAaguid);
        when(errorResponseFactory.badRequestException(any(), anyString()))
                .thenReturn(new WebApplicationException(Response.status(400).entity("tpm_error").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> tpmProcessor.verifyTPMCertificateExtenstion(aikCertificate, authData));
        assertEquals(400, ex.getResponse().getStatus());
    }

    @Test
    void verifyTPMCertificateExtenstion_ifExtensionAbsent_valid() {
        X509Certificate aikCertificate = mock(X509Certificate.class);
        AuthData authData = mock(AuthData.class);
        when(aikCertificate.getExtensionValue(FIDO_AAGUID_OID)).thenReturn(null);

        assertDoesNotThrow(() -> tpmProcessor.verifyTPMCertificateExtenstion(aikCertificate, authData));
    }
}
