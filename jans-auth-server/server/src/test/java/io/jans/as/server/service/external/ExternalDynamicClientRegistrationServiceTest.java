package io.jans.as.server.service.external;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.server.model.common.ExecutionContext;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 */
@Listeners(MockitoTestNGListener.class)
public class ExternalDynamicClientRegistrationServiceTest {

    @InjectMocks
    private ExternalDynamicClientRegistrationService externalDynamicClientRegistrationService;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Test
    public void modifyPostResponse_whenDefaultExternalCustomScriptIsNull_shouldReturnFalseWithoutNpe() {
        final boolean result = externalDynamicClientRegistrationService.modifyPostResponse(new JSONObject(), new ExecutionContext());
        assertFalse(result);
    }

    @Test
    public void modifyPutResponse_whenDefaultExternalCustomScriptIsNull_shouldReturnFalseWithoutNpe() {
        final boolean result = externalDynamicClientRegistrationService.modifyPutResponse(new JSONObject(), new ExecutionContext());
        assertFalse(result);
    }

    @Test
    public void modifyReadResponse_whenDefaultExternalCustomScriptIsNull_shouldReturnFalseWithoutNpe() {
        final boolean result = externalDynamicClientRegistrationService.modifyReadResponse(new JSONObject(), new ExecutionContext());
        assertFalse(result);
    }

    @Test
    public void isCertValidForClient_whenDefaultExternalCustomScriptIsNull_shouldReturnTrueWithoutNpe() {
        final boolean result = externalDynamicClientRegistrationService.isCertValidForClient(null, null);
        assertTrue(result);
    }

    @Test
    public void getDcrHmacSecret_whenDefaultExternalCustomScriptIsNull_shouldReturnEmptyStringWithoutNpe() {
        final String result = externalDynamicClientRegistrationService.getDcrHmacSecret(null, new Jwt());
        assertEquals(result, "");
    }

    @Test
    public void getDcrJwks_whenDefaultExternalCustomScriptIsNull_shouldReturnNullWithoutNpe() {
        JSONObject result = externalDynamicClientRegistrationService.getDcrJwks(null, new Jwt());
        assertNull(result);
    }

    @Test
    public void getSoftwareStatementHmacSecret_whenDefaultExternalCustomScriptIsNull_shouldReturnEmptyStringWithoutNpe() {
        String result = externalDynamicClientRegistrationService.getSoftwareStatementHmacSecret(null, new JSONObject(), new Jwt());
        assertEquals(result, "");
    }

    @Test
    public void getSoftwareStatementJwks_whenDefaultExternalCustomScriptIsNull_shouldReturnNullWithoutNpe() {
        JSONObject result = externalDynamicClientRegistrationService.getSoftwareStatementJwks(null, new JSONObject(), new Jwt());
        assertNull(result);
    }
}
