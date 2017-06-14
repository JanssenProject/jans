package org.xdi.oxd.client;

import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 05/05/2017
 */

public interface HttpClientInterface {

    @POST
    @Path("/setup-client")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public CommandResponse setupClient(SetupClientParams params);

    @POST
    @Path("/register-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public CommandResponse registerSite(RegisterSiteParams params);

    @POST
    @Path("/update-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public CommandResponse updateSite(UpdateSiteParams params);

    @POST
    @Path("/get-authorization-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public CommandResponse getAuthrizationUrl(GetAuthorizationUrlParams params);

    @POST
    @Path("/get-tokens-by-code")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public CommandResponse getTokenByCode(GetTokensByCodeParams params);

    @POST
    @Path("/get-user-info")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public CommandResponse getUserInfo(GetUserInfoParams params);

    @POST
    @Path("/get-client-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public CommandResponse getClientToken(GetClientTokenParams params);

    @POST
    @Path("/logout")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public CommandResponse getLogoutUrl(GetLogoutUrlParams params);
}
