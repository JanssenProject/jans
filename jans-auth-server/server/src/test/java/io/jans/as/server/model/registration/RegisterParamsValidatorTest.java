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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
