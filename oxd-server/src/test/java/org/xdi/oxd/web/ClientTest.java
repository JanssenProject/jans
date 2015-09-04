package org.xdi.oxd.web;

import junit.framework.Assert;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.DiscoveryParams;
import org.xdi.oxd.license.client.ClientUtils;
import org.xdi.oxd.license.client.Jackson;

/**
 * Created by yuriy on 8/24/2015.
 */
public class ClientTest {

    @Test
    public void test() throws Exception {
        Command command = new Command();
        command.setCommandType(CommandType.DISCOVERY);

        command.setParamsObject(new DiscoveryParams("https://seed.gluu.org/.well-known/openid-configuration"));

        String url = "http://localhost:8080/rest/command";

        ClientRequest clientRequest = new ClientRequest(url, new ApacheHttpClient4Executor(ClientUtils.createHttpClientTrustAll()));
        clientRequest.formParameter("request", Jackson.asJsonSilently(command));

        final ClientResponse response = clientRequest.post();

        System.out.println(response);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.getStatus() == 200);

    }
}
