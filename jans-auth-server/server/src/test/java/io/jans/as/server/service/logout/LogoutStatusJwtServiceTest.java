package io.jans.as.server.service.logout;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.model.config.ConfigurationFactory;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.DiscoveryService;
import io.jans.as.server.service.SessionIdService;
import io.jans.as.server.service.external.ExternalLogoutStatusJwtService;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.as.server.service.token.StatusListIndexService;
import io.jans.service.cdi.util.CdiUtil;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class LogoutStatusJwtServiceTest {

    private static final long HOUR = 60 * 60 * 1000L;

    @InjectMocks
    private LogoutStatusJwtService logoutStatusJwtService;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private WebKeysConfiguration webKeysConfiguration;

    @Mock
    private ClientService clientService;

    @Mock
    private DiscoveryService discoveryService;

    @Mock
    private StatusListIndexService statusListIndexService;

    @Mock
    private SessionIdService sessionIdService;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private ExternalLogoutStatusJwtService externalLogoutStatusJwtService;

    @Mock
    private AuthorizationGrant grant;

    @Mock
    private AbstractCryptoProvider cryptoProvider;

    @Mock
    private ConfigurationFactory configurationFactory;

    @BeforeMethod
    public void setUp() {
        Client client = new Client();
        client.setClientId("client-id");
        lenient().when(grant.getClient()).thenReturn(client);
        lenient().when(appConfiguration.getLogoutStatusJwtLifetime()).thenReturn(3600);
        lenient().when(appConfiguration.getDefaultSignatureAlgorithm()).thenReturn("RS256");
        lenient().when(appConfiguration.getStaticKid()).thenReturn("kid");
        lenient().when(errorResponseFactory.isFeatureFlagEnabled(FeatureFlagType.STATUS_LIST)).thenReturn(true);
        lenient().when(statusListIndexService.next()).thenReturn(7);
    }

    @Test
    public void createLogoutStatusJwt_whenScriptExtendsExp_shouldRegisterScriptExpirationBeforeSigning() throws Exception {
        Date scriptExpiration = new Date((System.currentTimeMillis() + 48 * HOUR) / 1000 * 1000);
        when(externalLogoutStatusJwtService.modifyLogoutStatusJwtMethod(any(Jwt.class), any(ExternalScriptContext.class)))
                .thenAnswer(invocation -> {
                    Jwt jwt = invocation.getArgument(0);
                    jwt.getClaims().setExpirationTime(scriptExpiration);
                    return true;
                });
        lenient().when(cryptoProvider.sign(anyString(), any(), any(), any())).thenReturn("signature");

        try (MockedStatic<CdiUtil> cdiUtil = mockStatic(CdiUtil.class)) {
            cdiUtil.when(() -> CdiUtil.bean(AbstractCryptoProvider.class)).thenReturn(cryptoProvider);
            cdiUtil.when(() -> CdiUtil.bean(ConfigurationFactory.class)).thenReturn(configurationFactory);

            logoutStatusJwtService.createLogoutStatusJwt(new ExecutionContext(), grant);
        }

        ArgumentCaptor<Date> captor = ArgumentCaptor.forClass(Date.class);
        verify(statusListIndexService).registerTokenExpiration(eq(7), captor.capture());
        assertEquals(captor.getValue(), scriptExpiration);
    }
}
