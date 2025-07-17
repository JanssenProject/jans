package io.jans.as.server.token.ws.rs;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.authorize.ws.rs.AuthzDetailsService;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.service.SessionIdService;
import io.jans.as.server.service.external.ExternalTokenExchangeService;
import io.jans.as.server.service.external.ExternalUpdateTokenService;
import org.apache.commons.lang3.StringUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static io.jans.as.server.util.TestUtil.assertEmpty;
import static io.jans.as.server.util.TestUtil.assertNotEmpty;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class TokenExchangeServiceTest {

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private SessionIdService sessionIdService;

    @Mock
    private TokenRestWebServiceValidator tokenRestWebServiceValidator;

    @Mock
    private AuthorizationGrantList authorizationGrantList;

    @Mock
    private ExternalUpdateTokenService externalUpdateTokenService;

    @Mock
    private TokenCreatorService tokenCreatorService;

    @Mock
    private AttributeService attributeService;

    @Mock
    private AuthzDetailsService authzDetailsService;

    @Mock
    private ExternalTokenExchangeService externalTokenExchangeService;

    @Mock
    private ApplicationAuditLogger applicationAuditLogger;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @InjectMocks
    private TokenExchangeService tokenExchangeService;

    @Test
    public void createNewDeviceSecret_whenScopeIsNull_shouldNotGenerateDeviceSecretAndShouldNotThrowNPE() {
        SessionId sessionId = new SessionId();
        Client client = new Client();
        client.setGrantTypes(new GrantType[]{GrantType.AUTHORIZATION_CODE, GrantType.TOKEN_EXCHANGE});

        String deviceSecret = tokenExchangeService.createNewDeviceSecret("sessionDn", client, null);

        assertEmpty(deviceSecret);
        assertTrue(sessionId.getDeviceSecrets().isEmpty());
    }

    @Test
    public void createNewDeviceSecret_whenScopeDeviceSSOIsNotPresent_shouldNotGenerateDeviceSecret() {
        SessionId sessionId = new SessionId();
        Client client = new Client();
        client.setGrantTypes(new GrantType[]{GrantType.AUTHORIZATION_CODE, GrantType.TOKEN_EXCHANGE});

        final String deviceSecret = tokenExchangeService.createNewDeviceSecret("sessionDn", client, "openid");

        assertEmpty(deviceSecret);
        assertTrue(sessionId.getDeviceSecrets().isEmpty());
    }

    @Test
    public void createNewDeviceSecret_whenTokenExchangeGrantIsNotPresent_shouldNotGenerateDeviceSecret() {
        SessionId sessionId = new SessionId();
        Client client = new Client();
        client.setGrantTypes(new GrantType[]{GrantType.AUTHORIZATION_CODE});

        final String deviceSecret = tokenExchangeService.createNewDeviceSecret("sessionDn", client, "openid device_sso");

        assertEmpty(deviceSecret);
        assertTrue(sessionId.getDeviceSecrets().isEmpty());
    }

    @Test
    public void createNewDeviceSecret_whenAllConditionsMet_shouldGenerateDeviceSecret() {
        SessionId sessionId = new SessionId();
        Client client = new Client();
        client.setGrantTypes(new GrantType[]{GrantType.AUTHORIZATION_CODE, GrantType.TOKEN_EXCHANGE});

        when(sessionIdService.getSessionByDn("sessionDn")).thenReturn(sessionId);

        final String deviceSecret = tokenExchangeService.createNewDeviceSecret("sessionDn", client, "openid device_sso");

        assertNotEmpty(deviceSecret);
        assertTrue(sessionId.getDeviceSecrets().contains(deviceSecret));
    }

    @Test
    public void rotateDeviceSecret_whenRotationIsDisabled_shouldNotRotateDeviceSecret() {
        when(appConfiguration.getRotateDeviceSecret()).thenReturn(false);

        SessionId sessionId = new SessionId();
        sessionId.getDeviceSecrets().add("a");

        final String newDeviceSecret = tokenExchangeService.rotateDeviceSecret(sessionId, "a");
        assertNull(newDeviceSecret);
        assertTrue(sessionId.getDeviceSecrets().contains("a"));
        assertEquals(sessionId.getDeviceSecrets().size(), 1);
    }

    @Test
    public void rotateDeviceSecret_whenRotationIsEnabled_shouldRotateDeviceSecret() {
        when(appConfiguration.getRotateDeviceSecret()).thenReturn(true);

        SessionId sessionId = new SessionId();
        sessionId.getDeviceSecrets().add("a");

        final String newDeviceSecret = tokenExchangeService.rotateDeviceSecret(sessionId, "a");
        assertTrue(StringUtils.isNotBlank(newDeviceSecret) && !"a".equals(sessionId.getDeviceSecrets().get(0)));
        assertFalse(sessionId.getDeviceSecrets().contains("a"));
        assertEquals(sessionId.getDeviceSecrets().size(), 1);
    }
}
