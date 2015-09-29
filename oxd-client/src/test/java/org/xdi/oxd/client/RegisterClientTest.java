package org.xdi.oxd.client;

import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.RegisterClientParams;
import org.xdi.oxd.common.response.RegisterClientOpResponse;

import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 13/08/2013
 */

public class RegisterClientTest {

    @Parameters({"host", "port", "discoveryUrl", "redirectUrl", "clientName"})
    @Test
    public void minimum(String host, int port, String discoveryUrl, String redirectUrl, String clientName) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            final RegisterClientParams params = new RegisterClientParams();
            params.setDiscoveryUrl(discoveryUrl);
            params.setRedirectUrl(Lists.newArrayList(redirectUrl));
            params.setClientName(clientName);

            final Command command = new Command(CommandType.REGISTER_CLIENT);
            command.setParamsObject(params);
            final CommandResponse response = client.send(command);
            Assert.assertNotNull(response);
            System.out.println(response);

            final RegisterClientOpResponse r = response.dataAsResponse(RegisterClientOpResponse.class);
            Assert.assertNotNull(r);
            Assert.assertNotNull(r.getRegistrationClientUri());
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    @Parameters({"host", "port", "discoveryUrl", "redirectUrl", "clientName", "register_client_app_type",
            "register_client_response_types", "register_client_grant_types", "register_client_contacts",
            "register_client_jwks_uri", "register_client_token_endpoint_auth_method"
    })
//    @Test(invocationCount = 100, threadPoolSize = 20)
    @Test
    public void allParameters(String host, int port, String discoveryUrl, String redirectUrl, String clientName, String applicationTypes,
                              String responseTypes, String grantTypes, String contacts,
                              String jwksUri, String tokenEndpointAuthMethod) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            final RegisterClientParams params = new RegisterClientParams();
            params.setDiscoveryUrl(discoveryUrl);
            params.setRedirectUrl(Lists.newArrayList(redirectUrl));
            params.setClientName(clientName);
            params.setResponseTypes(responseTypes);
            params.setApplicationType(applicationTypes);
            params.setGrantTypes(grantTypes);
            params.setContacts(contacts);
            params.setJwksUri(jwksUri);
            params.setTokenEndpointAuthMethod(tokenEndpointAuthMethod);
            params.setRequestUris(new ArrayList<String>());

            final Command command = new Command(CommandType.REGISTER_CLIENT);
            command.setParamsObject(params);
            final CommandResponse response = client.send(command);
            Assert.assertNotNull(response);
            System.out.println(response);

            final RegisterClientOpResponse r = response.dataAsResponse(RegisterClientOpResponse.class);
            Assert.assertNotNull(r);
            Assert.assertNotNull(r.getRegistrationClientUri());
        } finally {
            CommandClient.closeQuietly(client);
        }
    }
}
