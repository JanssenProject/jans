package io.jans.as.server.model.token;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.token.ClientAssertionType;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Yuriy Z
 */
public class ClientAssertionTest {

    @Test
    public void verifyAudience_whenServerIssuerMatches_shouldPass() throws InvalidJwtException {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        String clientId = "clientId";
        ClientAssertionType clientAssertionType = ClientAssertionType.JWT_BEARER;
        String encodedAssertion = "encodedAssertion";

        when(appConfiguration.getIssuer()).thenReturn("https://server.com");

        ClientAssertion clientAssertion = new ClientAssertion(appConfiguration, cryptoProvider, clientId, clientAssertionType, encodedAssertion);
        clientAssertion.verifyAudience(Collections.singletonList("https://server.com"));
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void verifyAudience_whenAudienceIsEmpty_shouldFail() throws InvalidJwtException {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        String clientId = "clientId";
        ClientAssertionType clientAssertionType = ClientAssertionType.JWT_BEARER;
        String encodedAssertion = "encodedAssertion";

        ClientAssertion clientAssertion = new ClientAssertion(appConfiguration, cryptoProvider, clientId, clientAssertionType, encodedAssertion);
        clientAssertion.verifyAudience(null);
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void verifyAudience_whenServerIssuerDoesNotMatch_shouldFail() throws InvalidJwtException {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        String clientId = "clientId";
        ClientAssertionType clientAssertionType = ClientAssertionType.JWT_BEARER;
        String encodedAssertion = "encodedAssertion";

        when(appConfiguration.getIssuer()).thenReturn("https://server.com");

        ClientAssertion clientAssertion = new ClientAssertion(appConfiguration, cryptoProvider, clientId, clientAssertionType, encodedAssertion);
        clientAssertion.verifyAudience(Collections.singletonList("https://notserver.com"));
    }

    @Test
    public void verifyAudience_whenServerIssuerDoesNotMatchButCheckIsRelaxedByConfiguration_shouldPass() throws InvalidJwtException {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        String clientId = "clientId";
        ClientAssertionType clientAssertionType = ClientAssertionType.JWT_BEARER;
        String encodedAssertion = "encodedAssertion";

        when(appConfiguration.getIssuer()).thenReturn("https://server.com");
        when(appConfiguration.getAllowClientAssertionAudWithoutStrictIssuerMatch()).thenReturn(true);

        ClientAssertion clientAssertion = new ClientAssertion(appConfiguration, cryptoProvider, clientId, clientAssertionType, encodedAssertion);
        clientAssertion.verifyAudience(Collections.singletonList("https://notserver.com"));
    }

    @Test
    public void verifyClientAssertionType_whenTypeIsJwtBearer_shouldPass() throws InvalidJwtException {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        String clientId = "clientId";
        ClientAssertionType clientAssertionType = ClientAssertionType.JWT_BEARER;
        String encodedAssertion = "encodedAssertion";

        ClientAssertion clientAssertion = new ClientAssertion(appConfiguration, cryptoProvider, clientId, clientAssertionType, encodedAssertion);
        clientAssertion.verifyClientAssertionType();
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void verifyClientAssertionType_whenTypeIsNotJwtBearer_shouldFail() throws InvalidJwtException {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        String clientId = "clientId";
        ClientAssertionType clientAssertionType = null;
        String encodedAssertion = "encodedAssertion";

        ClientAssertion clientAssertion = new ClientAssertion(appConfiguration, cryptoProvider, clientId, clientAssertionType, encodedAssertion);
        clientAssertion.verifyClientAssertionType();
    }

    @Test
    public void verifyEncodedAssertion_whenNotBlank_shouldPass() throws InvalidJwtException {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        String clientId = "clientId";
        ClientAssertionType clientAssertionType = ClientAssertionType.JWT_BEARER;
        String encodedAssertion = "encodedAssertion";

        ClientAssertion clientAssertion = new ClientAssertion(appConfiguration, cryptoProvider, clientId, clientAssertionType, encodedAssertion);
        clientAssertion.verifyEncodedAssertion();
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void verifyEncodedAssertion_whenBlank_shouldFail() throws InvalidJwtException {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        String clientId = "clientId";
        ClientAssertionType clientAssertionType = null;
        String encodedAssertion = "";

        ClientAssertion clientAssertion = new ClientAssertion(appConfiguration, cryptoProvider, clientId, clientAssertionType, encodedAssertion);
        clientAssertion.verifyEncodedAssertion();
    }
}
