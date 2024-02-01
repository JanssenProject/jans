package io.jans.as.server.authorize.ws.rs;

import io.jans.as.model.authzdetails.AuthzDetails;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.external.ExternalAuthzDetailTypeService;
import jakarta.ws.rs.WebApplicationException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashSet;

import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class AuthzDetailsServiceTest {

    @InjectMocks
    private AuthzDetailsService authzDetailsService;

    @Mock
    private Logger log;

    @Mock
    private ExternalAuthzDetailTypeService externalAuthzDetailTypeService;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Test
    public void checkAuthzDetails_whenRequestedAuthzDetailsIsWiderThenAuthorizedDetails_shouldNarrowGrantedDetails() {
        String requested = "[\n" +
                "  {\n" +
                "    \"type\": \"internal_a1\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"type\": \"internal_a2\"\n" +
                "  }\n" +
                "]";

        String authorized = "[\n" +
                "  {\n" +
                "    \"type\": \"internal_a1\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"type\": \"internal_a3\"\n" +
                "  }\n" +
                "]";

        String granted = "[\n" +
                "  {\n" +
                "    \"type\": \"internal_a1\"\n" +
                "  }\n" +
                "]";

        final AuthzDetails grantedDetails = authzDetailsService.checkAuthzDetails(AuthzDetails.of(requested), AuthzDetails.of(authorized));
        assertNotNull(grantedDetails);
        assertTrue(grantedDetails.similar(granted)); // it must reflect authorized
        assertFalse(grantedDetails.similar(requested)); // it must not be similar to requested
    }

    @Test
    public void checkAuthzDetails_whenAuthzDetailsIsEmpty_shouldReturnNull() {
        assertNull(authzDetailsService.checkAuthzDetails(null, null));
    }

    @Test
    public void validateAuthorizationDetails_withoutAuthzDetails_shouldPassSuccessfully() {
        Client client = new Client();
        
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setClient(client);

        authzDetailsService.validateAuthorizationDetails("", executionContext);
    }

    @Test
    public void validateAuthorizationDetails_withInvalidAuthzDetails_throwException() {
        Client client = new Client();

        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setClient(client);

        assertThrows(WebApplicationException.class, () -> authzDetailsService.validateAuthorizationDetails("not_valid_json", executionContext));
    }

    @Test
    public void validateAuthorizationDetails_withNotSupportedScriptType_throwException() {
        Client client = new Client();

        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setClient(client);

        assertThrows(WebApplicationException.class, () -> authzDetailsService.validateAuthorizationDetails("[{\"type\":\"internal_type\"}]", executionContext));
    }

    @Test
    public void validateAuthorizationDetails_withNotSupportedClientType_throwException() {
        Client client = new Client();

        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setClient(client);

        when(externalAuthzDetailTypeService.getSupportedAuthzDetailsTypes()).thenReturn(new HashSet<>(Collections.singletonList("internal_type")));

        assertThrows(WebApplicationException.class, () -> authzDetailsService.validateAuthorizationDetails("[{\"type\":\"internal_type\"}]", executionContext));
    }

    @Test
    public void validateAuthorizationDetails_withSupportedClientAndScriptType_shouldPassSuccessfully() {
        Client client = new Client();
        client.getAttributes().setAuthorizationDetailsTypes(Collections.singletonList("internal_type"));

        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setClient(client);

        when(externalAuthzDetailTypeService.getSupportedAuthzDetailsTypes()).thenReturn(new HashSet<>(Collections.singletonList("internal_type")));

        authzDetailsService.validateAuthorizationDetails("[{\"type\":\"internal_type\"}]", executionContext);
    }
}
