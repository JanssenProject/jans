package io.jans.as.server.service;

import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.auth.Authenticator;
import io.jans.as.server.auth.MTLSService;
import io.jans.as.server.service.external.ExternalDynamicClientRegistrationService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Listeners(MockitoTestNGListener.class)
public class MTLSServiceTest {

    private static final String tlsClientAuthSubjectDn = "UID=4b18f6a7-2972-455c-948d-b0e59a8c1da9,1.3.6.1.4.1.311.60.2.1.3=#13024252,2.5.4.15=#131450726976617465204f7267616e697a6174696f6e,2.5.4.5=#130e3133383834373735303030313139,CN=hubfintech.com.br,OU=7eebe017-cb01-498c-81e4-6b4149b18e93,O=HUB PAGAMENTOS S.A,L=Tambore,ST=SP,C=BR";
    private static final String certPem = "-----BEGIN CERTIFICATE-----\n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\n-----END CERTIFICATE-----";

    @InjectMocks
    private MTLSService mtlsService;

    @Mock
    private Logger log;

    @Mock
    private Authenticator authenticator;

    @Mock
    private SessionIdService sessionIdService;

    @Mock
    private AbstractCryptoProvider cryptoProvider;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private ExternalDynamicClientRegistrationService externalDynamicClientRegistrationService;

    @Test
    public void processRegisterMTLS_HappyFlow_ReturnsTrue() throws Exception {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        String jsonRequest = "{ \"tls_client_auth_subject_dn\":\"" + tlsClientAuthSubjectDn + "\" }";
        BufferedReader requestReader = new BufferedReader(new StringReader(jsonRequest));
        when(httpServletRequest.getReader()).thenReturn(requestReader);
        when(httpServletRequest.getHeader(eq("X-ClientCert"))).thenReturn(certPem);

        boolean result = mtlsService.processRegisterMTLS(httpServletRequest);

        assertTrue(result);
        verify(log).debug("Trying to authenticate client registration request via MTLS");
        verify(log).debug(anyString(), anyString());

        verifyNoMoreInteractions(log);
    }

    @Test
    public void processRegisterMTLS_ErrorReadingJsonRequest_ReturnsFalse() throws Exception {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getReader()).thenThrow(new IOException());
        when(httpServletRequest.getHeader(eq("X-ClientCert"))).thenReturn(certPem);

        boolean result = mtlsService.processRegisterMTLS(httpServletRequest);

        assertFalse(result);
        verify(log).debug("Trying to authenticate client registration request via MTLS");
        verify(log).error(eq("Error getting TLS_CLIENT_AUTH_SUBJECT_DN field from request registration body"), any(IOException.class));
        verify(log).debug(anyString(), (String) isNull());

        verifyNoMoreInteractions(log);
    }

    @Test
    public void processRegisterMTLS_CouldntCreateX509Cert_ReturnsFalse() throws Exception {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        String jsonRequest = "{ \"tls_client_auth_subject_dn\":\"" + tlsClientAuthSubjectDn + "\" }";
        BufferedReader requestReader = new BufferedReader(new StringReader(jsonRequest));
        when(httpServletRequest.getReader()).thenReturn(requestReader);
        when(httpServletRequest.getHeader(eq("X-ClientCert"))).thenReturn("ABC");

        boolean result = mtlsService.processRegisterMTLS(httpServletRequest);

        assertFalse(result);
        verify(log).debug("Trying to authenticate client registration request via MTLS");
        verify(log).debug("Failed to parse client certificate");

        verifyNoMoreInteractions(log);
    }

    @Test
    public void processRegisterMTLS_HeaderXClientCertNull_ReturnsFalse() throws Exception {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        String jsonRequest = "{ \"tls_client_auth_subject_dn\":\"" + tlsClientAuthSubjectDn + "\" }";
        BufferedReader requestReader = new BufferedReader(new StringReader(jsonRequest));
        when(httpServletRequest.getReader()).thenReturn(requestReader);

        boolean result = mtlsService.processRegisterMTLS(httpServletRequest);

        assertFalse(result);
        verify(log).debug("Trying to authenticate client registration request via MTLS");
        verify(log).debug("Client certificate is missed in `X-ClientCert` header");

        verifyNoMoreInteractions(log);
    }

}
