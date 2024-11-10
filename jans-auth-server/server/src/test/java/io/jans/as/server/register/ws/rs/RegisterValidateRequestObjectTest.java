package io.jans.as.server.register.ws.rs;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtHeader;
import io.jans.as.server.service.external.ExternalDynamicClientRegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;

import java.util.List;

import static io.jans.as.model.crypto.signature.SignatureAlgorithm.HS256;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@Listeners(MockitoTestNGListener.class)
public class RegisterValidateRequestObjectTest {

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private AbstractCryptoProvider cryptoProvider;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private SsaValidationConfigService ssaValidationConfigService;

    @Mock
    private ExternalDynamicClientRegistrationService externalDynamicClientRegistrationService;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private SsaValidationConfigContext ssaContext;

    @Mock
    private Logger log;

    @InjectMocks
    private RegisterValidator registerValidator;

    private Jwt jwt;

    @Test
    public void validateRequestObjectHmac_noBlockedUrlsCheck() throws Exception {
        MockitoAnnotations.openMocks(this);

        jwt = mock(Jwt.class);
        when(ssaContext.getJwt()).thenReturn(jwt);
        when(jwt.getSigningInput()).thenReturn("signingInput");
        when(jwt.getEncodedSignature()).thenReturn("encodedSignature");

        JwtHeader jwtHeader = new JwtHeader();
        when(jwt.getHeader()).thenReturn(jwtHeader);
        when(cryptoProvider.verifySignature(
                eq("signingInput"),
                eq("encodedSignature"),
                isNull(),
                isNull(),
                eq("secret"),
                eq(HS256)
        )).thenReturn(false);

        when(ssaValidationConfigService.isHmacValid(ssaContext)).thenReturn(false);
        when(appConfiguration.getDcrSignatureValidationSharedSecret()).thenReturn("secret");
        when(appConfiguration.getBlockedUrls()).thenReturn(List.of(
                "http://",
                "file://",
                "localhost",
                "127.0.",
                "192.168.",
                "172."
        ));

        String allowedUrl = "https://example.com/test";
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer(allowedUrl));

        InvalidJwtException exception = assertThrows(InvalidJwtException.class, () ->
                registerValidator.validateRequestObjectHmac(httpRequest, ssaContext)
        );

        assertEquals("Invalid cryptographic segment in the request object.", exception.getMessage());

        verify(log, never()).error(eq("URL '{}' is disallowed."), anyString());
    }

    @Test
    public void validateRequestObjectHmac_blockedUrlsCheck() throws Exception {
        MockitoAnnotations.openMocks(this);

        jwt = mock(Jwt.class);
        when(ssaContext.getJwt()).thenReturn(jwt);
        when(jwt.getSigningInput()).thenReturn("signingInput");
        when(jwt.getEncodedSignature()).thenReturn("encodedSignature");

        JwtHeader jwtHeader = new JwtHeader();
        when(jwt.getHeader()).thenReturn(jwtHeader);

        when(cryptoProvider.verifySignature(
                eq("signingInput"),
                eq("encodedSignature"),
                isNull(),
                isNull(),
                eq("secret"),
                eq(HS256)
        )).thenReturn(false);

        when(ssaValidationConfigService.isHmacValid(ssaContext)).thenReturn(false);
        when(appConfiguration.getDcrSignatureValidationSharedSecret()).thenReturn("secret");

        when(appConfiguration.getBlockedUrls()).thenReturn(List.of(
                "http://",
                "file://",
                "localhost",
                "127.0.",
                "192.168.",
                "172."
        ));

        String blockedUrl = "http://localhost/test";
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer(blockedUrl));

        InvalidJwtException exception = assertThrows(InvalidJwtException.class, () ->
                registerValidator.validateRequestObjectHmac(httpRequest, ssaContext)
        );

        assertEquals("The request object contains a disallowed URL: " + blockedUrl, exception.getMessage());

        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(log).error(logCaptor.capture(), eq(blockedUrl));

        assertTrue(logCaptor.getValue().contains("URL '{}' is disallowed."));
    }

}
