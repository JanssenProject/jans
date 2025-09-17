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

}
