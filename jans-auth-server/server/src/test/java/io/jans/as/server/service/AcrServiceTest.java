package io.jans.as.server.service;

import io.jans.as.common.util.RedirectUri;
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
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.HashMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.fail;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class AcrServiceTest {

    @InjectMocks
    private AcrService acrService;

    @Mock
    private Logger log;

    @Mock
    private Identity identity;

    @Mock
    private SessionIdService sessionIdService;

    @Mock
    private ExternalAuthenticationService externalAuthenticationService;

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
