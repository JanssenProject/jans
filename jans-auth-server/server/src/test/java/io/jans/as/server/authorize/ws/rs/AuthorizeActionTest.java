package io.jans.as.server.authorize.ws.rs;

import com.google.common.collect.Lists;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.auth.Authenticator;
import io.jans.as.server.i18n.LanguageBean;
import io.jans.as.server.model.auth.AuthenticationMode;
import io.jans.as.server.model.authorize.ScopeChecker;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.*;
import io.jans.as.server.service.ciba.CibaRequestService;
import io.jans.as.server.service.external.ExternalAuthenticationService;
import io.jans.as.server.service.external.ExternalConsentGatheringService;
import io.jans.as.server.service.external.ExternalPostAuthnService;
import io.jans.jsf2.message.FacesMessages;
import io.jans.jsf2.service.FacesService;
import io.jans.service.net.NetworkService;
import io.jans.util.OxConstants;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class AuthorizeActionTest {

    @Spy
    @InjectMocks
    private AuthorizeAction authorizeAction;

    @Mock
    private Logger log;

    @Mock
    private ClientService clientService;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private SessionIdService sessionIdService;

    @Mock
    private RedirectionUriService redirectionUriService;

    @Mock
    private ClientAuthorizationsService clientAuthorizationsService;

    @Mock
    private ExternalAuthenticationService externalAuthenticationService;

    @Mock
    private ExternalConsentGatheringService externalConsentGatheringService;

    @Mock
    private AuthenticationMode defaultAuthenticationMode;

    @Mock
    private LanguageBean languageBean;

    @Mock
    private NetworkService networkService;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private FacesService facesService;

    @Mock
    private FacesMessages facesMessages;

    @Mock
    private FacesContext facesContext;

    @Mock
    private ExternalContext externalContext;

    @Mock
    private ConsentGathererService consentGatherer;

    @Mock
    private AuthorizeService authorizeService;

    @Mock
    private RequestParameterService requestParameterService;

    @Mock
    private ScopeChecker scopeChecker;

    @Mock
    private ErrorHandlerService errorHandlerService;

    @Mock
    private AbstractCryptoProvider cryptoProvider;

    @Mock
    private CookieService cookieService;

    @Mock
    private Authenticator authenticator;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private ExternalPostAuthnService externalPostAuthnService;

    @Mock
    private CibaRequestService cibaRequestService;

    @Mock
    private Identity identity;

    @Mock
    private AuthorizeRestWebServiceValidator authorizeRestWebServiceValidator;

    @Test
    public void checkPermissionGranted_whenExceptionThrown_shouldDeny() {
        authorizeAction.setClientId("testId");

        // force throw exception
        final RuntimeException exception = new RuntimeException();
        when(clientService.getClient(anyString())).thenThrow(exception);

        authorizeAction.checkPermissionGranted();

        verify(log).error("Failed to perform checkPermissionGranted()", exception);
        verify(authorizeAction).permissionDenied();
    }

    @Test
    public void shouldSkipScript_forExplicitDefaultPasswordAuth_shouldReturnTrue() {
        final boolean skip = authorizeAction.shouldSkipScript(Lists.newArrayList(OxConstants.SCRIPT_TYPE_INTERNAL_RESERVED_NAME));
        assertTrue(skip);
    }

    @Test
    public void shouldSkipScript_forEmptyAcrsAndHighestFalseAndDefaultPassAuthn_shouldReturnTrue() {
        when(appConfiguration.getUseHighestLevelScriptIfAcrScriptNotFound()).thenReturn(false);
        when(defaultAuthenticationMode.getName()).thenReturn(OxConstants.SCRIPT_TYPE_INTERNAL_RESERVED_NAME);
        final boolean skip = authorizeAction.shouldSkipScript(Lists.newArrayList());
        assertTrue(skip);
    }

    @Test
    public void shouldSkipScript_forEmptyAcrsAndHighestTrue_shouldReturnFalse() {
        when(appConfiguration.getUseHighestLevelScriptIfAcrScriptNotFound()).thenReturn(true);
        final boolean skip = authorizeAction.shouldSkipScript(Lists.newArrayList());
        assertFalse(skip);
    }

    @Test
    public void shouldSkipScript_forEmptyAcrsAndHighestFalseAndDefaultIsNotDefaultPassAuthn_shouldReturnFalse() {
        when(appConfiguration.getUseHighestLevelScriptIfAcrScriptNotFound()).thenReturn(false);
        when(defaultAuthenticationMode.getName()).thenReturn("some_acr");
        final boolean skip = authorizeAction.shouldSkipScript(Lists.newArrayList());
        assertFalse(skip);
    }

    @Test
    public void getRequestedClaims_whenClientIdMissing_shouldReturnEmptyAndSkipFetch() throws Exception {
        authorizeAction.setClientId(null);
        authorizeAction.setRequestUri("https://evil.example/jwt");

        List<String> result = authorizeAction.getRequestedClaims();

        assertTrue(result.isEmpty());
        verify(authorizeAction, never()).fetchRequestUriContent(anyString(), any());
        verify(clientService, never()).getClient(anyString());
    }

    @Test
    public void getRequestedClaims_whenRequestUriNotInClientAllowlist_shouldReturnEmptyAndSkipFetch() throws Exception {
        authorizeAction.setClientId("c1");
        authorizeAction.setRequestUri("https://evil.example/jwt");
        authorizeAction.setState("st1");

        Client client = new Client();
        client.setRequestUris(new String[]{"https://allowed.example/jwt"});
        when(clientService.getClient("c1")).thenReturn(client);

        List<String> result = authorizeAction.getRequestedClaims();

        assertTrue(result.isEmpty());
        verify(authorizeAction, never()).fetchRequestUriContent(anyString(), any());
    }

    @Test
    public void getRequestedClaims_whenRequestUriIsBlocklisted_shouldReturnEmptyAndSkipFetch() throws Exception {
        authorizeAction.setClientId("c1");
        authorizeAction.setRequestUri("http://169.254.169.254/latest/meta-data/");
        authorizeAction.setState("st1");

        Client client = new Client();
        client.setRequestUris(new String[0]);
        when(clientService.getClient("c1")).thenReturn(client);
        when(appConfiguration.getRequestUriBlockList()).thenReturn(Lists.newArrayList("http://169.254.169.254/*"));

        List<String> result = authorizeAction.getRequestedClaims();

        assertTrue(result.isEmpty());
        verify(authorizeAction, never()).fetchRequestUriContent(anyString(), any());
    }

    @Test
    public void getRequestedClaims_whenRequestUriIsAllowed_shouldInvokeFetch() throws Exception {
        authorizeAction.setClientId("c1");
        authorizeAction.setRequestUri("https://allowed.example/jwt");
        authorizeAction.setState("st1");

        Client client = new Client();
        client.setRequestUris(new String[]{"https://allowed.example/jwt"});
        when(clientService.getClient("c1")).thenReturn(client);
        doReturn(null).when(authorizeAction).fetchRequestUriContent(anyString(), any());

        List<String> result = authorizeAction.getRequestedClaims();

        assertTrue(result.isEmpty());
        verify(authorizeAction).fetchRequestUriContent(eq("https://allowed.example/jwt"), eq(null));
    }

    @Test
    public void getRequestedClaims_whenCalledMultipleTimes_shouldCacheAndNotRepeatClientLookup() {
        authorizeAction.setClientId("c1");

        authorizeAction.getRequestedClaims();
        authorizeAction.getRequestedClaims();
        authorizeAction.getRequestedClaims();

        verify(clientService, org.mockito.Mockito.times(1)).getClient("c1");
    }
}
