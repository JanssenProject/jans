package io.jans.ca.plugin.adminui.service.webhook;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.model.webhook.WebhookEntry;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.model.configuration.ApiAppConfiguration;

class WebhookServiceTest {

    @InjectMocks
    private WebhookService webhookService;

    @Mock
    private ConfigurationFactory configurationFactory;

    @Mock
    private ApiAppConfiguration apiAppConfiguration;

    @Mock
    private Logger log;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testBlockedUrlsPatterns() {
        // given
        List<String> blockedUrls = Arrays.asList(
                "localhost",
                "127.0.",
                "192.168.",
                "172."
        );

        when(configurationFactory.getApiAppConfiguration()).thenReturn(apiAppConfiguration);
        when(apiAppConfiguration.getBlockedUrls()).thenReturn(blockedUrls);

        List<String> testUrls = Arrays.asList(
                "https://localhost:8080",       // blocked: contains localhost
                "https://127.0.0.1/resource",   // blocked: contains 127.0.
                "https://192.168.1.1/login",    // blocked: contains 192.168.
                "https://192.168.1.1/login/resource",    // blocked: contains 192.168.
                "https://172.16.0.1/dashboard", // blocked: contains 172.
                "https://17.127.12.127.0/resource",   // not blocked: safe URL
                "https://12.127.0/resource",   // not blocked: safe URL
                "https://login/192.168.1.1",    // not blocked: safe URL
                "https://127.192.168.1.1/login/resource",    // not blocked: safe URL
                "https://safe-url.com"         // not blocked: safe URL
        );

        List<Boolean> expectedResults = Arrays.asList(
                true, true, true, true, true, false, false, false, false, false
        );

        for (int i = 0; i < testUrls.size(); i++) {
            String testUrl = testUrls.get(i);
            boolean expected = expectedResults.get(i);

            WebhookEntry webhookEntry = new WebhookEntry();
            webhookEntry.setDisplayName("Test Webhook");
            webhookEntry.setUrl(testUrl);
            webhookEntry.setHttpMethod("GET");

            if (expected) {
                // when & then
                ApplicationException exception = assertThrows(ApplicationException.class,
                        () -> webhookService.validateWebhookEntry(webhookEntry),
                        "Expected ApplicationException for URL: " + testUrl
                );
                assertEquals("The provided URL is blocked.", exception.getMessage());
            } else {
                // when & then
                assertDoesNotThrow(() -> webhookService.validateWebhookEntry(webhookEntry),
                        "Did not expect ApplicationException for URL: " + testUrl
                );
            }

        }
    }

    @Test
    void testValidateWebhookEntry_BlockedUrl() {
        // given
        List<String> blockedUrls = Arrays.asList(
                "http://example.com",
                "file:///etc/passwd",
                "http://localhost",
                "https://192.168.",
                "http://127.0.0.1/resource"
        );

        when(configurationFactory.getApiAppConfiguration()).thenReturn(apiAppConfiguration);
        when(apiAppConfiguration.getBlockedUrls()).thenReturn(blockedUrls);

        WebhookEntry webhookEntry = new WebhookEntry();
        webhookEntry.setDisplayName("Test Webhook");
        webhookEntry.setUrl("https://192.168.");
        webhookEntry.setHttpMethod("GET");

        // when & then
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> webhookService.validateWebhookEntry(webhookEntry));

        assertEquals("The provided URL is blocked.", exception.getMessage());
        verify(log).error(ErrorResponse.WEBHOOK_URL_BLOCKED.getDescription());
    }

    @Test
    void testValidateWebhookEntry_BlockedProtocol() {
        // given
        List<String> blockedUrls = Arrays.asList(
                "http://example.com",
                "file:///etc/passwd",
                "http://localhost",
                "https://192.168.",
                "http://127.0.0.1/resource"
        );
        when(configurationFactory.getApiAppConfiguration()).thenReturn(apiAppConfiguration);
        when(apiAppConfiguration.getBlockedUrls()).thenReturn(blockedUrls);

        WebhookEntry webhookEntry = new WebhookEntry();
        webhookEntry.setDisplayName("Test Webhook");
        webhookEntry.setUrl("http://safe-url.com");
        webhookEntry.setHttpMethod("GET");

        // when & then
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> webhookService.validateWebhookEntry(webhookEntry));

        assertEquals("Webhook URL must start with 'https://.", exception.getMessage());
        verify(log, times(1)).error(ErrorResponse.WEBHOOK_URL_PREFIX.getDescription());
    }

    @Test
    void testValidateWebhookEntry_AllowedUrl() {
        // given
        List<String> blockedUrls = Arrays.asList(
                "http://example.com",
                "file:///etc/passwd",
                "http://localhost",
                "https://192.168.",
                "http://127.0.0.1/resource"
        );
        when(configurationFactory.getApiAppConfiguration()).thenReturn(apiAppConfiguration);
        when(apiAppConfiguration.getBlockedUrls()).thenReturn(blockedUrls);

        WebhookEntry webhookEntry = new WebhookEntry();
        webhookEntry.setDisplayName("Test Webhook");
        webhookEntry.setUrl("https://safe-url.com");
        webhookEntry.setHttpMethod("GET");

        // when & then
        assertDoesNotThrow(() -> webhookService.validateWebhookEntry(webhookEntry));
        verify(log, never()).error(ErrorResponse.WEBHOOK_URL_BLOCKED.getDescription());
    }
}
