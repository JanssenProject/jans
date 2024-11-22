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
    void testValidateWebhookEntry_BlockedUrl() {
        // given
        List<String> blockedUrls = Arrays.asList("http://blocked-url.com", "https://malicious-site.org");
        when(configurationFactory.getApiAppConfiguration()).thenReturn(apiAppConfiguration);
        when(apiAppConfiguration.getBlockedUrls()).thenReturn(blockedUrls);

        WebhookEntry webhookEntry = new WebhookEntry();
        webhookEntry.setDisplayName("Test Webhook");
        webhookEntry.setUrl("http://blocked-url.com");
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
        List<String> blockedUrls = Arrays.asList("https://blocked-url.com", "https://malicious-site.org");
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
        List<String> blockedUrls = Arrays.asList("https://blocked-url.com", "https://malicious-site.org");
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
