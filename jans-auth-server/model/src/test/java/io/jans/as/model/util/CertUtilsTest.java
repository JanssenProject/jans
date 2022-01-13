/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.util;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 */
public class CertUtilsTest {

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
    public void s256() {
        Assert.assertEquals("A4DtL2JmUMhAsvJj5tKyn64SqzmuXbMrJa0n761y5v0", CertUtils.confirmationMethodHashS256(TEST_PEM_1));
        Assert.assertEquals("A4DtL2JmUMhAsvJj5tKyn64SqzmuXbMrJa0n761y5v0", CertUtils.confirmationMethodHashS256(TEST_PEM_2));
        Assert.assertEquals("A4DtL2JmUMhAsvJj5tKyn64SqzmuXbMrJa0n761y5v0", CertUtils.confirmationMethodHashS256(TEST_PEM_3));
    }

    @Test
    public void equalsRdn_withCorrectValues_shouldReturnTrueAndIgnoreOrder() {
        String r1 = "C=GB,O=OpenBanking,OU=0015800000jfFGuAAM,CN=1g7yUiOr3p0QFnAB1UvInE";
        String r2 = "CN=1g7yUiOr3p0QFnAB1UvInE, OU=0015800000jfFGuAAM, O=OpenBanking, C=GB";

        assertTrue(CertUtils.equalsRdn(r1, r2));
    }

    @Test
    public void equalsRdn_withWrongValues_shouldReturnFalse() {
        String r1 = "C=FAILGB,O=OpenBanking,OU=0015800000jfFGuAAM,CN=1g7yUiOr3p0QFnAB1UvInE";
        String r2 = "CN=1g7yUiOr3p0QFnAB1UvInE, OU=0015800000jfFGuAAM, O=OpenBanking, C=GB";

        assertFalse(CertUtils.equalsRdn(r1, r2));
    }

    @Test
    public void equalsRdn_withCorrectValuesAndSpaced_shouldReturnTrue() {
        String r1 = "cn = myclient,o = My Dept,o = My Company";
        String r2 = "cn=myclient,o=My Dept,o=My Company";

        assertTrue(CertUtils.equalsRdn(r1, r2));
    }

    @Test
    public void equalsRdn_withDifferenceInSpaces_shouldReturnFalse() {
        String r1 = "cn = myclient,o = My Dept,o = My Company";
        String r2 = "cn=myclient,o=MyDept,o=MyCompany";

        assertFalse(CertUtils.equalsRdn(r1, r2));
    }

    @Test
    public void equalsRdn_withJurisdictionCountryName() {
        String r1 = "jurisdictionCountryName=BR,businessCategory=Private Organization," +
                "UID=c395f15d-23bd-477f-8d3d-725685268059,CN=castello.sensedia.com,serialNumber=08583723000172," +
                "OU=1eb7e8de-2a06-46d7-888d-b44e82c398cd,L=Campinas,ST=SP,O=Open Banking Brasil - Sensedia,C=BR";
        String r2 = "C = BR, ST = SP, L = Campinas, O = Open Banking Brasil - Sensedia, " +
                "OU = 1eb7e8de-2a06-46d7-888d-b44e82c398cd, CN = castello.sensedia.com, " +
                "serialNumber = 08583723000172, businessCategory = Private Organization, jurisdictionCountryName = BR, " +
                "UID = c395f15d-23bd-477f-8d3d-725685268059";

        assertTrue(CertUtils.equalsRdn(r1, r2));
    }
}
