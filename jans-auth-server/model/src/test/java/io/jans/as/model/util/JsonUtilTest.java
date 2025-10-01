package io.jans.as.model.util;

import org.json.JSONObject;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author Yuriy Z
 */
public class JsonUtilTest {

    @Test
    public void testToStringWithoutNulls() {
        // Create a JSONObject with some null values (using JSONObject.NULL)
        JSONObject input = new JSONObject();
        input.put("name", "John Doe");
        input.put("email", JSONObject.NULL);  // Should be filtered out
        input.put("age", 30);
        input.put("address", (Object) null);  // Should be filtered out

        // Get the filtered JSON string
        String result = JsonUtil.toStringWithoutNulls(input);

        // Build the expected JSONObject (only non-null entries)
        JSONObject expected = new JSONObject();
        expected.put("name", "John Doe");
        expected.put("age", 30);

        // Assert that the filtered JSON matches the expected JSON
        assertEquals(expected.toString(), result);
    }
}
