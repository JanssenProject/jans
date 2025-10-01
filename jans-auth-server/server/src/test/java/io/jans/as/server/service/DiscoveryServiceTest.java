package io.jans.as.server.service;

import io.jans.as.common.service.AttributeService;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.configuration.ConfigurationResponseClaim;
import io.jans.as.model.util.Util;
import io.jans.as.server.ciba.CIBAConfigurationService;
import io.jans.as.server.service.external.ExternalAuthenticationService;
import io.jans.as.server.service.external.ExternalAuthzDetailTypeService;
import io.jans.as.server.service.external.ExternalDynamicScopeService;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.tika.utils.StringUtils.isBlank;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class DiscoveryServiceTest {

    @InjectMocks
    private DiscoveryService discoveryService;

    @Mock
    private transient Logger log;

    @Mock
    private transient AppConfiguration appConfiguration;

    @Mock
    private transient ExternalAuthzDetailTypeService externalAuthzDetailTypeService;

    @Mock
    private transient CIBAConfigurationService cibaConfigurationService;

    @Mock
    private transient LocalResponseCache localResponseCache;

    @Mock
    private transient ExternalAuthenticationService externalAuthenticationService;

    @Mock
    private transient ExternalDynamicScopeService externalDynamicScopeService;

    @Mock
    private transient ScopeService scopeService;

    @Mock
    private transient AttributeService attributeService;

    @Test
    public void process_whenAcrMappingsArePresent_shouldReturnInJson() {
        Map<String, String> acrMappings = new HashMap<>();
        acrMappings.put("alias1", "acr1");

        when(appConfiguration.isFeatureEnabled(any())).thenReturn(true);
        when(appConfiguration.getEndSessionEndpoint()).thenReturn("https://as.com/end_session");
        when(appConfiguration.getAcrMappings()).thenReturn(acrMappings);

        final JSONObject json = discoveryService.process();
        final Map<String, String> acrMappingsFromJson = Util.toSerializableMapOfStrings(json.optJSONObject(ConfigurationResponseClaim.ACR_MAPPINGS).toMap());
        assertNotNull(acrMappingsFromJson);
        assertEquals("acr1", acrMappingsFromJson.get("alias1"));
    }

    @Test
    public void process_whenEndSessionFlagIsEnabled_shouldSetEndSessionFieldsToFalse() {
        when(appConfiguration.isFeatureEnabled(any())).thenReturn(true);
        when(appConfiguration.getEndSessionEndpoint()).thenReturn("https://as.com/end_session");
        when(appConfiguration.getFrontChannelLogoutSessionSupported()).thenReturn(true);
        final JSONObject json = discoveryService.process();

        assertEquals("https://as.com/end_session", json.optString(ConfigurationResponseClaim.END_SESSION_ENDPOINT));
        assertEquals(Boolean.TRUE, json.optBoolean(ConfigurationResponseClaim.FRONT_CHANNEL_LOGOUT_SESSION_SUPPORTED));
        assertEquals(Boolean.TRUE, json.optBoolean(ConfigurationResponseClaim.FRONTCHANNEL_LOGOUT_SUPPORTED));
        assertEquals(Boolean.TRUE, json.optBoolean(ConfigurationResponseClaim.BACKCHANNEL_LOGOUT_SESSION_SUPPORTED));
        assertEquals(Boolean.TRUE, json.optBoolean(ConfigurationResponseClaim.BACKCHANNEL_LOGOUT_SUPPORTED));
    }

    @Test
    public void process_whenEndSessionFlagIsDisabled_shouldSetEndSessionFieldsToFalse() {
        when(appConfiguration.isFeatureEnabled(any())).thenReturn(false);
        when(appConfiguration.getFrontChannelLogoutSessionSupported()).thenReturn(true);
        final JSONObject json = discoveryService.process();

        assertTrue(isBlank(json.optString(ConfigurationResponseClaim.END_SESSION_ENDPOINT)));
        assertEquals(Boolean.FALSE, json.optBoolean(ConfigurationResponseClaim.FRONT_CHANNEL_LOGOUT_SESSION_SUPPORTED));
        assertEquals(Boolean.FALSE, json.optBoolean(ConfigurationResponseClaim.FRONTCHANNEL_LOGOUT_SUPPORTED));
        assertEquals(Boolean.FALSE, json.optBoolean(ConfigurationResponseClaim.BACKCHANNEL_LOGOUT_SESSION_SUPPORTED));
        assertEquals(Boolean.FALSE, json.optBoolean(ConfigurationResponseClaim.BACKCHANNEL_LOGOUT_SUPPORTED));
    }

    @Test
    public void getAcrValuesList_whenCalled_shouldContainInternalAuthnAlias() {
        final List<String> acrValuesList = DiscoveryService.getAcrValuesList(new ArrayList<>());
        assertTrue(acrValuesList.contains("simple_password_auth"));
    }
}
