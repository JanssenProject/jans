package org.xdi.oxd.server.kong;

import org.codehaus.jackson.JsonNode;
import org.jboss.resteasy.client.ClientResponse;

import javax.ws.rs.*;

/**
 * Created by yuriy on 17.10.16.
 */

public interface KongApiService {

    @POST
    @Consumes({"application/x-www-form-urlencoded"})
    @Produces({"application/json"})
    ClientResponse<JsonNode> addApi(@FormParam("name") String name,
                                    @FormParam("request_host") String requestHost,
                                    @FormParam("request_path") String requestPath,
                                    @FormParam("strip_request_path") Boolean stripRequestPath,
                                    @FormParam("preserve_host") Boolean preserveHost,
                                    @FormParam("upstream_url") String upstreamUrl);

    @GET
    @Produces({"application/json"})
    ClientResponse<JsonNode> getApis();

    @Path("{apiId}")
    @DELETE
    ClientResponse<String> deleteApi(@PathParam("apiId") String apiId);

    @POST
    @Path("{apiId}/plugins/")
    @Consumes({"application/x-www-form-urlencoded"})
    @Produces({"application/json"})
    ClientResponse<JsonNode> addKongUmaRsPlugin(@PathParam("apiId") String apiId,
                                                @FormParam("name") String name,
                                                @FormParam("config.oxd_host") String oxdHost,
                                                @FormParam("config.oxd_port") String oxdPort,
                                                @FormParam("config.uma_server_host") String umaServerHost,
                                                @FormParam("config.protection_document") String protectionDocumentJson
    );

}
