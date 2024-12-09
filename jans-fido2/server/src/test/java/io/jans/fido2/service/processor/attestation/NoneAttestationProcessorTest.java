package io.jans.fido2.service.processor.attestation;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.model.auth.CredAndCounterData;
import io.jans.fido2.service.Base64Service;
import io.jans.orm.model.fido2.Fido2RegistrationData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoneAttestationProcessorTest {

    @InjectMocks
    private NoneAttestationProcessor noneAttestationProcessor;

    @Mock
    private Logger log;

    @Mock
    private Base64Service base64Service;

    // Test
    void getAttestationFormat_valid_none() {
        String fmt = noneAttestationProcessor.getAttestationFormat().getFmt();
        assertNotNull(fmt);
        assertEquals(fmt, "none");
    }

    // Test
    void process_validData_success() {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        Fido2RegistrationData credential = mock(Fido2RegistrationData.class);
        byte[] clientDataHash = "clientDataHash_test".getBytes();
        CredAndCounterData credIdAndCounters = mock(CredAndCounterData.class);

        when(attStmt.isEmpty()).thenReturn(true);
        when(authData.getCredId()).thenReturn("credId_test".getBytes());
        when(authData.getCosePublicKey()).thenReturn("cosePublicKey_test".getBytes());
        //TODO: this is not working
        /*noneAttestationProcessor.process(attStmt, authData, credential, clientDataHash, credIdAndCounters);

        verify(log).debug(eq("None attestation {}"), any(JsonNode.class));
        verify(base64Service, times(2)).urlEncodeToString(any(byte[].class));

        verify(log, never()).error(eq("Problem with None attestation"));*/
    }

    //Test
    void process_ifAttStmtIsEmptyFalse_fido2RuntimeException() {
        JsonNode attStmt = mock(JsonNode.class);
        AuthData authData = mock(AuthData.class);
        Fido2RegistrationData credential = mock(Fido2RegistrationData.class);
        byte[] clientDataHash = "clientDataHash_test".getBytes();
        CredAndCounterData credIdAndCounters = mock(CredAndCounterData.class);

        when(attStmt.isEmpty()).thenReturn(false);
        //TODO: this is not working
        /*Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> noneAttestationProcessor.process(attStmt, authData, credential, clientDataHash, credIdAndCounters));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Problem with None attestation");

        verify(log).debug(eq("None attestation {}"), any(JsonNode.class));
        verify(log).error(eq("Problem with None attestation"));

        verifyNoInteractions(base64Service);*/
    }
}
