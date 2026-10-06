package io.jans.as.server.discovery.ws.rs;

import io.jans.as.server.service.DiscoveryService;
import io.jans.as.server.service.LocalResponseCache;
import io.jans.as.server.service.external.ExternalDiscoveryService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Tests for OAuthAuthorizationServerMetadataWS.
 *
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class OAuthAuthorizationServerMetadataWSTest {

    @InjectMocks
    private OAuthAuthorizationServerMetadataWS oAuthAuthorizationServerMetadataWS;

    @Mock
    private Logger log;

    @Mock
    private DiscoveryService discoveryService;

    @Mock
    private ExternalDiscoveryService externalDiscoveryService;

    @Mock
    private LocalResponseCache localResponseCache;

    @BeforeMethod
    public void setUp() {
        when(localResponseCache.getDiscoveryResponse()).thenReturn(null);
    }

    @Test
    public void getMetadata_whenScriptModificationAccepted_shouldReturnModifiedResponse() {
        JSONObject discoveryResponse = new JSONObject();
        discoveryResponse.put("issuer", "https://example.com");
        when(discoveryService.process()).thenReturn(discoveryResponse);
        when(externalDiscoveryService.modifyDiscovery(any(), any())).thenAnswer(invocation -> {
            invocation.<JSONObject>getArgument(0).put("scripted", true);
            return true;
        });

        Response response = oAuthAuthorizationServerMetadataWS.getMetadata(null, null);
        JSONObject responseJson = new JSONObject(response.getEntity().toString());

        assertEquals(response.getStatus(), 200);
        assertTrue(responseJson.getBoolean("scripted"));
        assertEquals(responseJson.getString("issuer"), "https://example.com");
    }

    @Test
    public void getMetadata_whenCached_shouldReturnCachedResponse() {
        JSONObject cachedResponse = new JSONObject();
        cachedResponse.put("issuer", "https://cached.example.com");
        when(localResponseCache.getDiscoveryResponse()).thenReturn(cachedResponse);

        Response response = oAuthAuthorizationServerMetadataWS.getMetadata(null, null);

        assertEquals(response.getStatus(), 200);
        assertTrue(response.getEntity().toString().contains("https://cached.example.com"));
    }

    @Test
    public void getMetadata_whenScriptModificationRejected_shouldRevertToOriginal() {
        JSONObject discoveryResponse = new JSONObject();
        discoveryResponse.put("issuer", "https://example.com");
        when(discoveryService.process()).thenReturn(discoveryResponse);
        when(externalDiscoveryService.modifyDiscovery(any(), any())).thenAnswer(invocation -> {
            invocation.<JSONObject>getArgument(0).put("scripted", true);
            return false;
        });

        Response response = oAuthAuthorizationServerMetadataWS.getMetadata(null, null);
        JSONObject responseJson = new JSONObject(response.getEntity().toString());

        assertEquals(response.getStatus(), 200);
        assertFalse(responseJson.has("scripted"));
        assertEquals(responseJson.getString("issuer"), "https://example.com");
    }

    @Test
    public void getMetadata_whenDiscoveryServiceThrows_shouldReturnServerErrorWithDescription() {
        when(discoveryService.process()).thenThrow(new RuntimeException("boom"));

        try {
            oAuthAuthorizationServerMetadataWS.getMetadata(null, null);
            fail("Expected WebApplicationException to be thrown");
        } catch (WebApplicationException ex) {
            final Response response = ex.getResponse();
            final JSONObject responseJson = new JSONObject(response.getEntity().toString());

            assertEquals(response.getStatus(), 500);
            assertEquals(responseJson.getString("error"), "server_error");
            assertTrue(responseJson.has("error_description"));
            assertFalse(responseJson.getString("error_description").isEmpty());
        }
    }
}
