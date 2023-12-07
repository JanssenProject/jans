package io.jans.as.server.authorize.ws.rs;

import com.google.common.collect.Lists;
import io.jans.as.model.common.Prompt;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * @author Yuriy Z
 */
public class AuthzRequestTest {

    @Test
    public void addPrompt_whenCalled_shouldSetCorrectValue() {
        AuthzRequest authzRequest = new AuthzRequest();

        assertNull(authzRequest.getPrompt());
        assertTrue(authzRequest.getPromptList().isEmpty());

        authzRequest.addPrompt(Prompt.LOGIN);

        assertEquals(authzRequest.getPrompt(), "login");
        assertEquals(authzRequest.getPromptList(), Lists.newArrayList(Prompt.LOGIN));

        authzRequest.addPrompt(Prompt.CONSENT);

        assertTrue(authzRequest.getPrompt().contains("consent"));
        assertTrue(authzRequest.getPrompt().contains("login"));
        assertTrue(authzRequest.getPromptList().containsAll(Lists.newArrayList(Prompt.CONSENT, Prompt.LOGIN)));
    }

    @Test
    public void removePrompt_whenCalled_shouldSetCorrectValue() {
        AuthzRequest authzRequest = new AuthzRequest();
        authzRequest.setPromptList(Lists.newArrayList(Prompt.LOGIN, Prompt.CONSENT));

        assertTrue(authzRequest.getPrompt().contains("consent"));
        assertTrue(authzRequest.getPrompt().contains("login"));
        assertTrue(authzRequest.getPromptList().containsAll(Lists.newArrayList(Prompt.CONSENT, Prompt.LOGIN)));

        authzRequest.removePrompt(Prompt.CONSENT);

        assertFalse(authzRequest.getPrompt().contains("consent"));
        assertFalse(authzRequest.getPromptList().contains(Prompt.CONSENT));

        assertTrue(authzRequest.getPrompt().contains("login"));
        assertTrue(authzRequest.getPromptList().contains(Prompt.LOGIN));

        authzRequest.removePrompt(Prompt.LOGIN);

        assertTrue(authzRequest.getPrompt().isEmpty());
        assertTrue(authzRequest.getPromptList().isEmpty());

    }
}
