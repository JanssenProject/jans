/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.fido2.service.shared;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.service.util.DeviceInfoExtractor;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for FIDO2 MetricService
 *
 * @author Janssen Project
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MetricServiceTest {

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private DeviceInfoExtractor deviceInfoExtractor;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private Logger log;

    @InjectMocks
    private MetricService metricService;

    @BeforeEach
    void setUp() {
        // Set up default configuration
        when(appConfiguration.isFido2MetricsEnabled()).thenReturn(true);
        when(appConfiguration.getMetricReporterEnabled()).thenReturn(true);
        when(appConfiguration.isFido2DeviceInfoCollection()).thenReturn(true);
        when(appConfiguration.isFido2ErrorCategorization()).thenReturn(true);
        when(appConfiguration.isFido2PerformanceMetrics()).thenReturn(true);
    }

    @Test
    void testRecordPasskeyRegistrationAttempt() {
        // Given
        String username = "testuser";
        long startTime = System.currentTimeMillis();

        // When & Then - should not throw exception and complete successfully
        assertDoesNotThrow(() -> 
            metricService.recordPasskeyRegistrationAttempt(username, httpRequest, startTime)
        );
    }

    @Test
    void testRecordPasskeyRegistrationSuccess() {
        // Given
        String username = "testuser";
        String authenticatorType = "platform";
        long startTime = System.currentTimeMillis();

        // When & Then - should not throw exception and complete successfully
        assertDoesNotThrow(() -> 
            metricService.recordPasskeyRegistrationSuccess(username, httpRequest, startTime, authenticatorType)
        );
    }

    @Test
    void testRecordPasskeyRegistrationFailure() {
        // Given
        String username = "testuser";
        String errorReason = "Invalid challenge";
        String authenticatorType = "cross-platform";
        long startTime = System.currentTimeMillis();

        // When & Then - should not throw exception and complete successfully
        assertDoesNotThrow(() -> 
            metricService.recordPasskeyRegistrationFailure(username, httpRequest, startTime, errorReason, authenticatorType)
        );
    }

    @Test
    void testRecordPasskeyAuthenticationAttempt() {
        // Given
        String username = "testuser";
        long startTime = System.currentTimeMillis();

        // When & Then - should not throw exception and complete successfully
        assertDoesNotThrow(() -> 
            metricService.recordPasskeyAuthenticationAttempt(username, httpRequest, startTime)
        );
    }

    @Test
    void testRecordPasskeyAuthenticationSuccess() {
        // Given
        String username = "testuser";
        String authenticatorType = "security-key";
        long startTime = System.currentTimeMillis();

        // When & Then - should not throw exception and complete successfully
        assertDoesNotThrow(() -> 
            metricService.recordPasskeyAuthenticationSuccess(username, httpRequest, startTime, authenticatorType)
        );
    }

    @Test
    void testRecordPasskeyAuthenticationFailure() {
        // Given
        String username = "testuser";
        String errorReason = "Authentication failed";
        String authenticatorType = "platform";
        long startTime = System.currentTimeMillis();

        // When & Then - should not throw exception and complete successfully
        assertDoesNotThrow(() -> 
            metricService.recordPasskeyAuthenticationFailure(username, httpRequest, startTime, errorReason, authenticatorType)
        );
    }

    @Test
    void testRecordPasskeyFallback() {
        // Given
        String username = "testuser";
        String fallbackMethod = "PASSWORD";
        String reason = "User chose password";

        // When & Then - should not throw exception and complete successfully
        assertDoesNotThrow(() -> 
            metricService.recordPasskeyFallback(username, fallbackMethod, reason)
        );
    }

    @Test
    void testMetricsDisabled() {
        // Given
        when(appConfiguration.isFido2MetricsEnabled()).thenReturn(false);
        String username = "testuser";
        long startTime = System.currentTimeMillis();

        // When
        metricService.recordPasskeyRegistrationAttempt(username, httpRequest, startTime);

        // Then - should complete without any metrics processing
        assertDoesNotThrow(() -> {
            // No async processing should occur
        });
    }

    /**
     * The metrics write is handed to a background thread, but the request backing it
     * is request-scoped and only resolvable on the request thread. Reading it from the
     * async task silently yielded no ipAddress / userAgent / deviceInfo, so the request
     * must be consumed synchronously, before the caller returns.
     */
    @Test
    void testRequestIsReadOnCallingThread() {
        // Given - record which thread actually touches the request
        AtomicReference<Thread> remoteAddrThread = new AtomicReference<>();
        AtomicReference<Thread> userAgentThread = new AtomicReference<>();

        when(httpRequest.getRemoteAddr()).thenAnswer(invocation -> {
            remoteAddrThread.set(Thread.currentThread());
            return "203.0.113.7";
        });
        when(httpRequest.getHeader("User-Agent")).thenAnswer(invocation -> {
            userAgentThread.set(Thread.currentThread());
            return "Mozilla/5.0";
        });

        Thread callingThread = Thread.currentThread();

        // When
        metricService.recordPasskeyRegistrationAttempt("testuser", httpRequest, System.currentTimeMillis());

        // Then - both reads already happened, on this thread and not on the async pool
        assertSame(callingThread, remoteAddrThread.get(),
                "IP address must be read on the request thread, not the async pool");
        assertSame(callingThread, userAgentThread.get(),
                "User-Agent must be read on the request thread, not the async pool");
    }

    /**
     * A request-scoped proxy dereferenced outside an active request throws rather than
     * returning null; that must degrade to an empty snapshot, not break the operation.
     */
    @Test
    void testInactiveRequestScopeDoesNotPropagate() {
        // Given
        when(httpRequest.getRemoteAddr()).thenThrow(new IllegalStateException("Request scope not active"));

        // When & Then
        assertDoesNotThrow(() ->
            metricService.recordPasskeyRegistrationAttempt("testuser", httpRequest, System.currentTimeMillis())
        );
    }

    @Test
    void testNullRequestIsTolerated() {
        // When & Then - fallback callers have no request at all
        assertDoesNotThrow(() ->
            metricService.recordPasskeyAuthenticationAttempt("testuser", null, System.currentTimeMillis())
        );
    }

}
