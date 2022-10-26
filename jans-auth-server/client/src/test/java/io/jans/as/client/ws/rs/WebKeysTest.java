/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import io.jans.as.client.BaseTest;
import io.jans.as.model.util.Base64Util;
import org.testng.ITestContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

import static org.testng.Assert.assertEquals;

/**
 * @author Javier Rojas Blum
 * @version February 25, 2017
 */
public class WebKeysTest extends BaseTest {

    @Test(dataProvider = "webKeysDataProvider")
    public void webKeyTest(final String n, final String e, final String x5c) throws CertificateException {
        showTitle("webKeyTest");

        byte[] nBytes = Base64Util.base64urldecode(n);
        BigInteger modulus = new BigInteger(1, nBytes);

        byte[] eBytes = Base64Util.base64urldecode(e);
        BigInteger exponent = new BigInteger(1, eBytes);

        System.out.println("n: " + n);
        System.out.println("n: " + modulus);

        System.out.println("e: " + e);
        System.out.println("e: " + exponent);

        byte[] certBytes = Base64Util.base64urldecode(x5c);
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(certBytes));

        PublicKey publicKey = cert.getPublicKey();
        RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
        assertEquals(rsaPublicKey.getModulus(), modulus);
        assertEquals(rsaPublicKey.getPublicExponent(), exponent);
    }

    @DataProvider(name = "webKeysDataProvider")
    public Object[][] dataProvider(ITestContext context) {
        return new Object[][]{
                {
                        "5awKF1MZSGSAAlujSf-dRzvrK9D_vV85BMn7fZ-x5E-So580TrTxT9-vgfmTWzhDr0f240DqR6ojF_NGXh8V3QhFRM9i2p7dg7M3LO-mfYlrJ_x2Rlw-EdvMmYargk5gaM7sRQKwWnU6ajRZIDw3XbrLDvGeLWZhH1-RzV3NjlJ_0c85bXhyLg_MT9NpnGTP4CePLF0dLuQwo4ktQkkW_BwPaSUhHgPYA-M6IA9S31_vQLB4ZyN00EpdO57fEbhutkzrpb9iiXJh82DD0D5Z2eYyQdMX_7pN9frLKVhoCUelzZ887it0oIlLfpe8WUzuiDHWYThzQiepQfMBRQMJhQ",
                        "AQAB",
                        "MIIDAzCCAeugAwIBAgIgLuZ/WGm/NwCIWOVcrvj2QuLV6yxyWQD7GEsmM1SWgP8wDQYJKoZIhvcNAQELBQAwITEfMB0GA1UEAwwWb3hBdXRoIENBIENlcnRpZmljYXRlczAeFw0xNzAyMDgxMzM1NTJaFw0xODAyMDgxMzM2MDBaMCExHzAdBgNVBAMMFm94QXV0aCBDQSBDZXJ0aWZpY2F0ZXMwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDlrAoXUxlIZIACW6NJ/51HO+sr0P+9XzkEyft9n7HkT5KjnzROtPFP36+B+ZNbOEOvR/bjQOpHqiMX80ZeHxXdCEVEz2Lant2Dszcs76Z9iWsn/HZGXD4R28yZhquCTmBozuxFArBadTpqNFkgPDddussO8Z4tZmEfX5HNXc2OUn/RzzlteHIuD8xP02mcZM/gJ48sXR0u5DCjiS1CSRb8HA9pJSEeA9gD4zogD1LfX+9AsHhnI3TQSl07nt8RuG62TOulv2KJcmHzYMPQPlnZ5jJB0xf/uk31+sspWGgJR6XNnzzuK3SgiUt+l7xZTO6IMdZhOHNCJ6lB8wFFAwmFAgMBAAGjJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADANBgkqhkiG9w0BAQsFAAOCAQEAmuyIS597+LbwvKZgeshm6b8YspHYIFMRp9Pr06jp+P94oK7zgOe4x0U13+ReoTiMke0Zbq4aE93BxykyTJg+eL3qi9Nr6o6EPXC6NrSOwi7+OgkOxvy3ffOM0k9uH8kQgrSqyr4ra6GPyhAlEZShJZHtwEWSipohldi4uH1nKBR0QbFYlDrUxs1pErZT5hsDO3yaZ+XCJmsvwNqvcYTWsElbJrhMsiR3ymmjxDkQghT6TYc3LkerlFEjPE5YPT+57LTRr0Clj/NCHtYVJM32vEqZK+trQ44wpW9UfUgivsswgaH7qpUoUd3toAzNyjYq4aRT2f+ClKkJqr30nrt7iQ=="
                },
                {
                        "vZXUxthj0LTSeeR_XMHaakLCIXd5Ua_4hNra-8UOP7ayhiY3c2KzpnmeWJ1SjPpbzS3O7Wc8AJSY_nrLzg9XjH5723cx_9TKbTVvE8_HQu5ZsUH7LgoT4yxMhvOFL6ir3RKEyOiOVFBBb4fWVGxwDchVkR26nBKK8RdAqCxnIEw1vkG6zHEPl1WBiK2IQ6JxhrrOLoTquHGBPc2qT_des8a6Xe6GlbUq1h-3bKAUXjjwSmJ36aau5aUNuvUlnPdEGcI25sTwRp7jCzuM1VN6a7y1nPkIYltYndOdP8EDGsrkS9pfQx1Z8HDdbw-lFGuOK5QFS53TfOfRtt0RQpy-Tw",
                        "AQAB",
                        "MIIDAzCCAeugAwIBAgIgT1z8Ptulz9vG6rXRfn8gTEAk7apWNSQTD8cUWLn5DTQwDQYJKoZIhvcNAQELBQAwITEfMB0GA1UEAwwWb3hBdXRoIENBIENlcnRpZmljYXRlczAeFw0xNzAyMjUwMzQyNTVaFw0xODAyMjUwMzQzMDRaMCExHzAdBgNVBAMMFm94QXV0aCBDQSBDZXJ0aWZpY2F0ZXMwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC9ldTG2GPQtNJ55H9cwdpqQsIhd3lRr/iE2tr7xQ4/trKGJjdzYrOmeZ5YnVKM+lvNLc7tZzwAlJj+esvOD1eMfnvbdzH/1MptNW8Tz8dC7lmxQfsuChPjLEyG84UvqKvdEoTI6I5UUEFvh9ZUbHANyFWRHbqcEorxF0CoLGcgTDW+QbrMcQ+XVYGIrYhDonGGus4uhOq4cYE9zapP916zxrpd7oaVtSrWH7dsoBReOPBKYnfppq7lpQ269SWc90QZwjbmxPBGnuMLO4zVU3prvLWc+QhiW1id050/wQMayuRL2l9DHVnwcN1vD6UUa44rlAVLndN859G23RFCnL5PAgMBAAGjJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADANBgkqhkiG9w0BAQsFAAOCAQEAhyDfbnYMvn2JkfD+RuTnSmvNSiwv0/80yOH4G5Mq8f8DSnCLmdWFtUMuXIUqrsj233etfPmkeZA9hE3+XAokCoHduXzn5evRS46fuhrUuBTjVjH3w+T8iMOquain9dnWCFfbiekcvT962wm2Pf5LcFb1h+M2LNTDun/2uv92BUvrKK/k2+n5I/j8TTG21s8/jFA7SUL6mxKN5w4d2uwMFlLKnkrNKnAKSszn4ptWtAeKWEZFEozog9hSeyg0rv8B7a9mki7pdbBLRMqEo5Fh2uUhkIUgDp8FoW4wnPs4zQC9+y2R9TUwLQS0l8W1X73rwLOI3vD7vkVO6mqvzb5sMA=="
                },
                {
                        "1EV17EwdTr-qEuJpJisBbzxLA9dGQsxFEkXM-JM5XBV54S6Zeon-RymNlt5GiBpT-0fDXK3PNxt5R__cSbKOs6F_pRbGFSWxRxJgKHYp37eiW3PUee3rf6USIl6naj0HcHnycEiZxo2wwB1J12Iw3czqRbGhgcCusPO60EFdaTE3qH5owPLH_3FbdVcGd3BJFrKwT7CaWGfmtUsZtMDBii4tVUn8onaILYV4I0ZCwwvySB1jJbwd5gvMILz-GNMcvZ6-5k_ojQlaZrvmkzLjzi1y61PHGo_vLvpIlJ8EJpCtlZ8MQwadIT94bGXCHUta7B4XhxR5sLFmlShAzWdUcw",
                        "AQAB",
                        "MIIDBDCCAeygAwIBAgIhAO9HVmwNIiJv+DBg8oZ4EbnE+irSQ/NuKPf8pBCCew4iMA0GCSqGSIb3DQEBDAUAMCExHzAdBgNVBAMMFm94QXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMTcwMjI1MDM0MjU1WhcNMTgwMjI1MDM0MzA0WjAhMR8wHQYDVQQDDBZveEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1EV17EwdTr+qEuJpJisBbzxLA9dGQsxFEkXM+JM5XBV54S6Zeon+RymNlt5GiBpT+0fDXK3PNxt5R//cSbKOs6F/pRbGFSWxRxJgKHYp37eiW3PUee3rf6USIl6naj0HcHnycEiZxo2wwB1J12Iw3czqRbGhgcCusPO60EFdaTE3qH5owPLH/3FbdVcGd3BJFrKwT7CaWGfmtUsZtMDBii4tVUn8onaILYV4I0ZCwwvySB1jJbwd5gvMILz+GNMcvZ6+5k/ojQlaZrvmkzLjzi1y61PHGo/vLvpIlJ8EJpCtlZ8MQwadIT94bGXCHUta7B4XhxR5sLFmlShAzWdUcwIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQEMBQADggEBAK4BgzFnyInDQPJQPK0Z27fbDbxXmcNYr9hvM7+nH4TWcW/2NmiIVPIlwUJEVQ0u63fhhDzGGGKuLIyc/K0XU+QJ4+OtS6mxjlZFohPpVyXrEUYAYCSEukDSj/WBr90WrFTYIly+7h2qexm3GCPxqKrMiLUjTBNL/igjQPdu6A1NbBlTKGlYlcV5J8MZzh6+5pkpLgLyiNUUiq1qvcHgTxhxTtIjQnr2Og4a0ZVorq20mbv3tuVyNPJEZfNOQOMvgAa+s/gSlt03vi0i598T4L/MOhMUCWgNVuRf2Y6sSz1W7vErfRMocoFuXjexCneV2aWkURpiUXQM6o9dEgP03S0="
                },
                {
                        "ucSlx1c0N7sGdpY1ly9tNH9OLzudC30HJiLMQPoxL9b_gXWx-MaErv7C7LxHuBKqT7Cq0SdvAued-bKxhlaNApfVQrbNbrF_tLo9oHgNqH4Jjil422EIBzjjSlaxkWBBhF3oIvmo1e3MAewWbq3sVTKUZKVKdAPVRe_8Ddt352FIBFTgU16lhXwnw3TmHcwxdwGJ719D9GOmtQyRZf_trSWEIORJJyHT8vueCWIgrCi2xEi_7XVWR7VLKl-gl1oxI2VYUrIRYDzBuT9vb0QAljN84Mbr-EDJs63BnPOJkK5pC_jTqKIViazo5CnITnOYAGXi7_JasllCfTSZUsSs2w",
                        "AQAB",
                        "MIIDAzCCAeugAwIBAgIgPhwrD54M4f56OVV6s/5k276jK4Qawc07aAn3vT1THJAwDQYJKoZIhvcNAQENBQAwITEfMB0GA1UEAwwWb3hBdXRoIENBIENlcnRpZmljYXRlczAeFw0xNzAyMjUwMzQyNTVaFw0xODAyMjUwMzQzMDRaMCExHzAdBgNVBAMMFm94QXV0aCBDQSBDZXJ0aWZpY2F0ZXMwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC5xKXHVzQ3uwZ2ljWXL200f04vO50LfQcmIsxA+jEv1v+BdbH4xoSu/sLsvEe4EqpPsKrRJ28C5535srGGVo0Cl9VCts1usX+0uj2geA2ofgmOKXjbYQgHOONKVrGRYEGEXegi+ajV7cwB7BZurexVMpRkpUp0A9VF7/wN23fnYUgEVOBTXqWFfCfDdOYdzDF3AYnvX0P0Y6a1DJFl/+2tJYQg5EknIdPy+54JYiCsKLbESL/tdVZHtUsqX6CXWjEjZVhSshFgPMG5P29vRACWM3zgxuv4QMmzrcGc84mQrmkL+NOoohWJrOjkKchOc5gAZeLv8lqyWUJ9NJlSxKzbAgMBAAGjJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADANBgkqhkiG9w0BAQ0FAAOCAQEAKbHMN54sX8E9p9J86wfZpkcsdyaNgk8j1cje+nq/7PgLKbxHVhVVdJWdFQC3glMPnmMMNByudcZ+cCT05Gpw016Ts9TVy5jgglbyV5FNJuaYZnLe5mVb6KuUxJZLZmJEYXlTgMW9lYwTgbKOo4lP0GNykTnYaGk6+R7pHcB+2To2uNSsNz3cXX9Pb6lDk78VJKD1DN3/LGHD/Tpavi15j8C8vSEGsZkeeSl5tEEFTXHqZQqu+pLQdH8eEvmTtt1yXk2LdlEvoQmubkELZKW7guYIU+lAZrjdDr5oPaAkefYsH50fl6KaYCtsV9mGNUvoW+LTtCgcUlvXGdGhFJmV3Q=="
                }
        };
    }
}
