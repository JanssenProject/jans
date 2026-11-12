package io.jans.ca.plugin.adminui.test.services;

import io.jans.ca.plugin.adminui.model.auth.GenericResponse;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.model.webhook.ShortCodeRequest;
import io.jans.ca.plugin.adminui.model.webhook.WebhookEntry;
import io.jans.ca.plugin.adminui.service.webhook.WebhookService;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import jakarta.ws.rs.core.Response;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class WebhookServiceTest {

    @Mock
    private Logger log;

    @Mock
    private PersistenceEntryManager entryManager;

    @InjectMocks
    private WebhookService webhookService;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testTriggerEnabledWebhooksContinuesAfterInvalidShortcodeUrl() throws Exception {
        AtomicInteger validWebhookCalls = new AtomicInteger();
        AtomicInteger allWebhookCalls = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            allWebhookCalls.incrementAndGet();
            if ("/trigger".equals(exchange.getRequestURI().getPath())) {
                validWebhookCalls.incrementAndGet();
            }
            byte[] response = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (java.io.OutputStream output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            WebhookEntry invalidWebhook = createWebhook(
                    "invalid-id", "invalid-webhook", "http://${host}/legacy", true);
            WebhookEntry unmatchedShortcodeWebhook = createWebhook(
                    "unmatched-id", "unmatched-webhook", "http://${host}/legacy", true);
            WebhookEntry validWebhook = createWebhook(
                    "valid-id", "valid-webhook", "http://127.0.0.1:" + port + "/${path}", true);
            when(entryManager.findEntries(
                    eq(AppConstants.WEBHOOK_DN), eq(WebhookEntry.class), any(Filter.class)))
                    .thenReturn(List.of(invalidWebhook, unmatchedShortcodeWebhook, validWebhook));

            ShortCodeRequest invalidShortcode = createShortcode(
                    "invalid-id", Map.of("host", "127.0.0.1:" + port));
            ShortCodeRequest validShortcode = createShortcode("valid-id", Map.of("path", "trigger"));
            List<GenericResponse> responses = webhookService.triggerEnabledWebhooks(
                    Set.of("invalid-id", "unmatched-id", "valid-id"), List.of(invalidShortcode, validShortcode));

            assertEquals(responses.size(), 3);
            assertEquals(allWebhookCalls.get(), 1, "Only the valid webhook should reach the server");
            assertEquals(validWebhookCalls.get(), 1);
            GenericResponse invalidResponse = responses.stream()
                    .filter(response -> "invalid-id".equals(
                            response.getResponseObject().path("webhookId").asText()))
                    .findFirst()
                    .orElseThrow();
            assertFalse(invalidResponse.isSuccess());
            assertEquals(invalidResponse.getResponseCode(), 400);
            assertEquals(invalidResponse.getResponseObject().path("webhookName").asText(), "invalid-webhook");
            assertEquals(invalidResponse.getResponseObject().path("webhookMethod").asText(), "GET");

            GenericResponse unmatchedShortcodeResponse = responses.stream()
                    .filter(response -> "unmatched-id".equals(
                            response.getResponseObject().path("webhookId").asText()))
                    .findFirst()
                    .orElseThrow();
            assertFalse(unmatchedShortcodeResponse.isSuccess());
            assertEquals(unmatchedShortcodeResponse.getResponseCode(), 400);
            assertEquals(unmatchedShortcodeResponse.getResponseObject().path("webhookName").asText(), "unmatched-webhook");

            GenericResponse validResponse = responses.stream()
                    .filter(response -> "valid-id".equals(
                            response.getResponseObject().path("webhookId").asText()))
                    .findFirst()
                    .orElseThrow();
            assertTrue(validResponse.isSuccess());
            assertEquals(validResponse.getResponseCode(), 200);
        } finally {
            server.stop(0);
        }
    }

    @DataProvider(name = "invalidWebhookOrigins")
    public Object[][] invalidWebhookOrigins() {
        return new Object[][] {
                { "${scheme}://example.test/path" },
                { "https://${host}/path" }
        };
    }

    @Test(dataProvider = "invalidWebhookOrigins")
    public void testAddWebhookRejectsShortcodesInSchemeOrHostWithoutPersistence(String url) {
        WebhookEntry webhook = createWebhook(null, "invalid-webhook", url, true);

        ApplicationException exception = org.testng.Assert.expectThrows(
                ApplicationException.class, () -> webhookService.addWebhook(webhook));

        assertEquals(exception.getErrorCode(), Response.Status.BAD_REQUEST.getStatusCode());
        assertEquals(exception.getMessage(), ErrorResponse.WEBHOOK_URL_ORIGIN_NOT_FIXED.getDescription());
        verify(entryManager, never()).persist(any());
        verify(entryManager, never()).merge(any());
    }

    @Test(dataProvider = "invalidWebhookOrigins")
    public void testUpdateWebhookRejectsShortcodesInSchemeOrHostWithoutPersistence(String url) {
        WebhookEntry webhook = createWebhook("existing-id", "invalid-webhook", url, true);

        ApplicationException exception = org.testng.Assert.expectThrows(
                ApplicationException.class, () -> webhookService.updateWebhook(webhook));

        assertEquals(exception.getErrorCode(), Response.Status.BAD_REQUEST.getStatusCode());
        assertEquals(exception.getMessage(), ErrorResponse.WEBHOOK_URL_ORIGIN_NOT_FIXED.getDescription());
        verify(entryManager, never()).persist(any());
        verify(entryManager, never()).merge(any());
    }

    @Test
    public void testUpdateWebhookRejectsMissingRecord() {
        WebhookEntry webhook = createWebhook("missing-id", "missing-webhook", "https://example.test/path", true);
        when(entryManager.find(eq(WebhookEntry.class), anyString())).thenReturn(null);

        ApplicationException exception = org.testng.Assert.expectThrows(
                ApplicationException.class, () -> webhookService.updateWebhook(webhook));

        assertEquals(exception.getErrorCode(), Response.Status.BAD_REQUEST.getStatusCode());
        assertEquals(exception.getMessage(), ErrorResponse.WEBHOOK_RECORD_NOT_EXIST.getDescription());
        verify(entryManager, never()).persist(any());
        verify(entryManager, never()).merge(any());
    }

    private WebhookEntry createWebhook(String id, String name, String url, boolean enabled) {
        WebhookEntry webhook = new WebhookEntry();
        webhook.setInum(id);
        webhook.setDisplayName(name);
        webhook.setUrl(url);
        webhook.setHttpMethod("GET");
        webhook.setJansEnabled(enabled);
        return webhook;
    }

    private ShortCodeRequest createShortcode(String webhookId, Map<String, Object> values) {
        ShortCodeRequest shortCode = new ShortCodeRequest();
        shortCode.setWebhookId(webhookId);
        shortCode.setShortcodeValueMap(values);
        return shortCode;
    }
}
