package io.jans.fido2.service.verifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jans.fido2.exception.Fido2RpRuntimeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DomainVerifierTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    private DomainVerifier domainVerifier;

    @Mock
    private Logger log;

    @Mock
    private CommonVerifiers commonVerifiers;

    @Test
    void verifyDomain_valid_true() {
        String domain = "test.url";
        String originKey = "origin";
        String originValue = "https://test.url";
        ObjectNode clientDataNode = mapper.createObjectNode();
        clientDataNode.put(originKey, domain);
        when(commonVerifiers.verifyThatFieldString(clientDataNode, originKey)).thenReturn(originValue);

        boolean result = domainVerifier.verifyDomain(domain, clientDataNode);
        assertTrue(result);
        verify(log).debug("Domains comparison {} {}", domain, originValue);
        verifyNoMoreInteractions(log);
    }

    @Test
    void verifyDomain_originNotHost_true() {
        String domain = "test.url";
        String originKey = "origin";
        String originValue = "test.url";
        ObjectNode clientDataNode = mapper.createObjectNode();
        clientDataNode.put(originKey, domain);
        when(commonVerifiers.verifyThatFieldString(clientDataNode, originKey)).thenReturn(originValue);

        boolean result = domainVerifier.verifyDomain(domain, clientDataNode);
        assertTrue(result);
        verify(log).debug("Domains comparison {} {}", domain, originValue);
        verify(log).warn(contains("MalformedURLException"), anyString());
    }

    @Test
    void verifyDomain_domainNotEquals_true() {
        String domain = "testurl";
        String originKey = "origin";
        String originValue = "https://test1.testurl";
        ObjectNode clientDataNode = mapper.createObjectNode();
        clientDataNode.put(originKey, domain);
        when(commonVerifiers.verifyThatFieldString(clientDataNode, originKey)).thenReturn(originValue);

        boolean result = domainVerifier.verifyDomain(domain, clientDataNode);
        assertTrue(result);
        verify(log).debug("Domains comparison {} {}", domain, originValue);
        verifyNoMoreInteractions(log);
    }

    @Test
    void verifyDomain_effectiveDomainNotEndWith_fido2RpRuntimeException() {
        String domain = "test.url";
        String originKey = "origin";
        String originValue = "https://test1.url";
        ObjectNode clientDataNode = mapper.createObjectNode();
        clientDataNode.put(originKey, domain);
        when(commonVerifiers.verifyThatFieldString(clientDataNode, originKey)).thenReturn(originValue);

        Fido2RpRuntimeException ex = assertThrows(Fido2RpRuntimeException.class, () -> domainVerifier.verifyDomain(domain, clientDataNode));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Domains don't match");
        verify(log).debug("Domains comparison {} {}", domain, originValue);
        verifyNoMoreInteractions(log);
    }
}