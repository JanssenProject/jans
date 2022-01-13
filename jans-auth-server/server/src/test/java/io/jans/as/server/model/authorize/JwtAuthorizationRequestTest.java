package io.jans.as.server.model.authorize;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.exception.InvalidJwtException;
import org.testng.annotations.Test;

/**
 * @author Yuriy Zabrovarnyy
 */
public class JwtAuthorizationRequestTest {

    @Test(expectedExceptions = InvalidJwtException.class)
    public void createJwtAuthorizationRequest_whenEncryptionIsRequiredForUnencryptedRequestObject_shouldThrowException() throws InvalidJwtException {
        AppConfiguration appConfiguration = new AppConfiguration();
        appConfiguration.setRequireRequestObjectEncryption(true);

        String signedJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        new JwtAuthorizationRequest(appConfiguration, null, signedJwt, new Client());
    }
}
