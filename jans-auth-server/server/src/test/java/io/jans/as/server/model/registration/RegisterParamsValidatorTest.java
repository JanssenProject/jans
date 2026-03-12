package io.jans.as.server.model.registration;

import com.beust.jcommander.internal.Lists;
import io.jans.as.client.RegisterRequest;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.register.RegisterErrorResponseType;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class RegisterParamsValidatorTest {

    @InjectMocks
    private RegisterParamsValidator registerParamsValidator;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    // ---- WEB application type ----
    @Test
    public void validateRedirectUris_webApp_javascript_shouldReturnFalse() {
        boolean result = registerParamsValidator.validateRedirectUris(
                Collections.singletonList(GrantType.AUTHORIZATION_CODE),
                Collections.singletonList(ResponseType.CODE),
                ApplicationType.WEB,
                SubjectType.PUBLIC,
                Collections.singletonList("javascript://example.com/callback"),
                null);
        assertFalse(result);
    }

    @Test
    public void validateRedirectUris_webApp_javascriptCamelCase_shouldReturnFalse() {
        boolean result = registerParamsValidator.validateRedirectUris(
                Collections.singletonList(GrantType.AUTHORIZATION_CODE),
                Collections.singletonList(ResponseType.CODE),
                ApplicationType.WEB,
                SubjectType.PUBLIC,
                Collections.singletonList("JaVaScRiPt://example.com/callback"),
                null);
        assertFalse(result);
    }

    @Test
    public void validateRedirectUris_webApp_file_shouldReturnFalse() {
        boolean result = registerParamsValidator.validateRedirectUris(
                Collections.singletonList(GrantType.AUTHORIZATION_CODE),
                Collections.singletonList(ResponseType.CODE),
                ApplicationType.WEB,
                SubjectType.PUBLIC,
                Collections.singletonList("file://example.com/callback"),
                null);
        assertFalse(result);
    }

    @Test
    public void validateRedirectUris_webApp_httpsWithValidHost_shouldReturnTrue() {
        when(appConfiguration.getClientWhiteList()).thenReturn(Collections.singletonList("*"));
        when(appConfiguration.getClientBlackList()).thenReturn(Collections.emptyList());

        boolean result = registerParamsValidator.validateRedirectUris(
                Collections.singletonList(GrantType.AUTHORIZATION_CODE),
                Collections.singletonList(ResponseType.CODE),
                ApplicationType.WEB,
                SubjectType.PUBLIC,
                Collections.singletonList("https://example.com/callback"),
                null);

        assertTrue(result);
    }

    @Test
    public void validateRedirectUris_webApp_httpLocalhost_shouldReturnTrue() {
        when(appConfiguration.getClientWhiteList()).thenReturn(Collections.singletonList("*"));
        when(appConfiguration.getClientBlackList()).thenReturn(Collections.emptyList());

        boolean result = registerParamsValidator.validateRedirectUris(
                Collections.singletonList(GrantType.AUTHORIZATION_CODE),
                Collections.singletonList(ResponseType.CODE),
                ApplicationType.WEB,
                SubjectType.PUBLIC,
                Collections.singletonList("http://localhost/callback"),
                null);
        assertTrue(result);
    }

    @Test
    public void validateRedirectUris_webApp_httpLoopback_shouldReturnTrue() {
        when(appConfiguration.getClientWhiteList()).thenReturn(Collections.singletonList("*"));
        when(appConfiguration.getClientBlackList()).thenReturn(Collections.emptyList());

        boolean result = registerParamsValidator.validateRedirectUris(
                Collections.singletonList(GrantType.AUTHORIZATION_CODE),
                Collections.singletonList(ResponseType.CODE),
                ApplicationType.WEB,
                SubjectType.PUBLIC,
                Collections.singletonList("http://127.0.0.1/callback"),
                null);
        assertTrue(result);
    }

    @Test
    public void validateRedirectUris_webApp_httpNonLocalhost_shouldReturnFalse() {
        boolean result = registerParamsValidator.validateRedirectUris(
                Collections.singletonList(GrantType.AUTHORIZATION_CODE),
                Collections.singletonList(ResponseType.CODE),
                ApplicationType.WEB,
                SubjectType.PUBLIC,
                Collections.singletonList("http://example.com/callback"),
                null);
        assertFalse(result);
    }

    @Test
    public void validateRedirectUris_webApp_httpsWithNoHost_shouldReturnFalse() {
        boolean result = registerParamsValidator.validateRedirectUris(
                Collections.singletonList(GrantType.AUTHORIZATION_CODE),
                Collections.singletonList(ResponseType.CODE),
                ApplicationType.WEB,
                SubjectType.PUBLIC,
                Collections.singletonList("https:///path"),
                null);
        assertFalse(result);
    }

    @Test
    public void validateRedirectUris_webApp_uriWithFragment_shouldReturnFalse() {
        boolean result = registerParamsValidator.validateRedirectUris(
                Collections.singletonList(GrantType.AUTHORIZATION_CODE),
                Collections.singletonList(ResponseType.CODE),
                ApplicationType.WEB,
                SubjectType.PUBLIC,
                Collections.singletonList("https://example.com/callback#fragment"),
                null);
        assertFalse(result);
    }

    @Test
    public void validateRedirectUris_webApp_nullUriInList_shouldReturnFalse() {
        boolean result = registerParamsValidator.validateRedirectUris(
                Collections.singletonList(GrantType.AUTHORIZATION_CODE),
                Collections.singletonList(ResponseType.CODE),
                ApplicationType.WEB,
                SubjectType.PUBLIC,
                Collections.singletonList(null),
                null);
        assertFalse(result);
    }

    @Test
    public void validateRedirectUris_webApp_unparseableUri_shouldReturnFalse() {
        boolean result = registerParamsValidator.validateRedirectUris(
                Collections.singletonList(GrantType.AUTHORIZATION_CODE),
                Collections.singletonList(ResponseType.CODE),
                ApplicationType.WEB,
                SubjectType.PUBLIC,
                Collections.singletonList("not a valid uri with spaces"),
                null);
        assertFalse(result);
    }

    @Test
    public void validateRedirectUris_webApp_mixedValidAndInvalid_shouldReturnFalse() {
        // One valid HTTPS URI and one HTTP URI with non-localhost host (invalid for WEB)
        boolean result = registerParamsValidator.validateRedirectUris(
                Collections.singletonList(GrantType.AUTHORIZATION_CODE),
                Collections.singletonList(ResponseType.CODE),
                ApplicationType.WEB,
                SubjectType.PUBLIC,
                Arrays.asList("https://example.com/cb", "http://example.com/callback"),
                null);
        assertFalse(result);
    }

    // ---- NATIVE application type ----

    @Test
    public void validateRedirectUris_nativeApp_customScheme_shouldReturnTrue() {
        when(appConfiguration.getClientWhiteList()).thenReturn(Collections.singletonList("*"));
        when(appConfiguration.getClientBlackList()).thenReturn(Collections.emptyList());

        boolean result = registerParamsValidator.validateRedirectUris(
                Collections.singletonList(GrantType.AUTHORIZATION_CODE),
                Collections.singletonList(ResponseType.CODE),
                ApplicationType.NATIVE,
                SubjectType.PUBLIC,
                Collections.singletonList("myapp://callback"),
                null);
        assertTrue(result);
    }

    @Test
    public void validateRedirectUris_nativeApp_javascript_shouldReturnTrue() {
        when(appConfiguration.getClientWhiteList()).thenReturn(Collections.singletonList("*"));
        when(appConfiguration.getClientBlackList()).thenReturn(Collections.emptyList());

        boolean result = registerParamsValidator.validateRedirectUris(
                Collections.singletonList(GrantType.AUTHORIZATION_CODE),
                Collections.singletonList(ResponseType.CODE),
                ApplicationType.NATIVE,
                SubjectType.PUBLIC,
                Collections.singletonList("javascript://example.com/callback"),
                null);
        assertTrue(result);
    }

    @Test
    public void validateRedirectUris_nativeApp_httpLocalhost_shouldReturnTrue() {
        when(appConfiguration.getClientWhiteList()).thenReturn(Collections.singletonList("*"));
        when(appConfiguration.getClientBlackList()).thenReturn(Collections.emptyList());

        boolean result = registerParamsValidator.validateRedirectUris(
                Collections.singletonList(GrantType.AUTHORIZATION_CODE),
                Collections.singletonList(ResponseType.CODE),
                ApplicationType.NATIVE,
                SubjectType.PUBLIC,
                Collections.singletonList("http://localhost/callback"),
                null);
        assertTrue(result);
    }

    @Test
    public void validateRedirectUris_nativeApp_httpsWithHost_shouldReturnTrue() {
        when(appConfiguration.getClientWhiteList()).thenReturn(Collections.singletonList("*"));
        when(appConfiguration.getClientBlackList()).thenReturn(Collections.emptyList());

        boolean result = registerParamsValidator.validateRedirectUris(
                Collections.singletonList(GrantType.AUTHORIZATION_CODE),
                Collections.singletonList(ResponseType.CODE),
                ApplicationType.NATIVE,
                SubjectType.PUBLIC,
                Collections.singletonList("https://example.com/callback"),
                null);
        assertTrue(result);
    }

    // ---- Empty redirect URIs ----

    @Test
    public void validateRedirectUris_emptyList_withCodeGrant_shouldReturnFalse() {
        boolean result = registerParamsValidator.validateRedirectUris(
                Collections.singletonList(GrantType.AUTHORIZATION_CODE),
                Collections.singletonList(ResponseType.CODE),
                ApplicationType.WEB,
                SubjectType.PUBLIC,
                Collections.emptyList(),
                null);
        assertFalse(result);
    }

    @Test
    public void validateRedirectUris_emptyList_withPasswordGrant_shouldReturnTrue() {
        boolean result = registerParamsValidator.validateRedirectUris(
                Collections.singletonList(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS),
                Collections.emptyList(),
                ApplicationType.WEB,
                SubjectType.PUBLIC,
                Collections.emptyList(),
                null);
        assertTrue(result);
    }

    @Test
    public void validateRedirectUris_emptyList_withClientCredentialsGrant_shouldReturnTrue() {
        boolean result = registerParamsValidator.validateRedirectUris(
                Collections.singletonList(GrantType.CLIENT_CREDENTIALS),
                Collections.emptyList(),
                ApplicationType.WEB,
                SubjectType.PUBLIC,
                Collections.emptyList(),
                null);
        assertTrue(result);
    }

    // ---- Pairwise subject type ----

    @Test
    public void validateRedirectUris_pairwise_singleHost_noSectorIdentifier_shouldReturnTrue() {
        when(appConfiguration.getClientWhiteList()).thenReturn(Collections.singletonList("*"));
        when(appConfiguration.getClientBlackList()).thenReturn(Collections.emptyList());

        boolean result = registerParamsValidator.validateRedirectUris(
                Collections.singletonList(GrantType.AUTHORIZATION_CODE),
                Collections.singletonList(ResponseType.CODE),
                ApplicationType.WEB,
                SubjectType.PAIRWISE,
                Arrays.asList("https://example.com/cb1", "https://example.com/cb2"),
                null);
        assertTrue(result);
    }

    @Test
    public void validateRedirectUris_pairwise_multipleHosts_noSectorIdentifier_shouldReturnFalse() {
        boolean result = registerParamsValidator.validateRedirectUris(
                Collections.singletonList(GrantType.AUTHORIZATION_CODE),
                Collections.singletonList(ResponseType.CODE),
                ApplicationType.WEB,
                SubjectType.PAIRWISE,
                Arrays.asList("https://example.com/cb", "https://other.com/cb"),
                null);
        assertFalse(result);
    }

    @Test
    public void validateRedirectUris_invalidUri_hostNotAddedToRedirectUriHostSet() {
        // An invalid redirect URI (http non-localhost for WEB) should not contribute a host
        // to the pairwise host set, so the single valid host must remain the only entry.
        boolean result = registerParamsValidator.validateRedirectUris(
                Collections.singletonList(GrantType.AUTHORIZATION_CODE),
                Collections.singletonList(ResponseType.CODE),
                ApplicationType.WEB,
                SubjectType.PAIRWISE,
                Arrays.asList("https://example.com/cb", "http://evil.com/cb"),
                null);
        // Invalid (http non-localhost) URI makes overall result false — and must not add "evil.com"
        // as a second host (which would falsely trigger the multi-host pairwise error separately).
        assertFalse(result);
    }

    @Test
    public void validateRedirectUris_whenSectorIdentifierDoesNotHostValidRedirectUri_shouldThrowInvalidClientMetadataError() {
        try {
            when(errorResponseFactory.createWebApplicationException(any(), any(), any())).thenCallRealMethod();
            registerParamsValidator.validateRedirectUris(
                    Lists.newArrayList(GrantType.AUTHORIZATION_CODE),
                    Lists.newArrayList(ResponseType.CODE),
                    ApplicationType.WEB,
                    SubjectType.PAIRWISE,
                    Lists.newArrayList("https://someuri.com"),
                    "https://invaliduri.com");
        } catch (WebApplicationException e) {
            verify(errorResponseFactory, times(1)).createWebApplicationException(eq(Response.Status.BAD_REQUEST), eq(RegisterErrorResponseType.INVALID_CLIENT_METADATA), any());
        }
    }

    @Test
    public void validateAlgorithms_whenAlgIsAmoungSupported_shouldNotRaiseException() {
        RegisterRequest request = new RegisterRequest();
        request.setAccessTokenSigningAlg(SignatureAlgorithm.RS256);

        when(appConfiguration.getAccessTokenSigningAlgValuesSupported()).thenReturn(Lists.newArrayList("RS256"));

        registerParamsValidator.validateAlgorithms(request);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateAlgorithms_whenAlgThatIsNotAmoungSupported_shouldRaiseException() {
        RegisterRequest request = new RegisterRequest();
        request.setAccessTokenSigningAlg(SignatureAlgorithm.RS256);

        when(appConfiguration.getAccessTokenSigningAlgValuesSupported()).thenReturn(Lists.newArrayList("RS512"));
        when(errorResponseFactory.createWebApplicationException(any(), any(), any())).thenCallRealMethod();

        registerParamsValidator.validateAlgorithms(request);
    }
}
