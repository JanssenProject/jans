package io.jans.as.common.model.authzdetails;

import org.json.JSONArray;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashSet;

import static org.testng.Assert.*;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Yuriy Z
 */
public class AuthzDetailsTest {

    @Test
    public void ofSilently_withInvalidJson_shouldReturnNull() {
        assertNull(AuthzDetails.ofSilently("invalidJson"));
    }

    @Test
    public void ofSilently_withValidJson_shouldReturnNotNull() {
        assertNotNull(AuthzDetails.ofSilently("[]"));
    }

    @Test
    public void getTypes_withValidJson_shouldReturnNotNull() {
        final AuthzDetails details = AuthzDetails.ofSilently("[{\"type\":\"internal_type\"}]");
        assertNotNull(details);
        assertEquals(details.getTypes(), new HashSet<>(Collections.singletonList("internal_type")));
    }

    @Test
    public void getJsonArray_withValidJson_shouldReturnNotNull() {
        final AuthzDetails details = AuthzDetails.ofSilently("[{\"type\":\"internal_type\"}]");
        assertNotNull(details);

        final JSONArray array = details.asJsonArray();
        assertEquals(array.toString(), "[{\"type\":\"internal_type\"}]");
        assertTrue(array.similar(new JSONArray("[{\"type\":\"internal_type\"}]")));
    }
}
