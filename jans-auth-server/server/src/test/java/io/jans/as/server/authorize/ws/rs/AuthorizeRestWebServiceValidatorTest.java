package io.jans.as.server.authorize.ws.rs;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.util.RedirectUri;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.*;
import io.jans.as.server.service.external.ExternalAuthzDetailTypeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashSet;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class AuthorizeRestWebServiceValidatorTest {

    @InjectMocks
    private AuthorizeRestWebServiceValidator authorizeRestWebServiceValidator;

    @Mock
    private Logger log;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private ClientService clientService;

    @Mock
    private RedirectionUriService redirectionUriService;

    @Mock
    private DeviceAuthorizationService deviceAuthorizationService;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private SessionIdService sessionIdService;

    @Mock
    private Identity identity;

    @Mock
    private ExternalAuthzDetailTypeService externalAuthzDetailTypeService;

    @Test
    public void isAuthnMaxAgeValid_whenMaxAgeIsZero_shouldReturnTrue() {
        assertTrue(authorizeRestWebServiceValidator.isAuthnMaxAgeValid(0, new SessionId(), new Client()));
    }

    @Test
    public void isAuthnMaxAgeValid_whenMaxAgeIsZeroAndDisableAuthnForMaxAgeZeroIsFalse_shouldReturnTrue() {
        when(appConfiguration.getDisableAuthnForMaxAgeZero()).thenReturn(false);
        assertTrue(authorizeRestWebServiceValidator.isAuthnMaxAgeValid(0, new SessionId(), new Client()));
    }

    @Test
    public void isAuthnMaxAgeValid_whenMaxAgeIsZeroAndDisableAuthnForMaxAgeZeroIsTrue_shouldReturnFalse() {
        when(appConfiguration.getDisableAuthnForMaxAgeZero()).thenReturn(true);
        assertFalse(authorizeRestWebServiceValidator.isAuthnMaxAgeValid(0, new SessionId(), new Client()));
    }

    @Test
    public void isAuthnMaxAgeValid_whenMaxAgeIsNull_shouldReturnTrue() {
        assertTrue(authorizeRestWebServiceValidator.isAuthnMaxAgeValid(0, new SessionId(), new Client()));
    }

    @Test
    public void validateNotWebView_blockWebviewDisabled_valid() {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(appConfiguration.getBlockWebviewAuthorizationEnabled()).thenReturn(false);

        authorizeRestWebServiceValidator.validateNotWebView(httpServletRequest);
        verifyNoInteractions(log, httpServletRequest);
    }

    @Test
    public void validateNotWebView_blockWebviewEnabled_valid() {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(appConfiguration.getBlockWebviewAuthorizationEnabled()).thenReturn(true);

        authorizeRestWebServiceValidator.validateNotWebView(httpServletRequest);
        verifyNoInteractions(log);
    }

    @Test
    public void validateNotWebView_withRequestedWithHeader_throwUnauthorized() {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(appConfiguration.getBlockWebviewAuthorizationEnabled()).thenReturn(true);
        String testPackage = "test.app.package";
        when(httpServletRequest.getHeader(any())).thenReturn(testPackage);

        assertThrows(WebApplicationException.class, () -> authorizeRestWebServiceValidator.validateNotWebView(httpServletRequest));
        verify(log).error(anyString(), eq(testPackage));
    }

    @Test
    public void validateAuthorizationDetails_withoutAuthzDetails_shouldPassSuccessfully() {
        AuthzRequest authzRequest = new AuthzRequest();
        Client client = new Client();

        authorizeRestWebServiceValidator.validateAuthorizationDetails(authzRequest, client);
    }

    @Test
    public void validateAuthorizationDetails_withInvalidAuthzDetails_throwException() {
        final RedirectUri redirectUri = mock(RedirectUri.class);
        when(redirectUri.toString()).thenReturn("http://rp.com");

        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setAuthzDetailsString("not_valid_json");
        authzRequest.setRedirectUriResponse(new RedirectUriResponse(redirectUri, "", mock(HttpServletRequest.class), mock(ErrorResponseFactory.class)));
        Client client = new Client();

        assertThrows(WebApplicationException.class, () -> authorizeRestWebServiceValidator.validateAuthorizationDetails(authzRequest, client));
    }

    @Test
    public void validateAuthorizationDetails_withNotSupportedScriptType_throwException() {
        final RedirectUri redirectUri = mock(RedirectUri.class);
        when(redirectUri.toString()).thenReturn("http://rp.com");

        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setAuthzDetailsString("[{\"type\":\"internal_type\"}]");
        authzRequest.setRedirectUriResponse(new RedirectUriResponse(redirectUri, "", mock(HttpServletRequest.class), mock(ErrorResponseFactory.class)));
        Client client = new Client();

        assertThrows(WebApplicationException.class, () -> authorizeRestWebServiceValidator.validateAuthorizationDetails(authzRequest, client));
    }

    @Test
    public void validateAuthorizationDetails_withNotSupportedClientType_throwException() {
        final RedirectUri redirectUri = mock(RedirectUri.class);
        when(redirectUri.toString()).thenReturn("http://rp.com");
        when(externalAuthzDetailTypeService.getSupportedAuthzDetailsTypes()).thenReturn(new HashSet<>(Collections.singletonList("internal_type")));

        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setAuthzDetailsString("[{\"type\":\"internal_type\"}]");
        authzRequest.setRedirectUriResponse(new RedirectUriResponse(redirectUri, "", mock(HttpServletRequest.class), mock(ErrorResponseFactory.class)));
        Client client = new Client();

        assertThrows(WebApplicationException.class, () -> authorizeRestWebServiceValidator.validateAuthorizationDetails(authzRequest, client));
    }

    @Test
    public void validateAuthorizationDetails_withSupportedClientAndScriptType_shouldPassSuccessfully() {
        final RedirectUri redirectUri = mock(RedirectUri.class);
        when(externalAuthzDetailTypeService.getSupportedAuthzDetailsTypes()).thenReturn(new HashSet<>(Collections.singletonList("internal_type")));

        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setAuthzDetailsString("[{\"type\":\"internal_type\"}]");
        authzRequest.setRedirectUriResponse(new RedirectUriResponse(redirectUri, "", mock(HttpServletRequest.class), mock(ErrorResponseFactory.class)));
        Client client = new Client();
        client.getAttributes().setAuthorizationDetailsTypes(Collections.singletonList("internal_type"));

        authorizeRestWebServiceValidator.validateAuthorizationDetails(authzRequest, client);
    }
}
