package io.jans.as.server.par.ws.rs;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.model.session.SessionClient;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.ClientService;
import jakarta.ws.rs.WebApplicationException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;

/**
 * @author Yuriy Zabrovarnyy
 */
@Listeners(MockitoTestNGListener.class)
public class ParValidatorTest {

    @InjectMocks
    private ParValidator parValidator;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private ErrorResponseFactory errorResponseFactory; // mock is required

    @Mock
    private Identity identity;

    @Mock
    private ClientService clientService;

    @Mock
    private Logger log;

    @SuppressWarnings("java:S5976") // we do want separate methods with clear names. Clarity of use case.
    @Test
    public void validatePkce_whenFapiIsFalse_shouldNotThrowError() {
        when(appConfiguration.isFapi()).thenReturn(false);

        parValidator.validatePkce(null, null, null);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validatePkce_whenFapiIsTrueAndNoCodeChallenage_shouldThrowError() {
        when(appConfiguration.isFapi()).thenReturn(true);

        parValidator.validatePkce(null, "s256", null);
    }

    @Test
    public void validatePkce_whenFapiIsTrueAndCodeChallenageIsSet_shouldNotThrowError() {
        when(appConfiguration.isFapi()).thenReturn(true);

        parValidator.validatePkce("codechallangehere", "s256",null);
    }

    @Test
    public void validateAuthentication_whenClientIsPublicAndAllowed_shouldPassSuccessfully() {
        Client client = new Client();
        client.setClientId("testId");
        client.setTokenEndpointAuthMethod("none");

        SessionClient sessionClient = new SessionClient();
        sessionClient.setClient(client);

        when(appConfiguration.getParForbidPublicClient()).thenReturn(false);
        when(identity.getSessionClient()).thenReturn(sessionClient);

        parValidator.validateAuthentication("testId", "testState");
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateAuthentication_whenClientIsPublicAndForbidden_shouldThrowError() {
        Client client = new Client();
        client.setClientId("testId");
        client.setTokenEndpointAuthMethod("none");

        SessionClient sessionClient = new SessionClient();
        sessionClient.setClient(client);

        when(appConfiguration.getParForbidPublicClient()).thenReturn(true);
        when(identity.getSessionClient()).thenReturn(sessionClient);
        when(clientService.isPublic(client)).thenReturn(true);

        parValidator.validateAuthentication("testId", "testState");
    }

}
