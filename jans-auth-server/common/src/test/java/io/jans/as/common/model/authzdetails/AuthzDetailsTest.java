package io.jans.as.common.model.authzdetails;

import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashSet;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
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
}
