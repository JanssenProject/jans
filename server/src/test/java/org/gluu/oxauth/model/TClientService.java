package org.gluu.oxauth.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;
import org.gluu.oxauth.BaseTest;
import org.gluu.oxauth.register.ws.rs.RegisterRestWebService;
import org.gluu.oxauth.util.ServerUtil;
import org.gluu.oxauth.ws.rs.ClientTestUtil;

import javax.ejb.Stateless;
import javax.inject.Named;
import javax.ws.rs.core.Response;
import java.net.URI;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 */
@Stateless
@Named

public class TClientService {

    private TClientService() {
    }

    public static RegisterResponse register(RegisterRequest registerRequest, URI uri) throws JsonProcessingException {
        RegisterRestWebService registerWs = WebServiceFactory.instance().createRegisterWs(uri.toString());
        Response response = registerWs.requestRegister(ServerUtil.toPrettyJson(registerRequest.getJSONParameters()), null, null);
        String entity = response.readEntity(String.class);

        BaseTest.showResponse("TClientService", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        final RegisterResponse registerResponse = RegisterResponse.valueOf(entity);
        ClientTestUtil.assert_(registerResponse);
        return registerResponse;
    }
}
