package io.jans.fido2.service.processor.attestation;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.fido2.androind.AndroidKeyUtils;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.CertificateService;
import io.jans.fido2.service.mds.AttestationCertificateService;
import io.jans.fido2.service.verifier.AuthenticatorDataVerifier;
import io.jans.fido2.service.verifier.CertificateVerifier;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.security.cert.X509Certificate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AndroidKeyAttestationProcessorTest {

    @InjectMocks
    private AndroidKeyAttestationProcessor androidKeyAttestationProcessor;

    @Mock
    private Logger log;

    @Mock
    private CommonVerifiers commonVerifiers;

    @Mock
    private AuthenticatorDataVerifier authenticatorDataVerifier;

    @Mock
    private CertificateService certificateService;

    @Mock
    private CertificateVerifier certificateVerifier;

    @Mock
    private AndroidKeyUtils androidKeyUtils;

    @Mock
    private AttestationCertificateService attestationCertificateService;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Test
    void process_ifClientDataHashNotEqualsToAttestationChallenge_badRequestException() throws Exception {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        Fido2RegistrationData credential = mock(Fido2RegistrationData.class);
        byte[] clientDataHash = "test-clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = mock(CredAndCounterData.class);

        JsonNode x5cNode = mock(JsonNode.class);
        when(attStmt.get("x5c")).thenReturn(x5cNode);
        when(x5cNode.elements()).thenReturn(Collections.emptyIterator());
        when(certificateVerifier.verifyAttestationCertificates(any(), any())).thenReturn(mock(X509Certificate.class));
        ASN1Sequence extensionData = mock(ASN1Sequence.class);
        when(androidKeyUtils.extractAttestationSequence(any())).thenReturn(extensionData);
        ASN1Integer asn1Integer = new ASN1Integer(1L);
        when(extensionData.getObjectAt(AndroidKeyUtils.ATTESTATION_VERSION_INDEX)).thenReturn(asn1Integer);
        when(extensionData.getObjectAt(AndroidKeyUtils.ATTESTATION_SECURITY_LEVEL_INDEX)).thenReturn(asn1Integer);
        when(extensionData.getObjectAt(AndroidKeyUtils.KEYMASTER_SECURITY_LEVEL_INDEX)).thenReturn(asn1Integer);
        ASN1OctetString asn1OctetString = mock(ASN1OctetString.class);
        when(extensionData.getObjectAt(AndroidKeyUtils.ATTESTATION_CHALLENGE_INDEX)).thenReturn(asn1OctetString);
        when(asn1OctetString.getOctets()).thenReturn("test-octets".getBytes());
        when(errorResponseFactory.badRequestException(any(), any())).thenThrow(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException res = assertThrows(WebApplicationException.class, () -> androidKeyAttestationProcessor.process(attStmt, authData, credential, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(log).debug(eq("Android-key payload"));
        verify(log).warn(contains("Problem with android key"), anyString());
        verify(errorResponseFactory, times(2)).badRequestException(any(), any());
        verifyNoInteractions(commonVerifiers, authenticatorDataVerifier);
    }

    @Test
    void process_ifCertificateServiceThrowException_badRequestException() throws Exception {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        Fido2RegistrationData credential = mock(Fido2RegistrationData.class);
        byte[] clientDataHash = "test-clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = mock(CredAndCounterData.class);

        JsonNode x5cNode = mock(JsonNode.class);
        when(attStmt.get("x5c")).thenReturn(x5cNode);
        when(x5cNode.elements()).thenReturn(Collections.emptyIterator());
        when(androidKeyUtils.extractAttestationSequence(any())).thenThrow(new Exception("test exception"));
        when(errorResponseFactory.badRequestException(any(), any())).thenThrow(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException res = assertThrows(WebApplicationException.class, () -> androidKeyAttestationProcessor.process(attStmt, authData, credential, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(log).debug(eq("Android-key payload"));
        verify(log).warn(contains("Problem with android key"), anyString());
        verify(errorResponseFactory).badRequestException(any(), any());
        verifyNoInteractions(commonVerifiers, authenticatorDataVerifier);
        verifyNoMoreInteractions(errorResponseFactory);
    }
}
