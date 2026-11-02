package io.jans.fido2.service.verifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.conf.RequestedParty;
import io.jans.fido2.model.error.ErrorResponseFactory;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.util.List;

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

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private AppConfiguration appConfiguration;

    private void configureOrigins(String... origins) {
        RequestedParty requestedParty = new RequestedParty();
        requestedParty.setOrigins(List.of(origins));
        Fido2Configuration fido2Configuration = new Fido2Configuration();
        fido2Configuration.setRequestedParties(List.of(requestedParty));
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
    }

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
        when(errorResponseFactory.badRequestException(any(), any())).thenReturn(new WebApplicationException(Response.status(400).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> domainVerifier.verifyDomain(domain, clientDataNode));
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 400);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(log).debug("Domains comparison {} {}", domain, originValue);
        verifyNoMoreInteractions(log);
    }

    @Test
    void verifyDomain_listedSubdomain_true() {
        configureOrigins("testurl", "test1.testurl");
        ObjectNode clientDataNode = mapper.createObjectNode();
        when(commonVerifiers.verifyThatFieldString(clientDataNode, "origin")).thenReturn("https://test1.testurl");

        assertTrue(domainVerifier.verifyDomain("testurl", clientDataNode));
    }

    @Test
    void verifyDomain_unlistedSubdomain_rejected() {
        configureOrigins("testurl");
        ObjectNode clientDataNode = mapper.createObjectNode();
        when(commonVerifiers.verifyThatFieldString(clientDataNode, "origin")).thenReturn("https://test1.testurl");
        when(errorResponseFactory.badRequestException(any(), any())).thenReturn(new WebApplicationException(Response.status(400).build()));

        assertThrows(WebApplicationException.class, () -> domainVerifier.verifyDomain("testurl", clientDataNode));
    }
}
