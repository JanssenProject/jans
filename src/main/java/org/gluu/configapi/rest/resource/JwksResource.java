package org.gluu.configapi.rest.resource;

import com.github.fge.jsonpatch.JsonPatchException;
import org.gluu.configapi.filters.ProtectedApi;
import org.gluu.configapi.service.ConfigurationService;
import org.gluu.configapi.util.ApiConstants;
import org.gluu.configapi.util.Jackson;
import org.gluu.oxauth.model.config.Conf;
import org.gluu.oxauth.model.config.WebKeysConfiguration;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

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
        final Conf conf = configurationService.findWebKeysOnly();
        conf.setWebKeys(webkeys);
        configurationService.merge(conf);
        return Response.ok().build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { WRITE_ACCESS })
    public Response patch(String requestString) throws JsonPatchException, IOException {
        final Conf conf = configurationService.findWebKeysOnly();
        WebKeysConfiguration webKeys = conf.getWebKeys();
        webKeys = Jackson.applyPatch(requestString, webKeys);
        conf.setWebKeys(webKeys);
        configurationService.merge(conf);
        return Response.ok(webKeys.toString()).build();
    }
}
