package org.gluu.oxd;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.node.POJONode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.client.CommandClient;
import org.xdi.oxd.client.CommandClientPool;
import org.xdi.oxd.common.*;
import org.xdi.oxd.common.params.*;
import org.xdi.oxd.rs.protect.Jackson;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/")
public class RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(RestResource.class);

    private final CommandClientPool pool;

    public RestResource(OxdHttpsConfiguration configuration) {
        this.pool = new CommandClientPool(configuration.getOxdConnectionExpirationInSeconds(), configuration.getOxdHost(), Integer.parseInt(configuration.getOxdPort()));
    }

    @GET
    @Path("/health-check")
    @Produces(MediaType.APPLICATION_JSON)
    public String healthCheck() {
        return "running";
    }

    @POST
    @Path("/setup-client")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String setupClient(String params) {
        return response(send(CommandType.SETUP_CLIENT, read(params, SetupClientParams.class)));
    }

    @POST
    @Path("/get-client-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getClientToken(String params) {
        return response(send(CommandType.GET_CLIENT_TOKEN, read(params, GetClientTokenParams.class)));
    }

    @POST
    @Path("/introspect-access-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String introspectAccessToken(String params) {
        IntrospectAccessTokenParams p = read(params, IntrospectAccessTokenParams.class);
        return response(send(CommandType.INTROSPECT_ACCESS_TOKEN, p));
    }

    @POST
    @Path("/introspect-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String introspectRpt(String params) {
        IntrospectRptParams p = read(params, IntrospectRptParams.class);
        return response(send(CommandType.INTROSPECT_RPT, p));
    }

    @POST
    @Path("/register-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String registerSite(@HeaderParam("Authorization") String authorization, String params) {
        RegisterSiteParams p = read(params, RegisterSiteParams.class);
        p.setProtectionAccessToken(validateAccessToken(authorization));
        return response(send(CommandType.REGISTER_SITE, p));
    }

    @POST
    @Path("/update-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String updateSite(@HeaderParam("Authorization") String authorization, String params) {
        UpdateSiteParams p = read(params, UpdateSiteParams.class);
        p.setProtectionAccessToken(validateAccessToken(authorization));
        return response(send(CommandType.UPDATE_SITE, p));
    }

    @POST
    @Path("/remove-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String removeSite(@HeaderParam("Authorization") String authorization, String params) {
        RemoveSiteParams p = read(params, RemoveSiteParams.class);
        p.setProtectionAccessToken(validateAccessToken(authorization));
        return response(send(CommandType.REMOVE_SITE, p));
    }

    @POST
    @Path("/get-authorization-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getAuthorizationUrl(@HeaderParam("Authorization") String p_authorization, String params) {
        GetAuthorizationUrlParams p = read(params, GetAuthorizationUrlParams.class);
        p.setProtectionAccessToken(validateAccessToken(p_authorization));
        return response(send(CommandType.GET_AUTHORIZATION_URL, p));
    }

    @POST
    @Path("/get-tokens-by-code")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getTokenByCode(@HeaderParam("Authorization") String p_authorization, String params) {
        GetTokensByCodeParams p = read(params, GetTokensByCodeParams.class);
        p.setProtectionAccessToken(validateAccessToken(p_authorization));
        return response(send(CommandType.GET_TOKENS_BY_CODE, p));
    }

    @POST
    @Path("/get-user-info")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getUserInfo(@HeaderParam("Authorization") String p_authorization, String params) {
        GetUserInfoParams p = read(params, GetUserInfoParams.class);
        p.setProtectionAccessToken(validateAccessToken(p_authorization));
        return response(send(CommandType.GET_USER_INFO, p));
    }

    @POST
    @Path("/get-logout-uri")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getLogoutUri(@HeaderParam("Authorization") String p_authorization, String params) {
        GetLogoutUrlParams p = read(params, GetLogoutUrlParams.class);
        p.setProtectionAccessToken(validateAccessToken(p_authorization));
        return response(send(CommandType.GET_LOGOUT_URI, p));
    }

    @POST
    @Path("/get-access-token-by-refresh-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getAccessTokenByRefreshToken(String params) {
        return response(send(CommandType.GET_ACCESS_TOKEN_BY_REFRESH_TOKEN, read(params, GetAccessTokenByRefreshTokenParams.class)));
    }

    @POST
    @Path("/uma-rs-protect")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRsProtect(@HeaderParam("Authorization") String p_authorization, String params) {
        RsProtectParams p = read(params, RsProtectParams.class);
        p.setProtectionAccessToken(validateAccessToken(p_authorization));
        return response(send(CommandType.RS_PROTECT, p));
    }

    @POST
    @Path("/uma-rs-check-access")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRsCheckAccess(@HeaderParam("Authorization") String p_authorization, String params) {
        RsCheckAccessParams p = read(params, RsCheckAccessParams.class);
        p.setProtectionAccessToken(validateAccessToken(p_authorization));
        return response(send(CommandType.RS_CHECK_ACCESS, p));
    }

    @POST
    @Path("/uma-rp-get-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRpGetRpt(@HeaderParam("Authorization") String p_authorization, String params) {
        RpGetRptParams p = read(params, RpGetRptParams.class);
        p.setProtectionAccessToken(validateAccessToken(p_authorization));
        return response(send(CommandType.RP_GET_RPT, p));
    }

    @POST
    @Path("/uma-rp-get-claims-gathering-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRpGetClaimsGatheringUrl(@HeaderParam("Authorization") String authorization, String params) {
        RpGetClaimsGatheringUrlParams p = read(params, RpGetClaimsGatheringUrlParams.class);
        p.setProtectionAccessToken(validateAccessToken(authorization));
        return response(send(CommandType.RP_GET_CLAIMS_GATHERING_URL, p));
    }

    public static <T> T read(String params, Class<T> clazz) {
        try {
            return Jackson.createJsonMapper().readValue(params, clazz);
        } catch (IOException e) {
            LOG.error("Invalid params: " + params, e);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid parameters. Message: " + e.getMessage()).build());
        }
    }

    public static String response(CommandResponse commandResponse) {
        if (commandResponse == null) {
            LOG.error("Command response is null, please check oxd-server.log file of oxd-server application.");
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Command response is null, please check oxd-server.log file of oxd-server application.").build());
        }
        final String json = CoreUtils.asJsonSilently(commandResponse);
        LOG.trace("Send back response: {}", json);
        return json;
    }

    public static String validateAccessToken(String authorizationParameter) {
        final String prefix = "Bearer ";
        if (StringUtils.isNotEmpty(authorizationParameter) && authorizationParameter.startsWith(prefix)) {
            String accessToken = authorizationParameter.substring(prefix.length());
            if (StringUtils.isNotBlank(accessToken)) {
                return accessToken;
            }
        }
        LOG.debug("No access token provided in Authorization header. Forbidden.");
        throw new WebApplicationException(forbiddenErrorResponse(), Response.Status.FORBIDDEN);
    }

    public static String forbiddenErrorResponse() {
        final ErrorResponse error = new ErrorResponse("403");
        error.setErrorDescription("Forbidden Access");

        CommandResponse commandResponse = CommandResponse.error().setData(new POJONode(error));
        return CoreUtils.asJsonSilently(commandResponse);
    }


    public CommandResponse send(CommandType commandType, IParams params) {
        CommandClient client = checkOut();
        try {
            LOG.trace("Command " + commandType + " executed by client: " + client.getNameForLogger());
            CommandResponse response = client.send(new Command(commandType).setParamsObject(params));
            if (response != null) {
                pool.checkIn(client);
            } else {
                pool.expire(client);
            }
            return response;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            pool.expire(client);
            return null;
        }
    }

    private CommandClient checkOut() {
        CommandClient client = pool.checkOut();
        if (client == null) {
            LOG.error("Failed to initialize command client.");
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Command client is not able to connect to oxd-server.").build());
        }
        return client;
    }
}
