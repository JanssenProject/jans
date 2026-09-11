package io.jans.orm.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Z
 */
public class SensitiveDataMaskerTest {

    @Test
    public void maskJsonValues_forNull_shouldReturnNull() {
        assertNull(SensitiveDataMasker.maskJsonValues(null));
    }

    @Test
    public void maskJsonValues_forStringWithoutSensitiveKeys_shouldReturnUnchanged() {
        String value = "{\"cacheProviderType\":\"REDIS\",\"servers\":\"35.92.20.110:6379\",\"useSSL\":false}";

        assertEquals(SensitiveDataMasker.maskJsonValues(value), value);
    }

    @Test
    public void maskJsonValues_forRedisPassword_shouldMaskOnlyPasswordValue() {
        String value = "{\"cacheProviderType\":\"REDIS\",\"redisConfiguration\":{\"servers\":\"35.92.20.110:6379\","
                + "\"password\":\"b6b167746550e3b90ee0a7801ab4e3de495dd4c267e7a862\",\"useSSL\":false}}";

        String masked = SensitiveDataMasker.maskJsonValues(value);

        assertFalse(masked.contains("b6b167746550e3b90ee0a7801ab4e3de495dd4c267e7a862"));
        assertTrue(masked.contains("\"password\":\"" + SensitiveDataMasker.MASKED + "\""));
        assertTrue(masked.contains("\"servers\":\"35.92.20.110:6379\""));
        assertTrue(masked.contains("\"useSSL\":false"));
    }

    @Test
    public void maskJsonValues_forMultipleSensitiveKeys_shouldMaskEachOne() {
        String value = "{\"redisConfiguration\":{\"password\":\"secret1\",\"sslTrustStorePassword\":\"secret2\","
                + "\"sslKeyStorePassword\":\"secret3\"}}";

        String masked = SensitiveDataMasker.maskJsonValues(value);

        assertFalse(masked.contains("secret1"));
        assertFalse(masked.contains("secret2"));
        assertFalse(masked.contains("secret3"));
        assertTrue(masked.contains("\"password\":\"" + SensitiveDataMasker.MASKED + "\""));
        assertTrue(masked.contains("\"sslTrustStorePassword\":\"" + SensitiveDataMasker.MASKED + "\""));
        assertTrue(masked.contains("\"sslKeyStorePassword\":\"" + SensitiveDataMasker.MASKED + "\""));
    }

    @Test
    public void maskJsonValues_forPostgresAuthUserPassword_shouldMaskValue() {
        String value = "{\"postgresConfiguration\":{\"authUserName\":\"jans\",\"authUserPassword\":\"p3023+TmGX0u2oQGy4ffYw==\"}}";

        String masked = SensitiveDataMasker.maskJsonValues(value);

        assertFalse(masked.contains("p3023+TmGX0u2oQGy4ffYw=="));
        assertTrue(masked.contains("\"authUserPassword\":\"" + SensitiveDataMasker.MASKED + "\""));
        assertTrue(masked.contains("\"authUserName\":\"jans\""));
    }

    @Test
    public void maskJsonValues_forEmptyPasswordValue_shouldStillMask() {
        String value = "{\"sslTrustStorePassword\":\"\"}";

        assertEquals(SensitiveDataMasker.maskJsonValues(value),
                "{\"sslTrustStorePassword\":\"" + SensitiveDataMasker.MASKED + "\"}");
    }

    @Test
    public void maskJsonValues_isCaseInsensitiveOnKeyName() {
        String value = "{\"PWD\":\"secret\",\"Password\":\"secret2\"}";

        String masked = SensitiveDataMasker.maskJsonValues(value);

        assertFalse(masked.contains("secret"));
        assertFalse(masked.contains("secret2"));
    }

}
