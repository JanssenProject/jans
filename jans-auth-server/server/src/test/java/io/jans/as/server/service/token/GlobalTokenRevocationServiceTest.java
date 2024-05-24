package io.jans.as.server.service.token;

import com.google.common.collect.Lists;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.revoke.GlobalTokenRevocationRequest;
import io.jans.as.model.session.EndSessionErrorResponseType;
import io.jans.as.server.model.session.SessionClient;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.GrantService;
import io.jans.as.server.service.ScopeService;
import io.jans.as.server.service.SessionIdService;
import io.jans.as.server.service.UserService;
import jakarta.ws.rs.WebApplicationException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class GlobalTokenRevocationServiceTest {

    @InjectMocks
    private GlobalTokenRevocationService globalTokenRevocationService;

    @Mock
    private Logger log;

    @Mock
    private UserService userService;

    @Mock
    private SessionIdService sessionIdService;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Mock
    private Identity identity;

    @Mock
    private ScopeService scopeService;

    @Mock
    private GrantService grantService;

    @Test
    public void parseRequest_forValidRequest_shouldParseCorrectly() {
        String json = "{\n" +
                "  \"sub_id\": {\n" +
                "    \"format\": \"mail\",\n" +
                "    \"id\": \"user@example.com\"\n" +
                "  }\n" +
                "}";

        final GlobalTokenRevocationRequest request = globalTokenRevocationService.parseRequest(json);

        assertEquals("mail", request.getSubId().getFormat());
        assertEquals("user@example.com", request.getSubId().getId());
    }

    @Test
    public void validateAccess_withClientThatHasValidScope_shouldPassSuccessfully() {
        String[] scopes = {"11"};

        Client client = new Client();
        client.setScopes(scopes);

        SessionClient sessionClient = new SessionClient();
        sessionClient.setClient(client);

        when(identity.getSessionClient()).thenReturn(sessionClient);
        when(scopeService.getScopeIdsByDns(Arrays.asList(scopes))).thenReturn(Lists.newArrayList("global_token_revocation"));

        globalTokenRevocationService.validateAccess();;
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateAccess_withClientThatHasInvalidScope_shouldThrowError() {
        String[] scopes = {"22"};

        Client client = new Client();
        client.setScopes(scopes);

        SessionClient sessionClient = new SessionClient();
        sessionClient.setClient(client);

        when(identity.getSessionClient()).thenReturn(sessionClient);
        when(scopeService.getScopeIdsByDns(Arrays.asList(scopes))).thenReturn(Lists.newArrayList("edit"));
        when(errorResponseFactory.getErrorAsJson(EndSessionErrorResponseType.INVALID_REQUEST)).thenReturn("{}");

        globalTokenRevocationService.validateAccess();;
    }

    @Test(expectedExceptions = WebApplicationException.class)
    public void validateAccess_withoutAuthenticatedClient_shouldThrowException() {
        String[] scopes = {"11"};

        when(identity.getSessionClient()).thenReturn(null);
        when(scopeService.getScopeIdsByDns(Arrays.asList(scopes))).thenReturn(Lists.newArrayList("global_token_revocation"));
        when(errorResponseFactory.getErrorAsJson(EndSessionErrorResponseType.INVALID_REQUEST)).thenReturn("{}");

        globalTokenRevocationService.validateAccess();;
    }
}
