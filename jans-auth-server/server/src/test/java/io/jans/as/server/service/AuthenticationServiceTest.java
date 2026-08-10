package io.jans.as.server.service;

import com.codahale.metrics.Timer;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.service.common.UserService;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.server.security.Identity;
import io.jans.model.GluuStatus;
import io.jans.model.metric.MetricType;
import io.jans.model.security.Credentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for user authentication metric deduplication in {@link AuthenticationService}.
 *
 * Each {@link AuthenticationService} instance simulates one HTTP request (the bean is request scoped),
 * while a shared {@link SessionId} simulates the session which spans requests of a multi-step
 * authentication flow.
 */
@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest {

    @Mock
    private Logger log;

    @Mock
    private MetricService metricService;

    @Mock
    private SessionIdService sessionIdService;

    @Mock
    private UserService userService;

    @Mock
    private Credentials credentials;

    @Mock
    private Identity identity;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private AuthenticationProtectionService authenticationProtectionService;

    @Test
    public void incUserAuthenticationMetricIfNotReported_calledTwiceInSameRequest_reportsOnce() {
        when(sessionIdService.getSessionId()).thenReturn(null);
        AuthenticationService service = newService();

        service.incUserAuthenticationMetricIfNotReported(false);
        service.incUserAuthenticationMetricIfNotReported(false);

        verify(metricService, times(1)).incCounter(MetricType.USER_AUTHENTICATION_FAILURES);
        verify(metricService, never()).incCounter(eq(MetricType.USER_AUTHENTICATION_FAILURES), anyString());
    }

    @Test
    public void incUserAuthenticationMetricIfNotReported_successReportedInSession_suppressesNextRequestOutcome() {
        SessionId session = newSession();
        session.getSessionAttributes().put(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, "basic");
        when(sessionIdService.getSessionId()).thenReturn(session);

        AuthenticationService firstRequest = newService();
        firstRequest.incUserAuthenticationMetricIfNotReported(true);

        verify(metricService).incCounter(MetricType.USER_AUTHENTICATION_SUCCESS);
        verify(metricService).incCounter(MetricType.USER_AUTHENTICATION_SUCCESS, "basic");
        assertEquals(Boolean.TRUE.toString(),
                session.getSessionAttributes().get(AuthenticationService.AUTH_METRIC_SUCCESS_REPORTED));

        AuthenticationService secondRequest = newService();
        secondRequest.incUserAuthenticationMetricIfNotReported(false);
        secondRequest.incUserAuthenticationMetricIfNotReported(true);

        verify(metricService, never()).incCounter(MetricType.USER_AUTHENTICATION_FAILURES);
        verify(metricService, times(1)).incCounter(MetricType.USER_AUTHENTICATION_SUCCESS);
    }

    @Test
    public void incUserAuthenticationMetricIfNotReported_failureDoesNotSuppressLaterSuccess() {
        SessionId session = newSession();
        when(sessionIdService.getSessionId()).thenReturn(session);

        AuthenticationService failedAttempt = newService();
        failedAttempt.incUserAuthenticationMetricIfNotReported(false);

        verify(metricService).incCounter(MetricType.USER_AUTHENTICATION_FAILURES);
        assertNull(session.getSessionAttributes().get(AuthenticationService.AUTH_METRIC_SUCCESS_REPORTED));

        AuthenticationService retryAttempt = newService();
        retryAttempt.incUserAuthenticationMetricIfNotReported(true);

        verify(metricService).incCounter(MetricType.USER_AUTHENTICATION_SUCCESS);
    }

    @Test
    public void incUserAuthenticationMetricIfNotReported_noAcrInSession_reportsWithoutSubType() {
        SessionId session = newSession();
        when(sessionIdService.getSessionId()).thenReturn(session);

        newService().incUserAuthenticationMetricIfNotReported(true);

        verify(metricService).incCounter(MetricType.USER_AUTHENTICATION_SUCCESS);
        verify(metricService, never()).incCounter(eq(MetricType.USER_AUTHENTICATION_SUCCESS), anyString());
    }

    @Test
    public void authenticate_calledByScriptOnIntermediateStep_suppressesOutcomeReportOfNextStep() {
        SessionId session = newSession();
        session.getSessionAttributes().put(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, "twilio_sms");
        when(sessionIdService.getSessionId()).thenReturn(session);
        when(credentials.getUsername()).thenReturn("testuser");
        when(appConfiguration.getUpdateUserLastLogonTime()).thenReturn(false);

        Timer timer = Mockito.mock(Timer.class);
        when(timer.time()).thenReturn(Mockito.mock(Timer.Context.class));
        when(metricService.getTimer(MetricType.USER_AUTHENTICATION_RATE)).thenReturn(timer);

        User user = new User();
        user.setUserId("testuser");
        user.setStatus(GluuStatus.ACTIVE);
        when(userService.getUser("testuser")).thenReturn(user);

        // step 1: custom script validates the password via authenticate()
        AuthenticationService firstStepRequest = newService();
        assertTrue(firstStepRequest.authenticate("testuser"));

        verify(metricService).incCounter(MetricType.USER_AUTHENTICATION_SUCCESS);
        verify(metricService).incCounter(MetricType.USER_AUTHENTICATION_SUCCESS, "twilio_sms");
        assertEquals(Boolean.TRUE.toString(),
                session.getSessionAttributes().get(AuthenticationService.AUTH_METRIC_SUCCESS_REPORTED));

        // step 2 (new request): OTP check fails, Authenticator reports flow outcome
        AuthenticationService secondStepRequest = newService();
        secondStepRequest.incUserAuthenticationMetricIfNotReported(false);

        verify(metricService, never()).incCounter(MetricType.USER_AUTHENTICATION_FAILURES);
    }

    private AuthenticationService newService() {
        AuthenticationService service = new AuthenticationService();
        setField(service, "log", log);
        setField(service, "metricService", metricService);
        setField(service, "sessionIdService", sessionIdService);
        setField(service, "userService", userService);
        setField(service, "credentials", credentials);
        setField(service, "identity", identity);
        setField(service, "appConfiguration", appConfiguration);
        setField(service, "authenticationProtectionService", authenticationProtectionService);
        return service;
    }

    private static SessionId newSession() {
        SessionId sessionId = new SessionId();
        sessionId.setSessionAttributes(new HashMap<>());
        return sessionId;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = AuthenticationService.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to inject field: " + name, e);
        }
    }
}
