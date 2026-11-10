package io.jans.ca.plugin.adminui.test;

import io.jans.ca.plugin.adminui.rest.logging.AuditLoggerResource;
import io.jans.ca.plugin.adminui.utils.CommonUtils;
import jakarta.ws.rs.core.Response;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class AuditLoggerResourceUnitTest {

    @Mock
    private Logger log;

    @InjectMocks
    private AuditLoggerResource auditLoggerResource;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testAuditLoggingEscapesControlCharactersAndPreservesJsonValues() throws Exception {
        String controlCharacters = "first\nsecond\rthird\t\u0001";
        Map<String, Object> loggingRequest = new HashMap<>();
        loggingRequest.put("message", controlCharacters);

        try (Response response = auditLoggerResource.auditLogging(loggingRequest)) {
            assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        }

        ArgumentCaptor<String> jsonArgument = ArgumentCaptor.forClass(String.class);
        verify(log).info("{}", jsonArgument.capture());

        String loggedJson = jsonArgument.getValue();
        assertTrue(loggedJson.contains("\\n"));
        assertTrue(loggedJson.contains("\\r"));
        assertTrue(loggedJson.contains("\\t"));
        assertTrue(loggedJson.contains("\\u0001"));
        assertFalse(loggedJson.contains("\n"));
        assertFalse(loggedJson.contains("\r"));
        assertFalse(loggedJson.contains("\t"));
        assertFalse(loggedJson.contains("\u0001"));
        assertEquals(CommonUtils.jsonStringToMap(loggedJson), loggingRequest);
    }
}
