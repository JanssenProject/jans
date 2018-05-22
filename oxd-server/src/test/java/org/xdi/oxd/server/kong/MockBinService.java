package org.xdi.oxd.server.kong;

import org.codehaus.jackson.JsonNode;
import org.jboss.resteasy.client.ClientResponse;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * Created by yuriy on 18.10.16.
 */
public interface MockBinService {

    @Path("/status/200/hello")
    @GET
    @Produces({"application/json"})
    ClientResponse<JsonNode> status200Hello(@HeaderParam("Host") String host, @HeaderParam("Authorization") String authorization);
}
