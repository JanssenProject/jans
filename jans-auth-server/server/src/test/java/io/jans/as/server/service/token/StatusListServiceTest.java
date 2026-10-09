package io.jans.as.server.service.token;

import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.DiscoveryService;
import io.jans.as.server.service.cluster.StatusIndexPoolService;
import jakarta.ws.rs.WebApplicationException;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

@Listeners(MockitoTestNGListener.class)
public class StatusListServiceTest {

    @InjectMocks
    private StatusListService statusListService;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private DiscoveryService discoveryService;

    @Mock
    private StatusIndexPoolService statusTokenPoolService;

    @Mock
    private WebKeysConfiguration webKeysConfiguration;

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateTime_whenTimeIsNotEmpty_shouldThrowException() {
        statusListService.validateTime("123");
    }

    @Test
    public void validateTime_whenTimeIsEmpty_shouldPass() {
        statusListService.validateTime(null);
    }

    @Test
    public void addStatusClaimWithIndex_whenFeatureFlagDisabled_shouldSkip() {
        when(errorResponseFactory.isFeatureFlagEnabled(FeatureFlagType.STATUS_LIST)).thenReturn(false);

        JwtClaims claims = new JwtClaims();
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setStatusListIndex(5);

        statusListService.addStatusClaimWithIndex(claims, executionContext);

        assertNull(claims.getClaim("status"));
    }

    @Test
    public void addStatusClaimWithIndex_whenIndexIsNull_shouldSkip() {
        when(errorResponseFactory.isFeatureFlagEnabled(FeatureFlagType.STATUS_LIST)).thenReturn(true);

        JwtClaims claims = new JwtClaims();
        ExecutionContext executionContext = new ExecutionContext();
        // index is null by default

        statusListService.addStatusClaimWithIndex(claims, executionContext);

        assertNull(claims.getClaim("status"));
    }

    @Test
    public void addStatusClaimWithIndex_whenIndexIsNegative_shouldSkip() {
        when(errorResponseFactory.isFeatureFlagEnabled(FeatureFlagType.STATUS_LIST)).thenReturn(true);

        JwtClaims claims = new JwtClaims();
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setStatusListIndex(-1);

        statusListService.addStatusClaimWithIndex(claims, executionContext);

        assertNull(claims.getClaim("status"));
    }

    @Test
    public void addStatusClaimWithIndex_whenValidIndex_shouldSetStatusClaim() {
        when(errorResponseFactory.isFeatureFlagEnabled(FeatureFlagType.STATUS_LIST)).thenReturn(true);
        when(discoveryService.getStatusListEndpoint()).thenReturn("https://example.com/status");

        JwtClaims claims = new JwtClaims();
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setStatusListIndex(42);

        statusListService.addStatusClaimWithIndex(claims, executionContext);

        JSONObject statusClaim = claims.getClaimAsJSON("status");
        assertNotNull(statusClaim);
        assertTrue(statusClaim.has("status_list"));
        JSONObject statusList = statusClaim.getJSONObject("status_list");
        assertEquals(statusList.getInt("idx"), 42);
        assertEquals(statusList.getString("uri"), "https://example.com/status");
    }

    @Test
    public void addStatusClaimWithIndex_viaJsonWebResponseDelegate_shouldSetStatusClaim() {
        when(errorResponseFactory.isFeatureFlagEnabled(FeatureFlagType.STATUS_LIST)).thenReturn(true);
        when(discoveryService.getStatusListEndpoint()).thenReturn("https://example.com/status");

        JsonWebResponse jwr = new JsonWebResponse();
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setStatusListIndex(10);

        statusListService.addStatusClaimWithIndex(jwr, executionContext);

        JSONObject statusClaim = jwr.getClaims().getClaimAsJSON("status");
        assertNotNull(statusClaim);
        JSONObject statusList = statusClaim.getJSONObject("status_list");
        assertEquals(statusList.getInt("idx"), 10);
        assertEquals(statusList.getString("uri"), "https://example.com/status");
    }

    @Test
    public void addStatusClaimWithIndex_whenIndexIsZero_shouldSetStatusClaim() {
        when(errorResponseFactory.isFeatureFlagEnabled(FeatureFlagType.STATUS_LIST)).thenReturn(true);
        when(discoveryService.getStatusListEndpoint()).thenReturn("https://example.com/status");

        JwtClaims claims = new JwtClaims();
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setStatusListIndex(0);

        statusListService.addStatusClaimWithIndex(claims, executionContext);

        JSONObject statusClaim = claims.getClaimAsJSON("status");
        assertNotNull(statusClaim);
        assertEquals(statusClaim.getJSONObject("status_list").getInt("idx"), 0);
    }
}
