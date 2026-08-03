/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.fido2.service.metric;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.metric.Fido2MetricsConstants;
import io.jans.fido2.model.metric.Fido2UserMetrics;
import io.jans.fido2.model.metric.UserMetricsUpdateRequest;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for Fido2UserMetricsService field-length handling.
 *
 * <p>This is the second write path for the same browser user agent, so it
 * overflows its column exactly like the metrics entry table does. Truncation is
 * applied in saveUserMetrics so every update flow is covered by one guard.
 *
 * @author Janssen Project
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Fido2UserMetricsServiceTest {

    private static final String REAL_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private PersistenceEntryManager persistenceEntryManager;

    @InjectMocks
    private Fido2UserMetricsService fido2UserMetricsService;

    @BeforeEach
    void setUp() {
        when(appConfiguration.isFido2MetricsEnabled()).thenReturn(true);
        // No existing entry, so every flow takes the "create new" branch
        when(persistenceEntryManager.findEntries(anyString(), eq(Fido2UserMetrics.class), any(Filter.class)))
                .thenReturn(Collections.emptyList());
    }

    @Test
    void testOversizedUserAgentIsTruncatedOnRegistration() {
        // Given
        UserMetricsUpdateRequest request = baseRequest();
        request.setUserAgent("u".repeat(2000));

        // When
        fido2UserMetricsService.updateUserRegistrationMetrics(request);

        // Then
        Fido2UserMetrics saved = capturePersistedUserMetrics();
        assertEquals(Fido2MetricsConstants.MAX_LENGTH_USER_AGENT, saved.getLastUserAgent().length(),
                "user agent must be cut to the column width instead of failing the write");
    }

    @Test
    void testOversizedUserAgentIsTruncatedOnAuthentication() {
        // Given
        UserMetricsUpdateRequest request = baseRequest();
        request.setUserAgent("u".repeat(2000));

        // When
        fido2UserMetricsService.updateUserAuthenticationMetrics(request);

        // Then
        Fido2UserMetrics saved = capturePersistedUserMetrics();
        assertEquals(Fido2MetricsConstants.MAX_LENGTH_USER_AGENT, saved.getLastUserAgent().length());
    }

    @Test
    void testOversizedUserAgentIsTruncatedOnFallback() {
        // When
        fido2UserMetricsService.updateUserFallbackMetrics("inum-1234", "testuser", "203.0.113.7",
                "u".repeat(2000));

        // Then
        Fido2UserMetrics saved = capturePersistedUserMetrics();
        assertEquals(Fido2MetricsConstants.MAX_LENGTH_USER_AGENT, saved.getLastUserAgent().length());
    }

    /**
     * The preferred-value fields are copied straight from the device info parsed off
     * the request, so a malformed user agent can push a long value into them.
     */
    @Test
    void testOversizedPreferredValuesAreTruncated() {
        // Given
        UserMetricsUpdateRequest request = baseRequest();
        request.setSuccess(true);
        request.setAuthenticatorType("a".repeat(300));
        request.setBrowser("b".repeat(300));
        request.setOs("o".repeat(300));
        request.setDeviceType("d".repeat(300));

        // When
        fido2UserMetricsService.updateUserRegistrationMetrics(request);

        // Then
        Fido2UserMetrics saved = capturePersistedUserMetrics();
        assertEquals(Fido2MetricsConstants.MAX_LENGTH_AUTHENTICATOR_TYPE,
                saved.getPreferredAuthenticatorType().length());
        assertEquals(Fido2MetricsConstants.MAX_LENGTH_DEVICE_INFO_FIELD, saved.getPreferredBrowser().length());
        assertEquals(Fido2MetricsConstants.MAX_LENGTH_DEVICE_INFO_FIELD, saved.getPreferredOs().length());
        assertEquals(Fido2MetricsConstants.MAX_LENGTH_DEVICE_INFO_FIELD, saved.getPreferredDeviceType().length());
    }

    @Test
    void testOversizedUsernameIsTruncated() {
        // Given: on a failed attempt the username is unvalidated client input
        UserMetricsUpdateRequest request = new UserMetricsUpdateRequest("inum-1234", "n".repeat(900), false);
        request.setUserAgent(REAL_USER_AGENT);

        // When
        fido2UserMetricsService.updateUserRegistrationMetrics(request);

        // Then
        Fido2UserMetrics saved = capturePersistedUserMetrics();
        assertEquals(Fido2MetricsConstants.MAX_LENGTH_USERNAME, saved.getUsername().length());
    }

    /**
     * The regression guard: a real user agent fits the widened column and must reach
     * the database intact, otherwise the schema change bought nothing.
     */
    @Test
    void testTypicalUserAgentIsStoredIntact() {
        // Given
        UserMetricsUpdateRequest request = baseRequest();
        request.setUserAgent(REAL_USER_AGENT);

        // When
        fido2UserMetricsService.updateUserRegistrationMetrics(request);

        // Then
        Fido2UserMetrics saved = capturePersistedUserMetrics();
        assertEquals(REAL_USER_AGENT, saved.getLastUserAgent());
        verify(log, never()).warn(anyString(), any(), any());
    }

    @Test
    void testWarningIsLoggedOncePerWriteRegardlessOfFieldCount() {
        // Given
        UserMetricsUpdateRequest request = new UserMetricsUpdateRequest("inum-1234", "n".repeat(900), true);
        request.setUserAgent("u".repeat(2000));
        request.setBrowser("b".repeat(300));

        // When
        fido2UserMetricsService.updateUserRegistrationMetrics(request);
        capturePersistedUserMetrics();

        // Then
        verify(log).warn(anyString(), any(), any());
    }

    /**
     * The merge path re-saves an entity that was already shortened on a previous
     * write. Re-truncating a value that already fits must be a no-op, so a returning
     * user does not produce a warning on every single request.
     */
    @Test
    void testTruncationIsIdempotentOnAnAlreadyShortenedEntity() {
        // Given: an existing entry whose user agent was already cut to the limit
        Fido2UserMetrics existing = new Fido2UserMetrics("inum-1234", "testuser");
        existing.setDn("jansId=existing,ou=fido2-user-metrics,o=jans");
        existing.setLastUserAgent("u".repeat(Fido2MetricsConstants.MAX_LENGTH_USER_AGENT));
        when(persistenceEntryManager.findEntries(anyString(), eq(Fido2UserMetrics.class), any(Filter.class)))
                .thenReturn(Collections.singletonList(existing));

        UserMetricsUpdateRequest request = baseRequest();
        request.setUserAgent("u".repeat(Fido2MetricsConstants.MAX_LENGTH_USER_AGENT));

        // When
        fido2UserMetricsService.updateUserRegistrationMetrics(request);

        // Then
        ArgumentCaptor<Fido2UserMetrics> captor = ArgumentCaptor.forClass(Fido2UserMetrics.class);
        verify(persistenceEntryManager, timeout(5000)).merge(captor.capture());
        assertEquals(Fido2MetricsConstants.MAX_LENGTH_USER_AGENT, captor.getValue().getLastUserAgent().length());
        verify(log, never()).warn(anyString(), any(), any());
    }

    /**
     * Writes are asynchronous, so wait for the persist and hand back what was stored.
     */
    private Fido2UserMetrics capturePersistedUserMetrics() {
        ArgumentCaptor<Fido2UserMetrics> captor = ArgumentCaptor.forClass(Fido2UserMetrics.class);
        verify(persistenceEntryManager, timeout(5000)).persist(captor.capture());
        return captor.getValue();
    }

    private UserMetricsUpdateRequest baseRequest() {
        UserMetricsUpdateRequest request = new UserMetricsUpdateRequest("inum-1234", "testuser", true);
        request.setIpAddress("203.0.113.7");
        return request;
    }
}
