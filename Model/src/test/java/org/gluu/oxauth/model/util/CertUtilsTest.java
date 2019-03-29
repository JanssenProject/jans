package org.gluu.oxauth.model.util;

import org.gluu.oxauth.model.util.CertUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

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
}
