package org.xdi.oxd.client;

import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.ClientReadParams;
import org.xdi.oxd.common.params.RegisterClientParams;
import org.xdi.oxd.common.response.RegisterClientOpResponse;

import java.io.IOException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/06/2014
 */

public class ClientReadTest {
    @Parameters({"host", "port", "discoveryUrl", "redirectUrl", "clientName"})
    @Test
    public void clientRead(String host, int port, String discoveryUrl, String redirectUrl, String clientName) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            // register client first
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

            // read client
            final ClientReadParams clientReadParams = new ClientReadParams();
            clientReadParams.setRegistrationAccessToken(r.getRegistrationAccessToken());
            clientReadParams.setRegistrationClientUri(r.getRegistrationClientUri());

            final Command clientReadCommand = new Command(CommandType.CLIENT_READ);
            clientReadCommand.setParamsObject(clientReadParams);
            final CommandResponse clientReadResponse = client.send(clientReadCommand);
            Assert.assertNotNull(clientReadResponse);
            System.out.println(clientReadResponse);

            final RegisterClientOpResponse readR = clientReadResponse.dataAsResponse(RegisterClientOpResponse.class);
            Assert.assertNotNull(readR);
            Assert.assertNotNull(readR.getRegistrationClientUri());
            Assert.assertTrue(readR.getClientId().equals(r.getClientId()));

        } finally {
            CommandClient.closeQuietly(client);
        }
    }
}
