package io.jans.as.server.register.ws.rs.action;

import io.jans.as.client.RegisterRequest;
import io.jans.as.common.service.common.InumService;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.registration.RegisterParamsValidator;
import io.jans.as.server.register.ws.rs.RegisterJsonService;
import io.jans.as.server.register.ws.rs.RegisterService;
import io.jans.as.server.register.ws.rs.RegisterValidator;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.external.ExternalDynamicClientRegistrationService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * @author Yuriy Z
 */
@SuppressWarnings("java:S5979")
@Listeners(MockitoTestNGListener.class)
public class RegisterCreateActionTest {

    @InjectMocks
    @Spy
    private RegisterCreateAction registerCreateAction;

    @Mock
    private Logger log;

    @Mock
    private ApplicationAuditLogger applicationAuditLogger;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private InumService inumService;

    @Mock
    private ClientService clientService;

    @Mock
    private ExternalDynamicClientRegistrationService externalDynamicClientRegistrationService;

    @Mock
    private RegisterParamsValidator registerParamsValidator;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private StaticConfiguration staticConfiguration;

    @Mock
    private RegisterValidator registerValidator;

    @Mock
    private RegisterJsonService registerJsonService;

    @Mock
    private RegisterService registerService;

    @Test
    public void getClientLifetime_whenUpdateForbiddenInRequest_shouldReturnValueFromServerConfiguration() {
        when(appConfiguration.getDynamicRegistrationExpirationTime()).thenReturn(10);
        when(appConfiguration.getDcrForbidExpirationTimeInRequest()).thenReturn(true);

        RegisterRequest request = new RegisterRequest();
        request.setLifetime(5);

        assertEquals(registerCreateAction.getClientLifetime(request), 10);
    }

    @Test
    public void getClientLifetime_whenUpdateIsNotForbiddenInRequest_shouldReturnValueFromRequest() {
        when(appConfiguration.getDynamicRegistrationExpirationTime()).thenReturn(10);
        when(appConfiguration.getDcrForbidExpirationTimeInRequest()).thenReturn(false);

        RegisterRequest request = new RegisterRequest();
        request.setLifetime(5);

        assertEquals(registerCreateAction.getClientLifetime(request), 5);
    }
}
