package io.jans.util;

import jakarta.servlet.http.HttpServletRequest;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * @author Yuriy Z
 */
public class CoreCertUtilTest {

    // Sample XFCC header with all fields
    private static final String FULL_XFCC_HEADER = "Hash=f084aaef637355eda7e99c8dd985b5cdd9a8b6d92414e8dab70eb6f23ffdd157;Cert=\"-----BEGIN%20CERTIFICATE-----%0AMIIESzCCAzOgAwIBAgIUQtWE%2FtpqIoRBO0PTdIeolg7sUVUwDQYJKoZIhvcNAQEL%0ABQAwbTELMAkGA1UEBhMCVVMxCzAJBgNVBAgMAlRYMQ8wDQYDVQQHDAZBdXN0aW4x%0ADTALBgNVBAoMBEdsdXUxEzARBgNVBAMMCkphbnNzZW4gQ0ExHDAaBgkqhkiG9w0B%0ACQEWDXRlYW1AZ2x1dS5vcmcwHhcNMjYwMzA3MTEyMTQxWhcNMjcwMzA3MTEyMTQx%0AWjCBgDELMAkGA1UEBhMCVVMxCzAJBgNVBAgMAlRYMQ8wDQYDVQQHDAZBdXN0aW4x%0AGDAWBgNVBAoMD0dsdXUgRmVkZXJhdGlvbjEOMAwGA1UEAwwFYWRtaW4xKTAnBgkq%0AhkiG9w0BCQEWGmFkbWluQGRlbW9leGFtcGxlLmdsdXUub3JnMIIBIjANBgkqhkiG%0A9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4JXFI5cP2skvbE4egluTEQ4dVt%2FicY2UmyPh%0AF%2FvaiOFy%2FP1B1VZ%2FjhKCUmHO9UxaxYTYQKUowf9BYv0L1M6SQ6QNPZdgUZkGHCUQ%0A2SntXkf9ET7P6MoOG0zQG%2B3IGj3fwd0ZGuo2k4X6JUH9ME%2FFRlLMC9ifVjgWR382%0A8zx7T4I17DWhxjvPZHCW9AlxTtv7hIph5wSvnrcHhxyEsM4y8BTPggBRXJTPiURr%0AKIjj3fEa5A1a2d6U0jqKdyLbJyRpyzJNcZas85dDh8MIsBUO4py%2FbNl9zF13Xzxv%0A8oEdBV0d9%2Fh5GsSfW2k22RN1tR2XkIBpg9b%2B5rssXh7Q9A%2F8WQIDAQABo4HOMIHL%0AMBMGA1UdJQQMMAoGCCsGAQUFBwMCMB0GA1UdDgQWBBT3LiXXR9J5cgCaGaqNc60v%0AkXoVBjCBlAYDVR0jBIGMMIGJoXGkbzBtMQswCQYDVQQGEwJVUzELMAkGA1UECAwC%0AVFgxDzANBgNVBAcMBkF1c3RpbjENMAsGA1UECgwER2x1dTETMBEGA1UEAwwKSmFu%0Ac3NlbiBDQTEcMBoGCSqGSIb3DQEJARYNdGVhbUBnbHV1Lm9yZ4IUAjECI6bjfefR%0AW%2FP1w4IQ6tkaKLgwDQYJKoZIhvcNAQELBQADggEBAFnhXEeFw0MThgBIg4xl%2FF0k%0AxDLqQT4lAfVEH0qZ4MNKEsqYvMZ0%2BjnTF8WvYWE7wLZnt5vBLMXd845Xm9joPtRJ%0Av4YTe11mpjWvZ8wkk%2BqSKC61V0YX0mc9dOW5zxfIK1pmUm94bAGSsQkDXIPgudx8%0Aw%2Fjgc8HUwSV%2F0%2BGMdrJ9sH%2BRpfg8qgljEzcR0nAyycoFST7zgugD9JFDgm%2ButSjs%0AEH1BPPyi0QLNKJxDkn7lPBr34AaqdlbnqixVBHDfzPtM4xYRKlLIF0WsSTy1DdbD%0AZR0m3aHjNArwMB3bZVzaH4n252aw3K76EkY7HRWM6gdxcFzrn2UyH9zLwQjEWH8%3D%0A-----END%20CERTIFICATE-----%0A\";Subject=\"emailAddress=admin@demoexample.gluu.org,CN=admin,O=Gluu Federation,L=Austin,ST=TX,C=US\";URI=";

    private static final String EXPECTED_DECODED_CERT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIESzCCAzOgAwIBAgIUQtWE/tpqIoRBO0PTdIeolg7sUVUwDQYJKoZIhvcNAQEL\n" +
            "BQAwbTELMAkGA1UEBhMCVVMxCzAJBgNVBAgMAlRYMQ8wDQYDVQQHDAZBdXN0aW4x\n" +
            "DTALBgNVBAoMBEdsdXUxEzARBgNVBAMMCkphbnNzZW4gQ0ExHDAaBgkqhkiG9w0B\n" +
            "CQEWDXRlYW1AZ2x1dS5vcmcwHhcNMjYwMzA3MTEyMTQxWhcNMjcwMzA3MTEyMTQx\n" +
            "WjCBgDELMAkGA1UEBhMCVVMxCzAJBgNVBAgMAlRYMQ8wDQYDVQQHDAZBdXN0aW4x\n" +
            "GDAWBgNVBAoMD0dsdXUgRmVkZXJhdGlvbjEOMAwGA1UEAwwFYWRtaW4xKTAnBgkq\n" +
            "hkiG9w0BCQEWGmFkbWluQGRlbW9leGFtcGxlLmdsdXUub3JnMIIBIjANBgkqhkiG\n" +
            "9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4JXFI5cP2skvbE4egluTEQ4dVt/icY2UmyPh\n" +
            "F/vaiOFy/P1B1VZ/jhKCUmHO9UxaxYTYQKUowf9BYv0L1M6SQ6QNPZdgUZkGHCUQ\n" +
            "2SntXkf9ET7P6MoOG0zQG+3IGj3fwd0ZGuo2k4X6JUH9ME/FRlLMC9ifVjgWR382\n" +
            "8zx7T4I17DWhxjvPZHCW9AlxTtv7hIph5wSvnrcHhxyEsM4y8BTPggBRXJTPiURr\n" +
            "KIjj3fEa5A1a2d6U0jqKdyLbJyRpyzJNcZas85dDh8MIsBUO4py/bNl9zF13Xzxv\n" +
            "8oEdBV0d9/h5GsSfW2k22RN1tR2XkIBpg9b+5rssXh7Q9A/8WQIDAQABo4HOMIHL\n" +
            "MBMGA1UdJQQMMAoGCCsGAQUFBwMCMB0GA1UdDgQWBBT3LiXXR9J5cgCaGaqNc60v\n" +
            "kXoVBjCBlAYDVR0jBIGMMIGJoXGkbzBtMQswCQYDVQQGEwJVUzELMAkGA1UECAwC\n" +
            "VFgxDzANBgNVBAcMBkF1c3RpbjENMAsGA1UECgwER2x1dTETMBEGA1UEAwwKSmFu\n" +
            "c3NlbiBDQTEcMBoGCSqGSIb3DQEJARYNdGVhbUBnbHV1Lm9yZ4IUAjECI6bjfefR\n" +
            "W/P1w4IQ6tkaKLgwDQYJKoZIhvcNAQELBQADggEBAFnhXEeFw0MThgBIg4xl/F0k\n" +
            "xDLqQT4lAfVEH0qZ4MNKEsqYvMZ0+jnTF8WvYWE7wLZnt5vBLMXd845Xm9joPtRJ\n" +
            "v4YTe11mpjWvZ8wkk+qSKC61V0YX0mc9dOW5zxfIK1pmUm94bAGSsQkDXIPgudx8\n" +
            "w/jgc8HUwSV/0+GMdrJ9sH+Rpfg8qgljEzcR0nAyycoFST7zgugD9JFDgm+utSjs\n" +
            "EH1BPPyi0QLNKJxDkn7lPBr34AaqdlbnqixVBHDfzPtM4xYRKlLIF0WsSTy1DdbD\n" +
            "ZR0m3aHjNArwMB3bZVzaH4n252aw3K76EkY7HRWM6gdxcFzrn2UyH9zLwQjEWH8=\n" +
            "-----END CERTIFICATE-----\n";

    // ==================== parseXfccHeader tests ====================

    @Test
    public void parseXfccHeader_forValidValue_shouldParseCorrectly() {
        CoreCertUtil.ClientCert value = CoreCertUtil.parseXfccHeader(FULL_XFCC_HEADER);

        assertNotNull(value);
        assertEquals(value.getHash(), "f084aaef637355eda7e99c8dd985b5cdd9a8b6d92414e8dab70eb6f23ffdd157");
        assertEquals(value.getCert(), EXPECTED_DECODED_CERT);
        assertEquals(value.getSubject(), "emailAddress=admin@demoexample.gluu.org,CN=admin,O=Gluu Federation,L=Austin,ST=TX,C=US");
        assertEquals(value.getUri(), "");
    }

    @Test
    public void parseXfccHeader_withNullInput_shouldReturnNull() {
        assertNull(CoreCertUtil.parseXfccHeader(null));
    }

    @Test
    public void parseXfccHeader_withEmptyString_shouldReturnNull() {
        assertNull(CoreCertUtil.parseXfccHeader(""));
    }

    @Test
    public void parseXfccHeader_withOnlyHash_shouldParseHashOnly() {
        String header = "Hash=abc123def456";
        CoreCertUtil.ClientCert value = CoreCertUtil.parseXfccHeader(header);

        assertNotNull(value);
        assertEquals(value.getHash(), "abc123def456");
        assertNull(value.getCert());
        assertNull(value.getSubject());
        assertNull(value.getUri());
    }

    @Test
    public void parseXfccHeader_withHashAndSubject_shouldParseBoth() {
        String header = "Hash=abc123;Subject=\"CN=test,O=TestOrg\"";
        CoreCertUtil.ClientCert value = CoreCertUtil.parseXfccHeader(header);

        assertNotNull(value);
        assertEquals(value.getHash(), "abc123");
        assertEquals(value.getSubject(), "CN=test,O=TestOrg");
        assertNull(value.getCert());
        assertNull(value.getUri());
    }

    @Test
    public void parseXfccHeader_withUri_shouldParseUri() {
        String header = "Hash=abc123;URI=spiffe://cluster.local/ns/default/sa/myservice";
        CoreCertUtil.ClientCert value = CoreCertUtil.parseXfccHeader(header);

        assertNotNull(value);
        assertEquals(value.getHash(), "abc123");
        assertEquals(value.getUri(), "spiffe://cluster.local/ns/default/sa/myservice");
        assertNull(value.getCert());
        assertNull(value.getSubject());
    }

    @Test
    public void parseXfccHeader_withInvalidUrlEncodingInCert_shouldReturnNull() {
        // Invalid percent encoding (% followed by non-hex characters)
        String header = "Hash=abc123;Cert=\"%ZZinvalid\"";
        CoreCertUtil.ClientCert value = CoreCertUtil.parseXfccHeader(header);

        assertNull(value);
    }

    @Test
    public void parseXfccHeader_withSimpleCert_shouldDecodeCert() {
        String header = "Cert=\"test%20certificate\"";
        CoreCertUtil.ClientCert value = CoreCertUtil.parseXfccHeader(header);

        assertNotNull(value);
        assertEquals(value.getCert(), "test certificate");
    }

    // ==================== getClientCert tests ====================

    @Test
    public void getClientCert_withNullRequest_shouldReturnMissingCertHeaders() {
        CoreCertUtil.ClientCert value = CoreCertUtil.getClientCert(null);

        assertNotNull(value);
        assertNull(value.getHash());
        assertNull(value.getCert());
        assertNull(value.getSubject());
        assertNull(value.getUri());
    }

    @Test
    public void getClientCert_withXfccHeader_shouldParseXfccHeader() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader(CoreCertUtil.HEADER_XFCC_CERT)).thenReturn(FULL_XFCC_HEADER);

        CoreCertUtil.ClientCert value = CoreCertUtil.getClientCert(request);

        assertNotNull(value);
        assertEquals(value.getHash(), "f084aaef637355eda7e99c8dd985b5cdd9a8b6d92414e8dab70eb6f23ffdd157");
        assertEquals(value.getCert(), EXPECTED_DECODED_CERT);
        assertEquals(value.getSubject(), "emailAddress=admin@demoexample.gluu.org,CN=admin,O=Gluu Federation,L=Austin,ST=TX,C=US");
    }

    @Test
    public void getClientCert_withLegacyClientCertHeader_shouldReturnCertOnly() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader(CoreCertUtil.HEADER_XFCC_CERT)).thenReturn(null);
        when(request.getHeader(CoreCertUtil.HEADER_CLIENT_CERT)).thenReturn("legacy-cert-value");

        CoreCertUtil.ClientCert value = CoreCertUtil.getClientCert(request);

        assertNotNull(value);
        assertNull(value.getHash());
        assertEquals(value.getCert(), "legacy-cert-value");
        assertNull(value.getSubject());
        assertNull(value.getUri());
    }

    @Test
    public void getClientCert_withNoHeaders_shouldReturnMissingCertHeaders() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader(CoreCertUtil.HEADER_XFCC_CERT)).thenReturn(null);
        when(request.getHeader(CoreCertUtil.HEADER_CLIENT_CERT)).thenReturn(null);

        CoreCertUtil.ClientCert value = CoreCertUtil.getClientCert(request);

        assertNotNull(value);
        assertNull(value.getHash());
        assertNull(value.getCert());
        assertNull(value.getSubject());
        assertNull(value.getUri());
    }

    @Test
    public void getClientCert_withEmptyXfccHeader_shouldFallbackToLegacyHeader() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader(CoreCertUtil.HEADER_XFCC_CERT)).thenReturn("");
        when(request.getHeader(CoreCertUtil.HEADER_CLIENT_CERT)).thenReturn("fallback-cert");

        CoreCertUtil.ClientCert value = CoreCertUtil.getClientCert(request);

        assertNotNull(value);
        assertEquals(value.getCert(), "fallback-cert");
    }

    @Test
    public void getClientCert_withEmptyLegacyHeader_shouldReturnMissingCertHeaders() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader(CoreCertUtil.HEADER_XFCC_CERT)).thenReturn(null);
        when(request.getHeader(CoreCertUtil.HEADER_CLIENT_CERT)).thenReturn("");

        CoreCertUtil.ClientCert value = CoreCertUtil.getClientCert(request);

        assertNotNull(value);
        assertNull(value.getHash());
        assertNull(value.getCert());
    }

    @Test
    public void getClientCert_withXfccWithoutCert_shouldFallbackToLegacyHeader() {
        // XFCC header present with Hash/Subject but no Cert field
        // Should NOT mix XFCC metadata with legacy cert to avoid combining different certificate identities
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader(CoreCertUtil.HEADER_XFCC_CERT)).thenReturn("Hash=abc123;Subject=\"CN=test,O=TestOrg\"");
        when(request.getHeader(CoreCertUtil.HEADER_XFTCC_CERT)).thenReturn(null);
        when(request.getHeader(CoreCertUtil.HEADER_CLIENT_CERT)).thenReturn("legacy-cert-pem");

        CoreCertUtil.ClientCert value = CoreCertUtil.getClientCert(request);

        assertNotNull(value);
        // Should only have the legacy cert, no mixed metadata
        assertNull(value.getHash());
        assertNull(value.getSubject());
        assertNull(value.getUri());
        assertEquals(value.getCert(), "legacy-cert-pem");
    }

    @Test
    public void getClientCert_withXfccWithoutCertAndNoLegacyHeader_shouldReturnXfccMetadata() {
        // XFCC header present with Hash/Subject but no Cert, and no legacy header
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader(CoreCertUtil.HEADER_XFCC_CERT)).thenReturn("Hash=abc123;Subject=\"CN=test,O=TestOrg\"");
        when(request.getHeader(CoreCertUtil.HEADER_CLIENT_CERT)).thenReturn(null);

        CoreCertUtil.ClientCert value = CoreCertUtil.getClientCert(request);

        assertNotNull(value);
        // Should still have XFCC metadata even without cert
        assertEquals(value.getHash(), "abc123");
        assertEquals(value.getSubject(), "CN=test,O=TestOrg");
        assertNull(value.getCert());
    }

    // ==================== ClientCert class tests ====================

    @Test
    public void clientCert_constructor_shouldSetAllFields() {
        CoreCertUtil.ClientCert cert = new CoreCertUtil.ClientCert("hash1", "cert1", "subject1", "uri1");

        assertEquals(cert.getHash(), "hash1");
        assertEquals(cert.getCert(), "cert1");
        assertEquals(cert.getSubject(), "subject1");
        assertEquals(cert.getUri(), "uri1");
    }

    @Test
    public void clientCert_withNullValues_shouldAllowNullFields() {
        CoreCertUtil.ClientCert cert = new CoreCertUtil.ClientCert(null, null, null, null);

        assertNull(cert.getHash());
        assertNull(cert.getCert());
        assertNull(cert.getSubject());
        assertNull(cert.getUri());
    }

    // ==================== parseXftccHeader tests (Traefik format) ====================

    // Sample base64-encoded certificate (without PEM delimiters) for testing
    private static final String SAMPLE_BASE64_CERT = "MIIESzCCAzOgAwIBAgIUQtWE/tpqIoRBO0PTdIeolg7sUVUwDQYJKoZIhvcNAQEL" +
            "BQAwbTELMAkGA1UEBhMCVVMxCzAJBgNVBAgMAlRYMQ8wDQYDVQQHDAZBdXN0aW4x" +
            "DTALBgNVBAoMBEdsdXUxEzARBgNVBAMMCkphbnNzZW4gQ0ExHDAaBgkqhkiG9w0B" +
            "CQEWDXRlYW1AZ2x1dS5vcmcwHhcNMjYwMzA3MTEyMTQxWhcNMjcwMzA3MTEyMTQx";

    @Test
    public void parseXftccHeader_withValidBase64_shouldReturnPemFormat() {
        String result = CoreCertUtil.parseXftccHeader(SAMPLE_BASE64_CERT);

        assertNotNull(result);
        assertTrue(result.startsWith("-----BEGIN CERTIFICATE-----\n"));
        assertTrue(result.endsWith("\n-----END CERTIFICATE-----"));
        assertTrue(result.contains(SAMPLE_BASE64_CERT));
    }

    @Test
    public void parseXftccHeader_withNullInput_shouldReturnNull() {
        assertNull(CoreCertUtil.parseXftccHeader(null));
    }

    @Test
    public void parseXftccHeader_withEmptyString_shouldReturnNull() {
        assertNull(CoreCertUtil.parseXftccHeader(""));
    }

    @Test
    public void parseXftccHeader_withWhitespaceOnly_shouldReturnNull() {
        assertNull(CoreCertUtil.parseXftccHeader("   "));
    }

    @Test
    public void parseXftccHeader_withInvalidBase64_shouldReturnNull() {
        // Invalid base64 characters
        assertNull(CoreCertUtil.parseXftccHeader("not!valid@base64#"));
    }

    @Test
    public void parseXftccHeader_withLeadingTrailingWhitespace_shouldTrimAndParse() {
        String result = CoreCertUtil.parseXftccHeader("  " + SAMPLE_BASE64_CERT + "  ");

        assertNotNull(result);
        assertTrue(result.contains(SAMPLE_BASE64_CERT));
    }

    @Test
    public void parseXftccHeader_withUrlEncodedBase64_shouldDecodeAndParse() {
        // URL-encoded version of the sample cert (Traefik <2.9.4 behavior)
        // Base64 uses +, /, = which get URL-encoded as %2B, %2F, %3D
        String urlEncodedCert = SAMPLE_BASE64_CERT
                .replace("+", "%2B")
                .replace("/", "%2F")
                .replace("=", "%3D");

        String result = CoreCertUtil.parseXftccHeader(urlEncodedCert);

        assertNotNull(result);
        assertTrue(result.startsWith("-----BEGIN CERTIFICATE-----\n"));
        assertTrue(result.endsWith("\n-----END CERTIFICATE-----"));
        // The result should contain the decoded base64, not the URL-encoded version
        assertTrue(result.contains(SAMPLE_BASE64_CERT));
    }

    @Test
    public void parseXftccHeader_withPartiallyUrlEncodedBase64_shouldDecodeAndParse() {
        // Simulate a partially encoded cert (only some special chars encoded)
        String partiallyEncodedCert = SAMPLE_BASE64_CERT.replace("/", "%2F");

        String result = CoreCertUtil.parseXftccHeader(partiallyEncodedCert);

        assertNotNull(result);
        assertTrue(result.contains(SAMPLE_BASE64_CERT));
    }

    // ==================== getClientCert tests for XFTCC header ====================

    @Test
    public void getClientCert_withXftccHeader_shouldParseCertificate() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader(CoreCertUtil.HEADER_XFCC_CERT)).thenReturn(null);
        when(request.getHeader(CoreCertUtil.HEADER_XFTCC_CERT)).thenReturn(SAMPLE_BASE64_CERT);

        CoreCertUtil.ClientCert value = CoreCertUtil.getClientCert(request);

        assertNotNull(value);
        assertNotNull(value.getCert());
        assertTrue(value.getCert().startsWith("-----BEGIN CERTIFICATE-----"));
        assertTrue(value.getCert().contains(SAMPLE_BASE64_CERT));
        assertNull(value.getHash());
        assertNull(value.getSubject());
        assertNull(value.getUri());
    }

    @Test
    public void getClientCert_withXfccMetadataAndXftccCert_shouldNotMixMetadata() {
        // XFCC has metadata but no cert, XFTCC has cert
        // Should NOT mix metadata from XFCC with cert from XFTCC to avoid combining different certificate identities
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader(CoreCertUtil.HEADER_XFCC_CERT)).thenReturn("Hash=abc123;Subject=\"CN=test,O=TestOrg\"");
        when(request.getHeader(CoreCertUtil.HEADER_XFTCC_CERT)).thenReturn(SAMPLE_BASE64_CERT);

        CoreCertUtil.ClientCert value = CoreCertUtil.getClientCert(request);

        assertNotNull(value);
        // Should only have the cert, no mixed metadata
        assertNull(value.getHash());
        assertNull(value.getSubject());
        assertNull(value.getUri());
        assertNotNull(value.getCert());
        assertTrue(value.getCert().contains(SAMPLE_BASE64_CERT));
    }

    @Test
    public void getClientCert_withXfccCert_shouldPreferXfccOverXftcc() {
        // Both headers present with certs - XFCC should win
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader(CoreCertUtil.HEADER_XFCC_CERT)).thenReturn(FULL_XFCC_HEADER);
        when(request.getHeader(CoreCertUtil.HEADER_XFTCC_CERT)).thenReturn(SAMPLE_BASE64_CERT);

        CoreCertUtil.ClientCert value = CoreCertUtil.getClientCert(request);

        assertNotNull(value);
        // Should use XFCC cert, not XFTCC
        assertEquals(value.getCert(), EXPECTED_DECODED_CERT);
        assertEquals(value.getHash(), "f084aaef637355eda7e99c8dd985b5cdd9a8b6d92414e8dab70eb6f23ffdd157");
    }

    @Test
    public void getClientCert_withInvalidXftcc_shouldFallbackToLegacy() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader(CoreCertUtil.HEADER_XFCC_CERT)).thenReturn(null);
        when(request.getHeader(CoreCertUtil.HEADER_XFTCC_CERT)).thenReturn("invalid!base64@");
        when(request.getHeader(CoreCertUtil.HEADER_CLIENT_CERT)).thenReturn("legacy-cert-pem");

        CoreCertUtil.ClientCert value = CoreCertUtil.getClientCert(request);

        assertNotNull(value);
        assertEquals(value.getCert(), "legacy-cert-pem");
    }

    @Test
    public void getClientCert_withEmptyXftcc_shouldFallbackToLegacy() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader(CoreCertUtil.HEADER_XFCC_CERT)).thenReturn(null);
        when(request.getHeader(CoreCertUtil.HEADER_XFTCC_CERT)).thenReturn("");
        when(request.getHeader(CoreCertUtil.HEADER_CLIENT_CERT)).thenReturn("legacy-cert-pem");

        CoreCertUtil.ClientCert value = CoreCertUtil.getClientCert(request);

        assertNotNull(value);
        assertEquals(value.getCert(), "legacy-cert-pem");
    }

    // ==================== Constants tests ====================

    @Test
    public void headerConstants_shouldHaveExpectedValues() {
        assertEquals(CoreCertUtil.HEADER_XFCC_CERT, "X-Forwarded-Client-Cert");
        assertEquals(CoreCertUtil.HEADER_XFTCC_CERT, "X-Forwarded-Tls-Client-Cert");
        assertEquals(CoreCertUtil.HEADER_CLIENT_CERT, "X-ClientCert");
    }

    // ==================== Pattern tests ====================

    @Test
    public void patterns_shouldMatchExpectedFormats() {
        // Test Hash pattern
        java.util.regex.Matcher hashMatcher = CoreCertUtil.PARAM_XFCC_HASH.matcher("Hash=abc123;other=value");
        assertEquals(hashMatcher.find(), true);
        assertEquals(hashMatcher.group(1), "abc123");

        // Test Subject pattern
        java.util.regex.Matcher subjectMatcher = CoreCertUtil.PARAM_XFCC_SUBJECT.matcher("Subject=\"CN=test,O=Org\"");
        assertEquals(subjectMatcher.find(), true);
        assertEquals(subjectMatcher.group(1), "CN=test,O=Org");

        // Test Cert pattern
        java.util.regex.Matcher certMatcher = CoreCertUtil.PARAM_XFCC_CERT.matcher("Cert=\"encoded-cert\"");
        assertEquals(certMatcher.find(), true);
        assertEquals(certMatcher.group(1), "encoded-cert");

        // Test URI pattern
        java.util.regex.Matcher uriMatcher = CoreCertUtil.PARAM_XFCC_URI.matcher("URI=spiffe://test;other=value");
        assertEquals(uriMatcher.find(), true);
        assertEquals(uriMatcher.group(1), "spiffe://test");
    }
}
