package io.jans.as.server.register.ws.rs;

import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import io.jans.as.client.RegisterRequest;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.ciba.CIBARegisterClientMetadataService;
import io.jans.as.server.service.ScopeService;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class RegisterServiceTest {

    @InjectMocks
    @Spy
    private RegisterService registerService;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private Logger log;

    @Mock
    private ScopeService scopeService;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private AttributeService attributeService;

    @Mock
    private CIBARegisterClientMetadataService cibaRegisterClientMetadataService;

    @Test
    public void assignScopes_whenCalled_shouldAssignOnlyAllowedScopes() {
        final List<String> requestScopes = Lists.newArrayList("s1", "s2", "s3");

        when(appConfiguration.getDynamicRegistrationScopesParamEnabled()).thenReturn(true);
        when(scopeService.getDefaultScopesDn()).thenReturn(Lists.newArrayList("s1_dn", "s2_dn"));
        when(scopeService.getScopesDn(requestScopes)).thenReturn(Lists.newArrayList("s1_dn", "s2_dn", "s3_dn"));

        Client client = new Client();

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setScope(requestScopes);

        registerService.assignScopes(client, registerRequest);
        assertEqualsNoOrder(client.getScopes(), new String[]{"s1_dn", "s2_dn"});
    }

    @Test
    public void assignScopes_whenDynamicRegistrationScopesAreNotAllowed_shouldAssignNothing() {
        final List<String> requestScopes = Lists.newArrayList("s1", "s2", "s3");

        when(appConfiguration.getDynamicRegistrationScopesParamEnabled()).thenReturn(false);

        Client client = new Client();

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setScope(requestScopes);

        registerService.assignScopes(client, registerRequest);
        assertNull(client.getScopes());
    }

    @Test
    public void assignScopes_whenScopesAreNotRequested_shouldAssignNothing() {
        final List<String> requestScopes = Lists.newArrayList();

        when(appConfiguration.getDynamicRegistrationScopesParamEnabled()).thenReturn(true);

        Client client = new Client();

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setScope(requestScopes);

        registerService.assignScopes(client, registerRequest);
        assertNull(client.getScopes());
    }

    @Test
    public void assignScopes_forROPC_shouldAssignOnlyAllowedScopes() {
        final List<String> requestScopes = Lists.newArrayList("s1", "s2", "s3");
        final List<String> allowedForRopc = Lists.newArrayList("s2");

        when(appConfiguration.getDynamicRegistrationScopesParamEnabled()).thenReturn(true);
        when(appConfiguration.getDynamicRegistrationAllowedPasswordGrantScopes()).thenReturn(allowedForRopc);
        when(scopeService.getDefaultScopesDn()).thenReturn(Lists.newArrayList("s1_dn", "s2_dn"));
        when(scopeService.getScopesDn(allowedForRopc)).thenReturn(Lists.newArrayList("s2_dn"));

        Client client = new Client();
        client.setGrantTypes(new GrantType[]{GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS});

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setScope(requestScopes);

        registerService.assignScopes(client, registerRequest);
        assertEqualsNoOrder(client.getScopes(), new String[]{"s2_dn"});
    }

    @Test
    public void identifyResponseType_whenResponseTypeIsBlank_shouldFallbackToCodeValue() {
        when(appConfiguration.getAllResponseTypesSupported()).thenReturn(Sets.newHashSet(ResponseType.values()));

        final Set<ResponseType> result = registerService.identifyResponseTypes(new ArrayList<>(), new ArrayList<>());
        assertTrue(result.contains(ResponseType.CODE));
    }

    @Test
    public void identifyResponseType_whenResponseTypeIsNotSupported_shouldRemoveIt() {
        when(appConfiguration.getAllResponseTypesSupported()).thenReturn(Sets.newHashSet());

        final Set<ResponseType> result = registerService.identifyResponseTypes(new ArrayList<>(), new ArrayList<>());
        assertTrue(result.isEmpty());
    }

    @Test
    public void identifyResponseType_whenResponseTypeIsAbsentButAutofixEnabledForAuthorizationCode_shouldFix() {
        when(appConfiguration.getAllResponseTypesSupported()).thenReturn(Sets.newHashSet(ResponseType.values()));
        when(appConfiguration.getGrantTypesAndResponseTypesAutofixEnabled()).thenReturn(true);

        final Set<ResponseType> result = registerService.identifyResponseTypes(new ArrayList<>(), Lists.newArrayList(GrantType.AUTHORIZATION_CODE));
        assertTrue(result.contains(ResponseType.CODE));
    }

    @Test
    public void identifyResponseType_whenResponseTypeIsAbsentButAutofixEnabledForToken_shouldFix() {
        when(appConfiguration.getAllResponseTypesSupported()).thenReturn(Sets.newHashSet(ResponseType.values()));
        when(appConfiguration.getGrantTypesAndResponseTypesAutofixEnabled()).thenReturn(true);

        final Set<ResponseType> result = registerService.identifyResponseTypes(new ArrayList<>(), Lists.newArrayList(GrantType.IMPLICIT));
        assertTrue(result.contains(ResponseType.TOKEN));
    }

    @Test
    public void identifyGrantType_whenGrantTypeIsNotSupportedForDynamicRegistration_shouldNotReturnIt() {
        final HashSet<GrantType> allowedForRegistration = Sets.newHashSet(GrantType.values());
        allowedForRegistration.remove(GrantType.AUTHORIZATION_CODE);

        when(appConfiguration.getGrantTypesSupported()).thenReturn(Sets.newHashSet(GrantType.values()));
        when(appConfiguration.getGrantTypesSupportedByDynamicRegistration()).thenReturn(allowedForRegistration);

        final Set<GrantType> result = registerService.identifyGrantTypes(Lists.newArrayList(ResponseType.CODE), Lists.newArrayList(GrantType.AUTHORIZATION_CODE));
        assertFalse(result.contains(GrantType.AUTHORIZATION_CODE));
    }

    @Test
    public void identifyGrantType_whenGrantTypeIsSupportedForDynamicRegistration_shouldReturnIt() {
        final HashSet<GrantType> allowedForRegistration = Sets.newHashSet(GrantType.AUTHORIZATION_CODE);

        when(appConfiguration.getGrantTypesSupported()).thenReturn(Sets.newHashSet(GrantType.values()));
        when(appConfiguration.getGrantTypesSupportedByDynamicRegistration()).thenReturn(allowedForRegistration);

        final Set<GrantType> result = registerService.identifyGrantTypes(Lists.newArrayList(ResponseType.CODE), Lists.newArrayList(GrantType.AUTHORIZATION_CODE));
        assertTrue(result.contains(GrantType.AUTHORIZATION_CODE));
    }

    @Test
    public void identifyGrantType_whenGrantTypeIsBlank_shouldFallbackToCodeValue() {
        when(appConfiguration.getGrantTypesSupported()).thenReturn(Sets.newHashSet(GrantType.values()));
        when(appConfiguration.getGrantTypesSupportedByDynamicRegistration()).thenReturn(Sets.newHashSet(GrantType.values()));

        final Set<GrantType> result = registerService.identifyGrantTypes(new ArrayList<>(), new ArrayList<>());
        assertTrue(result.contains(GrantType.AUTHORIZATION_CODE));
    }

    @Test
    public void identifyGrantType_whenGrantTypeIsNotSupported_shouldRemoveIt() {
        when(appConfiguration.getGrantTypesSupported()).thenReturn(Sets.newHashSet());
        when(appConfiguration.getGrantTypesSupportedByDynamicRegistration()).thenReturn(Sets.newHashSet(GrantType.values()));

        final Set<GrantType> result = registerService.identifyGrantTypes(new ArrayList<>(), new ArrayList<>());
        assertTrue(result.isEmpty());
    }

    @Test
    public void identifyGrantType_whenGrantTypeIsAbsentButAutofixEnabledForAuthorizationCode_shouldFix() {
        when(appConfiguration.getGrantTypesSupported()).thenReturn(Sets.newHashSet(GrantType.values()));
        when(appConfiguration.getGrantTypesSupportedByDynamicRegistration()).thenReturn(Sets.newHashSet(GrantType.values()));
        when(appConfiguration.getGrantTypesAndResponseTypesAutofixEnabled()).thenReturn(true);

        final Set<GrantType> result = registerService.identifyGrantTypes(Lists.newArrayList(ResponseType.CODE), new ArrayList<>());
        assertTrue(result.contains(GrantType.AUTHORIZATION_CODE));
    }

    @Test
    public void identifyGrantType_whenGrantTypeIsAbsentButAutofixEnabledForToken_shouldFix() {
        when(appConfiguration.getGrantTypesSupported()).thenReturn(Sets.newHashSet(GrantType.values()));
        when(appConfiguration.getGrantTypesSupportedByDynamicRegistration()).thenReturn(Sets.newHashSet(GrantType.values()));
        when(appConfiguration.getGrantTypesAndResponseTypesAutofixEnabled()).thenReturn(true);

        final Set<GrantType> result = registerService.identifyGrantTypes(Lists.newArrayList(ResponseType.TOKEN), new ArrayList<>());
        assertTrue(result.contains(GrantType.IMPLICIT));
    }

    @Test
    public void identifyGrantType_whenGrantTypeIsAbsentButAutofixEnabledForIdToken_shouldFix() {
        when(appConfiguration.getGrantTypesSupported()).thenReturn(Sets.newHashSet(GrantType.values()));
        when(appConfiguration.getGrantTypesSupportedByDynamicRegistration()).thenReturn(Sets.newHashSet(GrantType.values()));
        when(appConfiguration.getGrantTypesAndResponseTypesAutofixEnabled()).thenReturn(true);

        final Set<GrantType> result = registerService.identifyGrantTypes(Lists.newArrayList(ResponseType.ID_TOKEN), new ArrayList<>());
        assertTrue(result.contains(GrantType.IMPLICIT));
    }

    @Test
    public void addDefaultCustomAttributes_whenCalledWithExistingConfigurationData_shouldPopulateRequestObject() throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode node = mapper.readTree("{\"jansInclClaimsInIdTkn\": true, \"jansTrustedClnt\": true}");
        when(appConfiguration.getDynamicRegistrationCustomAttributes()).thenReturn(Lists.newArrayList("jansInclClaimsInIdTkn", "jansTrustedClnt"));
        when(appConfiguration.getDynamicRegistrationDefaultCustomAttributes()).thenReturn(node);

        JSONObject requestObject = new JSONObject();
        registerService.addDefaultCustomAttributes(requestObject);

        assertTrue(requestObject.getBoolean("jansInclClaimsInIdTkn"));
        assertTrue(requestObject.getBoolean("jansTrustedClnt"));
    }

    @Test
    public void addDefaultCustomAttributes_whenCustomAttributePropertyIsEmpty_shouldNotTakeEffect() throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode node = mapper.readTree("{\"jansInclClaimsInIdTkn\": true, \"jansTrustedClnt\": true}");
        when(appConfiguration.getDynamicRegistrationCustomAttributes()).thenReturn(Lists.newArrayList());
        when(appConfiguration.getDynamicRegistrationDefaultCustomAttributes()).thenReturn(node);

        JSONObject requestObject = new JSONObject();
        registerService.addDefaultCustomAttributes(requestObject);

        assertTrue(requestObject.isEmpty());
    }

    @Test
    public void addDefaultCustomAttributes_whenCalledWithNoData_shouldNotTakeEffect() {
        JSONObject requestObject = new JSONObject();
        registerService.addDefaultCustomAttributes(requestObject);

        assertTrue(requestObject.isEmpty());
    }
}
