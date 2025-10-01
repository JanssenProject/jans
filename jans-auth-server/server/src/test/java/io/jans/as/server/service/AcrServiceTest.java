package io.jans.as.server.service;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.util.RedirectUri;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.authorize.ws.rs.AuthzRequest;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.external.ExternalAuthenticationService;
import io.jans.model.AuthenticationScriptUsageType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.auth.DummyPersonAuthenticationType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class AcrServiceTest {

    @InjectMocks
    private AcrService acrService;

    @Mock
    private Identity identity;

    @Mock
    private SessionIdService sessionIdService;

    @Mock
    private ExternalAuthenticationService externalAuthenticationService;

    @Mock
    private AppConfiguration appConfiguration;

    @Test
    public void getScriptName_whenAcrIsAgama_shouldReturnAgama() {
        assertEquals(AcrService.getScriptName("agama_flow-parameter1"), "agama");
        assertEquals(AcrService.getScriptName("agama_io.jans.flow-parameter1"), "agama");
        assertEquals(AcrService.getScriptName("agama_io.jans.flow"), "agama");
        assertEquals(AcrService.getScriptName("agama"), "agama");
    }

    @Test
    public void getScriptName_whenAcrNotAgama_shouldReturnProvidedAcr() {
        assertEquals(AcrService.getScriptName("basic"), "basic");
    }

    @Test
    public void removeParametersFromAgamaAcr_whenAcrHasParameters_shouldRemoveParameters() {
        assertEquals(AcrService.removeParametersFromAgamaAcr("agama_flow-parameter1"), "agama_flow");
        assertEquals(AcrService.removeParametersFromAgamaAcr("agama_io.jans.flow-parameter1"), "agama_io.jans.flow");
        assertEquals(AcrService.removeParametersFromAgamaAcr("agama_io.jans.flow"), "agama_io.jans.flow");
    }

    @Test
    public void removeParametersFromAgamaAcr_whenAuthzRequestIsWithAcrWithParameters_shouldRemoveParameters() {
        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setAcrValues("agama_io.jans.flow-parameter1 acr2");

        AcrService.removeParametersForAgamaAcr(authzRequest);

        assertEquals(authzRequest.getAcrValues(), "agama_io.jans.flow acr2");
    }

    @Test
    public void isAgama_whenAcrIsNullOrNonAgama_shouldReturnFalse() {
        assertFalse(AcrService.isAgama(null));
        assertFalse(AcrService.isAgama(""));
        assertFalse(AcrService.isAgama("asf"));
    }

    @Test
    public void isAgama_whenAcrStartsFromAgama_shouldReturnTrue() {
        assertTrue(AcrService.isAgama("agama_"));
        assertTrue(AcrService.isAgama("agama_flow"));
        assertTrue(AcrService.isAgama("agama_com.company.flow"));
    }

    @Test
    public void checkClientAuthorizedAcrs_whenClientAuthorizedAcrsEmpty_shouldPass() {
        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setAcrValues("acr1");

        acrService.checkClientAuthorizedAcrs(authzRequest, new Client());
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void checkClientAuthorizedAcrs_whenClientDoesNotAllowAcr_shouldThrowException() {
        RedirectUri redirectUri = mock(RedirectUri.class);
        when(redirectUri.toString()).thenReturn("http://rp.com");

        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setAcrValues("my_acr");
        authzRequest.setRedirectUriResponse(new RedirectUriResponse(redirectUri, "", mock(HttpServletRequest.class), mock(ErrorResponseFactory.class)));

        final Client client = new Client();
        client.getAttributes().setAuthorizedAcrValues(Lists.newArrayList("clientAcr"));

        acrService.checkClientAuthorizedAcrs(authzRequest, client);
    }

    @Test
    public void checkClientAuthorizedAcrs_whenClientAllowAcr_shouldPass() {
        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setAcrValues("my_acr");

        final Client client = new Client();
        client.getAttributes().setAuthorizedAcrValues(Lists.newArrayList("my_acr"));

        acrService.checkClientAuthorizedAcrs(authzRequest, client);
    }

    @Test
    public void checkClientAuthorizedAcrs_whenClientAllowAcrViaMappings_shouldPass() {
        Map<String, String> mapping = new HashMap<>();
        mapping.put("my_acr", "map2");

        when(appConfiguration.getAcrMappings()).thenReturn(mapping);

        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setAcrValues("my_acr");
        acrService.applyAcrMappings(authzRequest);

        final Client client = new Client();
        client.getAttributes().setAuthorizedAcrValues(Lists.newArrayList("my_acr"));

        acrService.checkClientAuthorizedAcrs(authzRequest, client);
    }

    @Test
    public void applyAcrMappings_whenMappingsIsNotSet_shouldDoNothing() {
        final String mapped = acrService.applyAcrMappings(Lists.newArrayList("acr1", "acr2"));

        assertEquals(mapped, "acr1 acr2");
    }

    @Test
    public void applyAcrMappings_whenMappingsIsSet_shouldMapCorrectly() {
        Map<String, String> mapping = new HashMap<>();
        mapping.put("acr2", "map2");

        when(appConfiguration.getAcrMappings()).thenReturn(mapping);
        final String mapped = acrService.applyAcrMappings(Lists.newArrayList("acr1", "acr2"));

        assertEquals(mapped, "acr1 map2");
    }

    @Test
    public void checkAcrScriptIsAvailable_forBlankAcr_shouldPass() {
        AuthzRequest authzRequest = new AuthzRequest();

        acrService.checkAcrScriptIsAvailable(authzRequest);
    }

    @Test
    public void checkAcrScriptIsAvailable_forBuildInAcr_shouldPass() {
        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setAcrValues("simple_password_auth");

        acrService.checkAcrScriptIsAvailable(authzRequest);
    }

    @Test
    public void checkAcrScriptIsAvailable_whenScriptIsAvailable_shouldPass() {
        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setAcrValues("my_acr");

        final CustomScriptConfiguration script = new CustomScriptConfiguration(new CustomScript(), new DummyPersonAuthenticationType(), new HashMap<>());
        when(externalAuthenticationService.determineCustomScriptConfiguration(AuthenticationScriptUsageType.INTERACTIVE, authzRequest.getAcrValuesList())).thenReturn(script);

        acrService.checkAcrScriptIsAvailable(authzRequest);
    }

    @Test
    public void checkAcrScriptIsAvailable_whenScriptIsNotAvailable_shouldFail() {
        RedirectUri redirectUri = mock(RedirectUri.class);
        when(redirectUri.toString()).thenReturn("http://rp.com");

        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setAcrValues("my_acr");
        authzRequest.setRedirectUriResponse(new RedirectUriResponse(redirectUri, "", mock(HttpServletRequest.class), mock(ErrorResponseFactory.class)));

        try {
            acrService.checkAcrScriptIsAvailable(authzRequest);
        } catch (WebApplicationException e) {
            return;
        }

        fail("Script is not available but exception is not thrown.");
    }
}
