package io.jans.as.server.service;

import io.jans.as.common.model.session.SessionId;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class DeviceAuthorizationServiceTest {

    @InjectMocks
    private DeviceAuthorizationService deviceAuthorizationService;

    @Mock
    private Logger log;

    @Mock
    private SessionIdService sessionIdService;

    @Test
    public void removeUserCodeFromSession_whenUserCodePresent_shouldRemoveItAndUpdateSession() {
        SessionId session = new SessionId();
        Map<String, String> attributes = new HashMap<>();
        attributes.put(DeviceAuthorizationService.SESSION_USER_CODE, "KPST-JWGW");
        attributes.put("other", "value");
        session.setSessionAttributes(attributes);

        deviceAuthorizationService.removeUserCodeFromSession(session);

        assertFalse(session.getSessionAttributes().containsKey(DeviceAuthorizationService.SESSION_USER_CODE));
        assertEquals(session.getSessionAttributes().get("other"), "value");
        verify(sessionIdService).updateSessionId(session);
    }

    @Test
    public void removeUserCodeFromSession_whenUserCodeAbsent_shouldNotUpdateSession() {
        SessionId session = new SessionId();
        session.setSessionAttributes(new HashMap<>());

        deviceAuthorizationService.removeUserCodeFromSession(session);

        assertTrue(session.getSessionAttributes().isEmpty());
        verify(sessionIdService, never()).updateSessionId(session);
    }

    @Test
    public void removeUserCodeFromSession_whenSessionNull_shouldDoNothing() {
        deviceAuthorizationService.removeUserCodeFromSession(null);

        verify(sessionIdService, never()).updateSessionId(org.mockito.ArgumentMatchers.any());
    }
}
