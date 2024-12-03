package io.jans.fido2.service.processor.attestation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.AttestationMode;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.Base64Service;
import io.jans.fido2.service.CertificateService;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.mds.AttestationCertificateService;
import io.jans.fido2.service.mds.LocalMdsService;
import io.jans.fido2.service.mds.MdsService;
import io.jans.fido2.service.verifier.CertificateVerifier;
import io.jans.fido2.service.verifier.CommonVerifiers;
import io.jans.fido2.service.verifier.SignatureVerifier;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import tss.tpm.TPMS_ATTEST;
import tss.tpm.TPMT_PUBLIC;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TPMProcessorTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    private TPMProcessor tpmProcessor;

    @Mock
    private Logger log;

    @Mock
    private CertificateService certificateService;

    @Mock
    private CommonVerifiers commonVerifiers;

    @Mock
    private AttestationCertificateService attestationCertificateService;

    @Mock
    private SignatureVerifier signatureVerifier;

    @Mock
    private CertificateVerifier certificateVerifier;

    @Mock
    private DataMapperService dataMapperService;

    @Mock
    private Base64Service base64Service;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private Fido2Configuration fido2Configuration;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @InjectMocks
    private AttestationCertificateService attestationCertificateServices;

    @Mock
    private LocalMdsService localMdsService;

    @Mock
    private MdsService mdsService;

    @Test
    void getAttestationFormat_valid_tpm() {
        String fmt = tpmProcessor.getAttestationFormat().getFmt();
        assertNotNull(fmt);
        assertEquals(fmt, "tpm");
    }

    @Test
    void process_ifCborReadTreeThrowError_fido2RuntimeException() throws IOException {
    /*    ObjectNode attStmt = mapper.createObjectNode();
        AuthData authData = new AuthData();
        authData.setCosePublicKey("test-cosePublicKey".getBytes());
        Fido2RegistrationData registration = new Fido2RegistrationData();
        byte[] clientDataHash = "test-clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = new CredAndCounterData();
        when(dataMapperService.cborReadTree(any())).thenThrow(new IOException("test IOException"));
        when(errorResponseFactory.badRequestException(any(), any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException res = assertThrows(WebApplicationException.class, () -> tpmProcessor.process(attStmt, authData, registration, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(dataMapperService).cborReadTree(any(byte[].class));
        verify(errorResponseFactory).badRequestException(any(), eq("Problem with TPM attestation: test IOException"));
        verifyNoInteractions(base64Service, certificateService, attestationCertificateService, certificateVerifier, appConfiguration, log, commonVerifiers, signatureVerifier);*/
    }

    @Test
    void process_ifX5cIsEmpty_badRequestException() throws IOException {
   /*     ObjectNode attStmt = mapper.createObjectNode();
        ArrayNode x5cArray = mapper.createArrayNode();
        attStmt.set("x5c", x5cArray);
        attStmt.put("pubArea", "test-pubArea");
        attStmt.put("certInfo", "test-certInfo");
        attStmt.put("ver", "2.0");
        attStmt.put("alg", -256);
        AuthData authData = new AuthData();
        authData.setCosePublicKey("test-cosePublicKey".getBytes());
        authData.setAttestationBuffer("test-attestationBuffer".getBytes());
        Fido2RegistrationData registration = new Fido2RegistrationData();
        byte[] clientDataHash = "test-clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = new CredAndCounterData();
        ObjectNode cborPublicKey = mapper.createObjectNode();
        cborPublicKey.put("-1", "test-PublicKey");
        when(dataMapperService.cborReadTree(any())).thenReturn(cborPublicKey);
        MessageDigest messageDigest = mock(MessageDigest.class);
        when(signatureVerifier.getDigest(-256)).thenReturn(messageDigest);
        when(messageDigest.digest()).thenReturn("test-hashedBuffer".getBytes());
        when(errorResponseFactory.badRequestException(any(), any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        when(fido2Configuration.getAttestationMode()).thenReturn(AttestationMode.MONITOR.getValue());

        WebApplicationException res = assertThrows(WebApplicationException.class, () -> tpmProcessor.process(attStmt, authData, registration, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(dataMapperService).cborReadTree(any(byte[].class));
        verify(base64Service).decode(any(String.class));

        verify(appConfiguration).getFido2Configuration();
        verifyNoInteractions(certificateService, attestationCertificateService, certificateVerifier, commonVerifiers);*/
    }

    @Test
    void process_ifX5cAndVerifyAttestationCertificatesThrowError_badRequestException() throws IOException {
       /* ObjectNode attStmt = mapper.createObjectNode();
        ArrayNode x5cArray = mapper.createArrayNode();
        x5cArray.add("certPath1");
        attStmt.set("x5c", x5cArray);
        attStmt.put("pubArea", "test-pubArea");
        attStmt.put("certInfo", "test-certInfo");
        attStmt.put("ver", "2.0");
        attStmt.put("alg", -256);
        AuthData authData = new AuthData();
        authData.setCosePublicKey("test-cosePublicKey".getBytes());
        authData.setAttestationBuffer("test-attestationBuffer".getBytes());
        Fido2RegistrationData registration = new Fido2RegistrationData();
        byte[] clientDataHash = "test-clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = new CredAndCounterData();
        ObjectNode cborPublicKey = mapper.createObjectNode();
        cborPublicKey.put("-1", "test-PublicKey");
        when(dataMapperService.cborReadTree(any())).thenReturn(cborPublicKey);
        MessageDigest messageDigest = mock(MessageDigest.class);
        when(signatureVerifier.getDigest(-256)).thenReturn(messageDigest);
        when(messageDigest.digest()).thenReturn("test-hashedBuffer".getBytes());
        List<X509Certificate> aikCertificates = Collections.singletonList(mock(X509Certificate.class));
        when(certificateService.getCertificates(anyList())).thenReturn(Collections.emptyList());
        when(certificateService.getCertificates(anyList())).thenReturn(aikCertificates);
        when(certificateVerifier.verifyAttestationCertificates(anyList(), anyList())).thenThrow(new WebApplicationException(Response.status(400).entity("test exception").build()));
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        when(fido2Configuration.getAttestationMode()).thenReturn(AttestationMode.MONITOR.getValue());

        WebApplicationException res = assertThrows(WebApplicationException.class, () -> tpmProcessor.process(attStmt, authData, registration, clientDataHash, credIdAndCounters));
        assertNotNull(res);
        assertNotNull(res.getResponse());
        assertEquals(res.getResponse().getStatus(), 400);
        assertEquals(res.getResponse().getEntity(), "test exception");

        verify(log, never()).warn("SkipValidateMdsInAttestation is enabled");
        verify(dataMapperService).cborReadTree(any(byte[].class));
        verify(base64Service).decode(any(String.class));
        verify(attestationCertificateService).getAttestationRootCertificates(authData, aikCertificates);
        verify(certificateVerifier).verifyAttestationCertificates(any(), any());
        verifyNoInteractions(commonVerifiers);*/
    }

    @Test
    void process_ifX5cAndVerifyAttestationCertificatesIsValid_success() throws IOException {
      /*  ObjectNode attStmt = mapper.createObjectNode();
        ArrayNode x5cArray = mapper.createArrayNode();
        x5cArray.add("certPath1");
        attStmt.set("x5c", x5cArray);
        String pubArea = "AAEACwAGBHIAIJ3/y/NsODrmmfuYaNxty4nXFTiEvigDkiwSQVi/rSKuABAAEAgAAAAAAAEAss+GHGDpvFEbV+MsBvJsXWTC4MKkyZoOFYCM2EF05SNlFZs4PMQWX1b13Rg0jz77aH3sMO2YmqOSmU00l6/yRabVSiRoAtmRl5pY3HJ+WRsjl//zaJmeHi3EWxUFPA7xAE+qecX7s4HW6aDDQZZgFAfSh95exV1CStYT3s9YvBg/PT3C6355hfK2TAdMqTGXvKRolqmQ8+hO8qMg9b73MXLneMEAp0d2vjufcH8nVvtcu72z9cke7yqmsKRuWg8BpV0r36Ji2UvzPElcdzylAm1n2oGn/POdkf8bQcCI48oc5QRAUoDiSOTuXlybUF0iIi/jOUFfhGnTB6vkwRNZ3w==";
        String certInfo = "/1RDR4AXACIACxHmjtRNtTcuFCluL4Ssx4OYdRiBkh4w/CKgb4tzx5RTACAzhxi3W0HuExVoYbtvYBWeg7Bli9xEDJvw2AMqf60mywAAAAFHcBdIVWl7S8aFYKUBc375jTRWVfsAIgALzYHYUq0K55IskuzIfukQ/H/o1LOOjz7EoGnTf6Yy8toAIgALfXLmQ1rhTAPBOQeQbAQQYPqvbON0RO/9OtVFOrp7UV4=";
        attStmt.put("pubArea", pubArea);
        attStmt.put("certInfo", certInfo);
        attStmt.put("ver", "2.0");
        attStmt.put("alg", -256);
        AuthData authData = new AuthData();
        authData.setCosePublicKey("test-cosePublicKey".getBytes());
        authData.setAttestationBuffer("test-attestationBuffer".getBytes());
        Fido2RegistrationData registration = new Fido2RegistrationData();
        byte[] clientDataHash = "test-clientDataHash".getBytes();
        CredAndCounterData credIdAndCounters = new CredAndCounterData();
        byte[] certInfoBuffer = Base64.getDecoder().decode(certInfo);
        byte[] pubAreaBuffer = Base64.getDecoder().decode(pubArea);
        TPMS_ATTEST tpmsAttest = TPMS_ATTEST.fromTpm(certInfoBuffer);
        TPMT_PUBLIC tpmtPublic = TPMT_PUBLIC.fromTpm(pubAreaBuffer);
        ObjectNode cborPublicKey = mapper.createObjectNode();
        cborPublicKey.put("-1", "test-PublicKey");
        when(dataMapperService.cborReadTree(any())).thenReturn(cborPublicKey);
        MessageDigest messageDigest = mock(MessageDigest.class);
        when(signatureVerifier.getDigest(-256)).thenReturn(messageDigest);
        when(messageDigest.digest()).thenReturn(tpmsAttest.extraData);
        List<X509Certificate> aikCertificates = Collections.singletonList(mock(X509Certificate.class));
        when(certificateService.getCertificates(anyList())).thenReturn(Collections.emptyList());
        when(certificateService.getCertificates(anyList())).thenReturn(aikCertificates);
        X509Certificate verifiedCert = mock(X509Certificate.class);
        when(certificateVerifier.verifyAttestationCertificates(anyList(), anyList())).thenReturn(verifiedCert);
        when(commonVerifiers.verifyBase64String(any())).thenReturn("test-signature");
        when(base64Service.decode(any(String.class))).thenReturn(Arrays.copyOfRange(tpmtPublic.unique.toTpm(), 2, tpmtPublic.unique.toTpm().length), certInfoBuffer, pubAreaBuffer);
        when(commonVerifiers.tpmParseToPublic(any())).thenReturn(tpmtPublic);
        when(commonVerifiers.tpmParseToAttest(any())).thenReturn(tpmsAttest);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        when(fido2Configuration.getAttestationMode()).thenReturn(AttestationMode.MONITOR.getValue());

        tpmProcessor.process(attStmt, authData, registration, clientDataHash, credIdAndCounters);
        verify(dataMapperService).cborReadTree(any(byte[].class));
        verify(base64Service, times(3)).decode(anyString());
        verify(certificateService, times(2)).getCertificates(anyList());
        verify(attestationCertificateService).getAttestationRootCertificates(any(AuthData.class), anyList());
        verify(log).trace("TPM attStmt 'alg': {}", -256);
        verify(base64Service, times(2)).urlEncodeToString(any());
        verifyNoMoreInteractions(log);*/
    }

    @Test
    void getAttestationRootCertificates_enterpriseAttestationEnabled() {
       /* String aaguid = "test-aaguid";
        AuthData authData = mock(AuthData.class);
        when(authData.getAaguid()).thenReturn(aaguid.getBytes(StandardCharsets.UTF_8));

        List<X509Certificate> attestationCertificates = Collections.singletonList(mock(X509Certificate.class));

        Fido2Configuration fido2Config = mock(Fido2Configuration.class);
        when(fido2Config.isEnterpriseAttestation()).thenReturn(true);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Config);

        String hexAaguid = Hex.encodeHexString(aaguid.getBytes(StandardCharsets.UTF_8));
        JsonNode metadata = mock(JsonNode.class);
        when(localMdsService.getAuthenticatorsMetadata(hexAaguid)).thenReturn(metadata);

        List<X509Certificate> result = attestationCertificateServices.getAttestationRootCertificates(authData, attestationCertificates);

        assertNotNull(result);
        verify(localMdsService).getAuthenticatorsMetadata(hexAaguid);*/
    }

    @Test
    void getAttestationRootCertificates_enterpriseAttestationDisabled() {
     /*   String aaguid = "test-aaguid";
        AuthData authData = mock(AuthData.class);
        when(authData.getAaguid()).thenReturn(aaguid.getBytes(StandardCharsets.UTF_8));

        List<X509Certificate> attestationCertificates = Collections.singletonList(mock(X509Certificate.class));

        Fido2Configuration fido2Config = mock(Fido2Configuration.class);
        when(fido2Config.isEnterpriseAttestation()).thenReturn(false);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Config);

        JsonNode fetchedMetadata = mock(JsonNode.class);
        when(mdsService.fetchMetadata(authData.getAaguid())).thenReturn(fetchedMetadata);
        doNothing().when(commonVerifiers).verifyThatMetadataIsValid(fetchedMetadata);

        List<X509Certificate> expectedCertificates = Collections.singletonList(mock(X509Certificate.class));
        when(attestationCertificateServices.getAttestationRootCertificates(fetchedMetadata, attestationCertificates))
                .thenReturn(expectedCertificates);

        List<X509Certificate> result = attestationCertificateServices.getAttestationRootCertificates(authData, attestationCertificates);

        assertNotNull(result);
        verify(mdsService).fetchMetadata(authData.getAaguid());
        verify(commonVerifiers).verifyThatMetadataIsValid(fetchedMetadata);*/
    }
}
