package io.jans.as.server.service;

import io.jans.as.model.configuration.AppConfiguration;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class LocalResponseCacheTest {

    @InjectMocks
    private LocalResponseCache localResponseCache;

    @Mock
    private AppConfiguration appConfiguration;

    @Test
    public void invalidateDiscoveryCache_whenCalled_shouldInvalidateCache() {
        localResponseCache.putDiscoveryResponse(new JSONObject());
        assertNotNull(localResponseCache.getDiscoveryResponse());

        localResponseCache.invalidateDiscoveryCache();
        assertNull(localResponseCache.getDiscoveryResponse());
    }

    @Test
    public void putDiscoveryResponse_whenCalled_shouldContainCache() {
        localResponseCache.putDiscoveryResponse(new JSONObject());
        assertNotNull(localResponseCache.getDiscoveryResponse());
    }

    @Test
    public void putAccessEvaluationDiscoveryResponse_whenCalled_shouldContainCache() {
        localResponseCache.putAccessEvaluationDiscoveryResponse(new JSONObject());
        assertNotNull(localResponseCache.getAccessEvaluationDiscoveryResponse());
    }
}
