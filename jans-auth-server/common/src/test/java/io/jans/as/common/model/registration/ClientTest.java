package io.jans.as.common.model.registration;

import com.google.common.collect.Lists;
import io.jans.as.model.common.AuthenticationMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Z
 */
public class ClientTest {

    @Test
    public void hasAuthenticationMethod_whenHasMainValue_shouldReturnTrue() {
        Client client = new Client();
        client.setTokenEndpointAuthMethod("client_secret_basic");

        assertTrue(client.hasAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC));
    }

    @Test
    public void hasAuthenticationMethod_whenAdditionalValueHasIt_shouldReturnTrue() {
        Client client = new Client();
        client.setTokenEndpointAuthMethod("client_secret_basic");
        client.getAttributes().setAdditionalTokenEndpointAuthMethods(Lists.newArrayList("client_secret_jwt"));

        assertTrue(client.hasAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC));
    }

    @Test
    public void hasAuthenticationMethod_whenDoesNotHaveValue_shouldReturnFalse() {
        Client client = new Client();
        client.setTokenEndpointAuthMethod("client_secret_basic");

        assertFalse(client.hasAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT));
    }
}
