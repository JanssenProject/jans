package io.jans.fido2.service.sg;

import io.jans.as.model.fido.u2f.exception.BadInputException;
import io.jans.as.model.fido.u2f.message.RawRegisterResponse;
import io.jans.util.security.SecurityProviderUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static io.jans.fido2.service.sg.RawRegistrationService.REGISTRATION_RESERVED_BYTE_VALUE;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RawRegistrationServiceTest {

    @InjectMocks
    private RawRegistrationService rawRegistrationService;

    @BeforeEach
    void setUp() {
        SecurityProviderUtility.installBCProvider();
    }

    @Test
    void parseRawRegisterResponse_ifReservedByteIsNotRegistrationReservedByte_badInputException() {
        String rawDataBase64 = "V1JPTkc";

        BadInputException ex = assertThrows(BadInputException.class, () -> rawRegistrationService.parseRawRegisterResponse(rawDataBase64));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "Incorrect value of reserved byte. Expected: " + REGISTRATION_RESERVED_BYTE_VALUE + ". Was: " + 87);
    }

    @Test
    void parseRawRegisterResponse_ifReservedByteIsRegistrationReservedByte_valid() {
        String rawDataBase64 = "BQQTkZFzsbTmuUoS_DS_jqpWRbZHp_J0YV8q4Xb4XTPYbIuvu-TRNubp8U-CKZuB5tDT-l6R3sQvNc6wXjGCm" +
                "L-OQK-ACAQk_whIvElk4Sfk-YLMY32TKs6jHagng7iv8U78_5K-RTTbNo-k29nb6F4HLpbYcU81xefhSNSlrAP3USwwggImMIIBz" +
                "KADAgECAoGBAPMsD5b5G58AphKuKWl4Yz27sbE_rXFy7nPRqtJ_r4E5DSZbFvfyuos-Db0095ubB0JoyM8ccmSO_eZQ6IekOLPKC" +
                "R7yC5kes-f7MaxyaphmmD4dEvmuKjF-fRsQP5tQG7zerToto8eIz0XjPaupiZxQXtSHGHHTuPhri2nfoZlrMAoGCCqGSM49BAMCM" +
                "FwxIDAeBgNVBAMTF0dsdXUgb3hQdXNoMiBVMkYgdjEuMC4wMQ0wCwYDVQQKEwRHbHV1MQ8wDQYDVQQHEwZBdXN0aW4xCzAJBgNVB" +
                "AgTAlRYMQswCQYDVQQGEwJVUzAeFw0xNjAzMDExODU5NDZaFw0xOTAzMDExODU5NDZaMFwxIDAeBgNVBAMTF0dsdXUgb3hQdXNoM" +
                "iBVMkYgdjEuMC4wMQ0wCwYDVQQKEwRHbHV1MQ8wDQYDVQQHEwZBdXN0aW4xCzAJBgNVBAgTAlRYMQswCQYDVQQGEwJVUzBZMBMGB" +
                "yqGSM49AgEGCCqGSM49AwEHA0IABICUKnzCE5PJ7tihiKkYu6E5Uy_sZ-RSqs_MnUJt0tB8G8GSg9nKo6P2424iV9lXX9Pil8qw4" +
                "ofZ-fAXXepbp4MwCgYIKoZIzj0EAwIDSAAwRQIgUWwawAB2udURWQziDXVjSOi_QcuXiRxylqj5thFwFhYCIQCGY-CTZFi7JdkhZ" +
                "05nDpbSYJBTOo1Etckh7k0qcvnO0TBFAiEA1v1jKTwGn5LRRGSab1kNdgEqD6qL08bougoJUNY1A5MCIGvtBFSNzhGvhQmdYYj5-" +
                "XOd5P4ucVk6TmkV1Xu73Dvj";

        RawRegisterResponse response = rawRegistrationService.parseRawRegisterResponse(rawDataBase64);
        assertNotNull(response);
        assertNotNull(response.getAttestationCertificate());
        assertEquals(response.getAttestationCertificate().getSigAlgName(), "SHA256WITHECDSA");
        assertNotNull(response.getAttestationCertificate().getPublicKey());
        assertEquals(response.getAttestationCertificate().getPublicKey().getFormat(), "X.509");
        assertEquals(response.getAttestationCertificate().getPublicKey().getAlgorithm(), "EC");
    }

    @Test
    void parseDer_validValues_valid() throws CertificateException, NoSuchProviderException {
        String a = "-----BEGIN CERTIFICATE-----\n" +
                "MIICxTCCAa2gAwIBAgIEala2gjANBgkqhkiG9w0BAQsFADATMREwDwYDVQQDEwhC\n" +
                "YWVsZHVuZzAeFw0yMzAyMTkwNjA5NTdaFw0yNDAyMTkwNjA5NTdaMBMxETAPBgNV\n" +
                "BAMTCEJhZWxkdW5nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAifku\n" +
                "JnJM3U/x3jWpjpUZeSVpbdUTirdB2Ta0mwXXaZmGwtrwZvS8pXdmegFUMnYB92RJ\n" +
                "98j4iYjivXElwwjFIc4YRa7hQicqMfa1H3BUtDwIpqlXM1jISr5TYAE/t/wpkrVH\n" +
                "A1QPNv7Fb07ormWKwktTMWyUoLo0chInv07Ip3m6F3X3O0jZFjE8N+7Fnv9oMdsN\n" +
                "sAAq+f/7jJSdzo/vzHebR0XUxB1YP6sTWRH6nlNw2h+0kTMf33CkXyDG1Y1qsBRK\n" +
                "MoOia10bi21B7Yd+lJo0ZnT1JNei4eEdPYxWQa43JMY6PnpJI9d5WKvye2NewXvO\n" +
                "pLap8WR3dgX6n6bUtwIDAQABoyEwHzAdBgNVHQ4EFgQUQVqwZ6AlNlPeeUOmw89A\n" +
                "u86n09gwDQYJKoZIhvcNAQELBQADggEBAGoV1ECn0h0IYEoQ8pxF1zbXnt35XEO4\n" +
                "ZPbq8cPuYj92X3c9TWeHJIHGO3ZYMS7eaBRL5HhCTqG3YjAsm6+bea9cBffeZwG3\n" +
                "EAl0u7e8CI6Ri6065Og4s0c7Y3ZJIJ4i6c5bVqPep8Oj3IFUUAwxz+95c2LX9cfL\n" +
                "hxzH8N2RzWvGoJBrmWNeQUuKVlMBVBX6n/EcWmCS/VYORw0mwJ9vdmPhGU3hGggG\n" +
                "S0rAVnQlIdvzWsaNllNWf6ETrrHceCflKsOuettjODZUAqiZ9aEd9WMDGHLtZw94\n" +
                "zONYICWg2o3Sx9/F26wHdjHn+gxB2Z45Dvd0rBMuCHqwJELxyvofc1E=\n" +
                "-----END CERTIFICATE-----";
        InputStream is = new ByteArrayInputStream(a.getBytes());

        X509Certificate response = rawRegistrationService.parseDer(is);
        assertNotNull(response);
        assertEquals(response.getSigAlgName(), "SHA256WITHRSA");
        assertNotNull(response.getPublicKey());
        assertEquals(response.getPublicKey().getFormat(), "X.509");
        assertEquals(response.getPublicKey().getAlgorithm(), "RSA");
    }
}
