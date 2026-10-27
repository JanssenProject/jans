package io.jans.as.server.service;

import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.auth.Authenticator;
import io.jans.as.server.auth.MTLSService;
import io.jans.as.server.service.external.ExternalDynamicClientRegistrationService;
import io.jans.util.security.SecurityProviderUtility;
import jakarta.servlet.http.HttpServletRequest;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
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
        when(httpServletRequest.getHeader(eq("X-Forwarded-Client-Cert"))).thenReturn(null);
        when(httpServletRequest.getHeader(eq("X-Forwarded-Tls-Client-Cert"))).thenReturn(null);
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
        when(httpServletRequest.getHeader(eq("X-Forwarded-Client-Cert"))).thenReturn(certPem);

        boolean result = mtlsService.processRegisterMTLS(httpServletRequest);

        assertFalse(result);
        verify(log).debug("Trying to authenticate client registration request via MTLS");
        verify(log).error(eq("Error getting TLS_CLIENT_AUTH_SUBJECT_DN field from request registration body"), any(IOException.class));
    }

    @Test
    public void processRegisterMTLS_CouldntCreateX509Cert_ReturnsFalse() throws Exception {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        String jsonRequest = "{ \"tls_client_auth_subject_dn\":\"" + tlsClientAuthSubjectDn + "\" }";
        BufferedReader requestReader = new BufferedReader(new StringReader(jsonRequest));
        when(httpServletRequest.getReader()).thenReturn(requestReader);
        when(httpServletRequest.getHeader(eq("X-Forwarded-Client-Cert"))).thenReturn("ABC");
        when(httpServletRequest.getHeader(eq("X-Forwarded-Tls-Client-Cert"))).thenReturn(null);
        when(httpServletRequest.getHeader(eq("X-ClientCert"))).thenReturn(null);

        boolean result = mtlsService.processRegisterMTLS(httpServletRequest);

        assertFalse(result);
        verify(log).debug("Trying to authenticate client registration request via MTLS");
        verify(log).debug("Client certificate is missed in `X-Forwarded-Client-Cert`, `X-Forwarded-Tls-Client-Cert` and `X-ClientCert` headers");

        verifyNoMoreInteractions(log);
    }

    @Test
    public void processRegisterMTLS_HeaderXClientCertNull_ReturnsFalse() throws Exception {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        String jsonRequest = "{ \"tls_client_auth_subject_dn\":\"" + tlsClientAuthSubjectDn + "\" }";
        BufferedReader requestReader = new BufferedReader(new StringReader(jsonRequest));
        when(httpServletRequest.getReader()).thenReturn(requestReader);
        when(httpServletRequest.getHeader(eq("X-Forwarded-Client-Cert"))).thenReturn(null);
        when(httpServletRequest.getHeader(eq("X-Forwarded-Tls-Client-Cert"))).thenReturn(null);
        when(httpServletRequest.getHeader(eq("X-ClientCert"))).thenReturn(null);

        boolean result = mtlsService.processRegisterMTLS(httpServletRequest);

        assertFalse(result);
        verify(log).debug("Trying to authenticate client registration request via MTLS");
        verify(log).debug("Client certificate is missed in `X-Forwarded-Client-Cert`, `X-Forwarded-Tls-Client-Cert` and `X-ClientCert` headers");

        verifyNoMoreInteractions(log);
    }

    @Test
    public void isTrustedByAnyAnchor_leafSignedByAnchor_returnsTrue() throws Exception {
        KeyPair caKeyPair = generateRsaKeyPair();
        KeyPair leafKeyPair = generateRsaKeyPair();
        X509Certificate caCert = buildCert(caKeyPair, caKeyPair.getPublic(), "CN=spiffe-ca", notBefore(), notAfter(), true);
        X509Certificate leafCert = buildCert(caKeyPair, leafKeyPair.getPublic(), "CN=spiffe-leaf", notBefore(), notAfter(), false);

        assertTrue(invokeIsTrustedByAnyAnchor(leafCert, Collections.singleton(new TrustAnchor(caCert, null))));
    }

    @Test
    public void isTrustedByAnyAnchor_expiredLeaf_returnsFalse() throws Exception {
        KeyPair caKeyPair = generateRsaKeyPair();
        KeyPair leafKeyPair = generateRsaKeyPair();
        X509Certificate caCert = buildCert(caKeyPair, caKeyPair.getPublic(), "CN=spiffe-ca", notBefore(), notAfter(), true);
        X509Certificate expiredLeafCert = buildCert(caKeyPair, leafKeyPair.getPublic(), "CN=spiffe-leaf",
                new Date(System.currentTimeMillis() - 172800_000L), new Date(System.currentTimeMillis() - 86400_000L), false);

        assertFalse(invokeIsTrustedByAnyAnchor(expiredLeafCert, Collections.singleton(new TrustAnchor(caCert, null))));
    }

    @Test
    public void isTrustedByAnyAnchor_leafSignedByDifferentKey_returnsFalse() throws Exception {
        KeyPair caKeyPair = generateRsaKeyPair();
        KeyPair otherKeyPair = generateRsaKeyPair();
        KeyPair leafKeyPair = generateRsaKeyPair();
        X509Certificate caCert = buildCert(caKeyPair, caKeyPair.getPublic(), "CN=spiffe-ca", notBefore(), notAfter(), true);
        X509Certificate wrongSignerLeafCert = buildCert(otherKeyPair, leafKeyPair.getPublic(), "CN=spiffe-leaf", notBefore(), notAfter(), false);

        assertFalse(invokeIsTrustedByAnyAnchor(wrongSignerLeafCert, Collections.singleton(new TrustAnchor(caCert, null))));
    }

    @Test
    public void isTrustedByAnyAnchor_noAnchors_returnsFalse() throws Exception {
        KeyPair leafKeyPair = generateRsaKeyPair();
        X509Certificate leafCert = buildCert(leafKeyPair, leafKeyPair.getPublic(), "CN=spiffe-leaf", notBefore(), notAfter(), false);

        assertFalse(invokeIsTrustedByAnyAnchor(leafCert, Collections.emptySet()));
    }

    private boolean invokeIsTrustedByAnyAnchor(X509Certificate leaf, Set<TrustAnchor> anchors) throws Exception {
        Method method = MTLSService.class.getDeclaredMethod("isTrustedByAnyAnchor", X509Certificate.class, Set.class);
        method.setAccessible(true);
        return (boolean) method.invoke(mtlsService, leaf, anchors);
    }

    private static KeyPair generateRsaKeyPair() throws Exception {
        SecurityProviderUtility.installBCProvider(true);
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    private static Date notBefore() {
        return new Date(System.currentTimeMillis() - 86400_000L);
    }

    private static Date notAfter() {
        return new Date(System.currentTimeMillis() + 86400_000L);
    }

    private static X509Certificate buildCert(KeyPair signerKeyPair, PublicKey subjectPublicKey, String subjectDn,
                                              Date notBefore, Date notAfter, boolean isCa) throws Exception {
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                new X500Name("CN=spiffe-ca"),
                BigInteger.valueOf(System.nanoTime()),
                notBefore, notAfter,
                new X500Name(subjectDn),
                subjectPublicKey);

        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(isCa));
        certBuilder.addExtension(Extension.keyUsage, true,
                new KeyUsage(isCa ? (KeyUsage.keyCertSign | KeyUsage.cRLSign) : KeyUsage.digitalSignature));

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(signerKeyPair.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(certBuilder.build(signer));
    }

}
