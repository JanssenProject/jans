package io.jans.as.server.token.ws.rs;

import io.jans.as.model.common.SubjectTokenType;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.common.AuthorizationGrantList;
import jakarta.ws.rs.WebApplicationException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class TxTokenValidatorTest {

    public static final OAuth2AuditLog AUDIT_LOG = new OAuth2AuditLog("", null);

    @Mock
    private Logger log;

    @Mock
    private ApplicationAuditLogger applicationAuditLogger;

    @Mock
    private AuthorizationGrantList authorizationGrantList;

    @Mock
    private AbstractCryptoProvider cryptoProvider;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @InjectMocks
    private TxTokenValidator txTokenValidator;

    @Test
    public void validateRequestedTokenType_forValidValue_shouldNotRaiseException() {
        txTokenValidator.validateRequestedTokenType("urn:ietf:params:oauth:token-type:txn_token", AUDIT_LOG);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateRequestedTokenType_forInvalidValue_shouldNotRaiseException() {
        txTokenValidator.validateRequestedTokenType("dummy_token", AUDIT_LOG);
    }

    @Test
    public void validateSubjectTokenType_forValidValue_shouldNotRaiseException() {
        final SubjectTokenType validated1 = txTokenValidator.validateSubjectTokenType(SubjectTokenType.ID_TOKEN.getName(), AUDIT_LOG);
        final SubjectTokenType validated2 = txTokenValidator.validateSubjectTokenType(SubjectTokenType.ACCESS_TOKEN.getName(), AUDIT_LOG);

        assertEquals(validated1, SubjectTokenType.ID_TOKEN);
        assertEquals(validated2, SubjectTokenType.ACCESS_TOKEN);
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateSubjectTokenType_forInvalidValue_shouldNotRaiseException() {
        txTokenValidator.validateSubjectTokenType("dummy_token", AUDIT_LOG);
    }
}
