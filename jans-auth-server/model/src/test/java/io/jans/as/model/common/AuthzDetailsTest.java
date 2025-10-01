package io.jans.as.model.common;

import io.jans.as.model.authzdetails.AuthzDetail;
import io.jans.as.model.authzdetails.AuthzDetails;
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
    public void isEmpty_forNull_shouldReturnTrue() {
        assertTrue(AuthzDetails.isEmpty(null));
    }

    @Test
    public void isEmpty_forEmptyDetails_shouldReturnTrue() {
        assertTrue(AuthzDetails.isEmpty(new AuthzDetails()));
    }

    @Test
    public void isEmpty_forNonEmptyDetails_shouldReturnFalse() {
        final AuthzDetails authzDetails = new AuthzDetails();
        authzDetails.getDetails().add(new AuthzDetail("{}"));
        assertFalse(AuthzDetails.isEmpty(authzDetails));
    }

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

    @Test
    public void similar_forSameJson_shouldReturnTrue() {
        String a1 = "[\n" +
                "  {\n" +
                "    \"type\": \"internal_a1\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"type\": \"internal_a2\"\n" +
                "  }\n" +
                "]";

        String a2 = "[\n" +
                "  {\n" +
                "    \"type\": \"internal_a1\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"type\": \"internal_a2\"\n" +
                "  }\n" +
                "]";

        assertTrue(AuthzDetails.similar(a1, a2));
    }

    @Test
    public void asJsonArray_whenCalled_shouldReturnExpectedArray() {
        String a1 = "[\n" +
                "  {\n" +
                "    \"type\": \"internal_a1\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"type\": \"internal_a2\"\n" +
                "  }\n" +
                "]";

        final AuthzDetails authzDetails = AuthzDetails.of(a1);
        assertTrue(authzDetails.asJsonArray().similar(new JSONArray(a1)));
    }
}
