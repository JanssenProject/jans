package io.jans.as.server.discovery.ws.rs;

import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.service.DiscoveryService;
import io.jans.as.server.service.LocalResponseCache;
import io.jans.as.server.service.external.ExternalDiscoveryService;
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
import static org.testng.Assert.assertTrue;

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
    private ErrorResponseFactory errorResponseFactory;

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
    public void getMetadata_whenNotCached_shouldReturnDiscoveryServiceResponse() {
        JSONObject discoveryResponse = new JSONObject();
        discoveryResponse.put("issuer", "https://example.com");
        when(discoveryService.process()).thenReturn(discoveryResponse);
        when(externalDiscoveryService.modifyDiscovery(any(), any())).thenReturn(true);

        Response response = oAuthAuthorizationServerMetadataWS.getMetadata(null, null);

        assertEquals(response.getStatus(), 200);
        assertTrue(response.getEntity().toString().contains("https://example.com"));
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
        when(externalDiscoveryService.modifyDiscovery(any(), any())).thenReturn(false);

        Response response = oAuthAuthorizationServerMetadataWS.getMetadata(null, null);

        assertEquals(response.getStatus(), 200);
        assertTrue(response.getEntity().toString().contains("https://example.com"));
    }
}
