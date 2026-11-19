package io.jans.as.server.model.token;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.server.model.config.ConfigurationFactory;
import io.jans.as.server.service.ClientService;
import io.jans.service.cdi.util.CdiUtil;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.testng.Assert.assertEquals;

@Listeners(MockitoTestNGListener.class)
public class JwrServiceTest {

    @InjectMocks
    private JwrService jwrService;

    @Mock
    private Logger log;

    @Mock
    private AbstractCryptoProvider cryptoProvider;

    @Mock
    private ClientService clientService;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private WebKeysConfiguration webKeysConfiguration;

    @Mock
    private ConfigurationFactory configurationFactory;

    @Test
    public void createJwr_withSigningKid_shouldSetKidInHeader() {
        JsonWebResponse jwr = createJwr("id-token-kid");

        assertEquals(((Jwt) jwr).getHeader().getKeyId(), "id-token-kid");
    }

    @Test
    public void createJwr_withoutSigningKid_shouldNotApplyClientIdTokenSigningKid() {
        lenient().when(appConfiguration.getStaticKid()).thenReturn("static-kid");

        JsonWebResponse jwr = createJwr(null);

        assertEquals(((Jwt) jwr).getHeader().getKeyId(), "static-kid");
    }

    private JsonWebResponse createJwr(String signingKid) {
        lenient().when(appConfiguration.getDefaultSignatureAlgorithm()).thenReturn("RS256");
        lenient().when(appConfiguration.getIssuer()).thenReturn("https://example.org");

        Client client = new Client();
        client.setClientId("client-id");
        client.getAttributes().setIdTokenSigningKid("id-token-kid");

        try (MockedStatic<CdiUtil> cdiUtil = mockStatic(CdiUtil.class)) {
            cdiUtil.when(() -> CdiUtil.bean(AbstractCryptoProvider.class)).thenReturn(cryptoProvider);
            cdiUtil.when(() -> CdiUtil.bean(ConfigurationFactory.class)).thenReturn(configurationFactory);
            cdiUtil.when(() -> CdiUtil.bean(ClientService.class)).thenReturn(clientService);
            return signingKid == null ? jwrService.createJwr(client) : jwrService.createJwr(client, signingKid);
        }
    }
}
