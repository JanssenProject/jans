package io.jans.as.server.util;

import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Yuriy Z
 */
public class ServerUtilTest {

    @Test
    public void prepareForLogs_whenCalled_shouldNotHaveClearTextClientPassword() {
        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("client_secret", new String[] {"124"});

        final Map<String, String[]> result = ServerUtil.prepareForLogs(parameters);

        assertEquals("*****", result.get("client_secret")[0]);
    }

    @Test
    public void prepareForLogs_whenCalled_shouldNotHaveClearTextPassword() {
        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("password", new String[] {"124"});

        final Map<String, String[]> result = ServerUtil.prepareForLogs(parameters);

        assertEquals("*****", result.get("password")[0]);
    }
}
