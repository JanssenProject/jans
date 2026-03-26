package io.jans.as.server.authzen.ws.rs;

import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.LocalResponseCache;
import io.jans.as.server.service.external.ExternalAccessEvaluationDiscoveryService;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static io.jans.as.model.configuration.ConfigurationResponseClaim.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * Tests for AccessEvaluationDiscoveryService.
 *
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class AccessEvaluationDiscoveryServiceTest {

    @InjectMocks
    private AccessEvaluationDiscoveryService accessEvaluationDiscoveryService;

    @Mock
    private Logger log;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private LocalResponseCache localResponseCache;

    @Mock
    private ExternalAccessEvaluationDiscoveryService externalAccessEvaluationDiscoveryService;

    @BeforeMethod
    public void setUp() {
        when(localResponseCache.getAccessEvaluationDiscoveryResponse()).thenReturn(null);
    }

    @Test
    public void discovery_shouldContainPolicyDecisionPoint() {
        when(externalAccessEvaluationDiscoveryService.modifyDiscovery(any(), any())).thenReturn(true);
        when(appConfiguration.isFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION)).thenReturn(true);
        when(appConfiguration.getIssuer()).thenReturn("https://example.com");
        when(appConfiguration.getEndSessionEndpoint()).thenReturn("https://example.com/jans-auth/restv1/end_session");

        ExecutionContext context = new ExecutionContext(null, null);
        JSONObject response = accessEvaluationDiscoveryService.discovery(context);

        assertEquals(response.getString(AUTHZEN_POLICY_DECISION_POINT), "https://example.com");
    }

    @Test
    public void discovery_shouldContainAccessEvaluationEndpoint() {
        when(externalAccessEvaluationDiscoveryService.modifyDiscovery(any(), any())).thenReturn(true);
        when(appConfiguration.isFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION)).thenReturn(true);
        when(appConfiguration.getEndSessionEndpoint()).thenReturn("https://example.com/jans-auth/restv1/end_session");

        ExecutionContext context = new ExecutionContext(null, null);
        JSONObject response = accessEvaluationDiscoveryService.discovery(context);

        assertTrue(response.has(AUTHZEN_ACCESS_EVALUATION_ENDPOINT));
        assertTrue(response.getString(AUTHZEN_ACCESS_EVALUATION_ENDPOINT).endsWith("/evaluation"));
    }

    @Test
    public void discovery_shouldContainAccessEvaluationsEndpoint() {
        when(appConfiguration.isFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION)).thenReturn(true);
        when(externalAccessEvaluationDiscoveryService.modifyDiscovery(any(), any())).thenReturn(true);

        ExecutionContext context = new ExecutionContext(null, null);
        JSONObject response = accessEvaluationDiscoveryService.discovery(context);

        assertTrue(response.has(AUTHZEN_ACCESS_EVALUATIONS_ENDPOINT));
        assertTrue(response.getString(AUTHZEN_ACCESS_EVALUATIONS_ENDPOINT).endsWith("/evaluations"));
    }

    @Test
    public void discovery_shouldContainSearchSubjectEndpoint() {
        when(appConfiguration.isFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION)).thenReturn(true);
        when(externalAccessEvaluationDiscoveryService.modifyDiscovery(any(), any())).thenReturn(true);

        ExecutionContext context = new ExecutionContext(null, null);
        JSONObject response = accessEvaluationDiscoveryService.discovery(context);

        assertTrue(response.has(AUTHZEN_SEARCH_SUBJECT_ENDPOINT));
        assertTrue(response.getString(AUTHZEN_SEARCH_SUBJECT_ENDPOINT).endsWith("/search/subject"));
    }

    @Test
    public void discovery_shouldContainSearchResourceEndpoint() {
        when(appConfiguration.isFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION)).thenReturn(true);
        when(externalAccessEvaluationDiscoveryService.modifyDiscovery(any(), any())).thenReturn(true);

        ExecutionContext context = new ExecutionContext(null, null);
        JSONObject response = accessEvaluationDiscoveryService.discovery(context);

        assertTrue(response.has(AUTHZEN_SEARCH_RESOURCE_ENDPOINT));
        assertTrue(response.getString(AUTHZEN_SEARCH_RESOURCE_ENDPOINT).endsWith("/search/resource"));
    }

    @Test
    public void discovery_shouldContainSearchActionEndpoint() {
        when(appConfiguration.isFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION)).thenReturn(true);
        when(externalAccessEvaluationDiscoveryService.modifyDiscovery(any(), any())).thenReturn(true);

        ExecutionContext context = new ExecutionContext(null, null);
        JSONObject response = accessEvaluationDiscoveryService.discovery(context);

        assertTrue(response.has(AUTHZEN_SEARCH_ACTION_ENDPOINT));
        assertTrue(response.getString(AUTHZEN_SEARCH_ACTION_ENDPOINT).endsWith("/search/action"));
    }

    @Test
    public void discovery_shouldContainCapabilities() {
        when(appConfiguration.isFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION)).thenReturn(true);
        when(externalAccessEvaluationDiscoveryService.modifyDiscovery(any(), any())).thenReturn(true);

        ExecutionContext context = new ExecutionContext(null, null);
        JSONObject response = accessEvaluationDiscoveryService.discovery(context);

        assertTrue(response.has(AUTHZEN_CAPABILITIES));
        assertNotNull(response.getJSONArray(AUTHZEN_CAPABILITIES));
    }

    @Test
    public void discovery_shouldContainLegacyEndpointForBackwardCompatibility() {
        when(appConfiguration.isFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION)).thenReturn(true);
        when(externalAccessEvaluationDiscoveryService.modifyDiscovery(any(), any())).thenReturn(true);

        ExecutionContext context = new ExecutionContext(null, null);
        JSONObject response = accessEvaluationDiscoveryService.discovery(context);

        assertTrue(response.has(ACCESS_EVALUATION_V1_ENDPOINT));
    }

    @Test
    public void discovery_whenFeatureDisabled_shouldReturnEmptyObject() {
        when(appConfiguration.isFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION)).thenReturn(false);

        ExecutionContext context = new ExecutionContext(null, null);
        JSONObject response = accessEvaluationDiscoveryService.discovery(context);

        assertFalse(response.has(AUTHZEN_POLICY_DECISION_POINT));
        assertFalse(response.has(AUTHZEN_ACCESS_EVALUATION_ENDPOINT));
    }

    @Test
    public void discovery_whenCached_shouldReturnCachedResponse() {
        JSONObject cachedResponse = new JSONObject();
        cachedResponse.put("cached", true);
        when(localResponseCache.getAccessEvaluationDiscoveryResponse()).thenReturn(cachedResponse);

        ExecutionContext context = new ExecutionContext(null, null);
        JSONObject response = accessEvaluationDiscoveryService.discovery(context);

        assertTrue(response.has("cached"));
        assertTrue(response.getBoolean("cached"));
    }
}
