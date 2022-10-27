package io.jans.as.server.token.ws.rs;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.model.session.SessionIdState;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrantType;
import io.jans.as.server.model.common.DeviceAuthorizationCacheControl;
import io.jans.as.server.model.common.RefreshToken;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Date;

import static io.jans.as.server.util.TestUtil.assertBadRequest;
import static org.junit.Assert.fail;
import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Yuriy Zabrovarnyy
 */
@Listeners(MockitoTestNGListener.class)
public class TokenRestWebServiceValidatorTest {

    public static final OAuth2AuditLog AUDIT_LOG = new OAuth2AuditLog("", null);

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private ApplicationAuditLogger applicationAuditLogger;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private AbstractCryptoProvider cryptoProvider;

    @InjectMocks
    private TokenRestWebServiceValidator validator;

    @Test
    public void validateSessionForTokenExchange_whenSessionIsNull_shouldThrowError() {
        try {
            validator.validateSessionForTokenExchange(null, "test_device_secret", AUDIT_LOG);
        } catch (WebApplicationException e) {
            assertBadRequest(e.getResponse());
            return;
        }
        fail("No error for invalid session.");
    }

    @Test
    public void validateSessionForTokenExchange_whenSessionIsNotAuthenticated_shouldThrowError() {
        try {
            SessionId sessionId =  new SessionId();
            sessionId.setState(SessionIdState.UNAUTHENTICATED);

            validator.validateSessionForTokenExchange(sessionId, "test_device_secret", AUDIT_LOG);
        } catch (WebApplicationException e) {
            assertBadRequest(e.getResponse());
            return;
        }
        fail("No error for unauthenticated session.");
    }

    @Test
    public void validateSessionForTokenExchange_whenSessionIsAuthenticated_shouldPassSuccessfully() {
        SessionId sessionId =  new SessionId();
        sessionId.setState(SessionIdState.AUTHENTICATED);

        validator.validateSessionForTokenExchange(sessionId, "test_device_secret", AUDIT_LOG);
    }

    @Test
    public void validateActorToken_withEmptyActorToken_shouldThrowError() {
        try {
            validator.validateActorToken(null, AUDIT_LOG);
        } catch (WebApplicationException e) {
            assertBadRequest(e.getResponse());
            return;
        }
        fail("No error for invalid actor token.");
    }

    @Test
    public void validateActorToken_withNotBlankActorToken_shouldPassSuccessfully() {
        validator.validateActorToken("not_blank_actor_token", AUDIT_LOG);
    }

    @Test
    public void validateAudience_withEmptyAudience_shouldThrowError() {
        try {
            validator.validateAudience(null, AUDIT_LOG);
        } catch (WebApplicationException e) {
            assertBadRequest(e.getResponse());
            return;
        }
        fail("No error for invalid audience.");
    }

    @Test
    public void validateAudience_withNotBlankAudience_shouldPassSuccessfully() {
        validator.validateAudience("not_blank_audience", AUDIT_LOG);
    }

    @Test
    public void validateSubjectTokenType_withInvalidTokenType_shouldThrowError() {
        try {
            validator.validateSubjectTokenType("urn:mytype", AUDIT_LOG);
        } catch (WebApplicationException e) {
            assertBadRequest(e.getResponse());
            return;
        }
        fail("No error for invalid subject token type.");
    }

    @Test
    public void validateSubjectTokenType_withValidTokenType_shouldPassSuccessfully() {
        validator.validateSubjectTokenType(Constants.SUBJECT_TOKEN_TYPE_ID_TOKEN, AUDIT_LOG);
    }

    @Test
    public void validateActorTokenType_withInvalidTokenType_shouldThrowError() {
        try {
            validator.validateActorTokenType("urn:mytype", AUDIT_LOG);
        } catch (WebApplicationException e) {
            assertBadRequest(e.getResponse());
            return;
        }
        fail("No error for invalid actor token type.");
    }

    @Test
    public void validateActorTokenType_withValidTokenType_shouldPassSuccessfully() {
        validator.validateActorTokenType(Constants.ACTOR_TOKEN_TYPE_DEVICE_SECRET, AUDIT_LOG);
    }


    @Test
    public void validateParams_whenGrantTypeIsBlank_shouldRaiseError() {
        try {
            validator.validateParams("", "some_code", "https://my.redirect", "refresh_token", AUDIT_LOG);
        } catch (WebApplicationException e) {
            assertBadRequest(e.getResponse());
            return;
        }
        fail("No error for blank grant type.");
    }

    @Test
    public void validateParams_whenGrantTypeIsAuthorizationCodeAndCodeIsBlank_shouldRaiseError() {
        try {
            validator.validateParams(GrantType.AUTHORIZATION_CODE.getValue(), "", "https://my.redirect", "refresh_token", AUDIT_LOG);
        } catch (WebApplicationException e) {
            assertBadRequest(e.getResponse());
            return;
        }
        fail("No error for blank code for AUTHORIZATION_CODE grant type.");
    }


    @Test
    public void validateParams_whenGrantTypeIsAuthorizationCodeAndRedirectUriIsBlank_shouldRaiseError() {
        try {
            validator.validateParams(GrantType.AUTHORIZATION_CODE.getValue(), "some_code", "", "refresh_token", AUDIT_LOG);
        } catch (WebApplicationException e) {
            assertBadRequest(e.getResponse());
            return;
        }
        fail("No error for blank redirect_uri for AUTHORIZATION_CODE grant type.");
    }

    @Test
    public void validateParams_whenGrantTypeIsRefreshTokenAndRefreshTokenIsBlank_shouldRaiseError() {
        try {
            validator.validateParams(GrantType.REFRESH_TOKEN.getValue(), "some_code", "https://my.redirect", "", AUDIT_LOG);
        } catch (WebApplicationException e) {
            assertBadRequest(e.getResponse());
            return;
        }
        fail("No error for blank refresh_token for REFRESH_TOKEN grant type.");
    }

    @Test
    public void validateParams_whenGrantTypeIsAuthorizationCodeAndCodeIsNotBlank_shouldNotRaiseError() {
        try {
            validator.validateParams(GrantType.AUTHORIZATION_CODE.getValue(), "some_code", "https://my.redirect", "", AUDIT_LOG);
        } catch (WebApplicationException e) {
            fail("Error occurs. We should not get it.");
        }
    }

    @Test
    public void validateParams_whenGrantTypeIsRefreshTokenAndRefreshTokenIsNotBlank_shouldNotRaiseError() {
        try {
            validator.validateParams(GrantType.REFRESH_TOKEN.getValue(), "", "https://my.redirect", "refresh_token", AUDIT_LOG);
        } catch (WebApplicationException e) {
            fail("Error occurs. We should not get it.");
        }
    }

    @Test
    public void validateGrantType_whenClientDotNotHaveGrantType_shouldRaiseError() {
        try {
            validator.validateGrantType(GrantType.AUTHORIZATION_CODE, new Client(), AUDIT_LOG);
        } catch (WebApplicationException e) {
            assertBadRequest(e.getResponse());
            return;
        }
        fail("No error for grant_type which is not allowed by client's grant_types.");
    }

    @Test
    public void validateClient_whenClientIsNull_shouldRaiseError() {
        try {
            validator.validateClient(null, AUDIT_LOG);
        } catch (WebApplicationException e) {
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), e.getResponse().getStatus());
            return;
        }
        fail("No error when client is null.");
    }

    @Test
    public void validateClient_whenClientIsDisabled_shouldRaiseError() {
        try {
            Client client = new Client();
            client.setDisabled(true);
            validator.validateClient(client, AUDIT_LOG);
        } catch (WebApplicationException e) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), e.getResponse().getStatus());
            return;
        }
        fail("No error when client is null.");
    }

    @Test
    public void validateDeviceAuthorizationCacheControl_whenDeviceAuthzIsNull_shouldRaiseError() {
        try {
            Client client = new Client();
            client.setClientId("testId");

            validator.validateDeviceAuthorization(client, "code", null, AUDIT_LOG);
        } catch (WebApplicationException e) {
            assertBadRequest(e.getResponse());
            return;
        }
        fail("No error when client is null.");
    }

    @Test
    public void validateDeviceAuthorizationCacheControl_whenDeviceAuthzDoesNotBelongToClient_shouldRaiseError() {
        try {
            Client client = new Client();
            client.setClientId("testId");

            DeviceAuthorizationCacheControl deviceAuthorization = new DeviceAuthorizationCacheControl();
            deviceAuthorization.setClient(client);

            validator.validateDeviceAuthorization(new Client(), "code", deviceAuthorization, AUDIT_LOG);
        } catch (WebApplicationException e) {
            assertBadRequest(e.getResponse());
            return;
        }
        fail("No error when client is null.");
    }

    @Test
    public void validateGrant_whenGrantIsNull_shouldRaiseError() {
        try {
            Client client = new Client();
            client.setClientId("testId");

            validator.validateGrant(null, client, "code", AUDIT_LOG);
        } catch (WebApplicationException e) {
            assertBadRequest(e.getResponse());
            return;
        }
        fail("No error when grant is null.");
    }

    @Test
    public void validateGrant_whenGrantDoesNotBelongToGivenClient_shouldRaiseError() {
        try {
            Client client = new Client();
            client.setClientId("testId");

            AuthorizationGrant grant = new AuthorizationGrant() {
                @Override
                public GrantType getGrantType() {
                    return GrantType.AUTHORIZATION_CODE;
                }
            };
            grant.init(new User(), AuthorizationGrantType.AUTHORIZATION_CODE, new Client(), new Date());

            validator.validateGrant(grant, client, "code", AUDIT_LOG);
        } catch (WebApplicationException e) {
            assertBadRequest(e.getResponse());
            return;
        }
        fail("No error when grant and client is not matched.");
    }

    @Test
    public void validateGrant_whenGrantMatchesToClient_shouldNotRaiseError() {
        Client client = new Client();
        client.setClientId("testId");

        AuthorizationGrant grant = new AuthorizationGrant() {
            @Override
            public GrantType getGrantType() {
                return GrantType.AUTHORIZATION_CODE;
            }
        };
        grant.init(new User(), AuthorizationGrantType.AUTHORIZATION_CODE, client, new Date());

        validator.validateGrant(grant, client, "code", AUDIT_LOG);
    }

    @Test
    public void validateRefreshToken_whenRefreshTokenIsNull_shouldRaiseError() {
        try {
            validator.validateRefreshToken(null, AUDIT_LOG);
        } catch (WebApplicationException e) {
            assertBadRequest(e.getResponse());
            return;
        }
        fail("No error when refreshToken is null.");
    }

    @Test
    public void validateRefreshToken_whenRefreshTokenIsExpired_shouldRaiseError() {
        try {
            validator.validateRefreshToken(new RefreshToken("code", new Date(), new Date(0)), AUDIT_LOG);
        } catch (WebApplicationException e) {
            assertBadRequest(e.getResponse());
            return;
        }
        fail("No error when refreshToken is expired.");
    }

    @Test
    public void validateUser_whenUserIsNull_shouldRaiseError() {
        try {
            validator.validateUser(null, AUDIT_LOG);
        } catch (WebApplicationException e) {
            assertEquals(e.getResponse().getStatus(), 401);
            return;
        }
        fail("No error when user is null.");
    }

    @Test
    public void validateUser_whenUserIsValid_shouldNotRaiseError() {
        try {
            final User user = new User();
            user.setUserId("test_user");
            user.setCreatedAt(new Date());

            validator.validateUser(user, AUDIT_LOG);
            return;
        } catch (WebApplicationException e) {
            // ignore
        }
        fail("Error for valid user is raised.");
    }
}
