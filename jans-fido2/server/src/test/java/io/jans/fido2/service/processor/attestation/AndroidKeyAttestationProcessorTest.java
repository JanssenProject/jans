package io.jans.fido2.service.processor.attestation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.CertificateService;
import io.jans.fido2.service.CoseService;
import io.jans.fido2.service.mds.AttestationCertificateService;
import io.jans.fido2.service.verifier.AuthenticatorDataVerifier;
import io.jans.fido2.service.verifier.CertificateVerifier;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERTaggedObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AndroidKeyAttestationProcessorTest {

    private static final String KEY_DESCRIPTION_OID = "1.3.6.1.4.1.11129.2.1.17";
    private static final int TAG_PURPOSE = 1;
    private static final int TAG_ALL_APPLICATIONS = 600;
    private static final int TAG_ORIGIN = 702;
    private static final int KM_ORIGIN_GENERATED = 0;
    private static final int KM_PURPOSE_SIGN = 2;

    private static final byte[] CLIENT_DATA_HASH = "test_clientDataHash".getBytes();

    @InjectMocks
    private AndroidKeyAttestationProcessor processor;

    @Mock
    private Logger log;
    @Mock
    private AppConfiguration appConfiguration;
    @Mock
    private CommonVerifiers commonVerifiers;
    @Mock
    private AuthenticatorDataVerifier authenticatorDataVerifier;
    @Mock
    private CertificateVerifier certificateVerifier;
    @Mock
    private CoseService coseService;
    @Mock
    private Base64Service base64Service;
    @Mock
    private CertificateService certificateService;
    @Mock
    private AttestationCertificateService attestationCertificateService;
    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Test
    void getAttestationFormat_valid_androidKey() {
        assertEquals("android-key", processor.getAttestationFormat().getFmt());
    }

    @Test
    void process_ifNoX5c_rejected() {
        JsonNode attStmt = mock(JsonNode.class);
        when(commonVerifiers.verifyAlgorithm(any(), anyInt())).thenReturn(1);
        when(commonVerifiers.verifyBase64String(any())).thenReturn("sig");
        when(attStmt.hasNonNull("x5c")).thenReturn(false);
        when(errorResponseFactory.badRequestException(any(), anyString()))
                .thenReturn(new WebApplicationException(Response.status(400).entity("android_key_error").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> processor.process(attStmt, mock(AuthData.class), mock(Fido2RegistrationData.class), CLIENT_DATA_HASH, mock(CredAndCounterData.class)));
        assertEquals(400, ex.getResponse().getStatus());
        verify(errorResponseFactory).badRequestException(any(), eq("No certificate in the android-key attestation statement"));
    }

    @Test
    void process_ifX5cEmpty_rejected() {
        JsonNode attStmt = mock(JsonNode.class);
        when(commonVerifiers.verifyAlgorithm(any(), anyInt())).thenReturn(1);
        when(commonVerifiers.verifyBase64String(any())).thenReturn("sig");
        when(attStmt.hasNonNull("x5c")).thenReturn(true);
        JsonNode x5cNode = mock(JsonNode.class);
        when(attStmt.get("x5c")).thenReturn(x5cNode);
        when(x5cNode.elements()).thenReturn(Collections.emptyIterator());
        when(certificateService.getCertificates(anyList())).thenReturn(Collections.emptyList());
        when(errorResponseFactory.badRequestException(any(), anyString()))
                .thenReturn(new WebApplicationException(Response.status(400).entity("android_key_error").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> processor.process(attStmt, mock(AuthData.class), mock(Fido2RegistrationData.class), CLIENT_DATA_HASH, mock(CredAndCounterData.class)));
        assertEquals(400, ex.getResponse().getStatus());
        verify(errorResponseFactory).badRequestException(any(), eq("x5c certificates are empty"));
    }

    @Test
    void process_ifKeyDescriptionExtensionMissing_rejected() {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        X509Certificate credCert = mock(X509Certificate.class);
        stubPathToExtension(attStmt, authData, credCert);
        when(credCert.getExtensionValue(KEY_DESCRIPTION_OID)).thenReturn(null);
        when(errorResponseFactory.badRequestException(any(), anyString()))
                .thenReturn(new WebApplicationException(Response.status(400).entity("android_key_error").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> processor.process(attStmt, authData, mock(Fido2RegistrationData.class), CLIENT_DATA_HASH, mock(CredAndCounterData.class)));
        assertEquals(400, ex.getResponse().getStatus());
        verify(errorResponseFactory).badRequestException(any(), eq("Missing Android key-description extension " + KEY_DESCRIPTION_OID));
    }

    @Test
    void process_ifAttestationChallengeMismatch_rejected() throws IOException {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        X509Certificate credCert = mock(X509Certificate.class);
        stubPathToExtension(attStmt, authData, credCert);
        when(credCert.getExtensionValue(KEY_DESCRIPTION_OID))
                .thenReturn(keyDescriptionExtension("WRONG-challenge".getBytes(), KM_ORIGIN_GENERATED, KM_PURPOSE_SIGN, false));
        when(errorResponseFactory.badRequestException(any(), anyString()))
                .thenReturn(new WebApplicationException(Response.status(400).entity("android_key_error").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> processor.process(attStmt, authData, mock(Fido2RegistrationData.class), CLIENT_DATA_HASH, mock(CredAndCounterData.class)));
        assertEquals(400, ex.getResponse().getStatus());
        verify(errorResponseFactory).badRequestException(any(), eq("Attestation challenge in key-description extension does not match clientDataHash"));
    }

    @Test
    void process_ifAllApplicationsPresent_rejected() throws IOException {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        X509Certificate credCert = mock(X509Certificate.class);
        stubPathToExtension(attStmt, authData, credCert);
        when(credCert.getExtensionValue(KEY_DESCRIPTION_OID))
                .thenReturn(keyDescriptionExtension(CLIENT_DATA_HASH, KM_ORIGIN_GENERATED, KM_PURPOSE_SIGN, true));
        when(errorResponseFactory.badRequestException(any(), anyString()))
                .thenReturn(new WebApplicationException(Response.status(400).entity("android_key_error").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> processor.process(attStmt, authData, mock(Fido2RegistrationData.class), CLIENT_DATA_HASH, mock(CredAndCounterData.class)));
        assertEquals(400, ex.getResponse().getStatus());
        verify(errorResponseFactory).badRequestException(any(), eq("allApplications must not be present in the attested key authorization list"));
    }

    @Test
    void process_ifOriginNotGenerated_rejected() throws IOException {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        X509Certificate credCert = mock(X509Certificate.class);
        stubPathToExtension(attStmt, authData, credCert);
        when(credCert.getExtensionValue(KEY_DESCRIPTION_OID))
                .thenReturn(keyDescriptionExtension(CLIENT_DATA_HASH, 1 /* not GENERATED */, KM_PURPOSE_SIGN, false));
        when(errorResponseFactory.badRequestException(any(), anyString()))
                .thenReturn(new WebApplicationException(Response.status(400).entity("android_key_error").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> processor.process(attStmt, authData, mock(Fido2RegistrationData.class), CLIENT_DATA_HASH, mock(CredAndCounterData.class)));
        assertEquals(400, ex.getResponse().getStatus());
        verify(errorResponseFactory).badRequestException(any(), eq("Attested key origin is not KM_ORIGIN_GENERATED"));
    }

    @Test
    void process_ifPurposeMissingSign_rejected() throws IOException {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        X509Certificate credCert = mock(X509Certificate.class);
        stubPathToExtension(attStmt, authData, credCert);
        when(credCert.getExtensionValue(KEY_DESCRIPTION_OID))
                .thenReturn(keyDescriptionExtension(CLIENT_DATA_HASH, KM_ORIGIN_GENERATED, 0 /* not SIGN */, false));
        when(errorResponseFactory.badRequestException(any(), anyString()))
                .thenReturn(new WebApplicationException(Response.status(400).entity("android_key_error").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> processor.process(attStmt, authData, mock(Fido2RegistrationData.class), CLIENT_DATA_HASH, mock(CredAndCounterData.class)));
        assertEquals(400, ex.getResponse().getStatus());
        verify(errorResponseFactory).badRequestException(any(), eq("Attested key purpose does not include KM_PURPOSE_SIGN"));
    }

    @Test
    void process_validVector_success() throws IOException {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        X509Certificate credCert = mock(X509Certificate.class);
        CredAndCounterData credIdAndCounters = mock(CredAndCounterData.class);
        stubPathToExtension(attStmt, authData, credCert);
        when(credCert.getExtensionValue(KEY_DESCRIPTION_OID))
                .thenReturn(keyDescriptionExtension(CLIENT_DATA_HASH, KM_ORIGIN_GENERATED, KM_PURPOSE_SIGN, false));

        // Attestation enforcement disabled → skip the metadata chain step.
        Fido2Configuration fido2Config = mock(Fido2Configuration.class);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Config);
        when(fido2Config.getAttestationMode()).thenReturn("disabled");
        when(authData.getCredId()).thenReturn("cred-id".getBytes());
        when(base64Service.urlEncodeToString(any())).thenReturn("encoded");

        processor.process(attStmt, authData, mock(Fido2RegistrationData.class), CLIENT_DATA_HASH, credIdAndCounters);

        verify(credIdAndCounters).setAttestationType("android-key");
        verify(credIdAndCounters).setSignatureAlgorithm(1);
    }

    /** Wires the collaborators so process() reaches verifyKeyDescription with a matching public key. */
    private void stubPathToExtension(JsonNode attStmt, AuthData authData, X509Certificate credCert) {
        when(commonVerifiers.verifyAlgorithm(any(), anyInt())).thenReturn(1);
        when(commonVerifiers.verifyBase64String(any())).thenReturn("sig");
        when(attStmt.hasNonNull("x5c")).thenReturn(true);
        JsonNode x5cNode = mock(JsonNode.class);
        when(attStmt.get("x5c")).thenReturn(x5cNode);
        when(x5cNode.elements()).thenReturn(Collections.singletonList((JsonNode) new TextNode("cert")).iterator());
        when(certificateService.getCertificates(anyList())).thenReturn(List.of(credCert));
        when(authData.getAuthDataDecoded()).thenReturn("authData".getBytes());
        when(authData.getCosePublicKey()).thenReturn("cose".getBytes());
        PublicKey pk = mock(PublicKey.class);
        when(coseService.getPublicKeyFromUncompressedECPoint(any())).thenReturn(pk);
        when(credCert.getPublicKey()).thenReturn(pk); // same instance → credential/cert key match passes
    }

    /**
     * Builds a DER-encoded Android key-description extension value (as returned by
     * {@code X509Certificate.getExtensionValue}) with the given attestation challenge and a single
     * teeEnforced AuthorizationList carrying origin, purpose and (optionally) allApplications.
     */
    private byte[] keyDescriptionExtension(byte[] challenge, int origin, int purpose, boolean allApplications) throws IOException {
        ASN1EncodableVector tee = new ASN1EncodableVector();
        if (allApplications) {
            tee.add(new DERTaggedObject(true, TAG_ALL_APPLICATIONS, DERNull.INSTANCE));
        }
        tee.add(new DERTaggedObject(true, TAG_PURPOSE, new DERSet(new ASN1Integer(purpose))));
        tee.add(new DERTaggedObject(true, TAG_ORIGIN, new ASN1Integer(origin)));
        DERSequence teeEnforced = new DERSequence(tee);

        ASN1EncodableVector kd = new ASN1EncodableVector();
        kd.add(new ASN1Integer(3));                 // [0] attestationVersion
        kd.add(new ASN1Integer(0));                 // [1] attestationSecurityLevel
        kd.add(new ASN1Integer(3));                 // [2] keymasterVersion
        kd.add(new ASN1Integer(0));                 // [3] keymasterSecurityLevel
        kd.add(new DEROctetString(challenge));      // [4] attestationChallenge
        kd.add(new DEROctetString(new byte[0]));    // [5] uniqueId
        kd.add(new DERSequence());                  // [6] softwareEnforced (empty)
        kd.add(teeEnforced);                        // [7] teeEnforced
        byte[] inner = new DERSequence(kd).getEncoded();
        return new DEROctetString(inner).getEncoded();
    }
}
