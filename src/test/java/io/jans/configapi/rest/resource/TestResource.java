package io.jans.configapi.rest.resource;

import io.jans.as.common.model.registration.Client;
import io.jans.configapi.util.TestUtil;
import javax.inject.Inject;
import javax.ws.rs.*;

import org.slf4j.Logger;

@Path("/test")
public class TestResource extends BaseResource {

    @Inject
    Logger log;

    @Inject
    TestUtil testUtil;

    @GET
    public String createTestToken(@QueryParam("method") String method, @QueryParam("path") String path)
            throws Exception {
        String token = this.testUtil.createTestToken(method, path);
        return token;
    }

    @POST
    public void createTestClient() throws Exception {
        Client client = testUtil.init();
        System.out.println(" ********************* clientid = "+client.getClientId()+" ********************* ");
    }

    @DELETE
    public void deleteTestClient() throws Exception {
        this.testUtil.deleteTestClient();
    }
}
