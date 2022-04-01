/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.util;

import io.jans.as.model.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.security.cert.X509Certificate;

import static org.testng.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 */
public class CertUtilsTest extends BaseTest {

    public static final String TEST_PEM_1 = "-----BEGIN CERTIFICATE-----\n" +
            "MIIBBjCBrAIBAjAKBggqhkjOPQQDAjAPMQ0wCwYDVQQDDARtdGxzMB4XDTE4MTAx\n" +
            "ODEyMzcwOVoXDTIyMDUwMjEyMzcwOVowDzENMAsGA1UEAwwEbXRsczBZMBMGByqG\n" +
            "SM49AgEGCCqGSM49AwEHA0IABNcnyxwqV6hY8QnhxxzFQ03C7HKW9OylMbnQZjjJ\n" +
            "/Au08/coZwxS7LfA4vOLS9WuneIXhbGGWvsDSb0tH6IxLm8wCgYIKoZIzj0EAwID\n" +
            "SQAwRgIhAP0RC1E+vwJD/D1AGHGzuri+hlV/PpQEKTWUVeORWz83AiEA5x2eXZOV\n" +
            "bUlJSGQgjwD5vaUaKlLR50Q2DmFfQj1L+SY=\n" +
            "-----END CERTIFICATE-----";

    public static final String TEST_PEM_2 = "-----BEGIN CERTIFICATE-----MIIBBjCBrAIBAjAKBggqhkjOPQQDAjAPMQ0wCwYDVQQDDARtdGxzMB4XDTE4MTAxODEyMzcwOVoXDTIyMDUwMjEyMzcwOVowDzENMAsGA1UEAwwEbXRsczBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABNcnyxwqV6hY8QnhxxzFQ03C7HKW9OylMbnQZjjJ/Au08/coZwxS7LfA4vOLS9WuneIXhbGGWvsDSb0tH6IxLm8wCgYIKoZIzj0EAwIDSQAwRgIhAP0RC1E+vwJD/D1AGHGzuri+hlV/PpQEKTWUVeORWz83AiEA5x2eXZOVbUlJSGQgjwD5vaUaKlLR50Q2DmFfQj1L+SY=-----END CERTIFICATE-----";
    public static final String TEST_PEM_3 = "MIIBBjCBrAIBAjAKBggqhkjOPQQDAjAPMQ0wCwYDVQQDDARtdGxzMB4XDTE4MTAxODEyMzcwOVoXDTIyMDUwMjEyMzcwOVowDzENMAsGA1UEAwwEbXRsczBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABNcnyxwqV6hY8QnhxxzFQ03C7HKW9OylMbnQZjjJ/Au08/coZwxS7LfA4vOLS9WuneIXhbGGWvsDSb0tH6IxLm8wCgYIKoZIzj0EAwIDSQAwRgIhAP0RC1E+vwJD/D1AGHGzuri+hlV/PpQEKTWUVeORWz83AiEA5x2eXZOVbUlJSGQgjwD5vaUaKlLR50Q2DmFfQj1L+SY=";

    @Test
    public void confirmationMethodHashS256_TestPem_rightConfirmation() {
        showTitle("confirmationMethodHashS256_TestPem_rightConfirmation");
        Assert.assertEquals(CertUtils.confirmationMethodHashS256(TEST_PEM_1), "A4DtL2JmUMhAsvJj5tKyn64SqzmuXbMrJa0n761y5v0");
        Assert.assertEquals(CertUtils.confirmationMethodHashS256(TEST_PEM_2), "A4DtL2JmUMhAsvJj5tKyn64SqzmuXbMrJa0n761y5v0");
        Assert.assertEquals(CertUtils.confirmationMethodHashS256(TEST_PEM_3), "A4DtL2JmUMhAsvJj5tKyn64SqzmuXbMrJa0n761y5v0");
    }

    @Test
    public void equalsRdn_withCorrectValues_shouldReturnTrueAndIgnoreOrder() {
        showTitle("equalsRdn_withCorrectValues_shouldReturnTrueAndIgnoreOrder");
        String r1 = "C=GB,O=OpenBanking,OU=0015800000jfFGuAAM,CN=1g7yUiOr3p0QFnAB1UvInE";
        String r2 = "CN=1g7yUiOr3p0QFnAB1UvInE, OU=0015800000jfFGuAAM, O=OpenBanking, C=GB";

        assertTrue(CertUtils.equalsRdn(r1, r2));
    }

    @Test
    public void equalsRdn_withWrongValues_shouldReturnFalse() {
        showTitle("equalsRdn_withWrongValues_shouldReturnFalse");
        String r1 = "C=FAILGB,O=OpenBanking,OU=0015800000jfFGuAAM,CN=1g7yUiOr3p0QFnAB1UvInE";
        String r2 = "CN=1g7yUiOr3p0QFnAB1UvInE, OU=0015800000jfFGuAAM, O=OpenBanking, C=GB";

        assertFalse(CertUtils.equalsRdn(r1, r2));
    }

    @Test
    public void equalsRdn_withCorrectValuesAndSpaced_shouldReturnTrue() {
        showTitle("equalsRdn_withCorrectValuesAndSpaced_shouldReturnTrue");
        String r1 = "cn = myclient,o = My Dept,o = My Company";
        String r2 = "cn=myclient,o=My Dept,o=My Company";

        assertTrue(CertUtils.equalsRdn(r1, r2));
    }

    @Test
    public void equalsRdn_withDifferenceInSpaces_shouldReturnFalse() {
        showTitle("equalsRdn_withDifferenceInSpaces_shouldReturnFalse");
        String r1 = "cn = myclient,o = My Dept,o = My Company";
        String r2 = "cn=myclient,o=MyDept,o=MyCompany";

        assertFalse(CertUtils.equalsRdn(r1, r2));
    }

    @Test
    public void equalsRdn_withJurisdictionCountryName() {
        showTitle("equalsRdn_withJurisdictionCountryName");
        String r1 = "jurisdictionCountryName=BR,businessCategory=Private Organization," +
                "UID=c395f15d-23bd-477f-8d3d-725685268059,CN=castello.sensedia.com,serialNumber=08583723000172," +
                "OU=1eb7e8de-2a06-46d7-888d-b44e82c398cd,L=Campinas,ST=SP,O=Open Banking Brasil - Sensedia,C=BR";
        String r2 = "C = BR, ST = SP, L = Campinas, O = Open Banking Brasil - Sensedia, " +
                "OU = 1eb7e8de-2a06-46d7-888d-b44e82c398cd, CN = castello.sensedia.com, " +
                "serialNumber = 08583723000172, businessCategory = Private Organization, jurisdictionCountryName = BR, " +
                "UID = c395f15d-23bd-477f-8d3d-725685268059";

        assertTrue(CertUtils.equalsRdn(r1, r2));
    }

    @Test
    public void x509CertificateFromPem_validCertificate_notNullParsedCertificate() {
        showTitle("x509CertificateFromPem_validCertificate_notNullParsedCertificate");
        SecurityProviderUtility.installBCProvider(true);
        String cert = "-----BEGIN%20CERTIFICATE-----%0AMIIDJzCCAg8CCQCp6GMQxw8GgzANBgkqhkiG9w0BAQsFADB3MQswCQYDVQQGEwJV%0AUzELMAkGA1UECAwCVFgxDzANBgNVBAcMBkF1c3RpbjEYMBYGA1UECgwPSmFuc3Nl%0Ab" +
                "iBQcm9qZWN0MRMwEQYDVQQDDApKYW5zc2VuIENBMRswGQYJKoZIhvcNAQkBFgxz%0AQGphbnMubG9jYWwwHhcNMjEwNDIwMTg1NjM4WhcNMjIwNDIwMTg1NjM4WjA0MTIw%0AMAYDVQQDDCkxODAxL" +
                "jdmNzM0OGQ4LWFjOWUtNDk1MS1hYmRmLWYyMjUzNzhmMzJm%0AZTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKdQcPocZ3rmmly1LLxQ%0AdUk0VcKq3uuR3qYd1%2BtISpWVOMVTuIh" +
                "z8j9286WfcFMyzQKDRHJCsYiCCKsenuBs%0ABE98nYmIqVOnJxYMBue9IDIYi9I5njzPy9pWnisCG5fKjHmnP288ifrEtwXESzw2%0AeQViZL0sgdo1ziPXyV5kaYjOgApWY56PE%2Fvuv3%2BXxJ2" +
                "iMzdEz6yOtOmJMHE3ZZCu%0AruhW5AGIwg6KazgaNoKWil8%2Bu8%2FZqruUdErvN21oXWFJWFrh%2FrSQq96V7S8e0nPF%0AgLLpeH%2FYGWHmVGS77XPz2c6XhQM0uRCIBcuvnvJeQyvZxrlHetU" +
                "WBYG8n7d1ZSer%0AYEkCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAbtaPcipL7GpLtQqY47DpV6jsQl9n%0Aws9url8SpKThIuATRw77Cj4XjL2DkNxANDTaueobQkt4vFw1edfbwExvavUpmsnR%0A" +
                "WeBtKMHDNEN%2BfCyBbhBi67K1ArZHkx5OWLERd4qL64T5CiAwWXVbE3gCaXMV9%2FA3%0A8%2FVvly5b4YRojK5UrpPIyG5gnE8YGVS7p3n4aIZ5r3%2FynPvFwwTCIIlPdctOABQU%0AfIctm8i8" +
                "%2FCHhdqFVvVxa0oZ9sTr4VJ3%2FKw41M9pI%2BzY754tjrnadGBSO%2FtIibjjI%0AScr80QwkiP7Cq6LRDT3VUok2OighFFAmyAfZQg9qR5udbMd%2BDynAyvJjBQ%3D%3D%0A-----END%20CER" +
                "TIFICATE-----%0A";

        final X509Certificate x509Certificate = CertUtils.x509CertificateFromPem(cert);
        assertNotNull(x509Certificate);
        assertEquals(CertUtils.getCN(x509Certificate), "1801.7f7348d8-ac9e-4951-abdf-f225378f32fe");
    }

    @Test
    public void x509CertificateFromPem_invalidCertificate_nullLogError() {
        showTitle("x509CertificateFromPem_invalidCertificate_nullLogError");
        SecurityProviderUtility.installBCProvider(true);
        String cert = "-----UNKNOW-----%0AMIIDJzCCAg8CCQCp6GMQxw8GgzANBgkqhkiG9w0BAQsFADB3MQswCQYDVQQGEwJV%0AUzELMAkGA1UECAwCVFgxDzANBgNVBAcMBkF1c3RpbjEYMBYGA1UECgwPSmFuc3Nl%0Ab" +
                "iBQcm9qZWN0MRMwEQYDVQQDDApKYW5zc2VuIENBMRswGQYJKoZIhvcNAQkBFgxz%0AQGphbnMubG9jYWwwHhcNMjEwNDIwMTg1NjM4WhcNMjIwNDIwMTg1NjM4WjA0MTIw%0AMAYDVQQDDCkxODAxL" +
                "jdmNzM0OGQ4LWFjOWUsTr4VJ3%2FKw41M9pI%2BzY754tjrnadGBSO%2FtIibjjI%0AScr80QwkiP7Cq6LRDT3VUok2OighFFAmyAfZQg9qR5udbMd%2BDynAyvJjBQ%3D%3D%0A-----END%20CER" +
                "TIFICATE-----%0A";

        final X509Certificate x509Certificate = CertUtils.x509CertificateFromPem(cert);
        assertNull(x509Certificate);
    }

}
