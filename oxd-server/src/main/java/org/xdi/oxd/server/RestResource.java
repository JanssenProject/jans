package org.xdi.oxd.server;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.node.POJONode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.*;
import org.xdi.oxd.common.params.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/")
public class RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(RestResource.class);

    public RestResource() {
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
        Command commmand = new Command(CommandType.SETUP_CLIENT, read(params, SetupClientParams.class));
        ServerLauncher.getInjector().getInstance(Processor.class).process(commmand);
        return process(CommandType.SETUP_CLIENT, params, SetupClientParams.class, null);
    }

    @POST
    @Path("/get-client-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getClientToken(String params) {
        return process(CommandType.GET_CLIENT_TOKEN, params, GetClientTokenParams.class, null);
    }

    @POST
    @Path("/introspect-access-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String introspectAccessToken(String params) {
        return process(CommandType.INTROSPECT_ACCESS_TOKEN, params, IntrospectAccessTokenParams.class, null);
    }

    @POST
    @Path("/introspect-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String introspectRpt(String params) {
        return process(CommandType.INTROSPECT_RPT, params, IntrospectRptParams.class, null);
    }

    @POST
    @Path("/register-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String registerSite(@HeaderParam("Authorization") String authorization, String params) {
        return process(CommandType.REGISTER_SITE, params, RegisterSiteParams.class, authorization);
    }

    @POST
    @Path("/update-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String updateSite(@HeaderParam("Authorization") String authorization, String params) {
        return process(CommandType.UPDATE_SITE, params, UpdateSiteParams.class, authorization);
    }

    @POST
    @Path("/remove-site")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String removeSite(@HeaderParam("Authorization") String authorization, String params) {
        return process(CommandType.REMOVE_SITE, params, RemoveSiteParams.class, authorization);
    }

    @POST
    @Path("/get-authorization-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getAuthorizationUrl(@HeaderParam("Authorization") String authorization, String params) {
        return process(CommandType.GET_AUTHORIZATION_URL, params, GetAuthorizationUrlParams.class, authorization);
    }

    @POST
    @Path("/get-tokens-by-code")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getTokenByCode(@HeaderParam("Authorization") String authorization, String params) {
        return process(CommandType.GET_TOKENS_BY_CODE, params, GetTokensByCodeParams.class, authorization);
    }

    @POST
    @Path("/get-user-info")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getUserInfo(@HeaderParam("Authorization") String authorization, String params) {
        return process(CommandType.GET_USER_INFO, params, GetUserInfoParams.class, authorization);
    }

    @POST
    @Path("/get-logout-uri")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getLogoutUri(@HeaderParam("Authorization") String authorization, String params) {
        return process(CommandType.GET_LOGOUT_URI, params, GetLogoutUrlParams.class, authorization);
    }

    @POST
    @Path("/get-access-token-by-refresh-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getAccessTokenByRefreshToken(String params) {
        return process(CommandType.GET_ACCESS_TOKEN_BY_REFRESH_TOKEN, params, GetAccessTokenByRefreshTokenParams.class, null);
    }

    @POST
    @Path("/uma-rs-protect")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRsProtect(@HeaderParam("Authorization") String authorization, String params) {
        return process(CommandType.RS_PROTECT, params, RsProtectParams.class, authorization);
    }

    @POST
    @Path("/uma-rs-check-access")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRsCheckAccess(@HeaderParam("Authorization") String authorization, String params) {
        return process(CommandType.RS_CHECK_ACCESS, params, RsCheckAccessParams.class, authorization);
    }

    @POST
    @Path("/uma-rp-get-rpt")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRpGetRpt(@HeaderParam("Authorization") String authorization, String params) {
        return process(CommandType.RP_GET_RPT, params, RpGetRptParams.class, authorization);
    }

    @POST
    @Path("/uma-rp-get-claims-gathering-url")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String umaRpGetClaimsGatheringUrl(@HeaderParam("Authorization") String authorization, String params) {
        return process(CommandType.RP_GET_CLAIMS_GATHERING_URL, params, RpGetClaimsGatheringUrlParams.class, authorization);
    }

    public static <T> T read(String params, Class<T> clazz) {
        try {
            return CoreUtils.createJsonMapper().readValue(params, clazz);
        } catch (IOException e) {
            LOG.error("Invalid params: " + params, e);
            throw new ServerErrorException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid parameters. Message: " + e.getMessage()).build());
        }
    }

    public static <T extends IParams> String process(CommandType commandType, String paramsAsString, Class<T> paramsClass, String authorization) {
        T params = read(paramsAsString, paramsClass);
        if (params instanceof HasProtectionAccessTokenParams) {
            ((HasProtectionAccessTokenParams) params).setProtectionAccessToken(validateAccessToken(authorization));
        }
        Command command = new Command(commandType, params);
        final String json = CoreUtils.asJsonSilently(ServerLauncher.getInjector().getInstance(Processor.class).process(command));
        LOG.trace("Send back response: {}", json);
        return json;
    }

    public static String validateAccessToken(String authorization) {
        final String prefix = "Bearer ";
        if (StringUtils.isNotEmpty(authorization) && authorization.startsWith(prefix)) {
            String accessToken = authorization.substring(prefix.length());
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
}
