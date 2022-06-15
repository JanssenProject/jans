package io.jans.as.server.model.authorize;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.exception.InvalidJwtException;
import jakarta.ws.rs.WebApplicationException;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;

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

    @Test
    public void validateRequestUri_whichIsAllowedByClient_shouldBeOk() {
        String requestUri = "https://myrp.com/request_uri";

        Client client = new Client();
        client.setRequestUris(new String[]{"https://myrp.com/request_uri"});
        JwtAuthorizationRequest.validateRequestUri(requestUri, client, new AppConfiguration(), "", new ErrorResponseFactory());
    }

    @Test
    public void validateRequestUri_withNoRestrictions_shouldBeOk() {
        String requestUri = "https://myrp.com/request_uri";

        JwtAuthorizationRequest.validateRequestUri(requestUri, new Client(), new AppConfiguration(), "", new ErrorResponseFactory());
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateRequestUri_whichIsNotAllowedByClient_shouldRaiseException() {
        String requestUri = "https://myrp.com/request_uri";

        Client client = new Client();
        client.setRequestUris(new String[]{"https://myrp.com"});
        JwtAuthorizationRequest.validateRequestUri(requestUri, client, new AppConfiguration(), "", new ErrorResponseFactory());
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateRequestUri_whichIsBlockListed_shouldRaiseException() {
        String requestUri = "https://myrp.com/request_uri";

        final AppConfiguration appConfiguration = new AppConfiguration();
        appConfiguration.setRequestUriBlockList(Arrays.asList("myrp.com", "evil.com"));
        JwtAuthorizationRequest.validateRequestUri(requestUri, new Client(), appConfiguration, "", new ErrorResponseFactory());
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateRequestUri_forLocalhost_shouldRaiseException() {
        String requestUri = "https://localhost/request_uri";

        final AppConfiguration appConfiguration = new AppConfiguration();
        appConfiguration.setRequestUriBlockList(Collections.singletonList("localhost"));
        JwtAuthorizationRequest.validateRequestUri(requestUri, new Client(), appConfiguration, "", new ErrorResponseFactory());
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateRequestUri_forLocalhostIp_shouldRaiseException() {
        String requestUri = "https://127.0.0.1/request_uri";

        final AppConfiguration appConfiguration = new AppConfiguration();
        appConfiguration.setRequestUriBlockList(Collections.singletonList("127.0.0.1"));
        JwtAuthorizationRequest.validateRequestUri(requestUri, new Client(), appConfiguration, "", new ErrorResponseFactory());
    }

    @Test
    public void validateRequestUri_whichIsNotBlockListed_shouldBeOk() {
        String requestUri = "https://myrp.com/request_uri";

        final AppConfiguration appConfiguration = new AppConfiguration();
        appConfiguration.setRequestUriBlockList(Arrays.asList("evil.com", "second.com"));
        JwtAuthorizationRequest.validateRequestUri(requestUri, new Client(), appConfiguration, "", new ErrorResponseFactory());
    }
}
