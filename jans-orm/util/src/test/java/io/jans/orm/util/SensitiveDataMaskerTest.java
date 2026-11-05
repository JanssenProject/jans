package io.jans.orm.util;

import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

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

    @Test
    public void maskJsonValues_forPasswordValueWithEscapedQuote_shouldMaskEntireValue() {
        String value = "{\"password\":\"sec\\\"ret\",\"useSSL\":false}";

        String masked = SensitiveDataMasker.maskJsonValues(value);

        assertFalse(masked.contains("sec\\\"ret"));
        assertTrue(masked.contains("\"password\":\"" + SensitiveDataMasker.MASKED + "\""));
        assertTrue(masked.contains("\"useSSL\":false"));
    }

    @Test
    public void maskJsonValues_forNumericPasswordValue_shouldMaskValue() {
        String value = "{\"pwd\":123456,\"useSSL\":false}";

        String masked = SensitiveDataMasker.maskJsonValues(value);

        assertFalse(masked.contains("123456"));
        assertTrue(masked.contains("\"pwd\":\"" + SensitiveDataMasker.MASKED + "\""));
        assertTrue(masked.contains("\"useSSL\":false"));
    }

    @Test
    public void maskJsonValues_forUnicodeEscapedKeyName_shouldMaskValue() {
        String value = "{\"pass\\u0077ord\":\"secret\",\"useSSL\":false}";

        String masked = SensitiveDataMasker.maskJsonValues(value);

        assertFalse(masked.contains("secret"));
        assertTrue(masked.contains("\"password\":\"" + SensitiveDataMasker.MASKED + "\""));
        assertTrue(masked.contains("\"useSSL\":false"));
    }

    @Test
    public void maskJsonValues_forNestedArrayWithSensitiveKeys_shouldMaskEachOccurrence() {
        String value = "{\"servers\":[{\"host\":\"db1\",\"password\":\"secret1\"},"
                + "{\"host\":\"db2\",\"password\":\"secret2\"}]}";

        String masked = SensitiveDataMasker.maskJsonValues(value);

        assertFalse(masked.contains("secret1"));
        assertFalse(masked.contains("secret2"));
        assertTrue(masked.contains("\"host\":\"db1\""));
        assertTrue(masked.contains("\"host\":\"db2\""));
    }

    @Test
    public void maskJsonValues_forMalformedJsonWithUnicodeEscapedKeyName_shouldStillMaskValue() {
        // trailing comma before the closing brace makes this invalid JSON, forcing the raw-text fallback
        String value = "{\"pass\\u0077ord\":\"secret\",\"useSSL\":false,}";

        String masked = SensitiveDataMasker.maskJsonValues(value);

        assertFalse(masked.contains("secret"));
        assertTrue(masked.contains(SensitiveDataMasker.MASKED));
    }

    @Test
    public void maskJsonValues_forUnterminatedPasswordValue_shouldMaskEntireValue() {
        String value = "{\"password\":\"first,second";

        String masked = SensitiveDataMasker.maskJsonValues(value);

        assertFalse(masked.contains("first"));
        assertFalse(masked.contains("second"));
        assertTrue(masked.contains(SensitiveDataMasker.MASKED));
    }

    @Test
    public void maskJsonValues_forUnescapedInternalQuoteInPasswordValue_shouldMaskEntireValue() {
        String value = "{\"password\":\"first\"second}";

        String masked = SensitiveDataMasker.maskJsonValues(value);

        assertFalse(masked.contains("first"));
        assertFalse(masked.contains("second"));
        assertTrue(masked.contains(SensitiveDataMasker.MASKED));
    }

    @Test
    public void maskJsonValues_forMalformedJsonWithNestedPassword_shouldMaskNestedValue() {
        String value = "{\"cfg\":{\"password\":\"secret\"},";

        String masked = SensitiveDataMasker.maskJsonValues(value);

        assertFalse(masked.contains("secret"));
        assertTrue(masked.contains("\"password\":\"" + SensitiveDataMasker.MASKED + "\""));
    }

    @Test
    public void maskJsonValues_forSecretAndTokenKeys_shouldMaskValues() {
        String value = "{\"clientSecret\":\"s1\",\"accessToken\":\"t1\",\"refresh_token\":\"t2\",\"host\":\"db1\"}";

        String masked = SensitiveDataMasker.maskJsonValues(value);

        assertFalse(masked.contains("s1"));
        assertFalse(masked.contains("t1"));
        assertFalse(masked.contains("t2"));
        assertTrue(masked.contains("\"host\":\"db1\""));
    }

    @Test
    public void maskJsonValues_forBareScalarFollowedByNestedPassword_shouldMaskNestedValue() {
        String value = "{\"cfg\":null{\"password\":\"secret\"}}";

        String masked = SensitiveDataMasker.maskJsonValues(value);

        assertFalse(masked.contains("secret"));
        assertTrue(masked.contains(SensitiveDataMasker.MASKED));
    }

    @Test
    public void maskJsonValues_forMismatchedContainerCloser_shouldMaskThroughEndOfInput() {
        String value = "{\"password\":{\"token\":\"first\"],\"recovery\":\"second\"}}";

        String masked = SensitiveDataMasker.maskJsonValues(value);

        assertFalse(masked.contains("first"));
        assertFalse(masked.contains("second"));
        assertTrue(masked.contains(SensitiveDataMasker.MASKED));
    }

    @Test
    public void maskValue_forNativeMapWithSensitiveKeys_shouldMaskNestedValues() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("password", "p1");
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("clientSecret", "s1");
        map.put("host", "db1");
        map.put("nested", nested);

        String masked = SensitiveDataMasker.maskValue(map);

        assertFalse(masked.contains("p1"));
        assertFalse(masked.contains("s1"));
        assertTrue(masked.contains("\"host\":\"db1\""));
    }

    @Test
    public void maskValue_forStringAndNull_shouldDelegateToJsonMasking() {
        assertNull(SensitiveDataMasker.maskValue(null));
        assertEquals(SensitiveDataMasker.maskValue("plain"), "plain");
        assertFalse(SensitiveDataMasker.maskValue("{\"password\":\"x1\"}").contains("x1"));
    }

    @Test
    public void allowlistProperties_shouldDropNonAllowlistedKeys() {
        Properties props = new Properties();
        props.setProperty("db.schema.name", "jans");
        props.setProperty("connection.uri", "jdbc:postgresql://h/db?password=abc");
        props.setProperty("connection.driver-property.password", "abc");

        Map<String, String> result = SensitiveDataMasker.allowlistProperties(props, "db.schema.name", "missing");

        assertEquals(result.size(), 1);
        assertEquals(result.get("db.schema.name"), "jans");
    }

    @Test
    public void maskJsonValues_forVeryLongUnterminatedPasswordValue_shouldMaskWithoutError() {
        String value = "{\"password\":\"" + "x".repeat(100_000);

        String masked = SensitiveDataMasker.maskJsonValues(value);

        assertFalse(masked.contains("xxxx"));
        assertTrue(masked.contains(SensitiveDataMasker.MASKED));
    }

    @Test
    public void maskJsonValues_forVeryLongNonJsonText_shouldNotThrow() {
        String value = "\"" + "x".repeat(100_000);

        assertEquals(SensitiveDataMasker.maskJsonValues(value), value);
    }

    @Test
    public void maskJsonValues_forJsonEmbeddedInStringValue_shouldMaskNestedSecret() {
        String value = "{\"cfg\":\"{\\\"password\\\":\\\"leak1\\\"}\",\"list\":[\"{\\\"clientSecret\\\":\\\"leak2\\\"}\"]}";

        String masked = SensitiveDataMasker.maskJsonValues(value);

        assertFalse(masked.contains("leak1"));
        assertFalse(masked.contains("leak2"));
    }

    @Test
    public void maskValue_forListWithEmbeddedJson_shouldMaskNestedSecret() {
        String masked = SensitiveDataMasker.maskValue(java.util.List.of("{\"password\":\"leak3\"}"));

        assertFalse(masked.contains("leak3"));
    }

    @Test
    public void maskJsonValues_forAbbreviatedTokenKey_shouldMaskValue() {
        String masked = SensitiveDataMasker.maskJsonValues("{\"jansRegistrationAccessTkn\":\"leak4\",\"host\":\"db1\"}");

        assertFalse(masked.contains("leak4"));
        assertTrue(masked.contains("\"host\":\"db1\""));
    }

    @Test
    public void maskValue_forCharArray_shouldMaskWholeValue() {
        assertEquals(SensitiveDataMasker.maskValue("secret".toCharArray()), SensitiveDataMasker.MASKED);
    }

    @Test
    public void maskValue_forCyclicMap_shouldNotThrow() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("self", map);

        assertEquals(SensitiveDataMasker.maskValue(map), SensitiveDataMasker.MASKED);
    }

}
