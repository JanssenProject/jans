package io.jans.as.client.util;

import org.json.JSONObject;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ClientUtilTest {

    @Test
    void integerOrNull_ifFieldContainsNullValue_null() {
        String json = "{\"testKey\":null}";
        JSONObject objectNode = new JSONObject(json);

        Integer result = ClientUtil.integerOrNull(objectNode, "testKey");
        assertNull(result);
    }

    @Test
    void integerOrNull_ifFieldDoesNotExist_null() {
        String json = "{\"testKey1\":null}";
        JSONObject objectNode = new JSONObject(json);

        Integer result = ClientUtil.integerOrNull(objectNode, "testKey");
        assertNull(result);
    }

    @Test
    void integerOrNull_ifFieldContainsValue_success() {
        String json = "{\"testKey\":1}";
        JSONObject objectNode = new JSONObject(json);

        Integer result = ClientUtil.integerOrNull(objectNode, "testKey");
        assertNotNull(result);
        assertEquals(result, Integer.valueOf("1"));
    }
}