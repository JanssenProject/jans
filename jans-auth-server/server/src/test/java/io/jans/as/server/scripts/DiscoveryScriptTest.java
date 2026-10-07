package io.jans.as.server.scripts;

import io.jans.as.common.model.registration.Client;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.discovery.DiscoveryType;
import org.json.JSONObject;
import org.testng.annotations.Test;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * Demonstrates how to unit-test {@link DiscoveryType} interception scripts.
 *
 * <p>The strategy follows the recommendation in
 * <a href="https://github.com/JanssenProject/jans/issues/12663#issuecomment-3581063779">jans#12663</a>:
 * exercise the script's {@code modifyResponse} method directly, covering positive
 * and negative paths that depend on what the script logic actually reads from the
 * {@link ExecutionContext}.
 *
 * @author Yuriy Z
 */
public class DiscoveryScriptTest {

    // ---------------------------------------------------------------------
    // Scenario 1 — script does not touch ExecutionContext.
    // A bare `new ExecutionContext()` is enough; no setup required.
    // ---------------------------------------------------------------------

    @Test
    public void modifyResponse_contextFreeScript_withEmptyContext_addsKey() {
        DiscoveryType script = contextFreeScript();
        JSONObject response = new JSONObject();

        boolean modified = script.modifyResponse(response, new ExecutionContext());

        assertTrue(modified);
        assertEquals(response.getString("key_from_java"), "value_from_script_on_java");
    }

    // ---------------------------------------------------------------------
    // Scenario 2 — script reads `executionContext.getClient().getClientId()`.
    // Negative: empty ExecutionContext means client == null, so NPE.
    // ---------------------------------------------------------------------

    @Test
    public void modifyResponse_clientAwareScript_withEmptyContext_throwsNpe() {
        DiscoveryType script = clientAwareScript();
        JSONObject response = new JSONObject();

        assertThrows(NullPointerException.class,
                () -> script.modifyResponse(response, new ExecutionContext()));
    }

    // ---------------------------------------------------------------------
    // Scenario 3 — same script, positive path with a real Client wired in.
    // Shows the "explicit setter" mocking style from the linked comment.
    // ---------------------------------------------------------------------

    @Test
    public void modifyResponse_clientAwareScript_withClientSet_addsClientId() {
        Client client = new Client();
        client.setClientId("test_id");

        ExecutionContext context = new ExecutionContext();
        context.setClient(client);

        DiscoveryType script = clientAwareScript();
        JSONObject response = new JSONObject();

        boolean modified = script.modifyResponse(response, context);

        assertTrue(modified);
        assertEquals(response.getString("client_id_from_script"), "test_id");
    }

    // ---------------------------------------------------------------------
    // Scenario 4 — same script, positive path using Mockito to stub the
    // pieces of ExecutionContext the script actually touches. Useful when
    // wiring a real ExecutionContext is heavy or has side effects.
    // ---------------------------------------------------------------------

    @Test
    public void modifyResponse_clientAwareScript_withMockedContext_addsClientId() {
        Client client = mock(Client.class);
        when(client.getClientId()).thenReturn("mocked_id");

        ExecutionContext context = mock(ExecutionContext.class);
        when(context.getClient()).thenReturn(client);

        DiscoveryType script = clientAwareScript();
        JSONObject response = new JSONObject();

        boolean modified = script.modifyResponse(response, context);

        assertTrue(modified);
        assertEquals(response.getString("client_id_from_script"), "mocked_id");
    }

    // ---------------------------------------------------------------------
    // Scenario 5 — script signals "do not modify" by returning false.
    // ExternalDiscoveryService propagates this back to the caller; the
    // discovery JSON should remain untouched.
    // ---------------------------------------------------------------------

    @Test
    public void modifyResponse_optOutScript_returnsFalseAndLeavesResponseUntouched() {
        DiscoveryType script = optOutScript();
        JSONObject response = new JSONObject().put("issuer", "https://as.example.org");

        boolean modified = script.modifyResponse(response, new ExecutionContext());

        assertEquals(modified, false);
        assertEquals(response.length(), 1);
        assertEquals(response.getString("issuer"), "https://as.example.org");
    }

    // ---------------------------------------------------------------------
    // Inline DiscoveryType implementations used by the scenarios above.
    // They mirror the snippets discussed in the linked comment and the
    // sample script under docs/script-catalog/discovery/.
    // ---------------------------------------------------------------------

    private static DiscoveryType contextFreeScript() {
        return new BaseDiscoveryScript() {
            @Override
            public boolean modifyResponse(Object responseAsJsonObject, Object context) {
                JSONObject response = (JSONObject) responseAsJsonObject;
                response.accumulate("key_from_java", "value_from_script_on_java");
                return true;
            }
        };
    }

    private static DiscoveryType clientAwareScript() {
        return new BaseDiscoveryScript() {
            @Override
            public boolean modifyResponse(Object responseAsJsonObject, Object context) {
                ExecutionContext executionContext = (ExecutionContext) context;
                JSONObject response = (JSONObject) responseAsJsonObject;
                response.accumulate("client_id_from_script", executionContext.getClient().getClientId());
                return true;
            }
        };
    }

    private static DiscoveryType optOutScript() {
        return new BaseDiscoveryScript() {
            @Override
            public boolean modifyResponse(Object responseAsJsonObject, Object context) {
                return false;
            }
        };
    }

    private static abstract class BaseDiscoveryScript implements DiscoveryType {
        @Override
        public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
            return true;
        }

        @Override
        public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
            return true;
        }

        @Override
        public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
            return true;
        }

        @Override
        public int getApiVersion() {
            return 1;
        }
    }
}
