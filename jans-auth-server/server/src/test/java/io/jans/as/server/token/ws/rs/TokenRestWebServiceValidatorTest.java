package io.jans.as.server.token.ws.rs;

import io.jans.as.model.common.GrantType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import jakarta.ws.rs.WebApplicationException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static io.jans.as.server.util.TestUtil.assertBadRequest;
import static org.junit.Assert.fail;

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

    @InjectMocks
    private TokenRestWebServiceValidator tokenRestWebServiceValidator;

    @Test
    public void validateParams_whenGrantTypeIsBlank_shouldRaiseError() {
        try {
            tokenRestWebServiceValidator.validateParams("", "some_code", "https://my.redirect", "refresh_token", AUDIT_LOG);
        } catch (WebApplicationException e) {
            assertBadRequest(e.getResponse());
            return;
        }
        fail("No error for blank grant type.");
    }

    @Test
    public void validateParams_whenGrantTypeIsAuthorizationCodeAndCodeIsBlank_shouldRaiseError() {
        try {
            tokenRestWebServiceValidator.validateParams(GrantType.AUTHORIZATION_CODE.getValue(), "", "https://my.redirect", "refresh_token", AUDIT_LOG);
        } catch (WebApplicationException e) {
            assertBadRequest(e.getResponse());
            return;
        }
        fail("No error for blank code for AUTHORIZATION_CODE grant type.");
    }


    @Test
    public void validateParams_whenGrantTypeIsAuthorizationCodeAndRedirectUriIsBlank_shouldRaiseError() {
        try {
            tokenRestWebServiceValidator.validateParams(GrantType.AUTHORIZATION_CODE.getValue(), "some_code", "", "refresh_token", AUDIT_LOG);
        } catch (WebApplicationException e) {
            assertBadRequest(e.getResponse());
            return;
        }
        fail("No error for blank redirect_uri for AUTHORIZATION_CODE grant type.");
    }

    @Test
    public void validateParams_whenGrantTypeIsRefreshTokenAndRefreshTokenIsBlank_shouldRaiseError() {
        try {
            tokenRestWebServiceValidator.validateParams(GrantType.REFRESH_TOKEN.getValue(), "some_code", "https://my.redirect", "", AUDIT_LOG);
        } catch (WebApplicationException e) {
            assertBadRequest(e.getResponse());
            return;
        }
        fail("No error for blank refresh_token for REFRESH_TOKEN grant type.");
    }

    @Test
    public void validateParams_whenGrantTypeIsAuthorizationCodeAndCodeIsNotBlank_shouldNotRaiseError() {
        try {
            tokenRestWebServiceValidator.validateParams(GrantType.AUTHORIZATION_CODE.getValue(), "some_code", "https://my.redirect", "", AUDIT_LOG);
        } catch (WebApplicationException e) {
            fail("Error occurs. We should not get it.");
        }
    }

    @Test
    public void validateParams_whenGrantTypeIsRefreshTokenAndRefreshTokenIsNotBlank_shouldNotRaiseError() {
        try {
            tokenRestWebServiceValidator.validateParams(GrantType.REFRESH_TOKEN.getValue(), "", "https://my.redirect", "refresh_token", AUDIT_LOG);
        } catch (WebApplicationException e) {
            fail("Error occurs. We should not get it.");
        }
    }

    @Test
    public void validateGrantType_whenClientDotNotHaveGrantType_shouldRaiseError() {
        try {
            tokenRestWebServiceValidator.validateGrantType(GrantType.AUTHORIZATION_CODE, new GrantType[0], AUDIT_LOG);
        } catch (WebApplicationException e) {
            assertBadRequest(e.getResponse());
            return;
        }
        fail("No error for grant_type which is not allowed by client's grant_types.");
    }
}
