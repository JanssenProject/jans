package org.gluu.configapi.rest.resource;

import org.gluu.configapi.filters.ProtectedApi;
import org.gluu.configapi.service.ConfigurationService;
import org.gluu.configapi.util.ApiConstants;
import org.gluu.oxauth.model.config.Conf;
import org.gluu.oxauth.model.config.WebKeysConfiguration;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Yuriy Zabrovarnyy
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.JWKS)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JwksResource extends BaseResource {

    @Inject
    ConfigurationService configurationService;

    @GET
    @ProtectedApi(scopes = { READ_ACCESS })
    public Response get()  {
        final String json = configurationService.find("oxAuthConfWebKeys").getWebKeys().toString();
        return Response.ok(json).build();
    }

    @PUT
    @ProtectedApi(scopes = { WRITE_ACCESS })
    public Response put(WebKeysConfiguration webkeys)  {
        final Conf conf = configurationService.find();
        conf.setWebKeys(webkeys);
        configurationService.merge(conf);
        return Response.ok().build();
    }
}
