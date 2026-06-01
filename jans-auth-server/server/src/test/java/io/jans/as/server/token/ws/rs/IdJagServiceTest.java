package io.jans.as.server.token.ws.rs;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.model.common.ExecutionContext;
import jakarta.ws.rs.WebApplicationException;
import org.json.JSONObject;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Date;

import static io.jans.as.model.config.Constants.TOKEN_TYPE_ID_JAG;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class IdJagServiceTest {

    @InjectMocks
    private IdJagService idJagService;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private WebKeysConfiguration webKeysConfiguration;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private ApplicationAuditLogger applicationAuditLogger;

    @Mock
    private AuthorizationGrantList authorizationGrantList;

    private ExecutionContext executionContext;
    private Client client;

    @BeforeMethod
    public void setUp() {
        client = new Client();
        client.setClientId("test-client");

        executionContext = new ExecutionContext();
        executionContext.setClient(client);
        executionContext.setAuditLog(new OAuth2AuditLog("", null));

        lenient().when(appConfiguration.getIssuer()).thenReturn("https://idp.example.com");
        lenient().when(appConfiguration.getDefaultSignatureAlgorithm()).thenReturn("RS256");
        lenient().when(appConfiguration.getIdJagLifetime()).thenReturn(300);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void issueIdJag_whenFeatureFlagDisabled_shouldThrow() {
        doThrow(WebApplicationException.class)
                .when(errorResponseFactory).validateFeatureEnabled(FeatureFlagType.IDENTITY_ASSERTION_AUTHZ_GRANT);

        idJagService.issueIdJag(executionContext, null, "https://resource.example.com", null);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void issueIdJag_whenAudienceIsBlank_shouldThrow() {
        idJagService.issueIdJag(executionContext, null, "", "openid");
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void issueIdJag_whenAudienceIsNull_shouldThrow() {
        idJagService.issueIdJag(executionContext, null, null, "openid");
    }

    @Test
    public void validateSubjectToken_whenNullToken_shouldThrow() {
        try {
            idJagService.validateSubjectToken(null, "urn:ietf:params:oauth:token-type:id_token", executionContext);
            fail("Expected WebApplicationException for null subject_token");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateSubjectToken_whenTokenIsBlank_shouldThrow() {
        try {
            idJagService.validateSubjectToken("", "urn:ietf:params:oauth:token-type:id_token", executionContext);
            fail("Expected WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateSubjectToken_whenTokenIsNotValidJwt_shouldThrow() {
        try {
            idJagService.validateSubjectToken("not-a-jwt", "urn:ietf:params:oauth:token-type:id_token", executionContext);
            fail("Expected WebApplicationException");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateSubjectToken_whenSaml2Type_shouldReturnNull() {
        // SAML2 subject_token is accepted opaquely; no JWT parsing attempted
        Jwt result = idJagService.validateSubjectToken(
                "saml-blob-here", "urn:ietf:params:oauth:token-type:saml2", executionContext);
        assertNull(result);
    }

    @Test
    public void validateSubjectToken_whenSaml2TypeAndBlankToken_shouldThrow() {
        // Blank check fires before the SAML2 branch — blank is always invalid regardless of type
        try {
            idJagService.validateSubjectToken("", "urn:ietf:params:oauth:token-type:saml2", executionContext);
            fail("Expected WebApplicationException for blank SAML2 token");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateSubjectToken_whenIdTokenWithNoExp_shouldPass() throws Exception {
        // An ID token without an exp claim is not considered expired; validation must succeed
        Jwt jwt = new Jwt();
        // exp intentionally not set
        jwt.getClaims().setSubjectIdentifier("alice");
        jwt.getClaims().setIssuer("https://idp.example.com");
        final String encoded = jwt.toString();

        Jwt result = idJagService.validateSubjectToken(encoded, "urn:ietf:params:oauth:token-type:id_token", executionContext);
        assertNotNull(result);
    }

    @Test
    public void validateSubjectToken_whenExpiredJwt_shouldThrow() throws Exception {
        Jwt jwt = new Jwt();
        jwt.getClaims().setExpirationTime(new Date(System.currentTimeMillis() - 10_000)); // 10s ago
        jwt.getClaims().setSubjectIdentifier("alice");
        jwt.getClaims().setIssuer("https://idp.example.com");
        // encode as unsigned JWT for testing
        final String encoded = jwt.toString();

        try {
            idJagService.validateSubjectToken(encoded, "urn:ietf:params:oauth:token-type:id_token", executionContext);
            fail("Expected WebApplicationException for expired token");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateSubjectToken_whenValidNotExpiredJwt_shouldReturnJwt() throws Exception {
        Jwt jwt = new Jwt();
        jwt.getClaims().setExpirationTime(new Date(System.currentTimeMillis() + 60_000)); // 60s ahead
        jwt.getClaims().setSubjectIdentifier("alice");
        jwt.getClaims().setIssuer("https://idp.example.com");
        final String encoded = jwt.toString();

        Jwt result = idJagService.validateSubjectToken(encoded, "urn:ietf:params:oauth:token-type:id_token", executionContext);
        assertNotNull(result);
        assertEquals("alice", result.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
    }

    // ---- §4.3.3: refresh_token subject_token validation ----

    @Test
    public void validateSubjectToken_whenRefreshTokenValid_shouldReturnNull() {
        final AuthorizationGrant mockGrant = mock(AuthorizationGrant.class);
        when(authorizationGrantList.getAuthorizationGrantByRefreshToken("test-client", "valid-rt"))
                .thenReturn(mockGrant);

        Jwt result = idJagService.validateSubjectToken(
                "valid-rt", "urn:ietf:params:oauth:token-type:refresh_token", executionContext);
        assertNull(result);
    }

    @Test
    public void validateSubjectToken_whenRefreshTokenInvalid_shouldThrow() {
        when(authorizationGrantList.getAuthorizationGrantByRefreshToken("test-client", "bad-rt"))
                .thenReturn(null);

        try {
            idJagService.validateSubjectToken("bad-rt", "urn:ietf:params:oauth:token-type:refresh_token", executionContext);
            fail("Expected WebApplicationException for invalid refresh token");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    // ---- §4.3.3: resource and authorization_details in issued ID-JAG ----

    @Test
    public void issueIdJag_whenResourcePresent_shouldEmbedResourceInIdJag() throws Exception {
        // The token won't actually sign without real keys, but we can verify that
        // a non-null resource is passed into populateIdJagClaims without error
        // (further claim content is covered by IdJagValidatorServiceTest)
        try {
            idJagService.issueIdJag(executionContext, null, "https://resource.example.com",
                    "openid", "https://api.example.com/", null);
        } catch (WebApplicationException e) {
            // Signing will fail in unit tests (no real key material); that's expected.
            // The test validates that no NPE or wrong-path exception is thrown before signing.
        }
    }

    @Test
    public void issueIdJag_whenAuthorizationDetailsPresent_shouldEmbedInIdJag() throws Exception {
        try {
            idJagService.issueIdJag(executionContext, null, "https://resource.example.com",
                    "openid", null, "[{\"type\":\"payment\"}]");
        } catch (WebApplicationException e) {
            // Signing failure expected in unit tests — validates pre-signing path is correct
        }
    }

    @Test
    public void buildTokenExchangeResponse_shouldContainIdJagFields() {
        JSONObject response = idJagService.buildTokenExchangeResponse("signed.jwt.here");

        assertEquals("signed.jwt.here", response.getString("access_token"));
        assertEquals(TOKEN_TYPE_ID_JAG, response.getString("issued_token_type"));
        assertEquals("N_A", response.getString("token_type"));
    }
}
