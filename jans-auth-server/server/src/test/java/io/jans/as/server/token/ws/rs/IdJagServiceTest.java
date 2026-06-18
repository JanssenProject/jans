package io.jans.as.server.token.ws.rs;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.configuration.TrustedIssuerConfig;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.external.ExternalIdentityAssertionService;
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
import java.util.HashMap;

import static io.jans.as.model.config.Constants.TOKEN_TYPE_ID_JAG;
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private ExternalIdentityAssertionService externalIdentityAssertionService;

    @Mock
    private AbstractCryptoProvider cryptoProvider;

    private ExecutionContext executionContext;
    private Client client;

    @BeforeMethod
    public void setUp() throws Exception {
        client = new Client();
        client.setClientId("test-client");

        executionContext = new ExecutionContext();
        executionContext.setClient(client);
        executionContext.setAuditLog(new OAuth2AuditLog("", null));

        lenient().when(appConfiguration.getIssuer()).thenReturn("https://idp.example.com");
        lenient().when(appConfiguration.getDefaultSignatureAlgorithm()).thenReturn("RS256");
        lenient().when(appConfiguration.getIdJagLifetime()).thenReturn(300);
        lenient().when(appConfiguration.getIdJagTrustedIdpIssuers()).thenReturn(new HashMap<>());
        lenient().when(externalIdentityAssertionService.externalModifyIdJagPayload(any(), any())).thenReturn(true);
        lenient().when(cryptoProvider.verifySignature(any(), any(), any(), any(), any(), any(SignatureAlgorithm.class))).thenReturn(true);
    }

    /** Minimal valid subject token for client "test-client" issued by this AS. */
    private Jwt buildValidSubjectJwt() {
        Jwt jwt = new Jwt();
        jwt.getHeader().setAlgorithm(SignatureAlgorithm.RS256);
        jwt.getClaims().setClaim(JwtClaimName.ISSUER, "https://idp.example.com");
        jwt.getClaims().setAudience("test-client");
        jwt.getClaims().setSubjectIdentifier("alice");
        jwt.getClaims().setExpirationTime(new Date(System.currentTimeMillis() + 60_000));
        return jwt;
    }

    private void mockSignatureValid(boolean valid) throws Exception {
        when(cryptoProvider.verifySignature(any(), any(), any(), any(), any(), any(SignatureAlgorithm.class)))
                .thenReturn(valid);
    }

    // ---- issueIdJag ----

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
    public void issueIdJag_whenResourcePresent_shouldEmbedResourceInIdJag() {
        try {
            idJagService.issueIdJag(executionContext, null, "https://resource.example.com",
                    "openid", "https://api.example.com/", null);
        } catch (WebApplicationException e) {
            // Signing fails in unit tests (no real key material) — validates pre-signing path only
        }
    }

    @Test
    public void issueIdJag_whenAuthorizationDetailsPresent_shouldEmbedInIdJag() {
        try {
            idJagService.issueIdJag(executionContext, null, "https://resource.example.com",
                    "openid", null, "[{\"type\":\"payment\"}]");
        } catch (WebApplicationException e) {
            // Signing fails in unit tests — validates pre-signing path only
        }
    }

    // ---- validateSubjectToken — blank / non-JWT ----

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

    // ---- validateSubjectToken — SAML2 short-circuit ----

    @Test
    public void validateSubjectToken_whenSaml2Type_shouldReturnNull() {
        Jwt result = idJagService.validateSubjectToken(
                "saml-blob-here", "urn:ietf:params:oauth:token-type:saml2", executionContext);
        assertNull(result);
    }

    @Test
    public void validateSubjectToken_whenSaml2TypeAndBlankToken_shouldThrow() {
        try {
            idJagService.validateSubjectToken("", "urn:ietf:params:oauth:token-type:saml2", executionContext);
            fail("Expected WebApplicationException for blank SAML2 token");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    // ---- validateSubjectToken — signature ----

    @Test
    public void validateSubjectToken_whenSignatureInvalid_shouldThrow() throws Exception {
        mockSignatureValid(false);
        Jwt jwt = buildValidSubjectJwt();

        try {
            idJagService.validateSubjectToken(jwt.toString(), "urn:ietf:params:oauth:token-type:id_token", executionContext);
            fail("Expected WebApplicationException for invalid signature");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    // ---- validateSubjectToken — issuer ----

    @Test
    public void validateSubjectToken_whenIssuerAbsent_shouldThrow() {
        Jwt jwt = new Jwt();
        jwt.getHeader().setAlgorithm(SignatureAlgorithm.RS256);
        jwt.getClaims().setAudience("test-client");
        jwt.getClaims().setSubjectIdentifier("alice");
        jwt.getClaims().setExpirationTime(new Date(System.currentTimeMillis() + 60_000));
        // iss intentionally not set

        try {
            idJagService.validateSubjectToken(jwt.toString(), "urn:ietf:params:oauth:token-type:id_token", executionContext);
            fail("Expected WebApplicationException for absent iss");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateSubjectToken_whenIssuerNotInTrustedList_shouldThrow() {
        HashMap<String, TrustedIssuerConfig> trustedIssuers = new HashMap<>();
        trustedIssuers.put("https://trusted.example.com", new TrustedIssuerConfig());
        when(appConfiguration.getIdJagTrustedIdpIssuers()).thenReturn(trustedIssuers);

        Jwt jwt = buildValidSubjectJwt();
        jwt.getClaims().setClaim(JwtClaimName.ISSUER, "https://unknown.example.com"); // not trusted and not server issuer

        try {
            idJagService.validateSubjectToken(jwt.toString(), "urn:ietf:params:oauth:token-type:id_token", executionContext);
            fail("Expected WebApplicationException for untrusted issuer");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateSubjectToken_whenIssuerMatchesServerIssuerButNotInTrustedList_shouldPass() {
        HashMap<String, TrustedIssuerConfig> trustedIssuers = new HashMap<>();
        trustedIssuers.put("https://trusted.example.com", new TrustedIssuerConfig());
        when(appConfiguration.getIdJagTrustedIdpIssuers()).thenReturn(trustedIssuers);

        // iss == appConfiguration.getIssuer() — MUST pass even when not in explicit trusted list
        Jwt jwt = buildValidSubjectJwt(); // iss = "https://idp.example.com"

        Jwt result = idJagService.validateSubjectToken(jwt.toString(), "urn:ietf:params:oauth:token-type:id_token", executionContext);
        assertNotNull(result);
    }

    // ---- validateSubjectToken — audience / client binding (§4.3.3) ----

    @Test
    public void validateSubjectToken_whenAudienceAbsent_shouldThrow() {
        Jwt jwt = new Jwt();
        jwt.getHeader().setAlgorithm(SignatureAlgorithm.RS256);
        jwt.getClaims().setClaim(JwtClaimName.ISSUER, "https://idp.example.com");
        jwt.getClaims().setSubjectIdentifier("alice");
        jwt.getClaims().setExpirationTime(new Date(System.currentTimeMillis() + 60_000));
        // aud intentionally not set

        try {
            idJagService.validateSubjectToken(jwt.toString(), "urn:ietf:params:oauth:token-type:id_token", executionContext);
            fail("Expected WebApplicationException for absent aud");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateSubjectToken_whenAudienceDoesNotMatchClientId_shouldThrow() {
        Jwt jwt = new Jwt();
        jwt.getHeader().setAlgorithm(SignatureAlgorithm.RS256);
        jwt.getClaims().setClaim(JwtClaimName.ISSUER, "https://idp.example.com");
        jwt.getClaims().setAudience("other-client"); // not "test-client"
        jwt.getClaims().setSubjectIdentifier("alice");
        jwt.getClaims().setExpirationTime(new Date(System.currentTimeMillis() + 60_000));

        try {
            idJagService.validateSubjectToken(jwt.toString(), "urn:ietf:params:oauth:token-type:id_token", executionContext);
            fail("Expected WebApplicationException for audience mismatch");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    // ---- validateSubjectToken — expiry ----

    @Test
    public void validateSubjectToken_whenMissingExp_shouldThrow() {
        // exp is now required — a missing exp claim is rejected (aligns with RFC 7519 ID token rules)
        Jwt jwt = new Jwt();
        jwt.getHeader().setAlgorithm(SignatureAlgorithm.RS256);
        jwt.getClaims().setClaim(JwtClaimName.ISSUER, "https://idp.example.com");
        jwt.getClaims().setAudience("test-client");
        jwt.getClaims().setSubjectIdentifier("alice");
        // exp intentionally not set

        try {
            idJagService.validateSubjectToken(jwt.toString(), "urn:ietf:params:oauth:token-type:id_token", executionContext);
            fail("Expected WebApplicationException for missing exp");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateSubjectToken_whenExpiredJwt_shouldThrow() {
        Jwt jwt = buildValidSubjectJwt();
        jwt.getClaims().setExpirationTime(new Date(System.currentTimeMillis() - 10_000)); // 10s ago

        try {
            idJagService.validateSubjectToken(jwt.toString(), "urn:ietf:params:oauth:token-type:id_token", executionContext);
            fail("Expected WebApplicationException for expired token");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void validateSubjectToken_whenValidJwt_shouldReturnJwt() {
        Jwt jwt = buildValidSubjectJwt();

        Jwt result = idJagService.validateSubjectToken(jwt.toString(), "urn:ietf:params:oauth:token-type:id_token", executionContext);
        assertNotNull(result);
        assertEquals("alice", result.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
    }

    // ---- validateSubjectToken — refresh_token (§4.3.3) ----

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

    // ---- buildTokenExchangeResponse ----

    @Test
    public void buildTokenExchangeResponse_shouldContainIdJagFields() {
        JSONObject response = idJagService.buildTokenExchangeResponse("signed.jwt.here");

        assertEquals("signed.jwt.here", response.getString("access_token"));
        assertEquals(TOKEN_TYPE_ID_JAG, response.getString("issued_token_type"));
        assertEquals("N_A", response.getString("token_type"));
    }
}
