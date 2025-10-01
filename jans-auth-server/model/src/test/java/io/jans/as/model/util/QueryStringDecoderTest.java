package io.jans.as.model.util;

import io.jans.as.model.BaseTest;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class QueryStringDecoderTest extends BaseTest {

    @Test
    public void decode_nullParam_emptyMap() {
        showTitle("decode_nullParam_emptyMap");

        Map<String, String> queryParamMap = QueryStringDecoder.decode(null);

        assertTrue(queryParamMap.isEmpty());
    }

    @Test
    public void decode_emptyParam_emptyMap() {
        showTitle("decode_emptyParam_emptyMap");

        Map<String, String> queryParamMap = QueryStringDecoder.decode("");

        assertTrue(queryParamMap.isEmpty());
    }

    @Test
    public void decode_simpleParam_validParam() {
        showTitle("decode_simpleParam_validParam");

        String key1 = "key1";
        String key2 = "key2";
        String simpleValue = "SIMPLE";
        String urlValue = "http://localhost:9000";
        String queryParamString = String.format("%s=%s&%s=%s", key1, simpleValue, key2, urlValue);

        Map<String, String> queryParamMap = QueryStringDecoder.decode(queryParamString);

        assertEquals(queryParamMap.size(), 2);
        assertEquals(queryParamMap.get(key1), simpleValue);
        assertEquals(queryParamMap.get(key2), urlValue);
    }

    @Test
    public void decode_encodedParam_validParam() {
        showTitle("decode_encodedParam_validParam");
        
        String key1 = "key1";
        String key3 = "key3";
        String urlValue = "http://localhost:9000";
        String encodedUrlValue = "http%3A%2F%2Flocalhost%3A9000";
        String queryParamString = String.format("%s=%s&%s=%s", key1, urlValue, key3, encodedUrlValue);

        Map<String, String> queryParamMap = QueryStringDecoder.decode(queryParamString);

        assertEquals(queryParamMap.size(), 2);
        assertEquals(queryParamMap.get(key1), urlValue);
        assertEquals(queryParamMap.get(key3), urlValue);
    }

    @Test
    public void decode_emptyKeyAndValueParam_validParam() {
        showTitle("decode_emptyKeyAndValueParam_validParam");

        String emptyKey = "";
        String key2 = "key2";
        String simpleValue = "SIMPLE";
        String emptyValue = "";
        String queryParamString = String.format("%s=%s&%s=%s", emptyKey, simpleValue, key2, emptyValue);

        Map<String, String> queryParamMap = QueryStringDecoder.decode(queryParamString);

        assertEquals(queryParamMap.size(), 1);
        assertEquals(queryParamMap.get(key2), emptyValue);
    }

    @Test
    public void decode_unsupportedDecodedParam_validParam() {
        showTitle("decode_unsupportedDecodedParam_validParam");

        String key1 = "key1";
        String key2 = "key2";
        String urlValue = "http://localhost:9000";
        String encodedUrlValue = "http%3A%2F%2Flocalhost%3A9000";
        String unsupportedValue = "http%3A%2F%2Flocalhost%3A9000%GG";
        String queryParamString = String.format("%s=%s&%s=%s", key1, encodedUrlValue, key2, unsupportedValue);

        Map<String, String> queryParamMap = QueryStringDecoder.decode(queryParamString);

        assertEquals(queryParamMap.size(), 1);
        assertEquals(queryParamMap.get(key1), urlValue);
    }
}
