package io.jans.as.server.token.ws.rs;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.configuration.TrustedIssuerConfig;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtType;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.UserService;
import jakarta.ws.rs.WebApplicationException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class IdJagValidatorServiceTest {

    @InjectMocks
    private IdJagValidatorService idJagValidatorService;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private AbstractCryptoProvider cryptoProvider;

    @Mock
    private UserService userService;

    @Mock
    private ApplicationAuditLogger applicationAuditLogger;

    private Client client;
    private ExecutionContext executionContext;

    @BeforeMethod
    public void setUp() {
        client = new Client();
        client.setClientId("test-client");

        executionContext = new ExecutionContext();
        executionContext.setClient(client);
        executionContext.setAuditLog(new OAuth2AuditLog("", null));

        lenient().when(appConfiguration.getIssuer()).thenReturn("https://resource.example.com");
        lenient().when(appConfiguration.getIdJagTrustedIdpIssuers()).thenReturn(new HashMap<>());
    }

    // ---- isIdJag ----

    @Test
    public void isIdJag_whenNullJwt_shouldReturnFalse() {
        assertFalse(idJagValidatorService.isIdJag(null));
    }

    @Test
    public void isIdJag_whenTypIsOauthIdJag_shouldReturnTrue() throws Exception {
        Jwt jwt = buildValidIdJag(client.getClientId(), "https://resource.example.com", "https://idp.example.com");
        assertTrue(idJagValidatorService.isIdJag(jwt));
    }

    @Test
    public void isIdJag_whenTypIsRegularJwt_shouldReturnFalse() {
        Jwt jwt = new Jwt();
        jwt.getHeader().setType(JwtType.JWT);
        assertFalse(idJagValidatorService.isIdJag(jwt));
    }

    @Test
    public void isIdJag_whenNoTypSet_shouldReturnFalse() {
        // A freshly constructed JWT has no typ header — must not be treated as an ID-JAG
        Jwt jwt = new Jwt();
        assertFalse(idJagValidatorService.isIdJag(jwt));
    }

    // ---- validateIdJag ----

    @Test
    public void validateIdJag_whenSignatureInvalid_shouldThrow() throws Exception {
        mockSignatureValid(false);
        Jwt jwt = buildValidIdJag(client.getClientId(), "https://resource.example.com", "https://idp.example.com");

        try {
            idJagValidatorService.validateIdJag(jwt, client, executionContext);
            fail("Expected WebApplicationException for invalid signature");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateIdJag_whenIssuerAbsent_shouldThrow() throws Exception {
        mockSignatureValid(true);
        Jwt jwt = new Jwt();
        jwt.getHeader().setType(JwtType.OAUTH_ID_JAG);
        jwt.getHeader().setAlgorithm(SignatureAlgorithm.RS256);
        // iss intentionally omitted
        jwt.getClaims().setAudience("https://resource.example.com");
        jwt.getClaims().setClaim(JwtClaimName.CLIENT_ID, client.getClientId());
        jwt.getClaims().setExpirationTime(new Date(System.currentTimeMillis() + 300_000));

        try {
            idJagValidatorService.validateIdJag(jwt, client, executionContext);
            fail("Expected WebApplicationException for absent iss");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateIdJag_whenAudAbsent_shouldThrow() throws Exception {
        mockSignatureValid(true);
        Jwt jwt = new Jwt();
        jwt.getHeader().setType(JwtType.OAUTH_ID_JAG);
        jwt.getHeader().setAlgorithm(SignatureAlgorithm.RS256);
        jwt.getClaims().setClaim(JwtClaimName.ISSUER, "https://idp.example.com");
        // aud intentionally omitted
        jwt.getClaims().setClaim(JwtClaimName.CLIENT_ID, client.getClientId());
        jwt.getClaims().setExpirationTime(new Date(System.currentTimeMillis() + 300_000));

        try {
            idJagValidatorService.validateIdJag(jwt, client, executionContext);
            fail("Expected WebApplicationException for absent aud");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateIdJag_whenClientIdAbsent_shouldThrow() throws Exception {
        mockSignatureValid(true);
        Jwt jwt = new Jwt();
        jwt.getHeader().setType(JwtType.OAUTH_ID_JAG);
        jwt.getHeader().setAlgorithm(SignatureAlgorithm.RS256);
        jwt.getClaims().setClaim(JwtClaimName.ISSUER, "https://idp.example.com");
        jwt.getClaims().setAudience("https://resource.example.com");
        // client_id intentionally omitted
        jwt.getClaims().setExpirationTime(new Date(System.currentTimeMillis() + 300_000));

        try {
            idJagValidatorService.validateIdJag(jwt, client, executionContext);
            fail("Expected WebApplicationException for absent client_id");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateIdJag_whenExpAbsent_shouldThrow() throws Exception {
        mockSignatureValid(true);
        Jwt jwt = new Jwt();
        jwt.getHeader().setType(JwtType.OAUTH_ID_JAG);
        jwt.getHeader().setAlgorithm(SignatureAlgorithm.RS256);
        jwt.getClaims().setClaim(JwtClaimName.ISSUER, "https://idp.example.com");
        jwt.getClaims().setAudience("https://resource.example.com");
        jwt.getClaims().setClaim(JwtClaimName.CLIENT_ID, client.getClientId());
        // exp intentionally omitted — verifyExpiration treats null exp as expired

        try {
            idJagValidatorService.validateIdJag(jwt, client, executionContext);
            fail("Expected WebApplicationException for absent exp");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateIdJag_whenFeatureFlagDisabled_shouldThrow() {
        doThrow(WebApplicationException.class)
                .when(errorResponseFactory).validateFeatureEnabled(FeatureFlagType.IDENTITY_ASSERTION_AUTHZ_GRANT);

        Jwt jwt = new Jwt();
        jwt.getHeader().setType(JwtType.OAUTH_ID_JAG);
        idJagValidatorService.validateIdJag(jwt, client, executionContext);
    }

    @Test
    public void validateIdJag_whenAudDoesNotMatchServerIssuer_shouldThrow() throws Exception {
        Jwt jwt = buildValidIdJag(client.getClientId(), "https://other-server.example.com", "https://idp.example.com");
        mockSignatureValid(true);

        try {
            idJagValidatorService.validateIdJag(jwt, client, executionContext);
            fail("Expected WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateIdJag_whenClientIdMismatch_shouldThrow() throws Exception {
        Jwt jwt = buildValidIdJag("other-client", "https://resource.example.com", "https://idp.example.com");
        mockSignatureValid(true);

        try {
            idJagValidatorService.validateIdJag(jwt, client, executionContext);
            fail("Expected WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateIdJag_whenExpired_shouldThrow() throws Exception {
        Jwt jwt = buildIdJag(client.getClientId(), "https://resource.example.com", "https://idp.example.com",
                new Date(System.currentTimeMillis() - 10_000), "alice");
        mockSignatureValid(true);

        try {
            idJagValidatorService.validateIdJag(jwt, client, executionContext);
            fail("Expected WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateIdJag_whenIssuerNotTrusted_shouldThrow() throws Exception {
        Map<String, TrustedIssuerConfig> trustedIssuers = new HashMap<>();
        trustedIssuers.put("https://trusted-idp.example.com", new TrustedIssuerConfig());
        when(appConfiguration.getIdJagTrustedIdpIssuers()).thenReturn(trustedIssuers);
        mockSignatureValid(true);

        Jwt jwt = buildValidIdJag(client.getClientId(), "https://resource.example.com", "https://untrusted-idp.example.com");

        try {
            idJagValidatorService.validateIdJag(jwt, client, executionContext);
            fail("Expected WebApplicationException for untrusted issuer");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateIdJag_whenIssuerTrusted_shouldPass() throws Exception {
        Map<String, TrustedIssuerConfig> trustedIssuers = new HashMap<>();
        trustedIssuers.put("https://idp.example.com", new TrustedIssuerConfig());
        when(appConfiguration.getIdJagTrustedIdpIssuers()).thenReturn(trustedIssuers);
        mockSignatureValid(true);

        Jwt jwt = buildValidIdJag(client.getClientId(), "https://resource.example.com", "https://idp.example.com");
        User mockUser = new User();
        when(userService.getUser("alice")).thenReturn(mockUser);

        User result = idJagValidatorService.validateIdJag(jwt, client, executionContext);
        assertNotNull(result);
        assertEquals(result, mockUser);
    }

    @Test
    public void validateIdJag_whenSubResolvesUser_shouldReturnUser() throws Exception {
        mockSignatureValid(true);
        Jwt jwt = buildValidIdJag(client.getClientId(), "https://resource.example.com", "https://idp.example.com");

        User mockUser = new User();
        when(userService.getUser("alice")).thenReturn(mockUser);

        User result = idJagValidatorService.validateIdJag(jwt, client, executionContext);
        assertEquals(result, mockUser);
    }

    @Test
    public void validateIdJag_whenSubNotFoundButEmailFound_shouldReturnUser() throws Exception {
        mockSignatureValid(true);
        Jwt jwt = buildValidIdJag(client.getClientId(), "https://resource.example.com", "https://idp.example.com");
        jwt.getClaims().setClaim(JwtClaimName.EMAIL, "alice@example.com");

        when(userService.getUser("alice")).thenReturn(null); // sub not found
        User emailUser = new User();
        when(userService.getUserByAttribute("mail", "alice@example.com")).thenReturn(emailUser);

        User result = idJagValidatorService.validateIdJag(jwt, client, executionContext);
        assertEquals(result, emailUser);
    }

    @Test
    public void validateIdJag_whenNoUserResolved_shouldReturnEmptyUser() throws Exception {
        mockSignatureValid(true);
        Jwt jwt = buildValidIdJag(client.getClientId(), "https://resource.example.com", "https://idp.example.com");

        when(userService.getUser("alice")).thenReturn(null);

        User result = idJagValidatorService.validateIdJag(jwt, client, executionContext);
        assertNotNull(result);
        assertSame(result, JwtGrantService.EMPTY_USER);
    }

    @Test
    public void validateIdJag_whenSubAndEmailNotFoundButAudSubFound_shouldReturnUser() throws Exception {
        mockSignatureValid(true);
        Jwt jwt = buildValidIdJag(client.getClientId(), "https://resource.example.com", "https://idp.example.com");
        jwt.getClaims().setClaim(JwtClaimName.AUD_SUB, "alice-resource-side");

        when(userService.getUser("alice")).thenReturn(null);           // sub lookup fails
        // no email claim set, so email lookup is skipped
        User audSubUser = new User();
        when(userService.getUser("alice-resource-side")).thenReturn(audSubUser);

        User result = idJagValidatorService.validateIdJag(jwt, client, executionContext);
        assertSame(audSubUser, result);
    }

    // ---- §4.4.1: authorization_details MUST be parsed as JSON array ----

    @Test
    public void validateIdJag_whenAuthorizationDetailsIsValidArray_shouldPass() throws Exception {
        mockSignatureValid(true);
        Jwt jwt = buildValidIdJag(client.getClientId(), "https://resource.example.com", "https://idp.example.com");
        jwt.getClaims().setClaim(JwtClaimName.AUTHORIZATION_DETAILS, "[{\"type\":\"payment\"}]");

        User mockUser = new User();
        when(userService.getUser("alice")).thenReturn(mockUser);

        User result = idJagValidatorService.validateIdJag(jwt, client, executionContext);
        assertNotNull(result);
    }

    @Test
    public void validateIdJag_whenAuthorizationDetailsIsNotArray_shouldThrow() throws Exception {
        mockSignatureValid(true);
        Jwt jwt = buildValidIdJag(client.getClientId(), "https://resource.example.com", "https://idp.example.com");
        jwt.getClaims().setClaim(JwtClaimName.AUTHORIZATION_DETAILS, "not-a-json-array");

        try {
            idJagValidatorService.validateIdJag(jwt, client, executionContext);
            fail("Expected WebApplicationException for invalid authorization_details");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateIdJag_whenAuthorizationDetailsAbsent_shouldPass() throws Exception {
        mockSignatureValid(true);
        Jwt jwt = buildValidIdJag(client.getClientId(), "https://resource.example.com", "https://idp.example.com");
        // No authorization_details claim

        User mockUser = new User();
        when(userService.getUser("alice")).thenReturn(mockUser);

        User result = idJagValidatorService.validateIdJag(jwt, client, executionContext);
        assertNotNull(result);
    }

    // ---- TokenRestWebServiceValidator.validateIdJagSubjectTokenType ----
    // (tested in TokenRestWebServiceValidatorTest, included here for completeness)

    // ---- helpers ----

    private Jwt buildValidIdJag(String clientId, String audience, String issuer) throws Exception {
        return buildIdJag(clientId, audience, issuer,
                new Date(System.currentTimeMillis() + 300_000), "alice");
    }

    private Jwt buildIdJag(String clientId, String audience, String issuer, Date exp, String sub) throws Exception {
        Jwt jwt = new Jwt();
        jwt.getHeader().setType(JwtType.OAUTH_ID_JAG);
        jwt.getHeader().setAlgorithm(SignatureAlgorithm.RS256);
        jwt.getClaims().setClaim(JwtClaimName.ISSUER, issuer);
        jwt.getClaims().setSubjectIdentifier(sub);
        jwt.getClaims().setAudience(audience);
        jwt.getClaims().setClaim(JwtClaimName.CLIENT_ID, clientId);
        jwt.getClaims().setExpirationTime(exp);
        jwt.getClaims().setIat(new Date());
        return jwt;
    }

    private void mockSignatureValid(boolean valid) throws Exception {
        when(cryptoProvider.verifySignature(any(), any(), any(), any(), any(), any(SignatureAlgorithm.class)))
                .thenReturn(valid);
    }
}
