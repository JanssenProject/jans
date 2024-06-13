package io.jans.as.server.token.ws.rs;

import io.jans.as.model.common.ExchangeTokenType;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.service.ClientService;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class TxTokenServiceTest {

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private ApplicationAuditLogger applicationAuditLogger;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private TxTokenValidator txTokenValidator;

    @Mock
    private AbstractCryptoProvider cryptoProvider;

    @Mock
    private ClientService clientService;

    @Mock
    private WebKeysConfiguration webKeysConfiguration;

    @Mock
    private AuthorizationGrantList authorizationGrantList;

    @InjectMocks
    private TxTokenService txTokenService;

    @Test
    public void createResponse_whenCalled_shouldSetTokenTypeToNA() {
        JSONObject response = TxTokenService.createResponse("token_code_gdfger");
        assertEquals(response.get("token_type"), "N_A");
    }

    @Test
    public void createResponse_whenCalled_mustNotHaveExpiresOrRefreshTokenOrScopeClaims() {
        JSONObject response = TxTokenService.createResponse("token_code_gdfger");
        assertFalse(response.has("expires_in"));
        assertFalse(response.has("refresh_token"));
        assertFalse(response.has("scope"));
    }

    @Test
    public void isTxTokenFlow_ifRequestedTokenTypeIsNotTxToken_shouldReturnFalse() {
        assertFalse(TxTokenService.isTxTokenFlow("access_token"));
    }

    @Test
    public void isTxTokenFlow_ifRequestedTokenTypeIsTxToken_shouldReturnTrue() {
        assertTrue(TxTokenService.isTxTokenFlow(ExchangeTokenType.TX_TOKEN.getName()));
    }
}
